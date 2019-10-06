package io.paleocrafter.booksearch.books

import io.ktor.application.Application
import io.ktor.auth.authenticate
import io.ktor.routing.route
import io.ktor.routing.routing
import io.paleocrafter.booksearch.auth.requirePermissions
import org.apache.http.HttpHost

fun Application.books() {
    val elasticConfig = environment.config.propertyOrNull("elasticsearch.hosts")?.getList()
        ?: throw IllegalStateException("The elasticsearch host must be provided! " +
            "Either specify it as 'elasticsearch.hosts' in config file or pass ELASTIC_HOST env variable.")
    val index = BookIndex(*elasticConfig.map { HttpHost.create(it) }.toTypedArray())

    BookCache.rebuild()

    routing {
        route("/api/books") {
            authenticate {
                bookReading()

                bookSearch(index)

                requirePermissions("books.manage") {
                    bookManagement(index)
                }
            }
        }
    }
}
