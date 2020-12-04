package io.paleocrafter.booksearch.books

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
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
    val restricted = bool("restricted").default(false)
    val citationTemplate = varchar("citation_template", 255).nullable()
    val searchedByDefault = bool("searched_by_default").default(true)
}

class Book(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Book>(Books) {
        fun readingPermission(id: UUID) = "books.$id.read"
    }

    var content by Books.content
    var title by Books.title
    var author by Books.author
    var series by Books.series
    var orderInSeries by Books.orderInSeries
    var searchable by Books.searchable
    var cover by Books.cover
    var coverMime by Books.coverMime
    var indexing by Books.indexing
    var restricted by Books.restricted
    var citationTemplate by Books.citationTemplate
    var searchedByDefault by Books.searchedByDefault

    val resolved: ResolvedBook
        get() = ResolvedBook(
            id.value,
            title,
            author,
            series,
            orderInSeries,
            searchable,
            indexing,
            restricted,
            BookTags.select { BookTags.book eq id }.mapTo(mutableSetOf()) { it[BookTags.tag] },
            citationTemplate,
            searchedByDefault
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
    val restricted: Boolean,
    val tags: Set<String>,
    val citationTemplate: String?,
    val searchedByDefault: Boolean
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
            "indexing" to indexing,
            "restricted" to restricted,
            "citationTemplate" to citationTemplate,
            "searchedByDefault" to searchedByDefault
        )
}

object BookTags : Table() {
    val book = reference("book", Books)
    val tag = varchar("tag", 255)

    override val primaryKey = PrimaryKey(book, tag)
}

object Chapters : UUIDTable() {
    val book = reference("book", Books).index()
    val tocReference = varchar("toc_reference", 255)
    val title = varchar("title", 255)
    val content = text("content")
    val indexedContent = text("indexed_content").nullable()
    val position = integer("position")
    val citationParameter = varchar("citation_parameter", 255).nullable()
}

class Chapter(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Chapter>(Chapters)

    var book by Book referencedOn Chapters.book
    var title by Chapters.title
    var tocReference by Chapters.tocReference
    var content by Chapters.content
    var indexedContent by Chapters.indexedContent
    var position by Chapters.position
    var citationParameter by Chapters.citationParameter

    val previous: Chapter?
        get() = find {
            (Chapters.book eq book.id) and (Chapters.position eq (position - 1))
        }.limit(1).firstOrNull()

    val next: Chapter?
        get() = find {
            (Chapters.book eq book.id) and (Chapters.position eq (position + 1))
        }.limit(1).firstOrNull()

    fun toJson() =
        mapOf(
            "id" to id.value,
            "title" to title,
            "citationParameter" to citationParameter
        )
}

data class ResolvedChapter(val id: UUID, val bookId: UUID, val title: String, val position: Int, val content: Element)

object Images : Table() {
    val book = reference("book", Books)
    val name = varchar("name", 255)
    val data = blob("data")

    override val primaryKey = PrimaryKey(book, name)
}

object ClassMappings : Table() {
    val book = reference("book", Books)
    val className = varchar("class_name", 255)
    val mapping = varchar("mapping", 255)

    override val primaryKey = PrimaryKey(book, className)
}
