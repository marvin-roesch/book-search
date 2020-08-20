package io.paleocrafter.booksearch.books

import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object BookCache {
    private val seriesPaths = ConcurrentHashMap<Optional<String>, Series>()
    private lateinit var seriesCache: List<Series>
    private val bookCache = ConcurrentHashMap<UUID, ResolvedBook>()
    private val tagCache = ConcurrentHashMap<String, CachedTag>()

    val linearSeries: Collection<Series>
        get() = seriesPaths.values

    val series: List<Series>
        get() = seriesCache

    val books: Collection<ResolvedBook>
        get() = bookCache.values

    val tags: Map<String, CachedTag>
        get() = tagCache

    fun rebuild() {
        transaction {
            rebuildSeries()
            rebuildTags()
            bookCache.clear()
            seriesPaths.values.flatMap { it.books }.forEach {
                bookCache[it.id] = it
            }
        }
    }

    private fun rebuildSeries() {
        seriesPaths.clear()
        transaction {
            for (book in Book.all()) {
                val destinationSeries = buildSeries(book.series)
                destinationSeries?.books?.add(book.resolved)
            }
        }
        seriesCache = seriesPaths.entries
            .filter { it.value.root }
            .sortedWith(compareBy({ it.key.isPresent }, { it.value.sortableName }))
            .map { it.value }
        seriesPaths.values.forEach { series -> series.books.sortWith(compareBy({ it.orderInSeries }, { it.sortableTitle })) }
    }

    private fun buildSeries(path: String?): Series? {
        val seriesHierarchy = path?.split("\\")?.map { Optional.of(it) } ?: listOf(Optional.empty())
        var seriesMap = seriesPaths
        var rebuiltPath: Optional<String>? = null
        for (seriesName in seriesHierarchy) {
            val root = rebuiltPath === null
            val localPath = if (rebuiltPath === null) seriesName else rebuiltPath.flatMap { p -> seriesName.map { "$p\\$it" } }
            rebuiltPath = localPath

            var series = seriesPaths[rebuiltPath]
            if (series === null) {
                series = Series(rebuiltPath, root, seriesName.orElse("No Series"), mutableListOf(), ConcurrentHashMap())
                seriesMap[seriesName] = series
                seriesPaths[rebuiltPath] = series
            }
            seriesMap = series.children
        }
        return seriesPaths[Optional.ofNullable(path)]
    }

    fun rebuildTags() {
        tagCache.clear()
        transaction {
            for (row in Books.innerJoin(BookTags, { id }, { book }).slice(BookTags.book, BookTags.tag, Books.searchedByDefault).selectAll()) {
                val tag = row[BookTags.tag]
                val book = row[BookTags.book].value
                val cache = tagCache.getOrPut(tag) { CachedTag(emptySet(), emptySet()) }
                tagCache[tag] = if (row[Books.searchedByDefault])
                    cache.copy(default = cache.default + book)
                else
                    cache.copy(optional = cache.optional + book)
            }
        }
    }

    fun find(id: UUID): ResolvedBook? {
        return bookCache[id]
    }

    fun updateBook(book: Book, updateSeries: Boolean = true) {
        val resolved = transaction { book.resolved }
        val oldBook = bookCache[resolved.id]
        bookCache[resolved.id] = resolved

        if (oldBook === null || oldBook.series != resolved.series) {
            if (updateSeries) {
                rebuildSeries()
            }
        } else {
            val series = seriesPaths[Optional.ofNullable(resolved.series)] ?: return
            series.books[series.books.indexOfFirst { it.id == resolved.id }] = resolved
            if (resolved.orderInSeries != oldBook.orderInSeries) {
                series.books.sortWith(compareBy({ it.orderInSeries }, { it.sortableTitle }))
            }
        }

        if (oldBook === null || oldBook.tags != resolved.tags) {
            val oldTags = oldBook?.tags ?: emptySet()
            val obsolete = oldTags - resolved.tags
            val new = resolved.tags - oldTags
            for (tag in obsolete) {
                val cache = tagCache[tag] ?: continue

                if (resolved.searchedByDefault) {
                    val newBooks = cache.default - resolved.id
                    if (cache.optional.isEmpty() && newBooks.isEmpty()) {
                        tagCache.remove(tag)
                    } else {
                        tagCache[tag] = cache.copy(default = newBooks)
                    }
                } else {
                    val newBooks = cache.optional - resolved.id
                    if (cache.default.isEmpty() && newBooks.isEmpty()) {
                        tagCache.remove(tag)
                    } else {
                        tagCache[tag] = cache.copy(optional = newBooks)
                    }
                }
            }

            for (tag in new) {
                val cache = tagCache.getOrPut(tag) { CachedTag(emptySet(), emptySet()) }
                tagCache[tag] = if (resolved.searchedByDefault)
                    cache.copy(default = cache.default + resolved.id)
                else
                    cache.copy(optional = cache.optional + resolved.id)
            }
        }
    }

    fun removeBook(id: UUID) {
        bookCache.remove(id)
        rebuildTags()
        rebuildSeries()
    }

    data class CachedTag(val default: Set<UUID>, val optional: Set<UUID>)
}
