package io.paleocrafter.booksearch

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.UUIDTable
import org.jetbrains.exposed.sql.Table
import java.util.UUID

object Books : UUIDTable() {
    val content = blob("content")
    val title = varchar("title", 255)
    val author = varchar("author", 255)
}

class Book(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Book>(Books)

    var content by Books.content
    var title by Books.title
    var author by Books.author
}

object TableOfContentEntries : IntIdTable() {
    val book = reference("book", Books).index()
    val tocReference = varchar("toc_reference", 255)
}

class TableOfContentEntry(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TableOfContentEntry>(TableOfContentEntries)

    var book by Book referencedOn TableOfContentEntries.book
    var tocReference by TableOfContentEntries.tocReference
}

object Images : Table() {
    val book = reference("book", Books).primaryKey(0)
    val name = varchar("name", 255).primaryKey(1)
    val data = blob("data")
}
