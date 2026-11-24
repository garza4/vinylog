package com.example.vinylog.activities

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.vinylog.dialogs.AlbumDetailDialog
import com.example.vinylog.dialogs.CoverImage
import com.example.vinylog.dialogs.MediaSaveDialog
import com.example.vinylog.games.isGameSystem
import com.example.vinylog.objects.Album
import com.example.vinylog.objects.database.DatabaseSingleton
import com.example.vinylog.ui.theme.VinyLogTheme
import com.example.vinylog.util.BarcodeScanner
import com.example.vinylog.util.LookupResult
import com.example.vinylog.util.ReleaseLookup
import com.example.vinylog.util.ScannedRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BarcodeScan(val barcode: String, val token: Long)

class AlbumView : ComponentActivity() {
    private lateinit var barcodeScanner: BarcodeScanner

    private val mediaType: String by lazy {
        intent.getStringExtra("mt")?.takeIf { it.isNotBlank() } ?: "Vinyl"
    }

    private val albumViewModel: AlbumViewModel by lazy {
        ViewModelProvider(
            this,
            AlbumViewModelFactory(application, mediaType),
        )[AlbumViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = DatabaseSingleton.getDatabase(applicationContext)
        barcodeScanner = BarcodeScanner(this)
        // Register before the activity is started so a scan result still arrives
        // if the screen is recreated while the camera is open.
        barcodeScanner.setScanResultListener(object : BarcodeScanner.ScanResultListener {
            override fun onScanResult(barcode: String) {
                albumViewModel.onBarcodeScanned(barcode)
            }

            override fun onScanCancelled() = Unit

            override fun onPermissionDenied() = Unit
        })

        setContent {
            VinyLogTheme {
                var showDialog by rememberSaveable { mutableStateOf(false) }
                val scannedBarcode by albumViewModel.scannedBarcode.observeAsState()
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Scaffold(
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = { showDialog = true },
                                interactionSource = remember { MutableInteractionSource() },
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            }
                        },
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            if (showDialog) {
                                MediaSaveDialog(
                                    mediaType = mediaType,
                                    onDismiss = { showDialog = false },
                                    db = db,
                                    barcodeScanner = barcodeScanner,
                                    scannedBarcode = scannedBarcode,
                                    onBarcodeHandled = { albumViewModel.consumeBarcode() },
                                    catalogLookup = !skipsCatalogLookup(mediaType),
                                )
                            }
                            if (isGameSystem(mediaType) || mediaType == "Books") {
                                Text(
                                    text = mediaType,
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
                                )
                            }
                            val albums by albumViewModel.albums.observeAsState(emptyList())
                            val enriching by albumViewModel.enriching.observeAsState(emptySet())
                            var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
                            val selected = albums.firstOrNull { it.id == selectedId }
                            LaunchedEffect(albums.map { it.id to it.needsCatalogDetails() }) {
                                albumViewModel.enrichIncomplete(albums)
                            }
                            if (selected != null) {
                                AlbumDetailDialog(
                                    album = selected,
                                    onDismiss = { selectedId = null },
                                )
                            }
                            if (albums.isEmpty()) {
                                Text(
                                    text = when {
                                        isGameSystem(mediaType) ->
                                            "No games yet. Tap + to scan a barcode or add one."
                                        mediaType == "Books" ->
                                            "No books yet. Tap + to scan a barcode or add one."
                                        else ->
                                            "No entries yet. Tap + to scan a barcode or add one."
                                    },
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(albums, key = { it.id }) { album ->
                                    AlbumRow(
                                        album = album,
                                        enriching = album.id in enriching,
                                        onClick = { selectedId = album.id },
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun skipsCatalogLookup(mediaType: String): Boolean {
    return isGameSystem(mediaType) || mediaType == "Books"
}

class AlbumViewModelFactory(
    private val application: Application,
    private val mediaType: String,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (AlbumViewModel::class.java.isAssignableFrom(modelClass)) {
            @Suppress("UNCHECKED_CAST")
            return AlbumViewModel(application, mediaType) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class AlbumViewModel(application: Application, private val mediaType: String) : ViewModel() {
    private val db = DatabaseSingleton.getDatabase(application)
    val albums: LiveData<List<Album>> = db.libraryDao().getAllByMediaType(mediaType)
    val scannedBarcode = MutableLiveData<BarcodeScan?>(null)
    val enriching = MutableLiveData<Set<Long>>(emptySet())
    private val enrichStarted = mutableSetOf<Long>()

    fun onBarcodeScanned(barcode: String) {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty()) return
        scannedBarcode.value = BarcodeScan(trimmed, System.nanoTime())
    }

    fun consumeBarcode() {
        scannedBarcode.value = null
    }

    fun enrichIncomplete(albums: List<Album>) {
        if (skipsCatalogLookup(mediaType)) return
        albums.filter { it.needsCatalogDetails() }.forEach { album ->
            if (!enrichStarted.add(album.id)) return@forEach
            enriching.value = enriching.value.orEmpty() + album.id
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) {
                    try {
                        ReleaseLookup().lookup(album.barcode.orEmpty())
                    } catch (exception: Exception) {
                        LookupResult.Failed("Couldn't reach the catalog.")
                    }
                }
                if (result is LookupResult.Found) {
                    db.libraryDao().update(album.applyRelease(result.release))
                }
                enriching.value = enriching.value.orEmpty() - album.id
            }
        }
    }
}

private fun Album.needsCatalogDetails(): Boolean {
    if (barcode.isNullOrBlank()) return false
    return songsOnRecord.isNullOrBlank() || yearPublished.isNullOrBlank() || coverUrl.isNullOrBlank()
}

private fun Album.applyRelease(release: ScannedRelease): Album {
    if (release.album.isNotBlank()) albumName = release.album
    if (release.artist.isNotBlank()) artistName = release.artist
    if (release.songs.isNotBlank()) songsOnRecord = release.songs
    if (release.year.isNotBlank()) yearPublished = release.year
    if (release.genre.isNotBlank()) {
        genre = release.genre
    } else if (genre.orEmpty().contains('>')) {
        genre = ""
    }
    if (release.coverUrl.isNotBlank()) coverUrl = release.coverUrl
    if (release.label.isNotBlank()) recordLabel = release.label
    return this
}

@Composable
private fun AlbumRow(
    album: Album,
    enriching: Boolean,
    onClick: () -> Unit,
) {
    val subtitle = listOf(album.artistName.orEmpty(), album.yearPublished.orEmpty())
        .filter { it.isNotBlank() }
        .joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CoverImage(
            url = album.coverUrl.orEmpty(),
            title = album.albumName.orEmpty(),
            modifier = Modifier.size(72.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.albumName.orEmpty().ifBlank { "Untitled" },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = when {
                    enriching -> "Looking up artist, year, and tracks…"
                    subtitle.isNotBlank() -> subtitle
                    else -> "No details yet"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
