package io.paleocrafter.booksearch.books

import kotlinx.coroutines.supervisorScope
import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.ResponseException
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.Operator
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.elasticsearch.search.builder.SearchSourceBuilder
import java.util.UUID
import kotlin.coroutines.suspendCoroutine
import kotlin.math.absoluteValue

class ChapterSearch(private val client: RestHighLevelClient) : BookIndex.Search() {
    suspend fun search(query: String, filter: List<UUID>, page: Int? = null): ChapterSearchResults? {
        val baseQuery = buildBaseQuery(query, filter)
        baseQuery.size(if (page == null) 1000 else 10)
        if (page != null) {
            baseQuery.from(page * 10)
        }

        val response = supervisorScope {
            try {
                suspendCoroutine<SearchResponse> {
                    client.searchAsync(SearchRequest("chapters").source(baseQuery), RequestOptions.DEFAULT, SuspendingActionListener(it))
                }
            } catch (e: ResponseException) {
                null
            } catch (e: ElasticsearchStatusException) {
                null
            }
        } ?: return null

        return ChapterSearchResults(
            response.hits.totalHits.value,
            response.hits.map {
                UUID.fromString(it.id)
            }
        )
    }

    suspend fun getContent(id: UUID, query: String): String? {
        val source = SearchSourceBuilder().query(QueryBuilders.idsQuery().addIds(id.toString()))
            .highlighter(
                buildHighlighter().highlightQuery(
                    QueryBuilders.queryStringQuery(query).defaultField("text.cs.lowercase").defaultOperator(Operator.AND)
                )
            )

        val response = supervisorScope {
            try {
                suspendCoroutine<SearchResponse> {
                    client.searchAsync(SearchRequest("chapters").source(source), RequestOptions.DEFAULT, SuspendingActionListener(it))
                }
            } catch (e: ResponseException) {
                null
            } catch (e: ElasticsearchStatusException) {
                null
            }
        } ?: return null

        val hit = response.hits.firstOrNull() ?: return null

        return hit.highlightFields?.get("text.cs")?.fragments?.first()?.string() ?: hit.sourceAsMap["text"] as? String
    }

    suspend fun searchBooks(query: String, filter: List<UUID>) =
        searchGrouped("book", query, filter)

    private suspend fun searchGrouped(field: String, query: String, filter: List<UUID>): GroupedSearchResults? {
        val baseQuery = buildBaseQuery(query, filter)
            .aggregation(
                AggregationBuilders.terms("groups").field(field)
                    .size(1000)
            )

        val baseResponse = supervisorScope {
            try {
                suspendCoroutine<SearchResponse> {
                    client.searchAsync(SearchRequest("chapters").source(baseQuery), RequestOptions.DEFAULT, SuspendingActionListener(it))
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
}

data class ChapterSearchResults(val totalHits: Long, val results: List<UUID>)
