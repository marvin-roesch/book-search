package io.paleocrafter.booksearch

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.authenticate
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ConditionalHeaders
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.fromFilePath
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.paleocrafter.booksearch.books.Book
import io.paleocrafter.booksearch.books.Books
import io.paleocrafter.booksearch.books.Chapters
import io.paleocrafter.booksearch.books.ClassMappings
import io.paleocrafter.booksearch.books.Images
import io.paleocrafter.booksearch.books.bookManagement
import io.paleocrafter.booksearch.books.bookSearch
import io.paleocrafter.booksearch.books.chapterView
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun Application.db() {
    Database.connect(
        environment.config.property("db.connection").getString(),
        environment.config.property("db.driver").getString()
    )
}

fun Application.main() {
    install(DefaultHeaders)
    install(CallLogging)
    install(ConditionalHeaders)
    install(Compression)
    install(StatusPages) {
        exception<NotImplementedError> { call.respond(HttpStatusCode.NotImplemented) }
        exception<JsonMappingException> { call.respond(HttpStatusCode.BadRequest, it.message ?: "") }
    }
    install(ContentNegotiation) {
        jackson {
        }
    }
}
