package io.paleocrafter.booksearch

import com.fasterxml.jackson.databind.JsonMappingException
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CachingHeaders
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ConditionalHeaders
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.CachingOptions
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.paleocrafter.booksearch.books.ManagementError
import org.jetbrains.exposed.sql.Database
import java.util.concurrent.TimeUnit

fun Application.db() {
    val connection = environment.config.propertyOrNull("db.connection")?.getString()
        ?: throw IllegalStateException("'db.connection' must be configured! Either specify it in config file or pass DB_CONNECTION env variable.")
    val driver = environment.config.propertyOrNull("db.driver")?.getString()
        ?: throw IllegalStateException("'db.driver' must be configured! Either specify it in config file or pass DB_DRIVER env variable.")

    val config = HikariConfig()
    config.jdbcUrl = connection
    config.driverClassName = driver
    config.maxLifetime = TimeUnit.MINUTES.toMillis(10)
    config.isAutoCommit = false
    config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"

    val username = environment.config.propertyOrNull("db.username")?.getString()
    val password = environment.config.propertyOrNull("db.password")?.getString()
    if (username != null && password != null) {
        config.username = username
        config.password = password
    }
    config.validate()

    val migrationUsername = environment.config.propertyOrNull("db.migration.username")?.getString()
    val migrationPassword = environment.config.propertyOrNull("db.migration.password")?.getString()
    if (migrationUsername == null || migrationPassword == null) {
        environment.log.warn("Migration username or password not provided, running without...")
    }

    val migrationDb = Database.connect(connection, driver, migrationUsername ?: "", migrationPassword ?: "")
    DbMigrations.run(migrationDb)

    val dataSource = HikariDataSource(config)
    Database.connect(dataSource)
}

fun Application.main() {
    install(DefaultHeaders)
    install(CallLogging)
    install(ConditionalHeaders)
    install(Compression)
    install(StatusPages) {
        exception<NotImplementedError> { call.respond(HttpStatusCode.NotImplemented) }
        exception<JsonMappingException> { call.respond(HttpStatusCode.BadRequest, it.message ?: "") }
        exception<ManagementError> {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf(
                    "message" to (it.message ?: "An unknown error has occurred while editing this book")
                )
            )
        }
    }
    install(ContentNegotiation) {
        jackson {
        }
    }
    install(CachingHeaders) {
        options { outgoingContent ->
            if (outgoingContent.contentType?.withoutParameters()?.match(ContentType.Image.Any) == true) {
                CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 30 * 24 * 60 * 60))
            } else {
                null
            }
        }
    }
}
