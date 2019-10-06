package io.paleocrafter.booksearch.auth

import io.paleocrafter.booksearch.DbMigration
import io.paleocrafter.booksearch.createOrModifyColumns
import io.paleocrafter.booksearch.execAndMap
import org.jetbrains.exposed.sql.BooleanColumnType
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.util.UUID

object CreateAuthTablesMigration : DbMigration("create-auth-tables") {
    override fun apply() {
        SchemaUtils.createMissingTablesAndColumns(Users)
    }
}

object AddSearchSettingsMigration : DbMigration("add-search-settings") {
    override fun apply() {
        Users.createOrModifyColumns(Users.defaultSearchScope, Users.groupResultsByDefault)
    }
}

object CreatePermissionsTablesMigration : DbMigration("create-permissions-tables") {
    override fun apply() {
        SchemaUtils.createMissingTablesAndColumns(Roles, UserRoles, Permissions, RolePermissions)
    }
}

object ConvertFlagsToPermissionsMigration : DbMigration("convert-flags-to-permissions") {
    override fun apply() {
        val manageBooksPermission = Permission.new("books.manage") { description = "Manage books" }
        val manageUsersPermission = Permission.new("users.manage") { description = "Manage users" }

        val adminRole = Role.new(UUID.randomUUID()) {
            name = "Admin"
            permissions = SizedCollection(manageBooksPermission, manageUsersPermission)
        }
        val bookManagerRole = Role.new(UUID.randomUUID()) {
            name = "Book Manager"
            permissions = SizedCollection(manageBooksPermission)
        }
        val userManagerRole = Role.new(UUID.randomUUID()) {
            name = "User Manager"
            permissions = SizedCollection(manageUsersPermission)
        }

        data class OldPermissions(val id: UUID, val canManageBooks: Boolean, val canManageUsers: Boolean)

        val canManageBooksColumn = Column<Boolean>(Users, "can_manage_books", BooleanColumnType())
        val canManageUsersColumn = Column<Boolean>(Users, "can_manage_users", BooleanColumnType())

        "SELECT ${Users.id.name}, ${canManageBooksColumn.name}, ${canManageUsersColumn.name} FROM users"
            .execAndMap {
                OldPermissions(
                    it.getObject(Users.id.name, UUID::class.java),
                    it.getBoolean(canManageBooksColumn.name),
                    it.getBoolean(canManageUsersColumn.name)
                )
            }
            .forEach {
                val user = User.findById(it.id) ?: return@forEach

                user.roles = when {
                    it.canManageBooks && it.canManageUsers -> SizedCollection(adminRole)
                    it.canManageBooks -> SizedCollection(bookManagerRole)
                    it.canManageUsers -> SizedCollection(userManagerRole)
                    else -> SizedCollection()
                }
            }

        with(TransactionManager.current()) {
            val statements = canManageBooksColumn.dropStatement() + canManageUsersColumn.dropStatement()
            for (statement in statements) {
                exec(statement)
            }
        }
    }
}
