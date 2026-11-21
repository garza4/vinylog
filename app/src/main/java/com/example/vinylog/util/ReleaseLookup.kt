package com.example.vinylog.util

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
)

sealed class LookupResult {
    data class Found(val release: ScannedRelease) : LookupResult()
    data object NotFound : LookupResult()
    data class Failed(val message: String) : LookupResult()
}

/**
 * Looks a scanned retail barcode up in MusicBrainz, then UPC Item DB.
 * Both calls run on the caller's thread. Call this off the main thread.
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
                val basic = parseSearchHit(searchJson, stored)
                if (basic != null) return LookupResult.Found(basic)
                sawError = true
                continue
            }
            val parsed = parseRelease(detailJson, stored)
            if (parsed != null) return LookupResult.Found(parsed)
        }

        val digits = stored.filter { it.isDigit() }
        if (digits.length >= 8) {
            try {
                val upc = parseUpcItem(fetch(upcUrl(digits)), stored)
                if (upc != null) return LookupResult.Found(upc)
                sawResponse = true
            } catch (exception: Exception) {
                sawError = true
            }
        }

        return when {
            sawResponse -> LookupResult.NotFound
            sawError -> LookupResult.Failed("Couldn't reach the catalog.")
            else -> LookupResult.NotFound
        }
    }

    companion object {
        const val USER_AGENT = "VinyLog/1.0 (com.example.vinylog)"

        fun searchUrl(barcode: String): String {
            val query = URLEncoder.encode("barcode:$barcode", Charsets.UTF_8.name())
            return "https://musicbrainz.org/ws/2/release/?query=$query&fmt=json&limit=1"
        }

        fun releaseUrl(releaseId: String): String {
            return "https://musicbrainz.org/ws/2/release/$releaseId?inc=artist-credits+recordings+genres+tags&fmt=json"
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

        fun parseSearchHit(json: String, barcode: String): ScannedRelease? {
            return try {
                val release = parseObject(json)?.array("releases")?.firstObject() ?: return null
                releaseToScanned(release, barcode, includeTracks = false)
            } catch (exception: Exception) {
                null
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
                val album = item.text("title")
                if (album.isBlank()) return null
                ScannedRelease(
                    album = album,
                    artist = item.text("brand"),
                    songs = "",
                    year = "",
                    genre = item.text("category"),
                    barcode = barcode,
                )
            } catch (exception: Exception) {
                null
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
                year = yearOf(release.text("date")),
                genre = genreOf(release),
                barcode = barcode.ifBlank { release.text("barcode") },
            )
        }

        private fun artistCredit(release: JsonObject): String {
            val credits = release.array("artist-credit") ?: return ""
            return buildString {
                for (element in credits) {
                    val credit = element.asObject() ?: continue
                    append(credit.text("name"))
                    append(credit.text("joinphrase"))
                }
            }.trim()
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

        private fun JsonObject.text(name: String): String {
            val element = get(name) ?: return ""
            if (element.isJsonNull || !element.isJsonPrimitive) return ""
            return element.asString
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

internal fun httpGet(url: String): String {
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
        if (code !in 200..299) error("HTTP $code")
        return body
    } finally {
        connection.disconnect()
    }
}
