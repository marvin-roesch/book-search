package io.paleocrafter.booksearch

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jsoup.Jsoup
import java.util.UUID

fun Route.bookSearch() {
    val index = BookIndex()

    put("/book/{id}/index") {
        val id = UUID.fromString(call.parameters["id"])
        val normalized = transaction {
            val book = Book.findById(id) ?: return@transaction null
            val classMappings = ClassMappings.select { ClassMappings.book eq book.id }.associate {
                it[ClassMappings.className] to BookStyle.valueOf(it[ClassMappings.mapping])
            }
            Chapter.find { Chapters.book eq book.id }.map {
                ResolvedChapter(id, it.title, Jsoup.parse(it.content).body()).also { resolved ->
                    BookNormalizer.normalize(resolved, classMappings)
                    it.indexedContent = resolved.content.html()
                }
            }
        } ?: return@put call.respond(
            HttpStatusCode.NotFound,
            mapOf("message" to "Book with ID '$id' does not exist")
        )

        index.index(id, normalized)

        call.respond(mapOf(
            "id" to id,
            "message" to "success"
        ))
    }

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

    post("/search") {
        val request = call.receive<SearchRequest>()

        val filter = transaction {
            if (request.seriesFilter == null && request.bookFilter == null) {
                Book.all()
            } else {
                val adjustedFilter = request.seriesFilter.orEmpty().map { Regex("^${Regex.escape(it)}($|\\\\)") }
                Book.all().filter { (it.series ?: "No Series").let { s -> adjustedFilter.any { r -> r.containsMatchIn(s) } } }
            }.map { it.id.value }
        } + request.bookFilter.orEmpty().map { UUID.fromString(it) }

        if (filter.isEmpty()) {
            return@post call.respond(mapOf(
                "totalHits" to 0,
                "results" to emptyList<Any>()
            ))
        }

        val searchResult = index.search(request.query, request.page, filter)

        val results = transaction {
            searchResult.results.map {
                val book = Book.findById(it.bookId) ?: return@transaction null
                mapOf(
                    "book" to book.title,
                    "chapter" to it.chapter,
                    "paragraphs" to it.paragraphs
                )
            }
        } ?: return@post call.respond(
            HttpStatusCode.NotFound,
            mapOf("message" to "Book does not exist")
        )

        call.respond(mapOf(
            "totalHits" to searchResult.totalHits,
            "results" to results
        ))
    }
}

private data class SearchRequest(val query: String, val page: Int, val seriesFilter: List<String>?, val bookFilter: List<String>?)
