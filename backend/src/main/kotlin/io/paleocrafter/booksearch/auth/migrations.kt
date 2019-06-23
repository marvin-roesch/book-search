package io.paleocrafter.booksearch.auth

import io.paleocrafter.booksearch.DbMigration
import org.jetbrains.exposed.sql.SchemaUtils

object CreateAuthTablesMigration : DbMigration("create-auth-tables") {
    override fun apply() {
        SchemaUtils.createMissingTablesAndColumns(Users)
    }
}
