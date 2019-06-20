package io.paleocrafter.booksearch.books

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Route.bookSearch(index: BookIndex) {
    get("/{id}/dictionary") {
        val id = UUID.fromString(call.parameters["id"])

        call.respond(index.getDictionary(id))
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

    fun buildFilter(seriesFilter: List<String>?, bookFilter: List<String>?) =
        transaction {
            if (seriesFilter == null && bookFilter == null) {
                Book.all()
            } else {
                val adjustedFilter = seriesFilter.orEmpty().map { Regex("^${Regex.escape(it)}($|\\\\)") }
                Book.all().filter { (it.series ?: "No Series").let { s -> adjustedFilter.any { r -> r.containsMatchIn(s) } } }
            }.map { it.id.value }
        } + bookFilter.orEmpty().map { UUID.fromString(it) }

    post("/search") {
        val request = call.receive<SearchRequest>()

        val filter = buildFilter(request.seriesFilter, request.bookFilter)
        if (filter.isEmpty()) {
            return@post call.respond(mapOf(
                "totalHits" to 0,
                "results" to emptyList<Any>()
            ))
        }

        val searchResult = index.search(request.query, request.page, filter) ?: return@post call.respond(
            HttpStatusCode.BadRequest,
            mapOf("message" to "The provided query is invalid!")
        )

        val results = transaction {
            searchResult.results.map {
                val book = Book.findById(it.bookId) ?: return@transaction null
                val chapter = Chapter.findById(it.chapterId) ?: return@transaction null
                mapOf(
                    "book" to book.toJson(),
                    "chapter" to chapter.toJson(),
                    "paragraphs" to it.paragraphs
                )
            }
        } ?: return@post call.respond(
            HttpStatusCode.NotFound,
            mapOf("message" to "Book or chapter does not exist")
        )

        call.respond(mapOf(
            "totalHits" to searchResult.totalHits,
            "results" to results
        ))
    }

    post("/grouped-search") {
        val request = call.receive<GroupedSearchRequest>()

        val filter = buildFilter(request.seriesFilter, request.bookFilter)
        if (filter.isEmpty()) {
            return@post call.respond(mapOf(
                "totalHits" to 0,
                "results" to emptyList<Any>()
            ))
        }

        val searchResult = index.searchGrouped(request.query, filter) ?: return@post call.respond(
            HttpStatusCode.BadRequest,
            mapOf("message" to "The provided query is invalid!")
        )

        val results = transaction {
            searchResult.results
                .map { bookResult ->
                    val book = Book.findById(bookResult.bookId)!!
                    book to bookResult
                }
                .sortedWith(
                    compareBy<Pair<Book, BookSearchResult>, Iterable<String>>(naturalOrder<String>().lexicographical()) {
                        (it.first.series ?: "").split("\\")
                    }.thenBy { it.first.orderInSeries }
                )
                .map { (book, bookResult) ->
                    book.toJson() +
                        ("chapters" to bookResult.chapters
                            .map { chapterResult ->
                                val chapter = Chapter.findById(chapterResult.chapterId)!!
                                chapter to chapterResult
                            }
                            .sortedBy { it.first.title }
                            .map { (chapter, chapterResult) ->
                                chapter.toJson() +
                                    ("totalOccurrences" to chapterResult.totalOccurrences) +
                                    ("results" to chapterResult.results)
                            }
                            .toList())
                }
                .toList()
        }

        call.respond(mapOf(
            "totalHits" to searchResult.totalHits,
            "results" to results
        ))
    }
}

private data class SearchRequest(val query: String, val page: Int, val seriesFilter: List<String>?, val bookFilter: List<String>?)

private data class GroupedSearchRequest(val query: String, val seriesFilter: List<String>?, val bookFilter: List<String>?)

private fun <T> Comparator<in T>.lexicographical(): Comparator<in Iterable<T>> =
    Comparator { left, right ->
        // Zipping limits the compared parts to the shorter list, then we perform a component-wise comparison
        // Short-circuits if any component of the left side is smaller or greater
        left.zip(right).fold(0) { acc, (a, b) -> if (acc != 0) return@Comparator acc else this.compare(a, b) }.let {
            // When all the parts are equal, the longer list wins (is greater)
            if (it == 0) {
                left.count() - right.count()
            } else {
                // Still required if the last part was not equal, the short-circuiting does not cover that
                it
            }
        }
    }
