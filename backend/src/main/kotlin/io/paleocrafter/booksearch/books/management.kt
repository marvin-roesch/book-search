package io.paleocrafter.booksearch.books

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.request.receive
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.patch
import io.ktor.routing.post
import io.ktor.routing.put
import io.paleocrafter.booksearch.auth.user
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.find
import kotlinx.coroutines.channels.mapNotNull
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import nl.siegmann.epublib.domain.Resources
import nl.siegmann.epublib.domain.TOCReference
import nl.siegmann.epublib.epub.EpubReader
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.file.Paths
import java.util.UUID
import javax.sql.rowset.serial.SerialBlob

private val logger = LoggerFactory.getLogger("BookManagement")

private fun TOCReference.buildId(parent: String, index: Int): String = "$parent.$index-$title"

val indexing = newFixedThreadPoolContext(4, "indexing")

fun Route.bookManagement(index: BookIndex) {
    delete("/{id}") {
        val id = UUID.fromString(call.parameters["id"])
        val book = transaction {
            Book.findById(id) ?: return@transaction null
        } ?: return@delete call.respond(
            HttpStatusCode.NotFound,
            mapOf("message" to "Book with ID '$id' does not exist")
        )

        index.prepare()
        index.deleteBook(id)

        val title = book.title

        transaction {
            Images.deleteWhere { Images.book eq book.id }
            Chapters.deleteWhere { Chapters.book eq book.id }
            ClassMappings.deleteWhere { ClassMappings.book eq book.id }
            book.delete()
        }

        BookCache.removeBook(id)

        call.respond(
            mapOf(
                "message" to "Book '$title' was successfully deleted!"
            )
        )
    }

    put("/") {
        val multipart = call.receiveMultipart()
        val channel = Channel<PartData>()
        launch {
            multipart.forEachPart { channel.send(it) }
            channel.close()
        }
        val file = channel
            .mapNotNull { it as? PartData.FileItem }
            .find { it.name == "book" && File(it.originalFileName).extension == "epub" }
            ?: return@put call.respond(
                HttpStatusCode.BadRequest,
                mapOf("message" to "Must provide epub file")
            )
        val buffer = file.streamProvider().use { it.readBytes() }
        val epubReader = EpubReader()
        val epub = try {
            epubReader.readEpub(buffer.inputStream())
        } catch (exception: Exception) {
            return@put call.respond(
                HttpStatusCode.BadRequest,
                mapOf("message" to "Must provide valid epub file")
            )
        }
        val bookId = UUID.randomUUID()
        val authorName = epub.metadata.authors.firstOrNull()?.let { "${it.firstname} ${it.lastname}" } ?: ""
        transaction {
            val book = Book.new(bookId) {
                content = SerialBlob(buffer)
                title = epub.title
                author = authorName
                epub.coverImage?.let {
                    cover = SerialBlob(it.data)
                    coverMime = it.mediaType.name
                }
            }
            BookCache.updateBook(bookId, book.resolved)
        }

        logger.info("New book '${epub.title}' uploaded by ${call.user?.username}")

        call.respond(mapOf("id" to bookId))
    }

    patch("/{id}") {
        val id = UUID.fromString(call.parameters["id"])
        val request = call.receive<BookPatchRequest>()
        transaction {
            val book = Book.findById(id) ?: return@transaction null
            book.title = request.title
            book.author = request.author
            book.series = request.series
            book.orderInSeries = request.orderInSeries

            BookCache.updateBook(id, book.resolved)
        } ?: return@patch call.respond(
            HttpStatusCode.NotFound,
            mapOf("message" to "Book with ID '$id' does not exist")
        )
        call.respond(
            mapOf("message" to "Book information was successfully updated")
        )
    }

    get("/{id}/table-of-contents") {
        val id = UUID.fromString(call.parameters["id"])
        val epubReader = EpubReader()
        val (epub, existingChapters) = transaction {
            val book = Book.findById(id) ?: return@transaction null

            epubReader.readEpub(book.content.binaryStream) to Chapter.find { Chapters.book eq book.id }.map { it.tocReference }
        } ?: return@get call.respond(
            HttpStatusCode.NotFound,
            mapOf("message" to "Book with ID '$id' does not exist")
        )

        fun serializeTableOfContents(id: String, toc: TOCReference): Map<String, Any> {
            return mapOf(
                "id" to id,
                "title" to toc.title,
                "selected" to (existingChapters.contains(id) || existingChapters.isEmpty()),
                "children" to toc.children.mapIndexed { index, tocReference ->
                    serializeTableOfContents(tocReference.buildId(id, index), tocReference)
                }
            )
        }

        call.respond(
            mapOf(
                "id" to id,
                "toc" to epub.tableOfContents.tocReferences.mapIndexed { index, tocReference ->
                    serializeTableOfContents(tocReference.buildId("", index), tocReference)
                }
            )
        )
    }

    put("/{id}/chapters") {
        val id = UUID.fromString(call.parameters["id"])
        val entries = call.receive<Set<String>>()
        val epubReader = EpubReader()
        transaction {
            val book = Book.findById(id) ?: return@transaction null

            Chapters.deleteWhere { Chapters.book eq id }
            Images.deleteWhere { Images.book eq id }
            book.searchable = false

            fun linearizeTableOfContents(id: String, toc: TOCReference): List<Pair<String, TOCReference>> {
                return listOf(id to toc) + toc.children.withIndex().flatMap { (index, tocReference) ->
                    linearizeTableOfContents(tocReference.buildId(id, index), tocReference)
                }
            }

            val epub = epubReader.readEpub(book.content.binaryStream)
            val chapters = epub.tableOfContents.tocReferences
                .withIndex()
                .flatMap { (index, tocReference) -> linearizeTableOfContents(tocReference.buildId("", index), tocReference) }
                .filter { it.first in entries }
                .splitOffFragments()

            for ((position, entry) in chapters.withIndex()) {
                val (tocId, chapter, content) = entry

                content.extractImages(id) { path ->
                    val uri = chapter.resource.href.resolveHref(path)

                    if (uri.isAbsolute) {
                        return@extractImages null
                    }

                    val name = Paths.get(uri.path).fileName.toString().urlDecoded
                    val resourcePath = uri.path.urlDecoded
                    val data = epub.resources.getByHref(resourcePath).data
                    Images.insertIgnore {
                        it[Images.book] = book.id
                        it[Images.name] = name
                        it[Images.data] = SerialBlob(data)
                    }
                    name
                }

                content.resolveStylesheets {
                    val uri = chapter.resource.href.resolveHref(it)

                    if (uri.isAbsolute) {
                        return@resolveStylesheets null
                    }

                    uri.path.urlDecoded
                }

                Chapter.new(UUID.randomUUID()) {
                    this.book = book
                    this.tocReference = tocId
                    this.title = chapter.title
                    this.content = content.outerHtml()
                    this.position = position
                }
            }

            BookCache.updateBook(id, book.resolved)
        } ?: return@put call.respond(
            HttpStatusCode.NotFound,
            mapOf("message" to "Book with ID '$id' does not exist")
        )

        call.respond(mapOf(
            "id" to id,
            "message" to "Table of contents for book was successfully updated"
        ))
    }

    get("/{id}/available-classes") {
        val id = UUID.fromString(call.parameters["id"])
        val epubReader = EpubReader()
        val classes = transaction {
            val book = Book.findById(id) ?: return@transaction null

            val epub = epubReader.readEpub(book.content.binaryStream)
            val chapters = Chapter.find { Chapters.book eq book.id }
                .map { Jsoup.parse(it.content) }

            val existingMappings = ClassMappings.select { ClassMappings.book eq book.id }.associate {
                it[ClassMappings.className] to BookStyle.valueOf(it[ClassMappings.mapping]).id
            }

            chapters
                .flatMap { chapter ->
                    chapter.extractClasses(epub.resources)
                }
                .groupBy { it.name }
                .map { it.value.first().copy(mapping = existingMappings[it.key] ?: "no-selection", occurrences = it.value.size) }
        } ?: return@get call.respond(
            HttpStatusCode.NotFound,
            mapOf("message" to "Book with ID '$id' does not exist")
        )

        call.respond(mapOf(
            "id" to id,
            "classes" to classes,
            "mappings" to BookStyle.values().groupBy { it.group }
        ))
    }

    put("/{id}/class-mappings") {
        val id = UUID.fromString(call.parameters["id"])
        val classMappings = call.receive<Map<String, String>>().mapValues {
            BookStyle.fromJson(it.value)
                ?: BookStyle.STRIP_CLASS
        }

        transaction {
            val book = Book.findById(id) ?: return@transaction null

            ClassMappings.deleteWhere { ClassMappings.book eq book.id }

            for ((className, style) in classMappings) {
                ClassMappings.insert {
                    it[ClassMappings.book] = book.id
                    it[ClassMappings.className] = className
                    it[mapping] = style.name
                }
            }

            book.searchable = false

            BookCache.updateBook(id, book.resolved)
        } ?: return@put call.respond(
            HttpStatusCode.NotFound,
            mapOf("message" to "Book with ID '$id' does not exist")
        )

        call.respond(mapOf(
            "id" to id
        ))
    }

    suspend fun index(book: Book, user: String?) {
        val id = transaction {
            book.indexing = true
            book.id.value
        }

        val normalized = transaction {
            val classMappings = ClassMappings.select { ClassMappings.book eq book.id }.associate {
                it[ClassMappings.className] to BookStyle.valueOf(it[ClassMappings.mapping])
            }
            Chapter.find { Chapters.book eq book.id }.map {
                ResolvedChapter(it.id.value, id, it.title, it.position, Jsoup.parse(it.content).body()).also { resolved ->
                    BookNormalizer.normalize(resolved, classMappings)
                    it.indexedContent = resolved.content.html()
                }
            }
        }

        try {
            index.index(id, normalized)
        } catch (e: Exception) {
            logger.error("Could not finish indexing '${book.title}'", e)
            transaction {
                book.indexing = false
            }
            return
        }

        transaction {
            book.searchable = true
            book.indexing = false
        }

        logger.info("Book '${book.title}' indexed by $user")
    }

    put("/{id}/index") {
        val id = UUID.fromString(call.parameters["id"])
        val book = transaction { Book.findById(id) ?: return@transaction null } ?: return@put call.respond(
            HttpStatusCode.NotFound,
            mapOf("message" to "Book with ID '$id' does not exist")
        )

        index.prepare()

        // Only index when necessary
        if (!transaction { book.indexing }) {
            transaction {
                book.indexing = true
                BookCache.updateBook(id, book.resolved)
            }
            GlobalScope.launch(indexing) {
                index(book, call.user?.username)
                transaction {
                    BookCache.updateBook(id, book.resolved)
                }
            }
        }

        call.respond(mapOf(
            "id" to id,
            "message" to "Indexing of '${book.title}' was successfully started and will soon be searchable!"
        ))
    }

    post("/reindex-all") {
        index.reset()
        index.prepare()

        val books = transaction {
            Book.all().filter { !it.indexing }.also {
                it.forEach { b ->
                    b.indexing = true
                    BookCache.updateBook(b.id.value, b.resolved)
                }
            }
        }

        GlobalScope.launch {
            coroutineScope {
                for (book in books) {
                    launch(indexing) {
                        index(book, call.user?.username)
                    }
                }
            }

            BookCache.rebuild()

            logger.info("All books reindexed")
        }

        call.respond(mapOf(
            "message" to "Re-indexing for all books was started!"
        ))
    }
}

private data class BookPatchRequest(val title: String, val author: String, val series: String?, val orderInSeries: Int)

private fun List<Pair<String, TOCReference>>.splitOffFragments(): List<SplitChapter> {
    val result = mutableListOf<SplitChapter>()

    for (i in 0 until this.size) {
        val (tocId, tocReference) = this[i]
        val content = Jsoup.parse(String(tocReference.resource.data))

        val startFragment = tocReference.fragmentId

        if (!startFragment.isNullOrEmpty()) {
            val fragmentElement = content.getElementById(startFragment)
            val parentParagraphs = fragmentElement.parents().filter { it.`is`("p") }
            val reference = if (parentParagraphs.isEmpty() || fragmentElement.`is`("p")) {
                fragmentElement
            } else {
                parentParagraphs.last()
            }
            reference.siblingNodes().take(reference.siblingIndex()).forEach { it.remove() }
        }

        val next = this.getOrNull(i + 1)?.second
        val endFragment = if (next?.resource?.href == tocReference.resource.href) next?.fragmentId else null

        if (!endFragment.isNullOrEmpty()) {
            val fragmentElement = content.getElementById(endFragment)
            val parentParagraphs = fragmentElement.parents().filter { it.`is`("p") }
            val reference = if (parentParagraphs.isEmpty() || fragmentElement.`is`("p")) {
                fragmentElement
            } else {
                parentParagraphs.last()
            }
            reference.siblingNodes().drop(reference.siblingIndex()).forEach { it.remove() }
            reference.remove()
        }

        result.add(SplitChapter(tocId, tocReference, content))
    }

    return result
}

private data class SplitChapter(val tocId: String, val tocReference: TOCReference, val content: Document)

private inline fun Document.extractImages(bookId: UUID, imageHandler: (path: String) -> String?) {
    val images = this.select("img")
    for (img in images) {
        val src = img.attr("src")
        val newSrc = imageHandler(src)
        if (newSrc != null) {
            img.attr("src", "/api/books/$bookId/images/$newSrc")
        }
    }
}

private inline fun Document.resolveStylesheets(pathHandler: (path: String) -> String?) {
    val styles = this.select("link[rel='stylesheet']")
    for (style in styles) {
        val href = style.attr("href")
        val newHref = pathHandler(href)
        if (newHref != null) {
            style.attr("href", newHref)
        }
    }
}

private fun String.resolveHref(path: String) = URI(this.urlEncoded).resolve(path.urlEncoded)

private val String.urlEncoded: String
    get() = this.split('/').joinToString("/") { URLEncoder.encode(it, Charsets.UTF_8.name()) }

private val String.urlDecoded: String
    get() = this.split('/').joinToString("/") { URLDecoder.decode(it, Charsets.UTF_8.name()) }

private fun Document.extractClasses(resources: Resources): List<HtmlClass> {
    val styles = this.select("link[rel='stylesheet']")
        .map { it.attr("href") }
        .mapNotNull { resources.getByIdOrHref(it) }
        .joinToString("\n") { String(it.data, Charset.forName(it.inputEncoding)) }

    return this.body().allElements
        .filter { it.`is`("p") || it.parents().any { p -> p.`is`("p") } }
        .flatMap { el ->
            el.classNames().map { HtmlClass(it, el.outerHtml(), styles) }
        }
}

private data class HtmlClass(val name: String, val sample: String, val styles: String, val mapping: String? = null, val occurrences: Int = 0)
