package io.paleocrafter.booksearch

import io.paleocrafter.booksearch.auth.AddSearchSettingsMigration
import io.paleocrafter.booksearch.auth.ConvertFlagsToPermissionsMigration
import io.paleocrafter.booksearch.auth.CreateAuthTablesMigration
import io.paleocrafter.booksearch.auth.CreatePermissionsTablesMigration
import io.paleocrafter.booksearch.books.AddCoverMigration
import io.paleocrafter.booksearch.books.AddIndexingIndicatorMigration
import io.paleocrafter.booksearch.books.AddSearchByDefaultMigration
import io.paleocrafter.booksearch.books.AddTagsTableMigration
import io.paleocrafter.booksearch.books.CreateBookTablesMigration
import io.paleocrafter.booksearch.books.CreateCitationsMigration
import io.paleocrafter.booksearch.books.RestrictedBooksMigration
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import java.sql.ResultSet

object DbMigrations : Table() {
    private val logger = LoggerFactory.getLogger("DbMigrations")
    private val versions = mapOf(
        0 to listOf(CreateAuthTablesMigration, CreateBookTablesMigration),
        1 to listOf(AddCoverMigration),
        2 to listOf(AddSearchSettingsMigration),
        3 to listOf(AddIndexingIndicatorMigration),
        4 to listOf(AddTagsTableMigration),
        5 to listOf(CreatePermissionsTablesMigration, ConvertFlagsToPermissionsMigration),
        6 to listOf(RestrictedBooksMigration),
        7 to listOf(CreateCitationsMigration),
        8 to listOf(AddSearchByDefaultMigration)
    ).toSortedMap()

    fun run(db: Database) {
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(ExecutedMigrations)

            val lastVersion = ExecutedMigrations.selectAll().orderBy(ExecutedMigrations.version, SortOrder.DESC).firstOrNull()
                ?.get(ExecutedMigrations.version) ?: 0
            val executed = ExecutedMigrations.select { ExecutedMigrations.version eq lastVersion }.map { it[ExecutedMigrations.name] }
            for ((versionNumber, migrations) in versions.tailMap(lastVersion)) {
                for (migration in migrations) {
                    if (migration.name in executed) {
                        continue
                    }

                    migration.apply()
                    logger.info("Executed migration '${migration.name}' from version $versionNumber")
                    ExecutedMigrations.insert {
                        it[this.version] = versionNumber
                        it[this.name] = migration.name
                        it[this.executedOn] = DateTime.now()
                    }
                }
            }
        }
    }
}

fun Table.createOrModifyColumns(vararg columns: Column<*>) {
    val table = this
    val byName = columns.groupBy { it.name }
    with(TransactionManager.current()) {
        val existing = this.db.dialect.tableColumns(table)[table].orEmpty()
            .filter { byName.containsKey(it.name) }
            .map { it.name }
        val statements = columns.flatMap { if (existing.contains(it.name)) it.modifyStatement() else it.ddl }
        for (statement in statements) {
            exec(statement)
        }
    }
}

fun <T : Any> String.execAndMap(transform: (ResultSet) -> T): List<T> {
    val result = arrayListOf<T>()
    TransactionManager.current().exec(this) { rs ->
        while (rs.next()) {
            result += transform(rs)
        }
    }
    return result
}

private object ExecutedMigrations : Table() {
    val version = integer("version").primaryKey(0)
    val name = varchar("migration", 255).primaryKey(1)
    val executedOn = datetime("executed_on")
}

abstract class DbMigration(val name: String) {
    abstract fun apply()
}
