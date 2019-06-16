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
import io.ktor.http.fromFilePath
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
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
        SchemaUtils.createMissingTablesAndColumns(Books, Chapters, Images, ClassMappings)
    }

    routing {
        route("api") {
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

            bookManagement()

            bookSearch()
        }
    }
}
