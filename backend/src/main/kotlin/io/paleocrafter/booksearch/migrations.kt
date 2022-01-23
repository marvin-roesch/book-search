package io.paleocrafter.booksearch

import io.paleocrafter.booksearch.auth.AddDefaultFilterMigration
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
import io.paleocrafter.booksearch.books.FixBookTagsPrimaryKey
import io.paleocrafter.booksearch.books.RestrictedBooksMigration
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.time.Instant

object DbMigrations : Table() {
    private val logger = LoggerFactory.getLogger("DbMigrations")
    private val initialMigrations = listOf(
        CreateAuthTablesMigration,
        CreateBookTablesMigration,
        AddTagsTableMigration,
        CreatePermissionsTablesMigration
    )
    private val versions = mapOf(
        0 to listOf(CreateAuthTablesMigration, CreateBookTablesMigration),
        1 to listOf(AddCoverMigration),
        2 to listOf(AddSearchSettingsMigration),
        3 to listOf(AddIndexingIndicatorMigration),
        4 to listOf(AddTagsTableMigration),
        5 to listOf(CreatePermissionsTablesMigration, ConvertFlagsToPermissionsMigration),
        6 to listOf(RestrictedBooksMigration),
        7 to listOf(CreateCitationsMigration),
        8 to listOf(AddSearchByDefaultMigration),
        9 to listOf(AddDefaultFilterMigration, FixBookTagsPrimaryKey)
    ).toSortedMap()

    fun run(db: Database) {
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(ExecutedMigrations)

            val lastVersion = ExecutedMigrations.selectAll().orderBy(ExecutedMigrations.version, SortOrder.DESC).firstOrNull()
                ?.get(ExecutedMigrations.version)

            if (lastVersion == null) {
                logger.info("No migrations have been run yet, running initial migrations...")

                for (migration in initialMigrations) {
                    migration.apply()
                    logger.info("Executed initial migration '${migration.name}'")
                }

                logger.info("Marking all migrations as already run...")
                for ((versionNumber, migrations) in versions) {
                    for (migration in migrations) {
                        ExecutedMigrations.insert {
                            it[this.version] = versionNumber
                            it[this.name] = migration.name
                            it[this.executedOn] = Instant.now()
                        }

                        logger.info("Marked migration '${migration.name}' from version $versionNumber as executed")
                    }
                }

                logger.info("Initial database setup completed")

                return@transaction
            }

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
                        it[this.executedOn] = Instant.now()
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
    val version = integer("version")
    val name = varchar("migration", 255)
    val executedOn = timestamp("executed_on")

    override val primaryKey = PrimaryKey(version, name)
}

abstract class DbMigration(val name: String) {
    abstract fun apply()
}
