package io.paleocrafter.booksearch.auth

import io.paleocrafter.booksearch.DbMigration
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager

object CreateAuthTablesMigration : DbMigration("create-auth-tables") {
    override fun apply() {
        SchemaUtils.createMissingTablesAndColumns(Users)
    }
}

object AddSearchSettingsMigration : DbMigration("add-search-settings") {
    override fun apply() {
        with(TransactionManager.current()) {
            val statements = Users.defaultSearchScope.ddl + Users.groupResultsByDefault.ddl
            for (statement in statements) {
                exec(statement)
            }
        }
    }
}
