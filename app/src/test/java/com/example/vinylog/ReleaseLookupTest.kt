package com.example.vinylog

import com.example.vinylog.objects.Album
import com.example.vinylog.util.LookupResult
import com.example.vinylog.util.ReleaseLookup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseLookupTest {
    @Test
    fun barcodeCandidates_expandTwelveAndThirteenDigitCodes() {
        assertEquals(listOf("012345678905", "0012345678905"), ReleaseLookup.barcodeCandidates("012345678905"))
        assertEquals(listOf("0012345678905", "012345678905"), ReleaseLookup.barcodeCandidates("0012345678905"))
        assertEquals(
            listOf("074644057928", "0074644057928"),
            ReleaseLookup.barcodeCandidates(" 074644057928 "),
        )
    }

    @Test
    fun parseRelease_readsArtistAlbumTracksYearAndGenre() {
        val release = ReleaseLookup.parseRelease(RELEASE_JSON, "074644057928")
        assertEquals("Kind of Blue", release?.album)
        assertEquals("Miles Davis", release?.artist)
        assertEquals("So What\nFreddie Freeloader", release?.songs)
        assertEquals("1987", release?.year)
        assertEquals("Jazz", release?.genre)
        assertEquals("074644057928", release?.barcode)
    }

    @Test
    fun parseReleaseId_isNullWhenTheCatalogHasNoMatch() {
        assertNull(ReleaseLookup.parseReleaseId("""{"releases":[]}"""))
    }

    @Test
    fun parseUpcItem_usesTitleAndBrand() {
        val item = ReleaseLookup.parseUpcItem(
            """{"code":"OK","items":[{"title":"The Thing","brand":"Universal","category":"Movies"}]}""",
            "012345678905",
        )
        assertEquals("The Thing", item?.album)
        assertEquals("Universal", item?.artist)
        assertEquals("Movies", item?.genre)
        assertEquals("012345678905", item?.barcode)
    }

    @Test
    fun lookup_usesTheEanFormWhenTheUpcMisses() {
        val fetched = mutableListOf<String>()
        val lookup = ReleaseLookup { url ->
            fetched += url
            when {
                url.contains("0012345678905") -> """{"releases":[{"id":"abc","title":"Dummy","artist-credit":[{"name":"Portishead","joinphrase":""}],"date":"1994"}]}"""
                url.contains("012345678905") -> """{"releases":[]}"""
                url.contains("/release/abc") -> """{"title":"Dummy","date":"1994-01-01","artist-credit":[{"name":"Portishead","joinphrase":""}],"genres":[{"name":"trip hop","count":4}],"media":[{"tracks":[{"title":"Glory Box"}]}]}"""
                else -> error("unexpected $url")
            }
        }

        val result = lookup.lookup("012345678905") as LookupResult.Found
        assertEquals("Dummy", result.release.album)
        assertEquals("Portishead", result.release.artist)
        assertEquals("Glory Box", result.release.songs)
        assertEquals("1994", result.release.year)
        assertEquals("Trip hop", result.release.genre)
        assertEquals("012345678905", result.release.barcode)
        assertTrue(fetched.none { it.contains("upcitemdb") })
    }

    @Test
    fun lookup_fallsBackToProductTitleWhenMusicBrainzMisses() {
        val lookup = ReleaseLookup { url ->
            when {
                url.contains("musicbrainz.org") -> """{"releases":[]}"""
                url.contains("upcitemdb") -> """{"code":"OK","items":[{"title":"Alien","brand":"Fox","category":"Movies"}]}"""
                else -> error("unexpected $url")
            }
        }

        val result = lookup.lookup("012345678905") as LookupResult.Found
        assertEquals("Alien", result.release.album)
        assertEquals("Fox", result.release.artist)
    }

    @Test
    fun lookup_reportsAFailureWhenEveryCatalogRequestFails() {
        val lookup = ReleaseLookup { throw java.io.IOException("offline") }
        val result = lookup.lookup("074644057928")
        assertTrue(result is LookupResult.Failed)
    }

    @Test
    fun album_keepsSongsBarcodeAndMediaForStorage() {
        val album = Album(
            "Kind of Blue",
            "Miles Davis",
            "So What\nFreddie Freeloader",
            "1987",
            "Jazz",
            "Vinyl",
            "074644057928",
        )
        assertEquals("Kind of Blue", album.albumName)
        assertEquals("Miles Davis", album.artistName)
        assertEquals("So What\nFreddie Freeloader", album.songsOnRecord)
        assertEquals("1987", album.yearPublished)
        assertEquals("Jazz", album.genre)
        assertEquals("Vinyl", album.mediaType)
        assertEquals("074644057928", album.barcode)
        assertEquals(0L, album.id)
    }

    private companion object {
        const val RELEASE_JSON = """
            {
              "title": "Kind of Blue",
              "date": "1987",
              "barcode": "074644057928",
              "artist-credit": [{"name": "Miles Davis", "joinphrase": ""}],
              "genres": [],
              "media": [{
                "tracks": [
                  {"title": "So What", "recording": {"genres": [{"name": "jazz", "count": 9}]}},
                  {"title": "Freddie Freeloader", "recording": {"genres": [{"name": "jazz", "count": 7}]}}
                ]
              }]
            }
        """
    }
}
