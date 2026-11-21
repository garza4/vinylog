package com.example.vinylog.activities

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.vinylog.dialogs.MediaSaveDialog
import com.example.vinylog.objects.Album
import com.example.vinylog.objects.database.DatabaseSingleton
import com.example.vinylog.ui.theme.VinyLogTheme
import com.example.vinylog.util.BarcodeScanner

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
                                )
                            }
                            val albums by albumViewModel.albums.observeAsState(emptyList())
                            if (albums.isEmpty()) {
                                Text(
                                    text = "No entries yet. Tap + to scan a barcode or add one.",
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 30.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                items(albums, key = { it.id }, span = { GridItemSpan(maxLineSpan) }) { album ->
                                    Text(
                                        text = album.toString(),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    )
                                    Divider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
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

class AlbumViewModel(application: Application, mediaType: String) : ViewModel() {
    private val db = DatabaseSingleton.getDatabase(application)
    val albums: LiveData<List<Album>> = db.libraryDao().getAllByMediaType(mediaType)
    val scannedBarcode = MutableLiveData<BarcodeScan?>(null)

    fun onBarcodeScanned(barcode: String) {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty()) return
        scannedBarcode.value = BarcodeScan(trimmed, System.nanoTime())
    }

    fun consumeBarcode() {
        scannedBarcode.value = null
    }
}
