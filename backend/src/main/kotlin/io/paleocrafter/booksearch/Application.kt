package io.paleocrafter.booksearch

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.*
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.jackson.jackson
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.put
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.channels.find
import kotlinx.coroutines.channels.mapNotNull
import kotlinx.coroutines.channels.produce
import nl.siegmann.epublib.domain.TOCReference
import nl.siegmann.epublib.epub.EpubReader
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.*

val db = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

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

    transaction {
        SchemaUtils.create(BookStubs)
    }

    routing {
        route("api") {
            put("book") {
                val multipart = call.receiveMultipart()
                val file = produce { multipart.forEachPart { send(it) } }
                    .mapNotNull { it as? PartData.FileItem }
                    .find { it.name == "book" && File(it.originalFileName).extension == "epub" }
                    ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("message" to "Must provide epub file")
                    )
                val epubReader = EpubReader()
                val book = try {
                    file.streamProvider().use { epubReader.readEpub(it) }
                } catch(exception: Exception) {
                    return@put call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("message" to "Must provide valid epub file")
                    )
                }
                val bookId = UUID.randomUUID()
                transaction(db) {
                    val blob = connection.createBlob()
                    file.streamProvider().use { input -> blob.setBinaryStream(1).use { input.copyTo(it) } }
                    BookStubs.insert {
                        it[id] = bookId
                        it[content] = blob
                    }
                }
                call.respond(mapOf("id" to bookId, "title" to book.title, "toc" to book.tableOfContents.tocReferences.map { serializeTableOfContents(it) }))
            }
        }
    }
}

fun serializeTableOfContents(toc: TOCReference): Map<String, Any> {
    return mapOf(
        "id" to "${toc.fragmentId}.${toc.resourceId}",
        "title" to toc.title,
        "children" to toc.children.map { serializeTableOfContents(it) }
    )
}

fun main() {
    embeddedServer(Netty, 3080, module = Application::main).start()
}
