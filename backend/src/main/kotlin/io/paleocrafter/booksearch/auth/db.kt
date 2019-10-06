package io.paleocrafter.booksearch.auth

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IdTable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
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

    val roles by Role via UserRoles

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

object Roles : UUIDTable() {
    val name = varchar("name", 255).uniqueIndex()
}

class Role(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Role>(Roles)

    var name by Roles.name

    val permissions by Permission via RolePermissions
}

object UserRoles : Table() {
    val user = reference("user", Users).primaryKey(0)
    val role = reference("role", Roles).primaryKey(1)
}

object Permissions : IdTable<String>() {
    override val id: Column<EntityID<String>> = varchar("id", 255).primaryKey().entityId()
    val description = Users.varchar("description", 255)
}

class Permission(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, Permission>(Permissions)

    var description by Permissions.description
}

object RolePermissions : Table() {
    val role = reference("role", Roles).primaryKey(0)
    val permission = reference("permission", Permissions).primaryKey(1)
}
