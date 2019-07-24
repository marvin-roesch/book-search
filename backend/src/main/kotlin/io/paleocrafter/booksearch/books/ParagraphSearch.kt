package io.paleocrafter.booksearch.books

import kotlinx.coroutines.supervisorScope
import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.action.search.MultiSearchRequest
import org.elasticsearch.action.search.MultiSearchResponse
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.ResponseException
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.elasticsearch.search.builder.SearchSourceBuilder
import java.util.UUID
import kotlin.coroutines.suspendCoroutine
import kotlin.math.absoluteValue

class ParagraphSearch(private val client: RestHighLevelClient) : BookIndex.Search() {
    suspend fun search(query: String, filter: List<UUID>, page: Int? = null, chapterFilter: List<UUID>? = null, sort: Boolean = false): SearchResults? {
        val baseQuery = buildBaseQuery(query, filter, chapterFilter)
        baseQuery.size(if (page == null) 1000 else 10)
        if (page != null) {
            baseQuery.from(page * 10)
        }

        if (sort) {
            baseQuery.sort("position")
        }

        baseQuery.highlighter(buildHighlighter())

        val baseResponse = supervisorScope {
            try {
                suspendCoroutine<SearchResponse> {
                    client.searchAsync(SearchRequest("paragraphs").source(baseQuery), RequestOptions.DEFAULT, SuspendingActionListener(it))
                }
            } catch (e: ResponseException) {
                null
            } catch (e: ElasticsearchStatusException) {
                null
            }
        } ?: return null

        return SearchResults(baseResponse.hits.totalHits.value, gatherContext(baseResponse.hits))
    }

    suspend fun searchBooks(query: String, filter: List<UUID>) =
        searchGrouped("book", query, filter)

    suspend fun searchChapters(book: UUID, query: String) =
        searchGrouped("chapter", query, listOf(book))

    private suspend fun searchGrouped(field: String, query: String, filter: List<UUID>): GroupedSearchResults? {
        val baseQuery = buildBaseQuery(query, filter)
            .aggregation(
                AggregationBuilders.terms("groups").field(field)
                    .size(1000)
            )

        val baseResponse = supervisorScope {
            try {
                suspendCoroutine<SearchResponse> {
                    client.searchAsync(SearchRequest("paragraphs").source(baseQuery), RequestOptions.DEFAULT, SuspendingActionListener(it))
                }
            } catch (e: ResponseException) {
                null
            } catch (e: ElasticsearchStatusException) {
                null
            }
        } ?: return null

        val totalHits = baseResponse.hits.totalHits.value
        val groups = baseResponse.aggregations.get<Terms>("groups").buckets

        return GroupedSearchResults(
            totalHits,
            groups.asSequence().map {
                GroupSearchResult(UUID.fromString(it.keyAsString), it.docCount.absoluteValue)
            }
        )
    }

    private suspend fun gatherContext(hits: Iterable<SearchHit>): List<SearchResult> {
        val contextRequest = MultiSearchRequest()
        for (hit in hits) {
            val source = hit.sourceAsMap
            val bookId = source["book"] as? String ?: throw IllegalStateException("Indexed paragraph must have book id!")
            val chapter = source["chapter"] as? String ?: throw IllegalStateException("Indexed paragraph must have chapter!")
            val position = source["position"] as? Int ?: throw IllegalStateException("Indexed paragraph must have position in chapter!")
            val contextQuery = SearchSourceBuilder()
            contextQuery.query(
                QueryBuilders.boolQuery()
                    .should(
                        QueryBuilders.boolQuery()
                            .must(QueryBuilders.termQuery("book", bookId))
                            .must(QueryBuilders.termQuery("chapter", chapter))
                            .must(QueryBuilders.rangeQuery("position").from(position - 2).to(position + 2))
                    )
                    .mustNot(
                        QueryBuilders.termQuery("position", position)
                    )
            )
            contextRequest.add(SearchRequest("paragraphs").source(contextQuery))
        }
        val contextResponses = if (hits.any()) {
            suspendCoroutine<MultiSearchResponse> {
                client.msearchAsync(contextRequest, RequestOptions.DEFAULT, SuspendingActionListener(it))
            }.responses
        } else {
            emptyArray()
        }

        return hits.mapIndexed { index, hit ->
            val context = contextResponses[index].response.hits.map {
                val source = it.sourceAsMap
                val position = source["position"] as? Int
                    ?: throw IllegalStateException("Indexed paragraph must have position in chapter!")
                val text = source["text"] as? String ?: throw IllegalStateException("Indexed paragraph must have text!")
                val classes = source["classes"] as? List<String>
                    ?: throw IllegalStateException("Indexed paragraph must have class array!")

                SearchParagraph(false, position, text, classes)
            }
            val source = hit.sourceAsMap
            val bookId = source["book"] as? String ?: throw IllegalStateException("Indexed paragraph must have book id!")
            val chapter = source["chapter"] as? String ?: throw IllegalStateException("Indexed paragraph must have chapter!")
            val position = source["position"] as? Int ?: throw IllegalStateException("Indexed paragraph must have position in chapter!")
            val text = hit.highlightFields["text.cs"]?.fragments?.first()?.string()
                ?: source["text"] as? String
                ?: throw IllegalStateException("Search result must have highlight!")
            val classes = source["classes"] as? List<String> ?: throw IllegalStateException("Indexed paragraph must have class array!")
            val paragraph = SearchParagraph(true, position, text, classes)

            SearchResult(
                UUID.fromString(bookId),
                UUID.fromString(chapter),
                paragraph.position,
                (context + paragraph).sortedBy { it.position }
            )
        }
    }

    suspend fun getDictionary(bookId: UUID): List<DictionaryEntry> {
        val query = SearchSourceBuilder().query(
            QueryBuilders.termQuery("book", bookId.toString())
        )
        query.aggregation(
            AggregationBuilders.terms(
                "dictionary"
            ).field("text.signature").size(999)
        )

        val response = suspendCoroutine<SearchResponse> {
            client.searchAsync(SearchRequest("paragraphs").source(query), RequestOptions.DEFAULT, SuspendingActionListener(it))
        }

        return response.aggregations.get<Terms>("dictionary").buckets
            .map {
                DictionaryEntry(it.key.toString(), it.docCount)
            }
            .sortedByDescending { it.occurrences }
    }
}

data class SearchResults(val totalHits: Long, val results: List<SearchResult>)

data class GroupedSearchResults(val totalHits: Long, val results: Sequence<GroupSearchResult>)

data class GroupSearchResult(val id: UUID, val occurrences: Long)

data class SearchResult(val bookId: UUID, val chapterId: UUID, val mainPosition: Int, val paragraphs: List<SearchParagraph>)

class SearchParagraph(val main: Boolean, val position: Int, val text: String, val classes: List<String>)

data class DictionaryEntry(val word: String, val occurrences: Long)
