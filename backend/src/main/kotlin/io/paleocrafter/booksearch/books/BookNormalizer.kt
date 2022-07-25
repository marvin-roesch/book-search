package io.paleocrafter.booksearch.books

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node

object BookNormalizer {
    fun normalize(chapter: ResolvedChapter, classMappings: Map<String, BookStyle>) {
        chapter.content.stripStyles()
        chapter.content.stripLinks()
        chapter.content.mapClasses(classMappings)
        chapter.content.enrichFootnotes()
    }

    private fun Element.enrichFootnotes() {
        select("[data-footnotes='true']").addClass("footnotes")
    }

    private fun Element.stripStyles() {
        for (elem in this.allElements) {
            elem.removeAttr("style")
        }
    }

    private fun Element.stripLinks() {
        val links = this.select("a")
        val iterator = links.iterator()
        while (iterator.hasNext()) {
            val link = iterator.next()

            if (link.attr("data-footnote") == "true" || link.attr("data-footnote-back") == "true") {
                continue
            }

            link.replaceWith(link.childNodes().toList())
            iterator.remove()
        }
    }

    private fun Element.mapClasses(classMappings: Map<String, BookStyle>) {
        val currentElements = this.allElements.toList()
        for (elem in currentElements) {
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
        remove()
    }
}
