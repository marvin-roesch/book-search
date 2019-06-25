package io.paleocrafter.booksearch.books

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.fromFilePath
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Route.bookReading() {
    data class Series(val name: String, val books: MutableList<Book>, val children: MutableMap<String, Series>) {
        fun toJson(): Map<String, Any> =
            mapOf(
                "name" to name,
                "books" to books.sortedBy { it.orderInSeries }.map { it.toJson() },
                "children" to children.map { it.value.toJson() }
            )
    }

    get("/series") {
        call.respond(
            transaction {
                val series = mutableMapOf<String, Series>()
                for (book in Book.all()) {
                    val seriesHierarchy = book.series?.split("\\") ?: listOf("No Series")

                    var destinationSeries: Series? = null
                    var seriesMap = series
                    for (seriesName in seriesHierarchy) {
                        val s = seriesMap.computeIfAbsent(seriesName) { Series(seriesName, mutableListOf(), mutableMapOf()) }
                        destinationSeries = s
                        seriesMap = s.children
                    }

                    destinationSeries?.books?.add(book)
                }
                series.map { it.value.toJson() }
            }
        )
    }

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

    get("/{id}/chapters") {
        val id = UUID.fromString(call.parameters["id"])
        val chapters = transaction {
            val book = Book.findById(id) ?: return@transaction null

            Chapter.find { Chapters.book eq book.id }.sortedBy { it.position }.map { it.toJson() }
        } ?: return@get call.respond(
            HttpStatusCode.NotFound,
            mapOf("message" to "Book with ID '$id' does not exist")
        )

        call.respond(chapters)
    }

    get("/{id}/images/{name}") {
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

    get("/{id}/cover") {
        val id = UUID.fromString(call.parameters["id"])
        val (imageType, imageData) = transaction {
            val book = Book.findById(id) ?: return@transaction null
            val cover = book.cover
            val mime = book.coverMime
            if (cover == null || mime == null) {
                return@transaction null
            }
            val type = ContentType.parse(mime)

            type to cover.binaryStream.readBytes()
        } ?: return@get call.respond(
            HttpStatusCode.NotFound,
            mapOf("message" to "Book with ID '$id' does not exist or does not have a cover")
        )

        call.respond(ByteArrayContent(imageData, imageType))
    }
}
