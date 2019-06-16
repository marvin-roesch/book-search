package io.paleocrafter.booksearch

import nl.siegmann.epublib.domain.Resources
import nl.siegmann.epublib.domain.TOCReference
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node

class BookIndexer {
    fun index(resources: Resources, content: List<TOCReference>, classMappings: Map<String, BookStyle>) {
        val entries = content.flatMap { this.collectEntries(resources, it, classMappings) }
    }

    private fun collectEntries(resources: Resources, reference: TOCReference, classMappings: Map<String, BookStyle>): List<IndexEntry> {
        val content = Jsoup.parse(String(reference.resource.data)).body()
        content.stripStyles()
        content.stripLinks()
        content.mapClasses(classMappings)

        val paragraphs = content.select("p")

        return paragraphs.map { IndexEntry(it.html(), it.classNames()) }
    }

    private fun Element.stripStyles() {
        for (elem in this.allElements) {
            elem.removeAttr("style")
        }
    }

    private fun Element.stripLinks() {
        val links = this.select("a")
        for (link in links) {
            link.replaceWith(link.childNodes())
        }
    }

    private fun Element.mapClasses(classMappings: Map<String, BookStyle>) {
        for (elem in this.allElements) {
            for (cls in elem.classNames()) {
                val style = classMappings[cls] ?: BookStyle.STRIP_CLASS
                elem.removeClass(cls)
                if (style == BookStyle.STRIP_ELEMENT) {
                    elem.remove()
                    break
                } else if (style != BookStyle.STRIP_CLASS) {
                    elem.addClass(style.id)
                }
            }
        }
    }

    private fun Node.replaceWith(nodes: Iterable<Node>) {
        nodes.fold(this) { last, node ->
            last.after(node)
            node
        }
        this.remove()
    }

    private data class IndexEntry(val text: String, val classes: Set<String>) {

    }
}
