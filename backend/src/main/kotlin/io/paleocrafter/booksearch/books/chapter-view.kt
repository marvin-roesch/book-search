package io.paleocrafter.booksearch.books

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Route.chapterView() {
    get("/chapter/{id}") {
        val id = UUID.fromString(call.parameters["id"])

        call.respond(
            transaction {
                val chapter = Chapter.findById(id) ?: return@transaction null

                mapOf(
                    "title" to chapter.title,
                    "content" to chapter.indexedContent
                )
            } ?: return@get call.respond(
                HttpStatusCode.NotFound,
                mapOf("message" to "Chapter with ID '$id' does not exist")
            )
        )
    }
}
