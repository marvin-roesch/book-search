package io.paleocrafter.booksearch.books

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.fromFilePath
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.paleocrafter.booksearch.auth.user
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.Locale
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

fun Route.bookReading() {
    get("/series") {
        call.respond(BookCache.series.mapNotNull { it.toJson(call.user.permissions) })
    }

    get("/tags") {
        call.respond(BookCache.tags)
    }

    requireBookPermissions {
        get("/{id}") {
            val id = UUID.fromString(call.parameters["id"])
            val book = BookCache.find(id) ?: return@get call.respond(
                HttpStatusCode.NotFound,
                mapOf("message" to "Book with ID '$id' does not exist")
            )
            call.respond(book.toJson() + ("tags" to book.tags))
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
}

data class Series(
    val path: Optional<String>,
    val root: Boolean,
    val name: String,
    val books: MutableList<ResolvedBook>,
    val children: ConcurrentHashMap<Optional<String>, Series>
) {
    val sortableName = name.sortable

    fun toJson(permissions: Set<String>): Map<String, Any>? {
        val filteredChildren = this.children.mapNotNull { it.value.toJson(permissions) }
        val filteredBooks = books.filter {
            !it.restricted || Book.readingPermission(it.id) in permissions
        }

        if (filteredBooks.isEmpty() && filteredChildren.isEmpty()) {
            return null
        }

        return mapOf(
            "name" to name,
            "books" to filteredBooks,
            "children" to filteredChildren
        )
    }
}


private val WHITESPACE = Regex("\\s")
private val ARTICLES = setOf("the", "a", "an")

val String.sortable: String
    get() = this.split(WHITESPACE, 2).let { words ->
        when {
            words.size <= 1 -> this
            words.first().toLowerCase(Locale.ROOT) in ARTICLES -> "${words.last()}, ${words.first()}"
            else -> this
        }
    }

private val NUMERIC_CITATION_PARAM = Regex("^(?:[A-Za-z]+ )?0*([0-9]+)(?:[.:] ?.*?)?$")
private val TEXT_CITATION_PARAM = Regex("^(?:[A-Za-z]+ )?([A-Za-z-]+)(?:[.:] ?.*?)?$")

fun extractCitationParameter(chapterTitle: String) =
    NUMERIC_CITATION_PARAM.find(chapterTitle)?.groupValues?.get(1)
        ?: TEXT_CITATION_PARAM.find(chapterTitle)?.groupValues?.get(1)?.let { parseNumber(it) }
        ?: chapterTitle.toLowerCase()

private val UNITS = listOf(
    "zero", "one", "two", "three", "four", "five", "six", "seven", "eight",
    "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen",
    "sixteen", "seventeen", "eighteen", "nineteen"
).withIndex().associate { it.value to it.index }
private val TENS = listOf("twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")
    .withIndex().associate { it.value to (it.index + 2) * 10 }

private val NUMBER_WORDS = UNITS + TENS

private fun parseNumber(text: String): String? {
    return text.toLowerCase().split("-").fold(0) { acc, number -> NUMBER_WORDS[number]?.let { acc + it } ?: return null }.toString()
}
