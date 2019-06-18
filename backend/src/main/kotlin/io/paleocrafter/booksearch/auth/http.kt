package io.paleocrafter.booksearch.auth

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.Principal
import io.ktor.auth.SessionAuthChallenge
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.auth.session
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.sessions.SessionStorageMemory
import io.ktor.sessions.Sessions
import io.ktor.sessions.cookie
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import io.ktor.util.hex
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun Application.auth() {
    transaction {
        SchemaUtils.createMissingTablesAndColumns(AppUsers)
    }

    val keyString = environment.config.propertyOrNull("crypto.key")?.getString()
        ?: throw IllegalStateException("'crypto.key' must be configured! Either specify it in config file or pass CRYPTO_KEY env variable.")
    val hashKey = SecretKeySpec(hex(keyString), "HmacSHA1")

    fun hash(password: String): String {
        val hmac = Mac.getInstance("HmacSHA1")
        hmac.init(hashKey)
        return hex(hmac.doFinal(password.toByteArray(Charsets.UTF_8)))
    }

    transaction {
        if (User.all().none()) {
            val defaultPassword = environment.config.propertyOrNull("auth.admin-password")?.getString()
                ?: throw IllegalStateException("There are no users yet and no default admin password is set! " +
                    "Either specify it as 'auth.admin-password' in config file or pass DEFAULT_PASSWORD env variable.")
            AppUsers.insert {
                it[username] = "admin"
                it[password] = hash(defaultPassword)
                it[canManageBooks] = true
                it[canManageUsers] = true
            }

            environment.log.info("Superuser 'admin' was created with the provided default password!")
        }
    }

    install(Sessions) {
        cookie<UserId>("AUTH_SESSION", storage = SessionStorageMemory())
    }

    install(Authentication) {
        session<UserId>("auth") {
            challenge = SessionAuthChallenge.Unauthorized
            validate {
                it
            }
        }
    }

    routing {
        route("/api") {
            post("/login") {
                val request = call.receive<CredentialRequest>()

                val user = transaction {
                    User.find { (AppUsers.username eq request.username) and (AppUsers.password eq hash(request.password)) }.firstOrNull()?.view
                } ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "message" to "Unknown username/password combination!"
                    )
                )

                call.sessions.set(UserId(user.id))

                call.respond(user)
            }

            authenticate("auth") {
                get("/identity") {
                    val user = call.user ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf(
                            "message" to "You need to be logged in to view this page!"
                        )
                    )

                    call.respond(user)
                }
            }
        }
    }
}

val ApplicationCall.userId: UUID?
    get() = authentication.principal<UserId>()?.id

val ApplicationCall.user: UserView?
    get() = transaction { User.findById(userId ?: return@transaction null)?.view }

data class CredentialRequest(val username: String, val password: String)

data class UserId(val id: UUID) : Principal
