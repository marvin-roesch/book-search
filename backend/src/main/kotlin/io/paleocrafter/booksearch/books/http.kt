package io.paleocrafter.booksearch.books

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.auth.authenticate
import io.ktor.routing.Route
import io.ktor.routing.route
import io.ktor.routing.routing
import io.paleocrafter.booksearch.auth.authorize
import io.paleocrafter.booksearch.auth.requirePermissions
import org.apache.http.HttpHost
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

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

fun Route.requireBookPermissions(paramName: String = "id", build: Route.() -> Unit): Route {
    return requireBookPermissions({ Book.findById(UUID.fromString(it.parameters[paramName])) }, build)
}

fun Route.requireBookPermissions(bookProvider: (call: ApplicationCall) -> Book?, build: Route.() -> Unit): Route {
    return authorize(
        { user ->
            val call = this

            transaction {
                val book = bookProvider(call) ?: return@transaction true

                !book.restricted || Book.readingPermission(book.id.value) in user.permissions
            }
        },
        build
    )
}
