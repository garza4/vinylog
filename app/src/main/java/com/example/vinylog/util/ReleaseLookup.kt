package com.example.vinylog.util

import com.example.vinylog.games.isGameSystem
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class ScannedRelease(
    val album: String,
    val artist: String,
    val songs: String,
    val year: String,
    val genre: String,
    val barcode: String,
    val coverUrl: String = "",
    val label: String = "",
    val catalogId: String = "",
    val hint: String = "",
)

sealed class LookupResult {
    data class Found(val release: ScannedRelease) : LookupResult()
    data object NotFound : LookupResult()
    data class Failed(val message: String) : LookupResult()
}

sealed class SearchResult {
    data class Hits(val releases: List<ScannedRelease>) : SearchResult()
    data object None : SearchResult()
    data class Failed(val message: String) : SearchResult()
}

/**
 * Looks a scanned retail barcode up in MusicBrainz, then UPC Item DB.
 * A product-catalog hit that has no tracks is searched again by album and artist.
 * Calls run on the caller's thread. Call this off the main thread.
 */
class ReleaseLookup(
    private val fetch: (String) -> String = ::httpGet,
) {
    fun lookup(rawBarcode: String): LookupResult {
        val stored = rawBarcode.trim()
        val candidates = barcodeCandidates(stored)
        if (candidates.isEmpty()) return LookupResult.NotFound

        var sawResponse = false
        var sawError = false
        var found: ScannedRelease? = null
        for (code in candidates) {
            val searchJson = try {
                fetch(searchUrl(code))
            } catch (exception: Exception) {
                sawError = true
                continue
            }
            sawResponse = true
            val id = parseReleaseId(searchJson) ?: continue
            val detailJson = try {
                fetch(releaseUrl(id))
            } catch (exception: Exception) {
                found = parseSearchHit(searchJson, stored)
                sawError = found == null
                break
            }
            found = parseRelease(detailJson, stored) ?: parseSearchHit(searchJson, stored)
            if (found != null) break
        }

        val digits = stored.filter { it.isDigit() }
        if (digits.length >= 8 && isThin(found)) {
            try {
                val upc = parseUpcItem(fetch(upcUrl(digits)), stored)
                if (upc != null) {
                    found = mergeReleases(found, upc)
                    sawResponse = true
                }
            } catch (exception: Exception) {
                sawError = true
            }
        }

        if (isThin(found)) {
            val byTitle = searchByTitle(found, stored)
            if (byTitle != null) {
                found = mergeReleases(byTitle, found)
                sawResponse = true
            } else if (found != null) {
                sawError = true
            }
        }

        if (found != null && found.album.isNotBlank() &&
            (found.songs.isBlank() || found.year.isBlank() || found.coverUrl.isBlank())
        ) {
            val extra = itunesFill(found)
            if (extra != null) {
                val merged = mergeReleases(found, extra) ?: found
                val keepCatalogCover = found.coverUrl.contains("coverartarchive.org")
                found = if (!keepCatalogCover && extra.coverUrl.isNotBlank()) {
                    merged.copy(coverUrl = extra.coverUrl)
                } else {
                    merged
                }
            }
        }

        val release = found
        return when {
            release != null && (release.album.isNotBlank() || release.artist.isNotBlank()) ->
                LookupResult.Found(release)
            sawResponse -> LookupResult.NotFound
            sawError -> LookupResult.Failed("Couldn't reach the catalog.")
            else -> LookupResult.NotFound
        }
    }

    fun search(title: String, artist: String, mediaType: String, barcode: String): SearchResult {
        val cleanedTitle = title.trim()
        val cleanedArtist = artist.trim()
        if (cleanedTitle.isBlank() && cleanedArtist.isBlank() && barcode.isBlank()) {
            return SearchResult.None
        }
        return when {
            mediaType == "Books" -> searchBooks(cleanedTitle, cleanedArtist, barcode)
            mediaType == "Movies" -> searchWikipedia(cleanedTitle, cleanedArtist, barcode, "film")
            isGameSystem(mediaType) -> searchWikipedia(cleanedTitle, cleanedArtist, barcode, "video game")
            else -> searchMusic(cleanedTitle, cleanedArtist, barcode)
        }
    }

    fun loadChoice(release: ScannedRelease): ScannedRelease {
        val id = release.catalogId
        return try {
            when {
                id.startsWith("mb:") -> {
                    val detailed = parseRelease(fetch(releaseUrl(id.removePrefix("mb:"))), release.barcode)
                    mergeReleases(detailed, release) ?: release
                }
                id.startsWith("itunes:") && release.songs.isBlank() -> {
                    val tracks = parseItunesTracks(fetch(itunesLookupUrl(id.removePrefix("itunes:"))))
                    if (tracks.isBlank()) release else release.copy(songs = tracks)
                }
                else -> release
            }
        } catch (cancelled: java.util.concurrent.CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            release
        }
    }

    private fun searchMusic(title: String, artist: String, barcode: String): SearchResult {
        var sawResponse = false
        var sawError = false
        val fromMusicBrainz = mutableListOf<ScannedRelease>()
        val fromItunes = mutableListOf<ScannedRelease>()
        val queryUrl = musicQueryUrl(title, artist)
        if (queryUrl != null) {
            try {
                fromMusicBrainz += parseSearchHits(fetch(queryUrl), barcode)
                sawResponse = true
            } catch (exception: Exception) {
                sawError = true
            }
        }
        val term = listOf(artist, title).filter { it.isNotBlank() }.joinToString(" ")
        if (term.isNotBlank()) {
            try {
                fromItunes += parseItunesAlbums(fetch(itunesSearchUrl(term, 8)))
                sawResponse = true
            } catch (exception: Exception) {
                sawError = true
            }
        }
        var hits = combineMusicHits(fromMusicBrainz, fromItunes)
        if (hits.isEmpty()) {
            val code = barcodeCandidates(barcode).firstOrNull()
            if (code != null && barcode.filter { it.isDigit() }.length >= 8) {
                try {
                    hits = parseSearchHits(fetch(searchUrl(code)), barcode)
                    sawResponse = true
                } catch (exception: Exception) {
                    sawError = true
                }
            }
        }
        return finishSearch(hits, sawResponse, sawError)
    }

    private fun searchBooks(title: String, artist: String, barcode: String): SearchResult {
        var sawResponse = false
        var sawError = false
        val hits = mutableListOf<ScannedRelease>()
        if (title.isNotBlank() || artist.isNotBlank()) {
            try {
                hits += parseOpenLibrary(fetch(openLibraryUrl(title, artist)), barcode)
                sawResponse = true
            } catch (exception: Exception) {
                sawError = true
            }
        }
        val isbn = isbnCandidate(barcode)
        if (isbn != null) {
            try {
                hits += parseOpenLibrary(fetch(openLibraryIsbnUrl(isbn)), barcode)
                sawResponse = true
            } catch (exception: Exception) {
                sawError = true
            }
        }
        if (hits.isEmpty()) {
            try {
                upcRelease(barcode)?.let { hits += it }
                if (barcode.filter { it.isDigit() }.length >= 8) sawResponse = true
            } catch (exception: Exception) {
                sawError = true
            }
        }
        return finishSearch(distinctReleases(hits), sawResponse, sawError)
    }

    private fun searchWikipedia(
        title: String,
        artist: String,
        barcode: String,
        required: String,
    ): SearchResult {
        var sawResponse = false
        var sawError = false
        val hits = mutableListOf<ScannedRelease>()
        val query = listOf(title, artist).filter { it.isNotBlank() }.joinToString(" ")
        if (query.isNotBlank()) {
            try {
                hits += parseWikipedia(fetch(wikipediaUrl("$query $required")), required)
                sawResponse = true
            } catch (exception: Exception) {
                sawError = true
            }
        }
        if (hits.isEmpty()) {
            try {
                upcRelease(barcode)?.let {
                    hits += it
                    sawResponse = true
                }
            } catch (exception: Exception) {
                sawError = true
            }
        }
        return finishSearch(distinctReleases(hits), sawResponse, sawError)
    }

    private fun upcRelease(barcode: String): ScannedRelease? {
        val digits = barcode.filter { it.isDigit() }
        if (digits.length < 8) return null
        return parseUpcItem(fetch(upcUrl(digits)), barcode)
    }

    private fun finishSearch(
        hits: List<ScannedRelease>,
        sawResponse: Boolean,
        sawError: Boolean,
    ): SearchResult {
        val usable = hits.filter { it.album.isNotBlank() || it.artist.isNotBlank() }.take(8)
        return when {
            usable.isNotEmpty() -> SearchResult.Hits(usable)
            sawResponse -> SearchResult.None
            sawError -> SearchResult.Failed("Couldn't reach the catalog.")
            else -> SearchResult.None
        }
    }

    private fun combineMusicHits(
        musicBrainz: List<ScannedRelease>,
        itunes: List<ScannedRelease>,
    ): List<ScannedRelease> {
        val result = musicBrainz.toMutableList()
        for (extra in itunes) {
            val matched = result.any { hit ->
                titlesMatch(hit.album, extra.album) &&
                    hit.artist.equals(extra.artist, ignoreCase = true)
            }
            if (matched) {
                for (index in result.indices) {
                    val current = result[index]
                    if (current.coverUrl.isNotBlank() || extra.coverUrl.isBlank()) continue
                    if (!titlesMatch(current.album, extra.album)) continue
                    if (!current.artist.equals(extra.artist, ignoreCase = true)) continue
                    result[index] = current.copy(coverUrl = extra.coverUrl)
                }
            } else if (extra.album.isNotBlank()) {
                result.add(extra)
            }
        }
        return result
    }

    private fun distinctReleases(releases: List<ScannedRelease>): List<ScannedRelease> {
        val seen = linkedSetOf<String>()
        return releases.filter { release ->
            val key = normalizeTitle(release.album) + "|" +
                release.artist.lowercase(Locale.US) + "|" + release.year
            seen.add(key)
        }
    }

    private fun searchByTitle(seed: ScannedRelease?, barcode: String): ScannedRelease? {
        val album = cleanCommerceTitle(seed?.album.orEmpty())
        if (album.isBlank()) return null
        val artist = seed?.artist.orEmpty().takeUnless { it.equals(seed?.label, ignoreCase = true) }.orEmpty()
        if (artist.isBlank()) return null
        val searchJson = try {
            fetch(titleSearchUrl(album, artist))
        } catch (exception: Exception) {
            return null
        }
        val id = parseMatchingReleaseId(searchJson, album) ?: return null
        val hit = parseSearchHit(searchJson, barcode)
        val detailJson = try {
            fetch(releaseUrl(id))
        } catch (exception: Exception) {
            return hit?.takeIf { titlesMatch(it.album, album) }
        }
        val detailed = parseRelease(detailJson, barcode)
        return mergeReleases(detailed, hit)?.takeIf { titlesMatch(it.album, album) }
    }

    private fun itunesFill(seed: ScannedRelease): ScannedRelease? {
        val term = listOf(seed.artist, seed.album).filter { it.isNotBlank() }.joinToString(" ")
        if (term.isBlank()) return null
        val json = try {
            fetch(itunesSearchUrl(term))
        } catch (exception: Exception) {
            return null
        }
        val album = parseItunesAlbum(json, seed.album, seed.artist) ?: return null
        if (seed.songs.isNotBlank()) return album
        val id = parseItunesCollectionId(json, seed.album, seed.artist) ?: return album
        val lookup = try {
            fetch(itunesLookupUrl(id))
        } catch (exception: Exception) {
            return album
        }
        val tracks = parseItunesTracks(lookup)
        return if (tracks.isBlank()) album else album.copy(songs = tracks)
    }

    companion object {
        const val USER_AGENT = "VinyLog/1.0 (com.example.vinylog)"
        private const val OPEN_LIBRARY_FIELDS =
            "key,title,author_name,first_publish_year,publisher,cover_i,subject"

        fun searchUrl(barcode: String): String {
            val query = URLEncoder.encode("barcode:$barcode", Charsets.UTF_8.name())
            return "https://musicbrainz.org/ws/2/release/?query=$query&fmt=json&limit=1"
        }

        fun releaseUrl(releaseId: String): String {
            return "https://musicbrainz.org/ws/2/release/$releaseId?inc=artist-credits+recordings+genres+tags+release-groups+labels&fmt=json"
        }

        fun titleSearchUrl(album: String, artist: String): String {
            val query = "release:\"${album.replace("\"", "")}\" AND artistname:\"${artist.replace("\"", "")}\""
            return "https://musicbrainz.org/ws/2/release/?query=${URLEncoder.encode(query, Charsets.UTF_8.name())}&fmt=json&limit=8"
        }

        fun itunesSearchUrl(term: String, limit: Int = 5): String {
            return "https://itunes.apple.com/search?term=${URLEncoder.encode(term, Charsets.UTF_8.name())}&entity=album&limit=$limit"
        }

        fun musicQueryUrl(title: String, artist: String): String? {
            val album = title.replace("\"", "").trim()
            val name = artist.replace("\"", "").trim()
            val query = when {
                album.isNotBlank() && name.isNotBlank() -> "release:\"$album\" AND artistname:\"$name\""
                album.isNotBlank() -> "release:\"$album\""
                name.isNotBlank() -> "artist:\"$name\""
                else -> return null
            }
            return "https://musicbrainz.org/ws/2/release/?query=${encode(query)}&fmt=json&limit=8"
        }

        fun openLibraryUrl(title: String, author: String): String {
            return "https://openlibrary.org/search.json?title=${encode(title)}&author=${encode(author)}&limit=8&fields=$OPEN_LIBRARY_FIELDS"
        }

        fun openLibraryIsbnUrl(isbn: String): String {
            return "https://openlibrary.org/search.json?isbn=${encode(isbn)}&limit=5&fields=$OPEN_LIBRARY_FIELDS"
        }

        fun wikipediaUrl(query: String): String {
            return "https://en.wikipedia.org/w/api.php?action=query&generator=search" +
                "&gsrsearch=${encode(query)}&gsrlimit=8&prop=pageimages|pageterms" +
                "&piprop=thumbnail&pithumbsize=500&wbptterms=description&format=json&redirects=1"
        }

        fun isbnCandidate(raw: String): String? {
            val trimmed = raw.trim().replace("-", "").replace(" ", "")
            return when {
                trimmed.matches(Regex("[0-9]{9}[0-9Xx]")) -> trimmed.uppercase(Locale.US)
                trimmed.matches(Regex("[0-9]{13}")) -> trimmed
                else -> null
            }
        }

        fun itunesLookupUrl(collectionId: String): String {
            return "https://itunes.apple.com/lookup?id=$collectionId&entity=song"
        }

        fun upcUrl(digits: String): String {
            return "https://api.upcitemdb.com/prod/trial/lookup?upc=$digits"
        }

        fun barcodeCandidates(raw: String): List<String> {
            val trimmed = raw.trim()
            val digits = trimmed.filter { it.isDigit() }
            val primary = if (digits.length >= 8) digits else trimmed
            if (primary.isEmpty()) return emptyList()
            val extra = when {
                digits.length == 12 -> listOf("0$digits")
                digits.length == 13 && digits.startsWith("0") -> listOf(digits.drop(1))
                else -> emptyList()
            }
            return (listOf(primary) + extra).distinct()
        }

        fun parseReleaseId(json: String): String? {
            return try {
                val releases = parseObject(json)?.array("releases") ?: return null
                if (releases.size() == 0) return null
                releases[0].asObject()?.text("id")?.takeIf { it.isNotBlank() }
            } catch (exception: Exception) {
                null
            }
        }

        fun parseMatchingReleaseId(json: String, album: String): String? {
            return try {
                val releases = parseObject(json)?.array("releases") ?: return null
                releases.mapNotNull { it.asObject() }
                    .firstOrNull { titlesMatch(it.text("title"), album) }
                    ?.text("id")
                    ?.takeIf { it.isNotBlank() }
            } catch (exception: Exception) {
                null
            }
        }

        fun cleanCommerceTitle(raw: String): String {
            var title = raw.trim()
            title = title.replace(Regex("\\s*\\[[^\\]]*]"), "")
            title = title.replace(Regex("\\s*\\((?:vinyl|lp|cd|cassette)\\)", RegexOption.IGNORE_CASE), "")
            title = title.replace(Regex("\\s+-\\s+(?:vinyl|lp|cd|cassette)\\s*$", RegexOption.IGNORE_CASE), "")
            return title.trim().trim('-', ' ')
        }

        fun parseSearchHit(json: String, barcode: String): ScannedRelease? {
            return try {
                val release = parseObject(json)?.array("releases")?.firstObject() ?: return null
                releaseToScanned(release, barcode, includeTracks = false)
            } catch (exception: Exception) {
                null
            }
        }

        fun parseSearchHits(json: String, barcode: String): List<ScannedRelease> {
            return try {
                val releases = parseObject(json)?.array("releases") ?: return emptyList()
                releases.mapNotNull { element ->
                    val release = element.asObject() ?: return@mapNotNull null
                    val scanned = releaseToScanned(release, barcode, includeTracks = false) ?: return@mapNotNull null
                    val id = release.text("id")
                    val format = release.array("media")
                        ?.mapNotNull { medium ->
                            medium.asObject()?.text("format")?.takeIf { it.isNotBlank() }
                        }
                        ?.distinct()
                        ?.joinToString(", ")
                        .orEmpty()
                    scanned.copy(
                        catalogId = if (id.isBlank()) "" else "mb:$id",
                        hint = listOf(format, release.text("country"))
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                    )
                }
            } catch (exception: Exception) {
                emptyList()
            }
        }

        fun parseItunesAlbums(json: String): List<ScannedRelease> {
            return try {
                val results = parseObject(json)?.array("results") ?: return emptyList()
                results.mapNotNull { element ->
                    val item = element.asObject() ?: return@mapNotNull null
                    val name = item.text("collectionName")
                    if (name.isBlank()) return@mapNotNull null
                    val wrapper = item.text("wrapperType")
                    if (wrapper.isNotBlank() && wrapper != "collection") return@mapNotNull null
                    val id = item.primitive("collectionId")
                    ScannedRelease(
                        album = name,
                        artist = item.text("artistName"),
                        songs = "",
                        year = yearOf(item.text("releaseDate")),
                        genre = item.text("primaryGenreName"),
                        barcode = "",
                        coverUrl = item.text("artworkUrl100").replace("100x100bb", "600x600bb"),
                        catalogId = if (id.isBlank()) "" else "itunes:$id",
                    )
                }
            } catch (exception: Exception) {
                emptyList()
            }
        }

        fun parseOpenLibrary(json: String, barcode: String): List<ScannedRelease> {
            return try {
                val docs = parseObject(json)?.array("docs") ?: return emptyList()
                docs.mapNotNull { element ->
                    val doc = element.asObject() ?: return@mapNotNull null
                    val title = doc.text("title")
                    if (title.isBlank()) return@mapNotNull null
                    val coverId = doc.primitive("cover_i")
                    val yearDigits = doc.primitive("first_publish_year").take(4)
                    val year = if (yearDigits.length == 4 && yearDigits.all { it.isDigit() }) yearDigits else ""
                    ScannedRelease(
                        album = title,
                        artist = doc.strings("author_name").joinToString(", "),
                        songs = "",
                        year = year,
                        genre = doc.strings("subject").firstOrNull().orEmpty(),
                        barcode = barcode,
                        coverUrl = if (coverId.isBlank()) "" else "https://covers.openlibrary.org/b/id/$coverId-L.jpg",
                        label = doc.strings("publisher").firstOrNull().orEmpty(),
                    )
                }
            } catch (exception: Exception) {
                emptyList()
            }
        }

        fun parseWikipedia(json: String, required: String): List<ScannedRelease> {
            return try {
                val pages = parseObject(json)?.get("query").asObject()?.get("pages").asObject() ?: return emptyList()
                val hits = mutableListOf<Pair<Int, ScannedRelease>>()
                for (entry in pages.entrySet()) {
                    val page = entry.value.asObject() ?: continue
                    val description = page.get("terms").asObject()?.strings("description")?.firstOrNull().orEmpty()
                    if (!description.contains(required, ignoreCase = true)) continue
                    val rawTitle = page.text("title")
                    if (rawTitle.isBlank()) continue
                    val (title, titleYear) = shortTitle(rawTitle, required)
                    val (person, year) = personAndYear(description)
                    hits += (page.primitive("index").toIntOrNull() ?: 999) to ScannedRelease(
                        album = title,
                        artist = person,
                        songs = "",
                        year = year.ifBlank { titleYear },
                        genre = "",
                        barcode = "",
                        coverUrl = page.get("thumbnail").asObject()?.text("source").orEmpty(),
                        hint = description,
                    )
                }
                hits.sortedBy { it.first }.map { it.second }
            } catch (exception: Exception) {
                emptyList()
            }
        }

        fun parseRelease(json: String, barcode: String): ScannedRelease? {
            return try {
                val release = parseObject(json) ?: return null
                releaseToScanned(release, barcode, includeTracks = true)
            } catch (exception: Exception) {
                null
            }
        }

        fun parseUpcItem(json: String, barcode: String): ScannedRelease? {
            return try {
                val root = parseObject(json) ?: return null
                if (root.text("code") != "OK") return null
                val item = root.array("items")?.firstObject() ?: return null
                val rawTitle = item.text("title")
                val album = cleanCommerceTitle(rawTitle).ifBlank { rawTitle }
                if (album.isBlank()) return null
                val label = item.text("brand")
                val commerceTitles = mutableListOf<String>()
                item.text("description").takeIf { it.isNotBlank() }?.let { commerceTitles.add(it) }
                item.array("offers")?.forEach { offer ->
                    offer.asObject()?.text("title")?.takeIf { it.isNotBlank() }?.let { commerceTitles.add(it) }
                }
                val artist = artistFromCommerceTitles(commerceTitles, album, label).ifBlank { label }
                val category = item.text("category")
                val cover = item.array("images")
                    ?.mapNotNull { element ->
                        element.takeIf { it.isJsonPrimitive && !it.isJsonNull }?.asString?.takeIf { url ->
                            url.startsWith("https://")
                        }
                    }
                    ?.firstOrNull()
                    .orEmpty()
                ScannedRelease(
                    album = album,
                    artist = artist,
                    songs = "",
                    year = "",
                    genre = if (category.contains('>')) "" else category,
                    barcode = barcode,
                    coverUrl = cover,
                    label = label,
                )
            } catch (exception: Exception) {
                null
            }
        }

        fun parseItunesAlbum(json: String, album: String, artist: String): ScannedRelease? {
            return try {
                val match = matchingItunesAlbum(json, album, artist) ?: return null
                val artwork = match.text("artworkUrl100").replace("100x100bb", "600x600bb")
                ScannedRelease(
                    album = match.text("collectionName").ifBlank { album },
                    artist = match.text("artistName"),
                    songs = "",
                    year = yearOf(match.text("releaseDate")),
                    genre = match.text("primaryGenreName"),
                    barcode = "",
                    coverUrl = artwork,
                )
            } catch (exception: Exception) {
                null
            }
        }

        fun parseItunesCollectionId(json: String, album: String, artist: String): String? {
            return try {
                matchingItunesAlbum(json, album, artist)?.primitive("collectionId")?.takeIf { it.isNotBlank() }
            } catch (exception: Exception) {
                null
            }
        }

        fun parseItunesTracks(json: String): String {
            return try {
                val results = parseObject(json)?.array("results") ?: return ""
                results.mapNotNull { it.asObject() }
                    .filter { it.text("wrapperType") == "track" }
                    .sortedBy { it.primitive("trackNumber").toIntOrNull() ?: 0 }
                    .map { it.text("trackName") }
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
            } catch (exception: Exception) {
                ""
            }
        }

        private fun matchingItunesAlbum(json: String, album: String, artist: String): JsonObject? {
            val results = parseObject(json)?.array("results") ?: return null
            return results.mapNotNull { it.asObject() }.firstOrNull { item ->
                titlesMatch(item.text("collectionName"), album) &&
                    (artist.isBlank() || item.text("artistName").equals(artist, ignoreCase = true))
            }
        }

        private fun releaseToScanned(release: JsonObject, barcode: String, includeTracks: Boolean): ScannedRelease? {
            val album = release.text("title")
            val artist = artistCredit(release)
            if (album.isBlank() && artist.isBlank()) return null
            return ScannedRelease(
                album = album,
                artist = artist,
                songs = if (includeTracks) trackTitles(release) else "",
                year = yearOf(release.text("date").ifBlank { release.text("first-release-date") }),
                genre = genreOf(release),
                barcode = barcode.ifBlank { release.text("barcode") },
                coverUrl = coverUrl(release),
                label = labelOf(release),
            )
        }

        private fun artistCredit(release: JsonObject): String {
            val credits = release.array("artist-credit") ?: return ""
            return buildString {
                for (element in credits) {
                    val credit = element.asObject() ?: continue
                    val name = credit.text("name").ifBlank {
                        credit.get("artist").asObject()?.text("name").orEmpty()
                    }
                    append(name)
                    append(credit.text("joinphrase"))
                }
            }.trim()
        }

        private fun coverUrl(release: JsonObject): String {
            val archive = release.get("cover-art-archive").asObject()
            val front = archive?.get("front")?.takeIf { it.isJsonPrimitive }?.asBoolean == true
            val id = release.text("id")
            if (front && id.isNotBlank()) return "https://coverartarchive.org/release/$id/front-500"
            val groupId = release.get("release-group").asObject()?.text("id").orEmpty()
            if (groupId.isNotBlank()) return "https://coverartarchive.org/release-group/$groupId/front-500"
            return ""
        }

        private fun labelOf(release: JsonObject): String {
            val info = release.array("label-info") ?: return ""
            return info.firstNotNullOfOrNull { element ->
                element.asObject()?.get("label").asObject()?.text("name")?.takeIf { it.isNotBlank() }
            }.orEmpty()
        }

        private fun artistFromCommerceTitles(titles: List<String>, album: String, brand: String): String {
            val counts = linkedMapOf<String, Int>()
            for (raw in titles) {
                val artist = artistBeforeAlbum(raw, album) ?: continue
                if (artist.equals(brand, ignoreCase = true)) continue
                counts[artist] = (counts[artist] ?: 0) + 1
            }
            return counts.maxByOrNull { it.value }?.key.orEmpty()
        }

        private fun artistBeforeAlbum(raw: String, album: String): String? {
            var cleaned = raw.trim()
            cleaned = cleaned.replace(Regex("^Find\\s+", RegexOption.IGNORE_CASE), "")
            cleaned = cleaned.replace(Regex("\\s+by\\s+.+$", RegexOption.IGNORE_CASE), "")
            cleaned = cleaned.replace(Regex("\\s+in\\s+Media\\.?$", RegexOption.IGNORE_CASE), "")
            cleaned = cleanCommerceTitle(cleaned)
            val parts = cleaned.split(Regex("\\s+-\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size < 2) return null
            val artist = parts.first()
            val remainder = parts.drop(1).joinToString(" - ")
            if (!titlesMatch(remainder, album) && !remainder.contains(album, ignoreCase = true)) return null
            if (artist.length < 2) return null
            return artist
        }

        private fun titlesMatch(found: String, wanted: String): Boolean {
            if (found.isBlank() || wanted.isBlank()) return false
            return normalizeTitle(found) == normalizeTitle(wanted)
        }

        private fun normalizeTitle(value: String): String {
            return cleanCommerceTitle(value).lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), " ").trim()
        }

        private fun mergeReleases(primary: ScannedRelease?, fallback: ScannedRelease?): ScannedRelease? {
            if (primary == null) return fallback
            if (fallback == null) return primary
            return primary.copy(
                album = primary.album.ifBlank { fallback.album },
                artist = primary.artist.ifBlank { fallback.artist },
                songs = primary.songs.ifBlank { fallback.songs },
                year = primary.year.ifBlank { fallback.year },
                genre = primary.genre.ifBlank { fallback.genre },
                barcode = primary.barcode.ifBlank { fallback.barcode },
                coverUrl = primary.coverUrl.ifBlank { fallback.coverUrl },
                label = primary.label.ifBlank { fallback.label },
            )
        }

        private fun isThin(release: ScannedRelease?): Boolean {
            if (release == null) return true
            val artistMissing = release.artist.isBlank() ||
                (release.label.isNotBlank() && release.artist.equals(release.label, ignoreCase = true))
            return release.songs.isBlank() || release.year.isBlank() || artistMissing
        }

        private fun trackTitles(release: JsonObject): String {
            val titles = mutableListOf<String>()
            val media = release.array("media") ?: return ""
            for (mediumElement in media) {
                val tracks = mediumElement.asObject()?.array("tracks") ?: continue
                for (trackElement in tracks) {
                    val title = trackElement.asObject()?.text("title").orEmpty()
                    if (title.isNotBlank()) titles.add(title)
                }
            }
            return titles.joinToString("\n")
        }

        private fun genreOf(release: JsonObject): String {
            val direct = topName(release.array("genres")).ifBlank { topName(release.array("tags")) }
            if (direct.isNotBlank()) return direct
            val counts = linkedMapOf<String, Int>()
            val media = release.array("media") ?: return ""
            for (mediumElement in media) {
                val tracks = mediumElement.asObject()?.array("tracks") ?: continue
                for (trackElement in tracks) {
                    val recording = trackElement.asObject()?.get("recording").asObject() ?: continue
                    val names = recording.array("genres") ?: recording.array("tags") ?: continue
                    for (nameElement in names) {
                        val genre = nameElement.asObject() ?: continue
                        val name = genre.text("name")
                        if (name.isBlank()) continue
                        val count = genre.get("count")?.takeIf { it.isJsonPrimitive }?.asInt ?: 1
                        counts[name] = (counts[name] ?: 0) + count
                    }
                }
            }
            val best = counts.maxByOrNull { it.value }?.key ?: return ""
            return best.titleWord()
        }

        private fun topName(array: JsonArray?): String {
            if (array == null || array.size() == 0) return ""
            val best = array.mapNotNull { it.asObject() }
                .maxByOrNull { it.get("count")?.takeIf { count -> count.isJsonPrimitive }?.asInt ?: 0 }
                ?: return ""
            return best.text("name").titleWord()
        }

        private fun yearOf(date: String): String {
            if (date.length < 4) return ""
            val year = date.take(4)
            return if (year.all { it.isDigit() }) year else ""
        }

        private fun parseObject(json: String): JsonObject? {
            val element = JsonParser.parseString(json)
            return element.asObject()
        }

        private fun JsonElement?.asObject(): JsonObject? {
            return this?.takeIf { it.isJsonObject }?.asJsonObject
        }

        private fun JsonObject.array(name: String): JsonArray? {
            return get(name)?.takeIf { it.isJsonArray }?.asJsonArray
        }

        private fun JsonObject.strings(name: String): List<String> {
            val values = array(name) ?: return emptyList()
            return values.mapNotNull { element ->
                if (!element.isJsonPrimitive || element.isJsonNull) null
                else element.asString.takeIf { it.isNotBlank() }
            }
        }

        private fun encode(value: String): String {
            return URLEncoder.encode(value, Charsets.UTF_8.name())
        }

        private fun shortTitle(raw: String, required: String): Pair<String, String> {
            val pattern = if (required == "film") {
                Regex("""^(.+?) \((\d{4}) film\)$""", RegexOption.IGNORE_CASE)
            } else {
                Regex("""^(.+?) \((?:(\d{4}) )?video game\)$""", RegexOption.IGNORE_CASE)
            }
            val match = pattern.find(raw) ?: return raw to ""
            return match.groupValues[1].trim() to match.groupValues.getOrNull(2).orEmpty()
        }

        private fun personAndYear(description: String): Pair<String, String> {
            val patterns = listOf(
                Regex("""(\d{4}).*directed by ([^.]+)""", RegexOption.IGNORE_CASE),
                Regex("""(\d{4}).*film by ([^.]+)""", RegexOption.IGNORE_CASE),
                Regex("""(\d{4}).*developed by ([^.]+)""", RegexOption.IGNORE_CASE),
            )
            for (pattern in patterns) {
                val match = pattern.find(description) ?: continue
                return match.groupValues[2].trim() to match.groupValues[1]
            }
            val year = Regex("""\d{4}""").find(description)?.value.orEmpty()
            return "" to year
        }

        private fun JsonObject.text(name: String): String {
            return primitive(name)
        }

        private fun JsonObject.primitive(name: String): String {
            val element = get(name) ?: return ""
            if (element.isJsonNull || !element.isJsonPrimitive) return ""
            val primitive = element.asJsonPrimitive
            return if (primitive.isNumber) primitive.asLong.toString() else primitive.asString
        }

        private fun JsonArray.firstObject(): JsonObject? {
            if (size() == 0) return null
            return get(0).asObject()
        }

        private fun String.titleWord(): String {
            return replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
            }
        }
    }
}

private val musicBrainzLock = Any()
private var lastMusicBrainzRequestAt = 0L

internal fun httpGet(url: String): String {
    var attempt = 0
    while (true) {
        if (url.contains("musicbrainz.org")) {
            synchronized(musicBrainzLock) {
                val wait = 1_100 - (System.currentTimeMillis() - lastMusicBrainzRequestAt)
                if (wait > 0) Thread.sleep(wait)
                lastMusicBrainzRequestAt = System.currentTimeMillis()
            }
        }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", ReleaseLookup.USER_AGENT)
            setRequestProperty("Accept", "application/json")
            connectTimeout = 12_000
            readTimeout = 12_000
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code == 503 && url.contains("musicbrainz.org") && attempt < 3) {
                attempt += 1
                Thread.sleep(1_500L * attempt)
                continue
            }
            if (code !in 200..299) error("HTTP $code")
            return body
        } finally {
            connection.disconnect()
        }
    }
}
