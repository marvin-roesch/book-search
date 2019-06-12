package io.paleocrafter.booksearch

import org.jetbrains.exposed.sql.Table

object BookStubs : Table() {
    val id = uuid("id").primaryKey()
    val content = blob("content")
}
