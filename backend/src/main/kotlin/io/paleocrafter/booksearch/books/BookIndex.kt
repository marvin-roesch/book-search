package io.paleocrafter.booksearch.books

import kotlinx.coroutines.supervisorScope
import org.apache.http.HttpHost
import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.admin.indices.close.CloseIndexRequest
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.search.MultiSearchRequest
import org.elasticsearch.action.search.MultiSearchResponse
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.ResponseException
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.client.indices.PutMappingRequest
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.Operator
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.reindex.DeleteByQueryRequest
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.significant.SignificantTerms
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder
import org.intellij.lang.annotations.Language
import java.util.UUID
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine
import kotlin.math.absoluteValue

class BookIndex(vararg hosts: HttpHost) {
    private val client = RestHighLevelClient(RestClient.builder(*hosts))

    @Language("JSON")
    private val chapterMapping = """
        {
            "properties": {
                "book": {
                    "type": "keyword"
                },
                "chapter": {
                    "type": "keyword"
                },
                "position": {
                    "type": "integer"
                },
                "classes": {
                    "type": "keyword"
                },
                "text": {
                    "type": "keyword",
                    "fields": {
                        "stripped": {
                            "type": "text",
                            "analyzer": "strip_html_analyzer"
                        },
                        "signature": {
                            "type": "text",
                            "analyzer": "signature_analyzer"
                        }
                    }
                }
            }
        }
    """.trimIndent()
    @Language("JSON")
    private val indexSettings = """
        {
            "max_inner_result_window": 1000,
            "analysis": {
                "analyzer": {
                    "strip_html_analyzer": {
                        "tokenizer": "classic",
                        "filter": ["lowercase"],
                        "char_filter": ["html_stripper"]
                    },
                    "signature_analyzer": {
                        "tokenizer": "standard",
                        "filter": [
                            "lowercase",
                            "possessive_stemmer",
                            "suffix_cleanup",
                            "en_US",
                            "english_stops",
                            "dictionary_stops",
                            "revert_hyphens",
                            "remove_duplicates"
                        ],
                        "char_filter": ["html_stripper", "normalize_apostrophes", "convert_hyphens"]
                    }
                },
                "char_filter": {
                    "html_stripper": {
                        "type": "html_strip"
                    },
                    "convert_hyphens": {
                        "type": "pattern_replace",
                        "pattern": "([^\\s]{3,})-([^\\s]{3,})",
                        "replacement": "$1_$2"
                    },
                    "normalize_apostrophes": {
                        "type": "mapping",
                        "mappings": [
                            "’ => '"
                        ]
                    }
                },
                "filter": {
                    "suffix_cleanup": {
                        "type": "length",
                        "min": 3
                    },
                    "possessive_stemmer": {
                        "type": "stemmer",
                        "name": "possessive_english"
                    },
                    "en_US" : {
                        "type" : "hunspell",
                        "locale" : "en_US",
                        "dedup" : true
                    },
                    "english_stops": {
                        "type": "stop",
                        "stopwords":  "_english_"
                    },
                    "dictionary_stops": {
                        "type": "stop",
                        "stopwords_path": "hunspell/en_US/stopwords.txt",
                        "ignore_case": true
                    },
                    "revert_hyphens": {
                        "type": "pattern_replace",
                        "pattern": "_",
                        "replacement": "-"
                    }
                }
            }
        }
    """.trimIndent()

    private val highlighter =
        HighlightBuilder().field(
            HighlightBuilder.Field("text.stripped")
                .numOfFragments(0)
                .preTags("<strong>")
                .postTags("</strong>")
        )

    private suspend fun ensureElasticIndex() {
        if (!suspendCoroutine<Boolean> {
                client.indices().existsAsync(GetIndexRequest("chapters"), RequestOptions.DEFAULT, SuspendingActionListener(it))
            }) {
            suspendCoroutine {
                client.indices().createAsync(
                    CreateIndexRequest("chapters"),
                    RequestOptions.DEFAULT,
                    SuspendingActionListener(it)
                )
            }
            suspendCoroutine {
                client.indices().closeAsync(
                    CloseIndexRequest("chapters"),
                    RequestOptions.DEFAULT,
                    SuspendingActionListener(it)
                )
            }
            suspendCoroutine {
                client.indices().putSettingsAsync(
                    UpdateSettingsRequest("chapters").settings(indexSettings, XContentType.JSON),
                    RequestOptions.DEFAULT,
                    SuspendingActionListener(it)
                )
            }
            suspendCoroutine {
                client.indices().putMappingAsync(
                    PutMappingRequest("chapters").source(chapterMapping, XContentType.JSON),
                    RequestOptions.DEFAULT,
                    SuspendingActionListener(it)
                )
            }
            suspendCoroutine {
                client.indices().openAsync(
                    OpenIndexRequest("chapters"),
                    RequestOptions.DEFAULT,
                    SuspendingActionListener(it)
                )
            }
        }
    }

    suspend fun reset() {
        if (suspendCoroutine {
                client.indices().existsAsync(GetIndexRequest("chapters"), RequestOptions.DEFAULT, SuspendingActionListener(it))
            }) {
            suspendCoroutine {
                client.indices().deleteAsync(DeleteIndexRequest("chapters"), RequestOptions.DEFAULT, SuspendingActionListener(it))
            }
        }
    }

    suspend fun index(id: UUID, chapters: Iterable<ResolvedChapter>) {
        ensureElasticIndex()

        this.delete(id)

        val entries = chapters.flatMap { this.collectEntries(it) }

        val bulkRequest = BulkRequest("chapters")
        for (entry in entries) {
            bulkRequest.add(IndexRequest().id(UUID.randomUUID().toString()).source(
                mapOf(
                    "book" to id.toString(),
                    "chapter" to entry.chapter,
                    "position" to entry.position,
                    "classes" to entry.classes,
                    "text" to entry.text
                )
            ))
        }
        suspendCoroutine {
            client.bulkAsync(bulkRequest, RequestOptions.DEFAULT, SuspendingActionListener(it))
        }
    }

    private fun collectEntries(chapter: ResolvedChapter): List<IndexEntry> {
        return chapter.content.select("p").mapIndexed { position, p ->
            IndexEntry(chapter.id.toString(), position, p.html(), p.classNames())
        }
    }

    suspend fun delete(id: UUID) {
        ensureElasticIndex()

        suspendCoroutine {
            client.deleteByQueryAsync(
                DeleteByQueryRequest("chapters").setQuery(QueryBuilders.termQuery("book", id.toString())),
                RequestOptions.DEFAULT,
                SuspendingActionListener(it)
            )
        }
    }

    private fun buildBaseQuery(query: String, bookFilter: List<UUID>, chapterFilter: List<UUID>? = null) =
        SearchSourceBuilder().query(
            QueryBuilders.boolQuery()
                .must(QueryBuilders.queryStringQuery(query).defaultField("text.stripped").defaultOperator(Operator.AND))
                .filter(
                    QueryBuilders.boolQuery()
                        .minimumShouldMatch(1)
                        .also { qry ->
                            bookFilter.forEach {
                                qry.should(QueryBuilders.termQuery("book", it.toString()))
                            }
                            if (chapterFilter !== null) {
                                chapterFilter.forEach {
                                    qry.must(QueryBuilders.termQuery("chapter", it.toString()))
                                }
                            }
                        }
                )
        )

    suspend fun search(query: String, filter: List<UUID>, page: Int? = null, chapterFilter: List<UUID>? = null): SearchResults? {
        val baseQuery = buildBaseQuery(query, filter, chapterFilter)
        baseQuery.size(if (page == null) 1000 else 10)
        if (page != null) {
            baseQuery.from(page * 10)
        }

        baseQuery.highlighter(highlighter)

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
            contextRequest.add(SearchRequest("chapters").source(contextQuery))
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
            val text = hit.highlightFields["text.stripped"]?.fragments?.first()?.string() ?: source["text"] as? String
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
            AggregationBuilders.significantText(
                "dictionary",
                "text.signature"
            ).size(1000)
        )

        val response = suspendCoroutine<SearchResponse> {
            client.searchAsync(SearchRequest("chapters").source(query), RequestOptions.DEFAULT, SuspendingActionListener(it))
        }

        return response.aggregations.get<SignificantTerms>("dictionary").buckets
            .map {
                DictionaryEntry(it.key.toString(), it.docCount)
            }
            .sortedByDescending { it.occurrences }
    }

    private data class IndexEntry(val chapter: String, val position: Int, val text: String, val classes: Set<String>)

    private class SuspendingActionListener<T>(private val continuation: Continuation<T>) : ActionListener<T> {
        override fun onFailure(e: Exception) {
            continuation.resumeWith(Result.failure(e))
        }

        override fun onResponse(response: T) {
            continuation.resumeWith(Result.success(response))
        }
    }
}

data class SearchResults(val totalHits: Long, val results: List<SearchResult>)

data class GroupedSearchResults(val totalHits: Long, val results: Sequence<GroupSearchResult>)

data class GroupSearchResult(val id: UUID, val occurrences: Long)

data class SearchResult(val bookId: UUID, val chapterId: UUID, val mainPosition: Int, val paragraphs: List<SearchParagraph>)

class SearchParagraph(val main: Boolean, val position: Int, val text: String, val classes: List<String>)

data class DictionaryEntry(val word: String, val occurrences: Long)
