package io.paleocrafter.booksearch.auth

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.UUIDTable
import java.util.UUID

object Users : UUIDTable() {
    val username = varchar("username", 255).uniqueIndex()
    val password = varchar("password", 255).index()
    val canManageBooks = bool("can_manage_books")
    val canManageUsers = bool("can_manage_users")
    val hasLoggedIn = bool("has_logged_in").default(false)
    val defaultSearchScope = varchar("default_search_scope", 255).default("paragraphs")
    val groupResultsByDefault = bool("group_results_by_default").default(false)
}

class User(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<User>(Users)

    var username by Users.username
    var password by Users.password
    var canManageBooks by Users.canManageBooks
    var canManageUsers by Users.canManageUsers
    var hasLoggedIn by Users.hasLoggedIn
    var defaultSearchScope by Users.defaultSearchScope
    var groupResultsByDefault by Users.groupResultsByDefault

    val view: UserView
        get() = UserView(id.value, username, canManageBooks, canManageUsers, defaultSearchScope, groupResultsByDefault)
}

data class UserView(
    val id: UUID,
    val username: String,
    val canManageBooks: Boolean,
    val canManageUsers: Boolean,
    val defaultSearchScope: String,
    val groupResultsByDefault: Boolean
)
