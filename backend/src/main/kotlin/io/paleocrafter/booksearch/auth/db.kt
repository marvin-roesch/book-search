package io.paleocrafter.booksearch.auth

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.UUIDTable
import java.util.UUID

object AppUsers : UUIDTable() {
    val username = varchar("username", 255).uniqueIndex()
    val password = varchar("password", 255).index()
    val canManageBooks = bool("can_manage_books")
    val canManageUsers = bool("can_manage_users")
    val hasLoggedIn = bool("has_logged_in").default(false)
}

class User(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<User>(AppUsers)

    var username by AppUsers.username
    var canManageBooks by AppUsers.canManageBooks
    var canManageUsers by AppUsers.canManageUsers
    var hasLoggedIn by AppUsers.hasLoggedIn

    val view: UserView
        get() = UserView(id.value, username, canManageBooks, canManageUsers)
}

data class UserView(val id: UUID, val username: String, val canManageBooks: Boolean, val canManageUsers: Boolean)
