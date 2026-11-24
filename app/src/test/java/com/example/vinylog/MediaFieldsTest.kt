package com.example.vinylog

import com.example.vinylog.media.entryFields
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaFieldsTest {
    @Test
    fun entryFields_matchTheLibrary() {
        assertEquals(
            listOf("Title", "Artist", "Label", "Release year", "Genre", "Tracks", "Barcode", "Cover URL"),
            entryFields("Vinyl").map { it.label },
        )
        assertEquals(entryFields("Vinyl").map { it.label }, entryFields("CDs").map { it.label })
        assertEquals(
            listOf("Title", "Director", "Studio", "Release year", "Genre", "Cast", "Runtime", "Format", "Barcode", "Poster URL"),
            entryFields("Movies").map { it.label },
        )
        assertEquals(
            listOf("Title", "Author", "Publisher", "Publication year", "Genre", "Pages", "Format", "ISBN", "Cover URL"),
            entryFields("Books").map { it.label },
        )
        assertEquals(
            listOf("Title", "Developer", "Publisher", "Release year", "Genre", "Rating", "Players", "Barcode", "Cover URL"),
            entryFields("Nintendo Switch").map { it.label },
        )
        assertEquals(
            listOf(
                "Title",
                "Creator",
                "Network",
                "First aired",
                "Genre",
                "Seasons",
                "Episodes",
                "Cast",
                "Format",
                "Barcode",
                "Cover URL",
            ),
            entryFields("TV Shows").map { it.label },
        )
    }
}
