package io.paleocrafter.booksearch

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IdTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import java.util.UUID

object Books : IdTable<UUID>() {
    override val id = uuid("id").primaryKey().entityId()
    val content = blob("content")
    val title = varchar("title", 255)
    val author = varchar("author", 255)
}

class Book(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, Book>(Books)

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
