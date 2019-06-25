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
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Application.books() {
    val elasticConfig = environment.config.propertyOrNull("elasticsearch.hosts")?.getList()
        ?: throw IllegalStateException("The elasticsearch host must be provided! " +
            "Either specify it as 'elasticsearch.hosts' in config file or pass ELASTIC_HOST env variable.")
    val index = BookIndex(*elasticConfig.map { HttpHost.create(it) }.toTypedArray())

    routing {
        route("/api/books") {
            authenticate {
                bookReading()

                bookSearch(index)

                authorize({ it.canManageBooks }) {
                    bookManagement(index)
                }
            }
        }
    }
}
