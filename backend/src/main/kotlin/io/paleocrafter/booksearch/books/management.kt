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
import jdk.internal.dynalink.support.ClassMap
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.find
import kotlinx.coroutines.channels.mapNotNull
import kotlinx.coroutines.launch
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
import java.io.File
import java.nio.charset.Charset
import java.util.UUID
import javax.sql.rowset.serial.SerialBlob

private val TOCReference.id: String
    get() = "$fragmentId.$resourceId"

fun Route.bookManagement(index: BookIndex) {
    get("/{id}") {
        val id = UUID.fromString(call.parameters["id"])
        val book = transaction {
            Book.findById(id) ?: return@transaction null
        } ?: return@get call.respond(
            HttpStatusCode.NotFound,
            mapOf("message" to "Book with ID '$id' does not exist")
        )
        call.respond(
            mapOf(
                "id" to id,
                "title" to book.title,
                "author" to book.author,
                "series" to book.series,
                "orderInSeries" to book.orderInSeries
            )
        )
    }

    delete("/{id}") {
        val id = UUID.fromString(call.parameters["id"])
        val book = transaction {
            Book.findById(id) ?: return@transaction null
        } ?: return@delete call.respond(
            HttpStatusCode.NotFound,
            mapOf("message" to "Book with ID '$id' does not exist")
        )

        index.delete(id)

        val title = book.title

        transaction {
            Images.deleteWhere { Images.book eq book.id }
            Chapters.deleteWhere { Chapters.book eq book.id }
            ClassMappings.deleteWhere { ClassMappings.book eq book.id }
            book.delete()
        }

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
        val bookAuthor = epub.metadata.authors.first()
        val authorName = "${bookAuthor.firstname} ${bookAuthor.lastname}"
        transaction {
            Book.new(bookId) {
                content = SerialBlob(buffer)
                title = epub.title
                author = authorName
            }
        }
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

        fun serializeTableOfContents(toc: TOCReference): Map<String, Any> {
            return mapOf(
                "id" to toc.id,
                "title" to toc.title,
                "selected" to (existingChapters.contains(toc.id) || existingChapters.isEmpty()),
                "children" to toc.children.map { serializeTableOfContents(it) }
            )
        }

        call.respond(
            mapOf(
                "id" to id,
                "toc" to epub.tableOfContents.tocReferences.map { serializeTableOfContents(it) }
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

            fun linearizeTableOfContents(toc: TOCReference): List<Pair<String, TOCReference>> {
                return listOf(toc.id to toc) + toc.children.flatMap { linearizeTableOfContents(it) }
            }

            val epub = epubReader.readEpub(book.content.binaryStream)
            val chapters = epub.tableOfContents.tocReferences
                .flatMap { linearizeTableOfContents(it) }
                .filter { it.first in entries }
                .map { Pair(it.first, it.second.title) to Jsoup.parse(String(it.second.resource.data)) }

            for ((position, chapter) in chapters.withIndex()) {
                val (metadata, content) = chapter
                content.extractImages(id, epub.resources) { name, data ->
                    Images.insertIgnore {
                        it[Images.book] = book.id
                        it[Images.name] = name
                        it[Images.data] = SerialBlob(data)
                    }
                }

                Chapter.new(UUID.randomUUID()) {
                    this.book = book
                    tocReference = metadata.first
                    this.title = metadata.second
                    this.content = content.outerHtml()
                    this.position = position
                }
            }
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
        } ?: return@put call.respond(
            HttpStatusCode.NotFound,
            mapOf("message" to "Book with ID '$id' does not exist")
        )

        call.respond(mapOf(
            "id" to id
        ))
    }

    put("/{id}/index") {
        val id = UUID.fromString(call.parameters["id"])
        val book = transaction {Book.findById(id) ?: return@transaction null } ?: return@put call.respond(
            HttpStatusCode.NotFound,
            mapOf("message" to "Book with ID '$id' does not exist")
        )
        val normalized = transaction {
            val classMappings = ClassMappings.select { ClassMappings.book eq book.id }.associate {
                it[ClassMappings.className] to BookStyle.valueOf(it[ClassMappings.mapping])
            }
            Chapter.find { Chapters.book eq book.id }.map {
                ResolvedChapter(it.id.value, id, it.title, Jsoup.parse(it.content).body()).also { resolved ->
                    BookNormalizer.normalize(resolved, classMappings)
                    it.indexedContent = resolved.content.html()
                }
            }
        }

        index.index(id, normalized)

        transaction {
            book.searchable = true
        }

        call.respond(mapOf(
            "id" to id,
            "message" to "Book '${book.title}' was successfully indexed and is now searchable!"
        ))
    }

    post("/reindex-all") {
        val books = transaction {
            Book.all().associate { book ->
                book.id.value to transaction {
                    val classMappings = ClassMappings.select { ClassMappings.book eq book.id }.associate {
                        it[ClassMappings.className] to BookStyle.valueOf(it[ClassMappings.mapping])
                    }
                    Chapter.find { Chapters.book eq book.id }.map {
                        ResolvedChapter(it.id.value, book.id.value, it.title, Jsoup.parse(it.content).body()).also { resolved ->
                            BookNormalizer.normalize(resolved, classMappings)
                            it.indexedContent = resolved.content.html()
                        }
                    }
                }
            }
        }

        index.reset()

        for ((id, normalized) in books) {
            index.index(id, normalized)
        }

        transaction {
            books.forEach { Book.findById(it.key)?.searchable = it.value.isNotEmpty() }
        }

        call.respond(mapOf(
            "message" to "All books were successfully re-indexed!"
        ))
    }
}

private data class BookPatchRequest(val title: String, val author: String, val series: String?, val orderInSeries: Int)

private inline fun Document.extractImages(bookId: UUID, resources: Resources, imageHandler: (name: String, data: ByteArray) -> Unit) {
    val images = this.select("img")
    for (img in images) {
        val src = img.attr("src")
        val data = resources.getByHref(src).data
        imageHandler(src, data)
        img.attr("src", "/api/books/$bookId/images/$src")
    }
}

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
