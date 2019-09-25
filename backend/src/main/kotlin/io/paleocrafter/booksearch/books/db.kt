package io.paleocrafter.booksearch.books

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jsoup.nodes.Element
import java.util.UUID

object Books : UUIDTable() {
    val content = blob("content")
    val title = varchar("title", 255)
    val author = varchar("author", 255)
    val series = varchar("series", 255).nullable()
    val orderInSeries = integer("order_in_series").default(0)
    val searchable = bool("searchable").default(false)
    val cover = blob("cover").nullable()
    val coverMime = varchar("cover_mime_type", 255).nullable()
    val indexing = bool("indexing").default(false)
}

class Book(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Book>(Books)

    var content by Books.content
    var title by Books.title
    var author by Books.author
    var series by Books.series
    var orderInSeries by Books.orderInSeries
    var searchable by Books.searchable
    var cover by Books.cover
    var coverMime by Books.coverMime
    var indexing by Books.indexing
    val tags by BookTag referrersOn BookTags.book

    val resolved: ResolvedBook
        get() = ResolvedBook(
            id.value,
            title,
            author,
            series,
            orderInSeries,
            searchable,
            indexing,
            tags.mapTo(mutableSetOf()) { it.tag }
        )
}

data class ResolvedBook(
    val id: UUID,
    val title: String,
    val author: String,
    val series: String?,
    val orderInSeries: Int,
    val searchable: Boolean,
    val indexing: Boolean,
    val tags: Set<String>
) {
    val sortableTitle = title.sortable

    fun toJson() =
        mapOf(
            "id" to id,
            "title" to title,
            "author" to author,
            "series" to series,
            "orderInSeries" to orderInSeries,
            "searchable" to searchable,
            "indexing" to indexing
        )
}

object BookTags : UUIDTable() {
    val book = reference("book", Books).index().primaryKey(0)
    val tag = varchar("tag", 255)
}

class BookTag(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<BookTag>(BookTags)

    var book by Book referencedOn BookTags.book
    var tag by BookTags.tag
}

object Chapters : UUIDTable() {
    val book = reference("book", Books).index()
    val tocReference = varchar("toc_reference", 255)
    val title = varchar("title", 255)
    val content = text("content")
    val indexedContent = text("indexed_content").nullable()
    val position = integer("position")
}

class Chapter(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Chapter>(Chapters)

    var book by Book referencedOn Chapters.book
    var title by Chapters.title
    var tocReference by Chapters.tocReference
    var content by Chapters.content
    var indexedContent by Chapters.indexedContent
    var position by Chapters.position

    val previous: Chapter?
        get() = Chapter.find {
            (Chapters.book eq book.id) and (Chapters.position eq (position - 1))
        }.limit(1).firstOrNull()

    val next: Chapter?
        get() = Chapter.find {
            (Chapters.book eq book.id) and (Chapters.position eq (position + 1))
        }.limit(1).firstOrNull()

    fun toJson() =
        mapOf(
            "id" to id.value,
            "title" to title
        )
}

data class ResolvedChapter(val id: UUID, val bookId: UUID, val title: String, val position: Int, val content: Element)

object Images : Table() {
    val book = reference("book", Books).primaryKey(0)
    val name = varchar("name", 255).primaryKey(1)
    val data = blob("data")
}

object ClassMappings : Table() {
    val book = reference("book", Books).primaryKey(0)
    val className = varchar("class_name", 255).primaryKey(1)
    val mapping = varchar("mapping", 255)
}
