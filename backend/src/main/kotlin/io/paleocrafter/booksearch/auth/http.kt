package io.paleocrafter.booksearch.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.Principal
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
import io.ktor.util.AttributeKey
import io.ktor.util.hex
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun Application.auth() {
    val keyString = environment.config.propertyOrNull("crypto.key")?.getString()
        ?: throw IllegalStateException("'crypto.key' must be configured! Either specify it in config file or pass CRYPTO_KEY env variable.")
    val oldHashKey = SecretKeySpec(hex(keyString), "HmacSHA1")
    val hasher = BCrypt.with(BCrypt.Version.VERSION_2A, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A))
    val verifier = BCrypt.verifyer(BCrypt.Version.VERSION_2A, LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A))

    fun isOldHash(hash: String) = !hash.startsWith("$2a$")

    fun verifyOld(password: String, stored: String): Boolean {
        val hmac = Mac.getInstance("HmacSHA1")
        hmac.init(oldHashKey)
        return hex(hmac.doFinal(password.toByteArray(Charsets.UTF_8))) == stored
    }

    fun preparePassword(password: String) = "$keyString.$password"

    fun hash(password: String) =
        hasher.hashToString(12, preparePassword(password).toCharArray())

    fun verify(password: String, stored: String) =
        if (isOldHash(stored))
            verifyOld(password, stored)
        else
            verifier.verify(preparePassword(password).toCharArray(), stored.toCharArray()).verified

    transaction {
        if (User.all().none()) {
            val defaultPassword = environment.config.propertyOrNull("auth.admin-password")?.getString()
                ?: throw IllegalStateException("There are no users yet and no default admin password is set! " +
                    "Either specify it as 'auth.admin-password' in config file or pass DEFAULT_PASSWORD env variable.")

            val manageBooksPermission = Permission.new("books.manage") { description = "Manage books" }
            val manageUsersPermission = Permission.new("users.manage") { description = "Manage users" }

            val adminRole = Role.new(UUID.randomUUID()) {
                name = "Admin"
                permissions = SizedCollection(manageBooksPermission, manageUsersPermission)
            }

            User.new {
                username = "admin"
                password = hash(defaultPassword)
                roles = SizedCollection(adminRole)
            }

            environment.log.info("Superuser 'admin' was created with the provided default password!")
        }
    }

    install(Sessions) {
        cookie<UserId>("AUTH_SESSION", directorySessionStorage(File(".sessions"), cached = false))
    }

    install(Authentication) {
        session<UserId> {
            challenge {
                call.respond(HttpStatusCode.Unauthorized)
            }
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
                    val dbUser = User.find { (Users.username eq request.username) }.firstOrNull() ?: return@transaction null

                    if (!verify(request.password, dbUser.password)) {
                        return@transaction null
                    }

                    if (isOldHash(dbUser.password)) {
                        dbUser.password = hash(request.password)
                        environment.log.info("User ${dbUser.username} was migrated to new password hash!")
                    }

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
                    call.respond(call.user)
                }

                patch("/password") {
                    val user = call.user
                    val request = call.receive<ChangePasswordRequest>()

                    if (
                        transaction {
                            !verify(request.oldPassword, User.findById(user.id)?.password ?: return@transaction true)
                        }
                    ) {
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
                    val userId = call.userId
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

            get("/roles") {
                call.respond(transaction { Role.all().map { it.view } })
            }

            requirePermissions("users.manage") {
                route("/roles") {
                    patch("/{id}") {
                        val id = UUID.fromString(call.parameters["id"])
                        val request = call.receive<PatchRoleRequest>()

                        val roleName = transaction {
                            val role = Role.findById(id) ?: return@transaction null

                            role.permissions = Permission.find { Permissions.id inList request.permissions }

                            role.name
                        } ?: return@patch call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("message" to "Role with ID '$id' does not exist!")
                        )

                        call.respond(
                            mapOf("message" to "Permissions for role '$roleName' were successfully updated!")
                        )
                    }

                    delete("/{id}") {
                        val id = UUID.fromString(call.parameters["id"])

                        val roleName = transaction {
                            val role = Role.findById(id) ?: return@transaction null
                            val name = role.name

                            UserRoles.deleteWhere { UserRoles.role eq role.id }
                            RolePermissions.deleteWhere { RolePermissions.role eq role.id }
                            role.delete()

                            name
                        } ?: return@delete call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("message" to "Role with ID '$id' does not exist!")
                        )

                        call.respond(
                            mapOf("message" to "Role '$roleName' was successfully deleted!")
                        )
                    }

                    put("/") {
                        val request = call.receive<CreateRoleRequest>()

                        if (request.name.isEmpty()) {
                            return@put call.respond(
                                HttpStatusCode.BadRequest,
                                mapOf("message" to "Role name must not be empty!")
                            )
                        }

                        if (transaction { Role.find { Roles.name eq request.name }.any() }) {
                            return@put call.respond(
                                HttpStatusCode.Conflict,
                                mapOf("message" to "Role with name '${request.name}' already exists!")
                            )
                        }

                        val role = transaction {
                            Role.new {
                                name = request.name
                                permissions = Permission.find { Permissions.id inList request.initialPermissions }
                            }.view
                        }

                        call.respond(
                            mapOf(
                                "message" to "Role '${request.name}' was successfully created!",
                                "role" to role
                            )
                        )
                    }
                }

                get("/permissions") {
                    call.respond(transaction { Permission.all().map { it.view } })
                }

                route("/users") {
                    get("/") {
                        call.respond(transaction { User.all().map { it.adminView } })
                    }

                    patch("/{id}") {
                        val id = UUID.fromString(call.parameters["id"])
                        val request = call.receive<PatchUserRequest>()

                        val userName = transaction {
                            val user = User.findById(id) ?: return@transaction null

                            user.roles = Role.find { Roles.id inList request.roles }

                            user.username
                        } ?: return@patch call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("message" to "User with ID '$id' does not exist!")
                        )

                        environment.log.info("User ${userName} was updated!")

                        call.respond(
                            mapOf("message" to "Roles for user '$userName' were successfully updated!")
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

                            UserRoles.deleteWhere { UserRoles.user eq user.id }
                            user.delete()

                            name
                        } ?: return@delete call.respond(
                            HttpStatusCode.NotFound,
                            mapOf("message" to "User with ID '$id' does not exist!")
                        )

                        environment.log.info("User ${userName} was deleted!")

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
                                roles = Role.find { Roles.id inList request.initialRoles }
                            }.adminView
                        }

                        environment.log.info("New user ${request.username} was created!")

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

val ApplicationCall.userId: UUID
    get() = authentication.principal<UserId>()?.id ?: throw RuntimeException("Cannot retrieve user ID from request")

val USER_KEY = AttributeKey<UserView>("User")

val ApplicationCall.user: UserView
    get() = this.attributes.computeIfAbsent(USER_KEY) {
        transaction {
            User.findById(userId)?.view ?: throw RuntimeException("Cannot find user $userId")
        }
    }

fun Route.authorize(check: ApplicationCall.(UserView) -> Boolean, build: Route.() -> Unit): Route {
    return authenticate {
        intercept(ApplicationCallPipeline.Call) {
            if (!call.check(call.user)) {
                call.respond(HttpStatusCode.Forbidden)
                return@intercept finish()
            }
        }

        build()
    }
}

fun Route.requirePermissions(vararg permissions: String, build: Route.() -> Unit): Route {
    return authorize(
        { user -> permissions.all { it in user.permissions } },
        build
    )
}

data class LoginRequest(val username: String, val password: String)

data class CreateRoleRequest(val name: String, val initialPermissions: List<String>)

data class PatchRoleRequest(val permissions: List<String>)

data class CreateUserRequest(val username: String, val password: String, val initialRoles: List<UUID>)

data class PatchUserRequest(val roles: List<UUID>)

data class ChangePasswordRequest(val oldPassword: String, val newPassword: String, val newPasswordRepeat: String)

data class ChangeSearchSettingsRequest(val defaultScope: String, val groupByDefault: Boolean)

data class UserId(val id: UUID) : Principal
