package io.paleocrafter.booksearch.books

import org.jetbrains.exposed.sql.transactions.transaction
import java.util.Optional
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object BookCache {
    private val seriesPaths = ConcurrentHashMap<Optional<String>, Series>()
    private lateinit var seriesCache: List<Series>
    private val bookCache = ConcurrentHashMap<UUID, ResolvedBook>()

    val linearSeries: Collection<Series>
        get() = seriesPaths.values

    val series: List<Series>
        get() = seriesCache

    val books: Collection<ResolvedBook>
        get() = bookCache.values

    fun rebuild() {
        transaction {
            rebuildSeries()
            bookCache.clear()
            seriesCache.flatMap { it.books }.forEach {
                bookCache[it.id] = it
            }
        }
    }

    fun rebuildSeries() {
        seriesPaths.clear()
        for (book in Book.all()) {
            val destinationSeries = buildSeries(book.series)
            destinationSeries?.books?.add(book.resolved)
        }
        seriesCache = seriesPaths.entries
            .sortedWith(compareBy({ it.key.isPresent }, { it.value.name }))
            .map { it.value }
        seriesPaths.values.forEach { it.books.sortBy { b -> b.orderInSeries } }
    }

    private fun buildSeries(path: String?): Series? {
        val seriesHierarchy = path?.split("\\") ?: listOf(null)
        var seriesMap = seriesPaths
        var rebuiltPath: Optional<String>? = null
        for (seriesName in seriesHierarchy) {
            rebuiltPath = if (rebuiltPath === null) Optional.ofNullable(seriesName) else rebuiltPath.map { "$it\\$seriesName" }
            val s = seriesMap.getOrPut(rebuiltPath) {
                Series(rebuiltPath, seriesName ?: "No Series", mutableListOf(), ConcurrentHashMap())
            }
            seriesMap = s.children
        }
        return seriesPaths[Optional.ofNullable(path)]
    }

    fun find(id: UUID): ResolvedBook? {
        return bookCache[id]
    }

    fun updateBook(id: UUID, book: ResolvedBook) {
        val oldBook = bookCache[id]
        bookCache[id] = book
        if (oldBook === null || oldBook.series != book.series) {
            rebuildSeries()
        }
    }

    fun removeBook(id: UUID) {
        bookCache.remove(id)
        rebuildSeries()
    }
}
