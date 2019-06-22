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

        call.respond(index.paragraphs.getDictionary(id))
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

    post("/paragraph-search") {
        val request = call.receive<SearchRequest>()

        val filter = buildFilter(request.seriesFilter, request.bookFilter)
        if (filter.isEmpty()) {
            return@post call.respond(mapOf(
                "totalHits" to 0,
                "results" to emptyList<Any>()
            ))
        }

        val searchResult = index.paragraphs.search(request.query, filter, request.page) ?: return@post call.respond(
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

    post("/chapter-search") {
        val request = call.receive<SearchRequest>()

        val filter = buildFilter(request.seriesFilter, request.bookFilter)
        if (filter.isEmpty()) {
            return@post call.respond(mapOf(
                "totalHits" to 0,
                "results" to emptyList<Any>()
            ))
        }

        val searchResult = index.chapters.search(request.query, filter, request.page) ?: return@post call.respond(
            HttpStatusCode.BadRequest,
            mapOf("message" to "The provided query is invalid!")
        )

        val results = transaction {
            searchResult.results.map {
                val chapter = Chapter.findById(it) ?: return@transaction null
                mapOf(
                    "book" to chapter.book.toJson(),
                    "chapter" to chapter.toJson()
                )
            }
        } ?: return@post call.respond(
            HttpStatusCode.NotFound,
            mapOf("message" to "Chapter does not exist")
        )

        call.respond(mapOf(
            "totalHits" to searchResult.totalHits,
            "results" to results
        ))
    }

    post("/paragraph-search/grouped") {
        val request = call.receive<GroupedSearchRequest>()

        val filter = buildFilter(request.seriesFilter, request.bookFilter)
        if (filter.isEmpty()) {
            return@post call.respond(mapOf(
                "totalHits" to 0,
                "results" to emptyList<Any>()
            ))
        }

        val searchResult = index.paragraphs.searchBooks(request.query, filter) ?: return@post call.respond(
            HttpStatusCode.BadRequest,
            mapOf("message" to "The provided query is invalid!")
        )

        val results = transaction {
            searchResult.results
                .map { bookResult ->
                    val book = Book.findById(bookResult.id)!!
                    book to bookResult
                }
                .sortedWith(
                    compareBy<Pair<Book, GroupSearchResult>, Iterable<String>>(naturalOrder<String>().lexicographical()) {
                        (it.first.series ?: "").split("\\")
                    }.thenBy { it.first.orderInSeries }.thenBy { it.first.title }
                )
                .map { (book, bookResult) ->
                    book.toJson() + ("totalOccurrences" to bookResult.occurrences)
                }
                .toList()
        }

        call.respond(mapOf(
            "totalHits" to searchResult.totalHits,
            "results" to results
        ))
    }

    post("/chapter-search/grouped") {
        val request = call.receive<GroupedSearchRequest>()

        val filter = buildFilter(request.seriesFilter, request.bookFilter)
        if (filter.isEmpty()) {
            return@post call.respond(mapOf(
                "totalHits" to 0,
                "results" to emptyList<Any>()
            ))
        }

        val searchResult = index.chapters.searchBooks(request.query, filter) ?: return@post call.respond(
            HttpStatusCode.BadRequest,
            mapOf("message" to "The provided query is invalid!")
        )

        val results = transaction {
            searchResult.results
                .map { bookResult ->
                    val book = Book.findById(bookResult.id)!!
                    book to bookResult
                }
                .sortedWith(
                    compareBy<Pair<Book, GroupSearchResult>, Iterable<String>>(naturalOrder<String>().lexicographical()) {
                        (it.first.series ?: "").split("\\")
                    }.thenBy { it.first.orderInSeries }.thenBy { it.first.title }
                )
                .map { (book, bookResult) ->
                    book.toJson() + ("totalOccurrences" to bookResult.occurrences)
                }
                .toList()
        }

        call.respond(mapOf(
            "totalHits" to searchResult.totalHits,
            "results" to results
        ))
    }

    post("/{id}/paragraph-search/grouped") {
        val id = UUID.fromString(call.parameters["id"])
        val request = call.receive<GroupedSearchRequest>()

        val searchResult = index.paragraphs.searchChapters(id, request.query) ?: return@post call.respond(
            HttpStatusCode.BadRequest,
            mapOf("message" to "The provided query is invalid!")
        )

        val results = transaction {
            searchResult.results
                .map { chapterResult ->
                    val chapter = Chapter.findById(chapterResult.id)!!
                    chapter to chapterResult
                }
                .sortedWith(compareBy({ it.first.position }, { it.first.title }))
                .map { (chapter, chapterResult) ->
                    chapter.toJson() + ("totalOccurrences" to chapterResult.occurrences)
                }
                .toList()
        }

        call.respond(mapOf("results" to results))
    }

    post("/{id}/chapter-search/grouped") {
        val id = UUID.fromString(call.parameters["id"])
        val request = call.receive<GroupedSearchRequest>()

        val searchResult = index.chapters.search(request.query, listOf(id)) ?: return@post call.respond(
            HttpStatusCode.BadRequest,
            mapOf("message" to "The provided query is invalid!")
        )

        val results = transaction {
            searchResult.results
                .map { Chapter.findById(it) ?: return@transaction null }
                .sortedWith(compareBy({ it.position }, { it.title }))
                .map { it.toJson() }
        } ?: return@post call.respond(
            HttpStatusCode.NotFound,
            mapOf("message" to "Chapter does not exist")
        )

        call.respond(mapOf("results" to results))
    }

    post("/chapters/{id}/paragraph-search") {
        val id = UUID.fromString(call.parameters["id"])
        val request = call.receive<GroupedSearchRequest>()

        val filter = buildFilter(request.seriesFilter, request.bookFilter)
        if (filter.isEmpty()) {
            return@post call.respond(mapOf(
                "totalHits" to 0,
                "results" to emptyList<Any>()
            ))
        }

        val searchResult = index.paragraphs.search(request.query, filter, chapterFilter = listOf(id)) ?: return@post call.respond(
            HttpStatusCode.BadRequest,
            mapOf("message" to "The provided query is invalid!")
        )

        val results = searchResult.results.map {
            mapOf(
                "paragraphs" to it.paragraphs
            )
        }

        call.respond(mapOf("results" to results))
    }

    post("/chapters/{id}/search") {
        val id = UUID.fromString(call.parameters["id"])
        val request = call.receive<GroupedSearchRequest>()

        val (book, chapter) = transaction {
            val chapter = Chapter.findById(id) ?: return@transaction null
            chapter.book.toJson() to chapter.toJson()
        } ?: return@post call.respond(
            HttpStatusCode.NotFound,
            mapOf("message" to "Chapter with ID '$id' does not exist")
        )

        val searchResult = index.chapters.getContent(id, request.query) ?: return@post call.respond(
            HttpStatusCode.BadRequest,
            mapOf("message" to "The provided query is invalid!")
        )

        call.respond(mapOf("book" to book, "chapter" to chapter, "content" to searchResult))
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
