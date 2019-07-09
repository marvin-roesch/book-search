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

    ITALIC("Text Modifiers", "italic", "Italic"),
    BOLD("Text Modifiers", "bold", "Bold"),
    RESET("Text Modifiers", "reset", "Reset"),

    FULL_WIDTH_IMAGE("Images", "fullWidthImage", "Full width image"),
    CENTRED_IMAGE("Images", "centeredImage", "Centered image"),

    DIALOG_LINE("Dialog", "dialogLine", "Line (paragraph)"),
    DIALOG_SPEAKER("Dialog", "dialogSpeaker", "Speaker"),
    DIALOG_TEXT("Dialog", "dialogText", "Text"),

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
