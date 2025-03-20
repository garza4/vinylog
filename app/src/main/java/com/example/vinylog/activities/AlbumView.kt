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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.vinylog.dialogs.MediaSaveDialog
import com.example.vinylog.objects.Album
import com.example.vinylog.objects.MCollection
import com.example.vinylog.objects.database.DatabaseSingleton
import com.example.vinylog.ui.theme.VinyLogTheme

class AlbumView : ComponentActivity() {
    private lateinit var barcodeScanner: com.example.vinylog.util.BarcodeScanner

    private val albumViewModel: AlbumViewModel by lazy {
        val bundle = intent.extras
        ViewModelProvider(this, AlbumViewModelFactory(application,bundle?.getString("mt"))).get(AlbumViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var mediaType: String? = null
        val db = DatabaseSingleton.getDatabase(applicationContext)
        barcodeScanner = com.example.vinylog.util.BarcodeScanner(this)

        setContent {
            VinyLogTheme {
                var showDialog by remember { mutableStateOf(false) }
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Scaffold(
                        floatingActionButton = {
                            FloatingActionButton(
                                onClick = { showDialog = true },
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            }
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier.padding(innerPadding),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            val bundle = intent.extras
                            mediaType = bundle?.getString("mt")
                            val myObj = bundle?.getSerializable("group") as? MCollection
                            if (showDialog) {
                                if (mediaType != null) {
                                    MediaSaveDialog(mediaType!!, onDismiss = { showDialog = false }, db,barcodeScanner=barcodeScanner)
                                }
                            }
                            val albums by albumViewModel.albums.observeAsState(emptyList())

                            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 30.dp)) {
                                albums.forEach { album ->
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        Text(text = album.toString(), onTextLayout = {})
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
}

class AlbumViewModelFactory(private val application: Application, private val mediaType: String?) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlbumViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return mediaType?.let { AlbumViewModel(application, it) } as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class AlbumViewModel(application: android.app.Application,mediaType:String) : ViewModel() {
    private val db = DatabaseSingleton.getDatabase(application)
    val albums: LiveData<List<Album>> = db.libraryDao().getAllByMediaType(mediaType)
}