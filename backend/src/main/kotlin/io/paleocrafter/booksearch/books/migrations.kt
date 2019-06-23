package io.paleocrafter.booksearch.books

import io.paleocrafter.booksearch.DbMigration
import nl.siegmann.epublib.epub.EpubReader
import org.jetbrains.exposed.sql.SchemaUtils
import javax.sql.rowset.serial.SerialBlob

object CreateBookTablesMigration : DbMigration("create-book-tables") {
    override fun apply() {
        SchemaUtils.createMissingTablesAndColumns(Books, Chapters, Images, ClassMappings)
    }
}

object AddCoverMigration : DbMigration("add-book-covers") {
    override fun apply() {
        SchemaUtils.createMissingTablesAndColumns(Books)

        val booksMissingCovers = Book.find { Books.cover.isNull() }
        val epubReader = EpubReader()
        for (book in booksMissingCovers) {
            val epub = epubReader.readEpub(book.content.binaryStream)
            epub.coverImage.let {
                book.cover = SerialBlob(it.data)
                book.coverMime = it.mediaType.name
            }
        }
    }
}
