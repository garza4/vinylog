package com.example.vinylog

import com.example.vinylog.dialogs.edited
import com.example.vinylog.dialogs.toDraft
import com.example.vinylog.objects.Album
import com.example.vinylog.util.LookupResult
import com.example.vinylog.util.ReleaseLookup
import com.example.vinylog.util.ScannedRelease
import com.example.vinylog.util.SearchResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun search_findsSignsWhenTheSavedTitleIsTheRetailEdition() {
        val fetched = mutableListOf<String>()
        val lookup = ReleaseLookup { url ->
            fetched += url
            SIGNS_WIKI_JSON
        }

        val result = lookup.search(
            "Mill Creek - Signs",
            "Mill Creek Botanicals",
            "Movies",
            "786936767155",
        ) as SearchResult.Hits

        assertEquals("Signs", result.releases[0].album)
        assertEquals("M. Night Shyamalan", result.releases[0].artist)
        assertEquals("2002", result.releases[0].year)
        assertEquals("wd:Q22432", result.releases[0].catalogId)
        assertEquals(listOf("Signs", "Signs of Life"), result.releases.map { it.album })
        assertTrue(fetched.single().contains("Signs+film"))
        assertFalse(fetched.single().contains("Mill"))
        assertFalse(fetched.single().contains("Botanical"))
        assertEquals("Signs", ReleaseLookup.catalogQueryTitle("Mill Creek - Signs", "Mill Creek Botanicals", "Movies"))
        assertEquals("", ReleaseLookup.catalogQueryCredit("Mill Creek - Signs", "Mill Creek Botanicals", "Movies"))
    }

    @Test
    fun lookupChoices_listsFilmMatchesInsteadOfTheRetailBrand() {
        val fetched = mutableListOf<String>()
        val lookup = ReleaseLookup { url ->
            fetched += url
            when {
                url.contains("upcitemdb") ->
                    """{"code":"OK","items":[{"title":"Mill Creek - Signs [BLU-RAY]","brand":"Mill Creek Botanicals","category":"Electronics > Video"}]}"""
                url.contains("wikipedia.org") -> SIGNS_WIKI_JSON
                else -> error("unexpected $url")
            }
        }

        val result = lookup.lookupChoices("786936767155", "Movies") as SearchResult.Hits
        assertEquals(listOf("Signs", "Signs of Life"), result.releases.map { it.album })
        assertEquals("M. Night Shyamalan", result.releases[0].artist)
        assertEquals("2002", result.releases[0].year)
        assertTrue(fetched.any { it.contains("Signs+film") })
        assertTrue(result.releases.none { it.artist.contains("Botanical") })
    }

    @Test
    fun lookupChoices_listsEveryMusicBrainzReleaseForTheBarcode() {
        val lookup = ReleaseLookup { url ->
            assertTrue(url.contains("limit=8"))
            """{"releases":[
                {"id":"one","title":"Dummy","date":"1994","country":"US","artist-credit":[{"name":"Portishead","joinphrase":""}],"media":[{"format":"Vinyl"}]},
                {"id":"two","title":"Dummy","date":"1994","country":"GB","artist-credit":[{"name":"Portishead","joinphrase":""}],"media":[{"format":"CD"}]}
            ]}"""
        }

        val result = lookup.lookupChoices("012345678905", "Vinyl") as SearchResult.Hits
        assertEquals(listOf("mb:one", "mb:two"), result.releases.map { it.catalogId })
        assertEquals(listOf("Vinyl · US", "CD · GB"), result.releases.map { it.hint })
    }

    @Test
    fun lookupChoices_listsOpenLibraryEditionsForAnIsbn() {
        val lookup = ReleaseLookup { url ->
            assertTrue(url.contains("openlibrary.org"))
            """{"docs":[
                {"title":"The Hobbit","author_name":["J.R.R. Tolkien"],"first_publish_year":1937,"publisher":["Allen & Unwin"],"isbn":["9780261102217"]},
                {"title":"The Hobbit","author_name":["J.R.R. Tolkien"],"first_publish_year":1999,"publisher":["HarperCollins"],"isbn":["9780261102217"]}
            ]}"""
        }

        val result = lookup.lookupChoices("9780261102217", "Books") as SearchResult.Hits
        assertEquals(listOf("1937", "1999"), result.releases.map { it.year })
        assertEquals("J.R.R. Tolkien", result.releases[0].artist)
        assertTrue(result.releases.all { it.catalogId == "ol:9780261102217" })
    }

    @Test
    fun lookupChoices_listsGameMatchesFromTheRetailTitle() {
        val lookup = ReleaseLookup { url ->
            when {
                url.contains("upcitemdb") ->
                    """{"code":"OK","items":[{"title":"Tears of the Kingdom","brand":"Nintendo","category":"Video Games"}]}"""
                url.contains("wikipedia.org") -> {
                    assertTrue(url.contains("video+game"))
                    WIKI_GAME_JSON
                }
                else -> error("unexpected $url")
            }
        }

        val result = lookup.lookupChoices("045496590123", "Nintendo Switch") as SearchResult.Hits
        assertEquals("The Legend of Zelda: Tears of the Kingdom", result.releases.single().album)
        assertEquals("Nintendo", result.releases.single().artist)
        assertEquals("2023", result.releases.single().year)
    }

    @Test
    fun search_usesWikipediaForTvShows() {
        val lookup = ReleaseLookup { url ->
            assertTrue(url.contains("tv+series"))
            """{"query":{"pages":{
              "1":{"index":1,"title":"The Office (American TV series)","pageprops":{"wikibase_item":"Q2385"},"terms":{"description":["American mockumentary television sitcom created by Greg Daniels"]}},
              "2":{"index":2,"title":"Office Space","terms":{"description":["1999 film directed by Mike Judge"]}}
            }}}"""
        }

        val result = lookup.search("The Office", "", "TV Shows", "") as SearchResult.Hits
        assertEquals("The Office", result.releases.single().album)
        assertEquals("Greg Daniels", result.releases.single().artist)
        assertEquals("wd:Q2385", result.releases.single().catalogId)
    }

    @Test
    fun parseWikidataDetails_readsATelevisionSeries() {
        val release = ReleaseLookup.parseWikidataDetails(
            """{"entities":{"Q1":{"claims":{
              "P170":[{"mainsnak":{"datavalue":{"type":"wikibase-entityid","value":{"id":"Q10"}}}}],
              "P449":[{"mainsnak":{"datavalue":{"type":"wikibase-entityid","value":{"id":"Q11"}}}}],
              "P161":[
                {"mainsnak":{"datavalue":{"type":"wikibase-entityid","value":{"id":"Q12"}}}},
                {"mainsnak":{"datavalue":{"type":"wikibase-entityid","value":{"id":"Q13"}}}}
              ],
              "P580":[{"mainsnak":{"datavalue":{"type":"time","value":{"time":"+2008-01-20T00:00:00Z"}}}}],
              "P2437":[{"mainsnak":{"datavalue":{"type":"quantity","value":{"amount":"+5","unit":"1"}}}}],
              "P1113":[{"mainsnak":{"datavalue":{"type":"quantity","value":{"amount":"+62","unit":"1"}}}}]
            }}}}""",
            """{"entities":{
              "Q10":{"labels":{"en":{"value":"Vince Gilligan"}}},
              "Q11":{"labels":{"en":{"value":"AMC"}}},
              "Q12":{"labels":{"en":{"value":"Bryan Cranston"}}},
              "Q13":{"labels":{"en":{"value":"Aaron Paul"}}}
            }}""",
        )
        assertEquals("Vince Gilligan", release?.artist)
        assertEquals("AMC", release?.label)
        assertEquals("2008", release?.year)
        assertEquals("5", release?.pages)
        assertEquals("62", release?.players)
        assertEquals("Bryan Cranston\nAaron Paul", release?.cast)
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
    fun loadChoice_fillsMovieDetailsFromWikidata() {
        val lookup = ReleaseLookup { url ->
            when {
                url.contains("wbgetentities") && url.contains("props=claims") -> WIKIDATA_CLAIMS_JSON
                url.contains("props=labels") -> WIKIDATA_LABELS_JSON
                else -> error("unexpected $url")
            }
        }
        val full = lookup.loadChoice(
            ScannedRelease(
                album = "The Thing",
                artist = "John Carpenter",
                songs = "",
                year = "1982",
                genre = "",
                barcode = "",
                catalogId = "wd:Q210756",
            ),
        )
        assertEquals("John Carpenter", full.artist)
        assertEquals("Universal Pictures", full.label)
        assertEquals("1982", full.year)
        assertEquals("109 min", full.runtime)
        assertEquals("Kurt Russell\nKeith David", full.cast)
        assertEquals("horror film", full.genre)
        assertTrue(full.coverUrl.contains("The%20Thing"))
    }

    @Test
    fun loadChoice_fillsBookPagesAndFormat() {
        val lookup = ReleaseLookup { url ->
            when {
                url.contains("openlibrary.org/search.json") ->
                    """{"docs":[{"title":"The Hobbit","author_name":["J.R.R. Tolkien"],"first_publish_year":1937,"publisher":["Allen & Unwin"],"cover_i":99,"subject":["Accessible book","Fantasy"],"number_of_pages_median":310,"isbn":["9780261102217"]}]}"""
                url.contains("openlibrary.org/isbn/") ->
                    """{"title":"The Hobbit","number_of_pages":310,"physical_format":"Hardcover","publishers":["Houghton Mifflin"],"publish_date":"September 15, 2002"}"""
                else -> error("unexpected $url")
            }
        }
        val found = lookup.search("The Hobbit", "Tolkien", "Books", "") as SearchResult.Hits
        assertEquals("310", found.releases[0].pages)
        assertEquals("Fantasy", found.releases[0].genre)
        assertEquals("ol:9780261102217", found.releases[0].catalogId)
        val full = lookup.loadChoice(found.releases[0])
        assertEquals("Hardcover", full.format)
        assertEquals("310", full.pages)
        assertEquals("The Hobbit", full.album)
    }

    @Test
    fun edited_keepsTheRowIdAndMediaType() {
        val album = Album("Old", "A", "track", "1999", "Jazz", "Books", "111", "https://cover", "Label")
        album.id = 4
        val updated = album.edited(album.toDraft().copy(title = "New", person = "B", tracks = "", pages = "320"))
        assertEquals(4L, updated.id)
        assertEquals("Books", updated.mediaType)
        assertEquals("New", updated.albumName)
        assertEquals("B", updated.artistName)
        assertEquals("", updated.songsOnRecord)
        assertEquals("320", updated.pageCount)
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

        const val WIKIDATA_CLAIMS_JSON = """
            {"entities":{"Q210756":{"claims":{
              "P57":[{"mainsnak":{"datavalue":{"type":"wikibase-entityid","value":{"id":"Q95008"}}}}],
              "P272":[{"mainsnak":{"datavalue":{"type":"wikibase-entityid","value":{"id":"Q168383"}}}}],
              "P161":[
                {"mainsnak":{"datavalue":{"type":"wikibase-entityid","value":{"id":"Q103157"}}}},
                {"mainsnak":{"datavalue":{"type":"wikibase-entityid","value":{"id":"Q310392"}}}}
              ],
              "P2047":[{"mainsnak":{"datavalue":{"type":"quantity","value":{"amount":"+109","unit":"http://www.wikidata.org/entity/Q7727"}}}}],
              "P136":[{"mainsnak":{"datavalue":{"type":"wikibase-entityid","value":{"id":"Q200092"}}}}],
              "P577":[{"mainsnak":{"datavalue":{"type":"time","value":{"time":"+1982-06-25T00:00:00Z"}}}}],
              "P18":[{"mainsnak":{"datavalue":{"type":"string","value":"The Thing still.jpg"}}}]
            }}}}
        """

        const val WIKIDATA_LABELS_JSON = """
            {"entities":{
              "Q95008":{"labels":{"en":{"value":"John Carpenter"}}},
              "Q168383":{"labels":{"en":{"value":"Universal Pictures"}}},
              "Q103157":{"labels":{"en":{"value":"Kurt Russell"}}},
              "Q310392":{"labels":{"en":{"value":"Keith David"}}},
              "Q200092":{"labels":{"en":{"value":"horror film"}}}
            }}
        """

        const val SIGNS_WIKI_JSON = """
            {"query":{"pages":{
              "1":{"index":1,"title":"Signs of Life (1989 film)","terms":{"description":["1989 film by John David Coles"]}},
              "2":{"index":2,"title":"Signs (film)","pageprops":{"wikibase_item":"Q22432"},"terms":{"description":["2002 film directed by M. Night Shyamalan"]}},
              "3":{"index":3,"title":"M. Night Shyamalan","terms":{"description":["American filmmaker"]}},
              "4":{"index":4,"title":"M. Night Shyamalan filmography","terms":{"description":["list article of films by M. Night Shyamalan"]}}
            }}}
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
