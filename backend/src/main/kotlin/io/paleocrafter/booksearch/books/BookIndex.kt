package io.paleocrafter.booksearch.books

import org.apache.http.HttpHost
import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.admin.indices.close.CloseIndexRequest
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.client.indices.PutMappingRequest
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.Operator
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.reindex.DeleteByQueryRequest
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder
import org.intellij.lang.annotations.Language
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

class BookIndex(vararg hosts: HttpHost) {
    private val client = RestHighLevelClient(
        RestClient.builder(*hosts)
            .setHttpClientConfigCallback {
                it.setKeepAliveStrategy { _, _ -> TimeUnit.MINUTES.toMillis(10) }
            }
    )

    @Language("JSON")
    private val chapterMapping = """
        {
            "properties": {
                "book": {
                    "type": "keyword"
                },
                "position": {
                    "type": "integer"
                },
                "text": {
                    "type": "text",
                    "fields": {
                        "cs": {
                            "type": "text",
                            "analyzer": "strip_html_analyzer",
                            "term_vector": "with_positions_offsets",
                            "fields": {
                                "lowercase": {
                                    "type": "text",
                                    "analyzer": "case_insensitive_analyzer",
                                    "term_vector": "with_positions_offsets"
                                }
                            }
                        }
                    }
                }
            }
        }
    """.trimIndent()

    @Language("JSON")
    private val paragraphMapping = """
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
                    "type": "text",
                    "fields": {
                        "cs": {
                            "type": "text",
                            "analyzer": "strip_html_analyzer",
                            "term_vector": "with_positions_offsets",
                            "fields": {
                                "lowercase": {
                                    "type": "text",
                                    "analyzer": "case_insensitive_analyzer",
                                    "term_vector": "with_positions_offsets"
                                }
                            }
                        },
                        "signature": {
                            "type": "text",
                            "analyzer": "signature_analyzer",
                            "fielddata": true
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
                        "char_filter": ["html_stripper"]
                    },
                    "case_insensitive_analyzer": {
                        "tokenizer": "classic",
                        "filter": ["lowercase"],
                        "char_filter": ["html_stripper"]
                    },
                    "signature_analyzer": {
                        "tokenizer": "standard",
                        "filter": [
                            "possessive_stemmer",
                            "suffix_cleanup",
                            "en_US",
                            "english_stops",
                            "dictionary_stops",
                            "revert_hyphens",
                            "remove_duplicates"
                        ],
                        "char_filter": [
                            "html_stripper",
                            "normalize_apostrophes",
                            "convert_hyphen_start",
                            "convert_hyphens",
                            "remove_numbers",
                            "remove_all_caps"
                        ]
                    }
                },
                "char_filter": {
                    "html_stripper": {
                        "type": "html_strip"
                    },
                    "convert_hyphen_start": {
                        "type": "pattern_replace",
                        "pattern": "(\\p{Upper}[^\\s-]+)-(?=[^-\\s]+)",
                        "replacement": "$1_"
                    },
                    "convert_hyphens": {
                        "type": "pattern_replace",
                        "pattern": "(?<=_)([^\\s-_]+)-(?=[^-\\s]+)",
                        "replacement": "$1_"
                    },
                    "remove_numbers": {
                        "type": "pattern_replace",
                        "pattern": "[0-9]+",
                        "replacement": ""
                    },
                    "remove_all_caps": {
                        "type": "pattern_replace",
                        "pattern": "\\p{Upper}{2,}",
                        "replacement": ""
                    },
                    "normalize_apostrophes": {
                        "type": "mapping",
                        "mappings": [
                            "\u0091=>\u0027",
                            "\u0092=>\u0027",
                            "\u2018=>\u0027",
                            "\u2019=>\u0027",
                            "\uFF07=>\u0027"
                        ]
                    }
                },
                "filter": {
                    "suffix_cleanup": {
                        "type": "length",
                        "min": 2
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

    val paragraphs = ParagraphSearch(client)
    val chapters = ChapterSearch(client)

    private suspend fun ensureElasticIndices() {
        ensureElasticIndex("chapters", chapterMapping)
        ensureElasticIndex("paragraphs", paragraphMapping)
    }

    private suspend fun ensureElasticIndex(index: String, mapping: String) {
        if (!suspendCoroutine<Boolean> {
                client.indices().existsAsync(
                    GetIndexRequest(index),
                    RequestOptions.DEFAULT,
                    SuspendingActionListener(it)
                )
            }) {
            suspendCoroutine<Any> {
                client.indices().createAsync(
                    CreateIndexRequest(index),
                    RequestOptions.DEFAULT,
                    SuspendingActionListener(it)
                )
            }

            suspendCoroutine<Any> {
                client.indices().closeAsync(
                    CloseIndexRequest(index),
                    RequestOptions.DEFAULT,
                    SuspendingActionListener(it)
                )
            }

            suspendCoroutine<Any> {
                client.indices().putSettingsAsync(
                    UpdateSettingsRequest(index).settings(indexSettings, XContentType.JSON),
                    RequestOptions.DEFAULT,
                    SuspendingActionListener(it)
                )
            }

            suspendCoroutine<Any> {
                client.indices().putMappingAsync(
                    PutMappingRequest(index).source(mapping, XContentType.JSON),
                    RequestOptions.DEFAULT,
                    SuspendingActionListener(it)
                )
            }

            suspendCoroutine<Any> {
                client.indices().openAsync(
                    OpenIndexRequest(index),
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
            suspendCoroutine<Any> {
                client.indices().deleteAsync(DeleteIndexRequest("chapters"), RequestOptions.DEFAULT, SuspendingActionListener(it))
            }
        }
        if (suspendCoroutine {
                client.indices().existsAsync(GetIndexRequest("paragraphs"), RequestOptions.DEFAULT, SuspendingActionListener(it))
            }) {
            suspendCoroutine<Any> {
                client.indices().deleteAsync(DeleteIndexRequest("paragraphs"), RequestOptions.DEFAULT, SuspendingActionListener(it))
            }
        }
    }

    suspend fun index(id: UUID, chapters: Iterable<ResolvedChapter>) {
        ensureElasticIndices()

        this.deleteBook(id)

        val bulkRequest = BulkRequest()
        for (chapter in chapters) {
            bulkRequest.add(IndexRequest("chapters").id(chapter.id.toString()).source(
                mapOf(
                    "book" to id.toString(),
                    "position" to chapter.position,
                    "text" to chapter.content.html()
                )
            ))
        }

        val paragraphs = chapters.flatMap { this.collectEntries(it) }

        for (entry in paragraphs) {
            bulkRequest.add(IndexRequest("paragraphs").id(UUID.randomUUID().toString()).source(
                mapOf(
                    "book" to id.toString(),
                    "chapter" to entry.chapter,
                    "position" to entry.position,
                    "classes" to entry.classes,
                    "text" to entry.text
                )
            ))
        }

        suspendCoroutine<BulkResponse> {
            client.bulkAsync(bulkRequest, RequestOptions.DEFAULT, SuspendingActionListener(it))
        }
    }

    private fun collectEntries(chapter: ResolvedChapter): List<ParagraphIndexEntry> {
        return chapter.content.select("p").mapIndexed { position, p ->
            ParagraphIndexEntry(chapter.id.toString(), position, p.html(), p.classNames())
        }
    }

    suspend fun deleteBook(id: UUID) {
        ensureElasticIndices()

        suspendCoroutine<Any> {
            client.deleteByQueryAsync(
                DeleteByQueryRequest("chapters", "paragraphs").setQuery(QueryBuilders.termQuery("book", id.toString())),
                RequestOptions.DEFAULT,
                SuspendingActionListener(it)
            )
        }
    }

    private data class ParagraphIndexEntry(val chapter: String, val position: Int, val text: String, val classes: Set<String>)

    open class Search {
        protected fun buildHighlighter() =
            HighlightBuilder().field(
                HighlightBuilder.Field("text.cs")
                    .matchedFields("text.cs", "text.cs.lowercase")
                    .numOfFragments(0)
                    .preTags("<mark>")
                    .postTags("</mark>")
            ).highlighterType("fvh")

        protected fun buildBaseQuery(query: String, bookFilter: List<UUID>, chapterFilter: List<UUID>? = null) =
            SearchSourceBuilder().query(
                QueryBuilders.boolQuery()
                    .must(QueryBuilders.queryStringQuery(query).defaultField("text.cs.lowercase").defaultOperator(Operator.AND))
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
    }
}

class SuspendingActionListener<T>(private val continuation: Continuation<T>) : ActionListener<T> {
    override fun onFailure(e: Exception) {
        continuation.resumeWith(Result.failure(e))
    }

    override fun onResponse(response: T) {
        continuation.resumeWith(Result.success(response))
    }
}
