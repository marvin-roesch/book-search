package io.paleocrafter.booksearch

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ConditionalHeaders
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.http.fromFilePath
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.patch
import io.ktor.routing.put
import io.ktor.routing.route
import io.ktor.routing.routing
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.find
import kotlinx.coroutines.channels.mapNotNull
import kotlinx.coroutines.launch
import nl.siegmann.epublib.domain.Resources
import nl.siegmann.epublib.domain.TOCReference
import nl.siegmann.epublib.epub.EpubReader
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.nio.charset.Charset
import java.util.UUID

fun Application.main() {
    install(DefaultHeaders)
    install(CallLogging)
    install(ConditionalHeaders)
    install(Compression)
    install(StatusPages) {
        exception<NotImplementedError> { call.respond(HttpStatusCode.NotImplemented) }
    }
    install(ContentNegotiation) {
        jackson {
        }
    }

    Database.connect(
        environment.config.property("db.connection").getString(),
        environment.config.property("db.driver").getString()
    )

    transaction {
        SchemaUtils.create(Books, Chapters, Images)
    }

    val index = BookIndex()

    routing {
        route("api") {
            put("book") {
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
                val book = transaction {
                    val blob = connection.createBlob()
                    buffer.inputStream().use { input -> blob.setBinaryStream(1).use { input.copyTo(it) } }
                    Book.new(bookId) {
                        content = blob
                        title = epub.title
                        author = authorName
                    }
                }
                call.respond(
                    mapOf(
                        "id" to bookId,
                        "title" to book.title,
                        "author" to book.author
                    )
                )
            }

            patch("/book/{id}") {
                val id = UUID.fromString(call.parameters["id"])
                val request = call.receive<BookPatchRequest>()
                val epubReader = EpubReader()
                val epub = transaction {
                    val book = Book.findById(id) ?: return@transaction null
                    book.title = request.title
                    book.author = request.author
                    Books.update({ Books.id.eq(id) }) {
                        it[title] = request.title
                        it[author] = request.author
                    }

                    epubReader.readEpub(book.content.binaryStream)
                } ?: return@patch call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("message" to "Book with ID '$id' does not exist")
                )
                call.respond(
                    mapOf(
                        "id" to id,
                        "toc" to epub.tableOfContents.tocReferences.map { serializeTableOfContents(it) }
                    )
                )
            }

            put("/book/{id}/table-of-contents") {
                val id = UUID.fromString(call.parameters["id"])
                val entries = call.receive<Set<String>>()
                val epubReader = EpubReader()
                val classes = transaction {
                    val book = Book.findById(id) ?: return@transaction null

                    Chapters.deleteWhere { Chapters.book eq id }
                    Images.deleteWhere { Images.book eq id }

                    val epub = epubReader.readEpub(book.content.binaryStream)
                    val linearized = epub.tableOfContents.tocReferences
                        .flatMap { linearizeTableOfContents(it) }
                        .filter { it.first in entries }
                        .map { it.first to Jsoup.parse(String(it.second.resource.data)) }

                    val processed = linearized
                        .flatMap { ref ->
                            ref.second.processContent(id, epub.resources) { name, data ->
                                val blob = connection.createBlob()
                                data.inputStream().use { input -> blob.setBinaryStream(1).use { input.copyTo(it) } }
                                Images.insertIgnore {
                                    it[Images.book] = book.id
                                    it[Images.name] = name
                                    it[Images.data] = blob
                                }
                            }
                        }
                        .groupBy { it.name }
                        .map { it.value.first().copy(occurrences = it.value.size) }

                    for ((ref, content) in linearized) {
                        Chapter.new {
                            this.book = book
                            tocReference = ref
                            this.content = content.outerHtml()
                        }
                    }

                    processed
                } ?: return@put call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("message" to "Book with ID '$id' does not exist")
                )

                call.respond(mapOf(
                    "id" to id,
                    "classes" to classes,
                    "mappings" to BookStyle.values().groupBy { it.group }
                ))
            }

            get("/book/{id}/images/{name}") {
                val id = UUID.fromString(call.parameters["id"])
                val name = call.parameters["name"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("message" to "Image name must be provided")
                )
                val (imageType, imageData) = transaction {
                    val book = Book.findById(id) ?: return@transaction null

                    val image = Images.select { (Images.book eq book.id) and (Images.name eq name) }.firstOrNull()
                        ?: return@transaction null
                    val type = ContentType.fromFilePath(image[Images.name]).firstOrNull() ?: ContentType.Application.OctetStream

                    type to image[Images.data].binaryStream.readBytes()
                } ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("message" to "Image '$name' for book with ID '$id' does not exist")
                )

                call.respond(ByteArrayContent(imageData, imageType))
            }

            put("/book/{id}/index") {
                val id = UUID.fromString(call.parameters["id"])
                val classMappings = call.receive<Map<String, String>>().mapValues { BookStyle.fromJson(it.value) ?: BookStyle.STRIP_CLASS }
                val chapters = transaction {
                    val book = Book.findById(id) ?: return@transaction null
                    Chapter.find { Chapters.book eq book.id }.map { it.title to it.content }
                } ?: return@put call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("message" to "Book with ID '$id' does not exist")
                )

                index.index(id, chapters, classMappings)

                call.respond(mapOf(
                    "id" to id,
                    "message" to "success"
                ))
            }
        }
    }
}

data class BookPatchRequest(val title: String, val author: String)

fun serializeTableOfContents(toc: TOCReference): Map<String, Any> {
    return mapOf(
        "id" to toc.id,
        "title" to toc.title,
        "children" to toc.children.map { serializeTableOfContents(it) }
    )
}

val TOCReference.id: String
    get() = "$fragmentId.$resourceId"

fun Document.processContent(bookId: UUID, resources: Resources, imageHandler: (name: String, data: ByteArray) -> Unit): List<HtmlClass> {
    val styles = this.select("link[rel='stylesheet']")
        .map { it.attr("href") }
        .mapNotNull { resources.getByIdOrHref(it) }
        .joinToString("\n") { String(it.data, Charset.forName(it.inputEncoding)) }

    val images = this.select("img")
    for (img in images) {
        val src = img.attr("src")
        val data = resources.getByHref(src).data
        imageHandler(src, data)
        img.attr("src", "/api/book/$bookId/images/$src")
    }

    return this.body().allElements
        .filter { it.`is`("p") || it.parents().any { p -> p.`is`("p") } }
        .flatMap { el ->
            el.classNames().map { HtmlClass(it, el.outerHtml(), styles) }
        }
}

data class HtmlClass(val name: String, val sample: String, val styles: String, val occurrences: Int = 0)

fun linearizeTableOfContents(toc: TOCReference): List<Pair<String, TOCReference>> {
    return listOf(toc.id to toc) + toc.children.flatMap { linearizeTableOfContents(it) }
}
