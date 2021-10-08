package io.paleocrafter.booksearch.auth

import io.ktor.auth.Principal
import io.paleocrafter.booksearch.jsonb
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.select
import java.util.UUID

object Users : UUIDTable() {
    val username = varchar("username", 255).uniqueIndex()
    val password = varchar("password", 255).index()
    val hasLoggedIn = bool("has_logged_in").default(false)
    val defaultSearchScope = varchar("default_search_scope", 255).default("paragraphs")
    val groupResultsByDefault = bool("group_results_by_default").default(false)
    val defaultFilter = jsonb<User.DefaultFilter>("default_filter").nullable()
}

class User(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<User>(Users)

    data class DefaultFilter(
        val seriesFilter: List<String>?,
        val bookFilter: List<String>?,
        val excluded: List<String>?,
        var knownBooks: Set<String> = emptySet()
    )

    var username by Users.username
    var password by Users.password
    var hasLoggedIn by Users.hasLoggedIn
    var defaultSearchScope by Users.defaultSearchScope
    var groupResultsByDefault by Users.groupResultsByDefault
    var defaultFilter by Users.defaultFilter

    var roles by Role via UserRoles

    val view: UserView
        get() = UserView(
            id.value,
            username,
            defaultSearchScope,
            groupResultsByDefault,
            permissions
        )

    val adminView: AdminUserView
        get() = AdminUserView(
            id.value,
            username,
            defaultSearchScope,
            groupResultsByDefault,
            permissions,
            roles.mapTo(mutableSetOf()) { it.id.value }
        )

    private val permissions: Set<String>
        get() =
            Users.innerJoin(UserRoles, { id }, { user })
                .innerJoin(RolePermissions, { UserRoles.role }, { role })
                .innerJoin(Permissions, { RolePermissions.permission }, { id })
                .slice(Permissions.id)
                .select { Users.id eq id }
                .withDistinct()
                .mapTo(mutableSetOf()) { it[Permissions.id].value }
}

data class UserView(
    val id: UUID,
    val username: String,
    val defaultSearchScope: String,
    val groupResultsByDefault: Boolean,
    val permissions: Set<String>
) : Principal

data class AdminUserView(
    val id: UUID,
    val username: String,
    val defaultSearchScope: String,
    val groupResultsByDefault: Boolean,
    val permissions: Set<String>,
    val roles: Set<UUID>
)

object Roles : UUIDTable() {
    val name = varchar("name", 255).uniqueIndex()
}

class Role(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Role>(Roles)

    var name by Roles.name

    var permissions by Permission via RolePermissions

    val view: Map<String, Any>
        get() = mapOf(
            "id" to id.value,
            "name" to name,
            "permissions" to permissions.map { it.id.value }
        )
}

object UserRoles : Table() {
    val user = reference("user", Users).primaryKey(0)
    val role = reference("role", Roles).primaryKey(1)

    override val primaryKey = PrimaryKey(user, role)
}

object Permissions : IdTable<String>() {
    override val id: Column<EntityID<String>> = varchar("id", 255).entityId()
    val description = varchar("description", 255)

    override val primaryKey = PrimaryKey(id)

    fun deleteById(id: String) {
        RolePermissions.deleteWhere { RolePermissions.permission eq id }
        deleteWhere { Permissions.id eq id }
    }
}

class Permission(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, Permission>(Permissions)

    var description by Permissions.description

    val view: Map<String, Any>
        get() = mapOf(
            "id" to id.value,
            "description" to description
        )
}

object RolePermissions : Table() {
    val role = reference("role", Roles)
    val permission = reference("permission", Permissions)

    override val primaryKey = PrimaryKey(role, permission)
}
