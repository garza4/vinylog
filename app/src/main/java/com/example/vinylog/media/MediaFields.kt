package com.example.vinylog.media

import com.example.vinylog.games.isGameSystem
import com.example.vinylog.objects.Album

enum class MediaKind { Music, Movie, Book, Game, Show }

fun mediaKind(mediaType: String): MediaKind = when {
    mediaType == "Books" -> MediaKind.Book
    mediaType == "Movies" -> MediaKind.Movie
    mediaType == "TV Shows" -> MediaKind.Show
    isGameSystem(mediaType) -> MediaKind.Game
    else -> MediaKind.Music
}

data class FieldSpec(
    val key: String,
    val label: String,
    val multiline: Boolean = false,
)

fun entryFields(mediaType: String): List<FieldSpec> = when (mediaKind(mediaType)) {
    MediaKind.Music -> listOf(
        FieldSpec("title", "Title"),
        FieldSpec("artist", "Artist"),
        FieldSpec("label", "Label"),
        FieldSpec("year", "Release year"),
        FieldSpec("genre", "Genre"),
        FieldSpec("tracks", "Tracks", multiline = true),
        FieldSpec("barcode", "Barcode"),
        FieldSpec("cover", "Cover URL"),
    )
    MediaKind.Movie -> listOf(
        FieldSpec("title", "Title"),
        FieldSpec("director", "Director"),
        FieldSpec("studio", "Studio"),
        FieldSpec("year", "Release year"),
        FieldSpec("genre", "Genre"),
        FieldSpec("cast", "Cast", multiline = true),
        FieldSpec("runtime", "Runtime"),
        FieldSpec("format", "Format"),
        FieldSpec("barcode", "Barcode"),
        FieldSpec("cover", "Poster URL"),
    )
    MediaKind.Book -> listOf(
        FieldSpec("title", "Title"),
        FieldSpec("author", "Author"),
        FieldSpec("publisher", "Publisher"),
        FieldSpec("year", "Publication year"),
        FieldSpec("genre", "Genre"),
        FieldSpec("pages", "Pages"),
        FieldSpec("format", "Format"),
        FieldSpec("barcode", "ISBN"),
        FieldSpec("cover", "Cover URL"),
    )
    MediaKind.Game -> listOf(
        FieldSpec("title", "Title"),
        FieldSpec("developer", "Developer"),
        FieldSpec("publisher", "Publisher"),
        FieldSpec("year", "Release year"),
        FieldSpec("genre", "Genre"),
        FieldSpec("rating", "Rating"),
        FieldSpec("players", "Players"),
        FieldSpec("barcode", "Barcode"),
        FieldSpec("cover", "Cover URL"),
    )
    MediaKind.Show -> listOf(
        FieldSpec("title", "Title"),
        FieldSpec("creator", "Creator"),
        FieldSpec("network", "Network"),
        FieldSpec("year", "First aired"),
        FieldSpec("genre", "Genre"),
        FieldSpec("seasons", "Seasons"),
        FieldSpec("episodes", "Episodes"),
        FieldSpec("cast", "Cast", multiline = true),
        FieldSpec("format", "Format"),
        FieldSpec("barcode", "Barcode"),
        FieldSpec("cover", "Cover URL"),
    )
}

fun creditLabel(mediaType: String): String {
    return entryFields(mediaType).first { it.key in CREDIT_KEYS }.label
}

fun Album.valueOf(key: String): String = when (key) {
    "title" -> albumName.orEmpty()
    "artist", "director", "author", "developer", "creator" -> artistName.orEmpty()
    "label", "studio", "publisher", "network" -> recordLabel.orEmpty()
    "year" -> yearPublished.orEmpty()
    "genre" -> genre.orEmpty()
    "tracks" -> songsOnRecord.orEmpty()
    "cast" -> castNames.orEmpty()
    "runtime" -> runtime.orEmpty()
    "pages", "seasons" -> pageCount.orEmpty()
    "format" -> formatName.orEmpty()
    "rating" -> contentRating.orEmpty()
    "players", "episodes" -> playerCount.orEmpty()
    "barcode" -> barcode.orEmpty()
    "cover" -> coverUrl.orEmpty()
    else -> ""
}

private val CREDIT_KEYS = setOf("artist", "director", "author", "developer", "creator")
