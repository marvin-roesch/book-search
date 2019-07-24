package io.paleocrafter.booksearch.auth

import io.paleocrafter.booksearch.DbMigration
import io.paleocrafter.booksearch.createOrModifyColumns
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager

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
