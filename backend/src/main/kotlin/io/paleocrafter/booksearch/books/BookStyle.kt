package io.paleocrafter.booksearch.books

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class BookStyle(val group: String, val id: String, val description: String) {
    LEVEL_1_HEADING("Text Style", "h1", "Level 1 heading"),
    LEVEL_2_HEADING("Text Style", "h2", "Level 2 heading"),
    LEVEL_3_HEADING("Text Style", "h3", "Level 3 heading"),
    CHAPTER_TEXT("Text Style", "chapterText", "Chapter text"),
    EPIGRAPH_TEXT("Text Style", "epigraphText", "Epigraph text"),
    EPIGRAPH_CITATION("Text Style", "epigraphCitation", "Epigraph citation"),
    EMBED("Text Style", "embed", "Embed (in-world books, letters etc.)"),
    FOOTNOTE("Text Style", "footnote", "Footnote"),

    ITALIC("Text Modifiers", "italic", "Italic"),
    BOLD("Text Modifiers", "bold", "Bold"),
    RESET("Text Modifiers", "reset", "Reset"),

    FULL_WIDTH_IMAGE("Images", "fullWidthImage", "Full width image"),
    CENTRED_IMAGE("Images", "centeredImage", "Centered image"),

    GRAPHIC_NOVEL_LINE("Graphic Novel/Comic", "graphicNovelLine", "Spoken line (paragraph)"),
    GRAPHIC_NOVEL_THOUGHT("Graphic Novel/Comic", "graphicNovelThought", "Thought (paragraph)"),
    GRAPHIC_NOVEL_SFX("Graphic Novel/Comic", "graphicNovelSfx", "(Sound) Effect"),
    GRAPHIC_NOVEL_SPEAKER("Graphic Novel/Comic", "graphicNovelSpeaker", "Speaker"),
    GRAPHIC_NOVEL_TEXT("Graphic Novel/Comic", "graphicNovelText", "Text"),

    STRIP_CLASS("Remove", "stripClass", "Strip class from elements"),
    STRIP_ELEMENT("Remove", "stripElement", "Discard elements with class");

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromJson(id: String): BookStyle? {
            return values().find { it.id == id }
        }
    }
}
