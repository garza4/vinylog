package com.example.vinylog

import com.example.vinylog.dialogs.edited
import com.example.vinylog.dialogs.toDraft
import com.example.vinylog.objects.Album
import com.example.vinylog.util.LookupResult
import com.example.vinylog.util.ReleaseLookup
import com.example.vinylog.util.ScannedRelease
import com.example.vinylog.util.SearchResult
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
    fun parseUpcItem_readsThePerformerFromOfferTitlesAndKeepsTheLabel() {
        val item = ReleaseLookup.parseUpcItem(UPC_JSON, "880882399016")
        assertEquals("Invisible People", item?.album)
        assertEquals("Chicano Batman", item?.artist)
        assertEquals("ATO RECORDS", item?.label)
        assertEquals("", item?.genre)
        assertEquals("https://example.com/cover.jpg", item?.coverUrl)
        assertEquals("", item?.songs)
    }

    @Test
    fun lookup_fillsTracksYearAndCoverWhenTheBarcodeMissesMusicBrainz() {
        val lookup = ReleaseLookup { url ->
            when {
                url.contains("musicbrainz.org/ws/2/release/?") && url.contains("barcode") -> """{"releases":[]}"""
                url.contains("upcitemdb") -> UPC_JSON
                url.contains("artistname") -> TITLE_SEARCH_JSON
                url.contains("/release/35b1ad81") -> RELEASE_DETAIL_JSON
                else -> error("unexpected $url")
            }
        }

        val result = lookup.lookup("880882399016") as LookupResult.Found
        assertEquals("Invisible People", result.release.album)
        assertEquals("Chicano Batman", result.release.artist)
        assertEquals("Color My Life\nBlank Slate", result.release.songs)
        assertEquals("2020", result.release.year)
        assertEquals("Soul", result.release.genre)
        assertEquals("ATO Records", result.release.label)
        assertEquals(
            "https://coverartarchive.org/release-group/57e6bd02-b736-41c0-bc1f-a82d08b979d1/front-500",
            result.release.coverUrl,
        )
        assertEquals("880882399016", result.release.barcode)
    }

    @Test
    fun search_listsEachPressingAndBorrowsTheItunesCover() {
        val fetched = mutableListOf<String>()
        val lookup = ReleaseLookup { url ->
            fetched += url
            when {
                url.contains("musicbrainz.org") -> TWO_PRESSINGS_JSON
                url.contains("itunes.apple.com/search") -> ITUNES_ALBUMS_JSON
                else -> error("unexpected $url")
            }
        }

        val result = lookup.search("Invisible People", "Chicano Batman", "Vinyl", "880882399016") as SearchResult.Hits
        assertEquals(2, result.releases.size)
        assertEquals("mb:aaa", result.releases[0].catalogId)
        assertEquals("mb:bbb", result.releases[1].catalogId)
        assertEquals("CD · US", result.releases[0].hint)
        assertEquals("Vinyl · GB", result.releases[1].hint)
        assertEquals("https://example.com/600x600bb.jpg", result.releases[0].coverUrl)
        assertEquals("https://example.com/600x600bb.jpg", result.releases[1].coverUrl)
        assertTrue(fetched.none { it.contains("wikipedia.org") || it.contains("openlibrary.org") })
    }

    @Test
    fun loadChoice_fetchesTracksForTheSelectedRelease() {
        val lookup = ReleaseLookup { url ->
            if (url.contains("/release/aaa")) RELEASE_JSON else error("unexpected $url")
        }
        val full = lookup.loadChoice(
            ScannedRelease(
                album = "Kind of Blue",
                artist = "Miles Davis",
                songs = "",
                year = "1987",
                genre = "",
                barcode = "074644057928",
                catalogId = "mb:aaa",
            ),
        )
        assertEquals("So What\nFreddie Freeloader", full.songs)
        assertEquals("074644057928", full.barcode)
    }

    @Test
    fun search_reportsAFailureWhenTheCatalogIsUnreachable() {
        val lookup = ReleaseLookup { throw java.io.IOException("offline") }
        val result = lookup.search("Kind of Blue", "Miles Davis", "Vinyl", "")
        assertTrue(result is SearchResult.Failed)
    }

    @Test
    fun search_usesOpenLibraryForBooks() {
        val fetched = mutableListOf<String>()
        val lookup = ReleaseLookup { url ->
            fetched += url
            """{"docs":[{"title":"The Hobbit","author_name":["J.R.R. Tolkien"],"first_publish_year":1937,"publisher":["Allen & Unwin"],"cover_i":99,"subject":["Fantasy"]}]}"""
        }

        val result = lookup.search("The Hobbit", "Tolkien", "Books", "") as SearchResult.Hits
        assertEquals("The Hobbit", result.releases[0].album)
        assertEquals("J.R.R. Tolkien", result.releases[0].artist)
        assertEquals("1937", result.releases[0].year)
        assertEquals("Fantasy", result.releases[0].genre)
        assertEquals("Allen & Unwin", result.releases[0].label)
        assertEquals("https://covers.openlibrary.org/b/id/99-L.jpg", result.releases[0].coverUrl)
        assertTrue(fetched.all { it.contains("openlibrary.org") })
    }

    @Test
    fun search_usesWikipediaForMoviesAndSkipsUnrelatedPages() {
        val lookup = ReleaseLookup { url ->
            assertTrue(url.contains("en.wikipedia.org"))
            WIKI_FILM_JSON
        }

        val result = lookup.search("The Thing", "", "Movies", "") as SearchResult.Hits
        assertEquals(1, result.releases.size)
        assertEquals("The Thing", result.releases[0].album)
        assertEquals("John Carpenter", result.releases[0].artist)
        assertEquals("1982", result.releases[0].year)
        assertEquals("https://example.com/thing.jpg", result.releases[0].coverUrl)
    }

    @Test
    fun search_usesWikipediaForGames() {
        val lookup = ReleaseLookup { url ->
            assertTrue(url.contains("video+game"))
            WIKI_GAME_JSON
        }

        val result = lookup.search("Tears of the Kingdom", "", "Nintendo Switch", "") as SearchResult.Hits
        assertEquals(1, result.releases.size)
        assertEquals("The Legend of Zelda: Tears of the Kingdom", result.releases[0].album)
        assertEquals("Nintendo", result.releases[0].artist)
        assertEquals("2023", result.releases[0].year)
    }

    @Test
    fun edited_keepsTheRowIdAndMediaType() {
        val album = Album("Old", "A", "track", "1999", "Jazz", "Books", "111", "https://cover", "Label")
        album.id = 4
        val updated = album.edited(album.toDraft().copy(album = "New", artist = "B", songs = ""))
        assertEquals(4L, updated.id)
        assertEquals("Books", updated.mediaType)
        assertEquals("New", updated.albumName)
        assertEquals("B", updated.artistName)
        assertEquals("", updated.songsOnRecord)
        assertEquals("Old", album.albumName)
        assertEquals("111", updated.barcode)
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

        const val UPC_JSON = """
            {
              "code": "OK",
              "items": [{
                "title": "Invisible People [LP] - VINYL",
                "description": "Find Chicano Batman - Invisible People - Vinyl by ATO RECORDS in Media.",
                "brand": "ATO RECORDS",
                "category": "Electronics > Video",
                "images": ["https://example.com/cover.jpg"],
                "offers": [
                  {"title": "Chicano Batman - Invisible People - Vinyl"},
                  {"title": "Chicano Batman - Invisible People (LP) (Vinyl)"}
                ]
              }]
            }
        """

        const val TITLE_SEARCH_JSON = """
            {
              "releases": [{
                "id": "35b1ad81-bb73-4dbf-8821-273c5f4544d4",
                "title": "Invisible People",
                "date": "2020-05-01",
                "artist-credit": [{"name": "Chicano Batman", "joinphrase": ""}],
                "release-group": {"id": "57e6bd02-b736-41c0-bc1f-a82d08b979d1"},
                "label-info": [{"label": {"name": "ATO Records"}}]
              }]
            }
        """

        const val TWO_PRESSINGS_JSON = """
            {"releases":[
              {"id":"aaa","title":"Invisible People","date":"2020-05-01","country":"US","artist-credit":[{"name":"Chicano Batman","joinphrase":""}],"media":[{"format":"CD","track-count":12}]},
              {"id":"bbb","title":"Invisible People","date":"2020-05-01","country":"GB","artist-credit":[{"name":"Chicano Batman","joinphrase":""}],"media":[{"format":"Vinyl","track-count":12}]}
            ]}
        """

        const val ITUNES_ALBUMS_JSON = """
            {"results":[{
              "wrapperType":"collection",
              "collectionType":"Album",
              "collectionName":"Invisible People",
              "artistName":"Chicano Batman",
              "collectionId":1498550615,
              "releaseDate":"2020-05-01T07:00:00Z",
              "primaryGenreName":"Alternative",
              "artworkUrl100":"https://example.com/100x100bb.jpg"
            }]}
        """

        const val WIKI_FILM_JSON = """
            {"query":{"pages":{
              "1":{"index":2,"title":"Thing (The Thing)","terms":{"description":["fictional character"]}},
              "2":{"index":1,"title":"The Thing (1982 film)","terms":{"description":["1982 film directed by John Carpenter"]},"thumbnail":{"source":"https://example.com/thing.jpg"}}
            }}}
        """

        const val WIKI_GAME_JSON = """
            {"query":{"pages":{
              "1":{"index":2,"title":"List of characters","terms":{"description":["Wikimedia list article"]}},
              "2":{"index":1,"title":"The Legend of Zelda: Tears of the Kingdom","terms":{"description":["2023 video game developed by Nintendo"]}}
            }}}
        """

        const val RELEASE_DETAIL_JSON = """
            {
              "id": "35b1ad81-bb73-4dbf-8821-273c5f4544d4",
              "title": "Invisible People",
              "date": "2020-05-01",
              "artist-credit": [{"name": "Chicano Batman", "joinphrase": ""}],
              "cover-art-archive": {"front": false},
              "release-group": {"id": "57e6bd02-b736-41c0-bc1f-a82d08b979d1"},
              "label-info": [{"label": {"name": "ATO Records"}}],
              "tags": [{"name": "soul", "count": 5}, {"name": "rock", "count": 1}],
              "media": [{
                "tracks": [
                  {"title": "Color My Life"},
                  {"title": "Blank Slate"}
                ]
              }]
            }
        """
    }
}
