package io.paleocrafter.booksearch

import com.fasterxml.jackson.databind.JsonMappingException
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ConditionalHeaders
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import org.jetbrains.exposed.sql.Database
import org.slf4j.event.Level

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
