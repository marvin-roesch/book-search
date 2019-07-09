package io.paleocrafter.booksearch.auth

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
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
import io.ktor.routing.Route
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.patch
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.sessions.Sessions
import io.ktor.sessions.clear
import io.ktor.sessions.cookie
import io.ktor.sessions.directorySessionStorage
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import io.ktor.util.hex
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun Application.auth() {
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
            User.new {
                username = "admin"
                password = hash(defaultPassword)
                canManageBooks = true
                canManageUsers = true
            }

            environment.log.info("Superuser 'admin' was created with the provided default password!")
        }
    }

    install(Sessions) {
        cookie<UserId>("AUTH_SESSION", directorySessionStorage(File(".sessions"), cached = false))
    }

    install(Authentication) {
        session<UserId> {
            challenge = SessionAuthChallenge.Unauthorized
            validate {
                it
            }
        }
    }

    routing {
        route("/api/auth") {
            post("/login") {
                if (call.sessions.get<UserId>() != null) {
                    return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf(
                            "message" to "You're already logged in!"
                        )
                    )
                }

                val request = call.receive<LoginRequest>()

                val (user, hasLoggedIn) = transaction {
                    val dbUser = User.find { (Users.username eq request.username) and (Users.password eq hash(request.password)) }
                        .firstOrNull() ?: return@transaction null

                    dbUser.view to dbUser.hasLoggedIn
                } ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf(
                        "message" to "Unknown username/password combination!"
                    )
                )

                call.sessions.set(UserId(user.id))

                if (!hasLoggedIn) {
                    transaction {
                        User.findById(user.id)?.hasLoggedIn = true
                    }
                }

                call.respond(
                    mapOf(
                        "message" to "You were successfully logged in!",
                        "user" to user,
                        "firstLogin" to !hasLoggedIn
                    )
                )
            }

            authenticate {
                post("/logout") {
                    call.sessions.clear<UserId>()

                    call.respond(
                        mapOf("message" to "You've been successfully logged out!")
                    )
                }

                get("/identity") {
                    val user = call.user ?: return@get call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf(
                            "message" to "You need to be logged in to view this page!"
                        )
                    )

                    call.respond(user)
                }

                patch("/password") {
                    val user = call.user ?: return@patch call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf(
                            "message" to "You need to be logged in to view this page!"
                        )
                    )
                    val request = call.receive<ChangePasswordRequest>()

                    if (transaction { User.findById(user.id)?.password != hash(request.oldPassword) }) {
                        return@patch call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf(
                                "message" to "The provided current password does not match our records!"
                            )
                        )
                    }

                    if (request.newPassword.isEmpty()) {
                        return@patch call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf(
                                "message" to "Your new password must not be empty!"
                            )
                        )
                    }

                    if (request.newPassword != request.newPasswordRepeat) {
                        return@patch call.respond(
                            HttpStatusCode.BadRequest,
                            mapOf(
                                "message" to "The repeated password does not match your new one!"
                            )
                        )
                    }

                    transaction {
                        User.findById(user.id)?.password = hash(request.newPassword)
                    }

                    call.respond(mapOf("message" to "Your password was successfully changed!"))
                }

                patch("/search-settings") {
                    val userId = call.userId ?: return@patch call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf(
                            "message" to "You need to be logged in to view this page!"
                        )
                    )
                    val request = call.receive<ChangeSearchSettingsRequest>()

                    val view = transaction {
                        val user = User.findById(userId) ?: return@transaction null

                        user.defaultSearchScope = request.defaultScope
                        user.groupResultsByDefault = request.groupByDefault

                        user.view
                    } ?: return@patch call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf(
                            "message" to "Your user does not seem to exist!"
                        )
                    )

                    call.respond(
                        mapOf(
                            "message" to "Your search settings were successfully changed!",
                            "identity" to view
                        )
                    )
                }
            }

            authorize({ it.canManageUsers }) {
                route("/users") {
                    get("/") {
                        call.respond(transaction { User.all().map { it.view } })
                    }

                    patch("/{id}") {
                        val id = UUID.fromString(call.parameters["id"])
                        val request = call.receive<PatchUserRequest>()

                        val userName = transaction {
                            val user = User.findById(id) ?: return@transaction null

                            user.canManageBooks = request.canManageBooks
                            user.canManageUsers = request.canManageUsers

                            user.username
                        } ?: return@patch call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("message" to "User with ID '$id' does not exist!")
                        )

                        call.respond(
                            mapOf("message" to "Permissions for user '$userName' were successfully updated!")
                        )
                    }

                    delete("/{id}") {
                        val id = UUID.fromString(call.parameters["id"])

                        if (id == call.userId) {
                            return@delete call.respond(
                                mapOf("message" to "You may not delete your own account!")
                            )
                        }

                        val userName = transaction {
                            val user = User.findById(id) ?: return@transaction null
                            val name = user.username

                            user.delete()

                            name
                        } ?: return@delete call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("message" to "User with ID '$id' does not exist!")
                        )

                        call.respond(
                            mapOf("message" to "User '$userName' was successfully deleted!")
                        )
                    }

                    put("/") {
                        val request = call.receive<CreateUserRequest>()

                        if (request.username.isEmpty() || request.password.isEmpty()) {
                            return@put call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("message" to "Username and password must not be empty!")
                            )
                        }

                        if (transaction { User.find { Users.username eq request.username }.any() }) {
                            return@put call.respond(
                                HttpStatusCode.Conflict,
                                mapOf("message" to "User with name '${request.username}' already exists!")
                            )
                        }

                        val user = transaction {
                            User.new {
                                username = request.username
                                password = hash(request.password)
                                canManageBooks = request.canManageBooks
                                canManageUsers = request.canManageUsers
                            }.view
                        }

                        call.respond(
                            mapOf(
                                "message" to "User '${user.username}' was successfully created!",
                                "user" to user
                            )
                        )
                    }
                }
            }
        }
    }
}

val ApplicationCall.userId: UUID?
    get() = authentication.principal<UserId>()?.id

val ApplicationCall.user: UserView?
    get() = transaction { User.findById(userId ?: return@transaction null)?.view }

fun Route.authorize(check: (UserView) -> Boolean, build: Route.() -> Unit): Route {
    return authenticate {
        intercept(ApplicationCallPipeline.Call) {
            val user = call.user
            if (user != null && !check(user)) {
                call.respond(HttpStatusCode.Forbidden)
                return@intercept finish()
            }
        }

        build()
    }
}

data class LoginRequest(val username: String, val password: String)

data class CreateUserRequest(val username: String, val password: String, val canManageBooks: Boolean, val canManageUsers: Boolean)

data class PatchUserRequest(val canManageBooks: Boolean, val canManageUsers: Boolean)

data class ChangePasswordRequest(val oldPassword: String, val newPassword: String, val newPasswordRepeat: String)

data class ChangeSearchSettingsRequest(val defaultScope: String, val groupByDefault: Boolean)

data class UserId(val id: UUID) : Principal
