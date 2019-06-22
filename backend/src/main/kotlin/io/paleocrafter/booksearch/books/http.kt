package io.paleocrafter.booksearch.books

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.fromFilePath
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.paleocrafter.booksearch.auth.authorize
import org.apache.http.HttpHost
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Application.books() {
    val elasticConfig = environment.config.propertyOrNull("elasticsearch.hosts")?.getList()
        ?: throw IllegalStateException("The elasticsearch host must be provided! " +
            "Either specify it as 'elasticsearch.hosts' in config file or pass ELASTIC_HOST env variable.")
    val index = BookIndex(*elasticConfig.map { HttpHost.create(it) }.toTypedArray())

    transaction {
        SchemaUtils.createMissingTablesAndColumns(Books, Chapters, Images, ClassMappings)
    }

    routing {
        route("/api/books") {
            authenticate {
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

                bookSearch(index)

                authorize({ it.canManageBooks }) {
                    bookManagement(index)
                }

                chapterView()

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
            }
        }
    }
}
