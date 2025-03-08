package com.example.vinylog.activities

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.example.vinylog.objects.Album
import com.example.vinylog.objects.MCollection
import com.example.vinylog.objects.database.AppDb
import com.example.vinylog.ui.theme.VinyLogTheme
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode

class AlbumView : ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var mediaType: String? = null
        val db = Room.databaseBuilder(
            applicationContext,
            AppDb::class.java, "library"
        ).allowMainThreadQueries().build()
        setContent {
            VinyLogTheme {
                var showDialog by remember { mutableStateOf(false) }
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Scaffold(
                        floatingActionButton = {
                            FloatingActionButton(onClick = { showDialog = true },interactionSource = remember { MutableInteractionSource() }) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            }
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .padding(innerPadding),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            val bundle = intent.extras
                            mediaType = bundle?.getString("mt")
                            val myObj = bundle?.getSerializable("group") as? MCollection
                            if(showDialog){
                                if (mediaType != null) {
                                    MediaSaveDialog(mediaType!!,onDismiss = { showDialog = false },db)
                                }
                            }

                            LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 30.dp) ){
                                myObj?.albums?.forEach{album ->
                                    item(span = {
                                        GridItemSpan(maxLineSpan)
                                    }){
                                        Text(text = album.toString(),onTextLayout = {})
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun MediaSaveDialog(mediaType:String, onDismiss:()->Unit, db: AppDb) {
        var choice by remember { mutableStateOf("") }
        var fieldEntries by remember { mutableStateOf(mapOf("artist" to "","album" to "","artist" to "","songs" to "", "year" to "", "genre" to "", "media" to mediaType)) }
        if(choice.isEmpty()){
            AlertDialog(
                title = {
                    Text(text = "Media Saver",onTextLayout = { })
                },
                text = {
                    Text(text = "Make a selection on whether to scan and save new media or use manual entry",onTextLayout = {  })
                },
                icon = { Icon(Icons.Rounded.Star, contentDescription = "Localized description") },
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {
                            choice="manual"
                        }
                    ) {
                        Text("Enter Manual",onTextLayout = {})
                    }
                },
                dismissButton = {
                    TextButton(
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {
                            choice="scan"
                            val options = BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(
                                    Barcode.FORMAT_QR_CODE,
                                    Barcode.FORMAT_AZTEC)
                                .build()

                        }
                    ) {
                        Text("Scan",onTextLayout = {})
                    }
                }
            )
        }else if(choice == "manual"){
            Dialog(onDismissRequest = { onDismiss() }){
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ){
                        fieldEntries.keys.forEach { entry ->
                                TextField(
                                    interactionSource = remember { MutableInteractionSource() },                                    value = fieldEntries[entry]!!,
                                    onValueChange = { fieldEntries += entry to it },
                                    label = {
                                        Text(entry,onTextLayout = {})
                                    }
                                )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ){
                            TextButton(
                                interactionSource = remember { MutableInteractionSource() },
                                modifier = Modifier.padding(8.dp),
                                onClick = {
                                    val newAlbum  = Album();
                                    newAlbum.albumName = fieldEntries["album"]
                                    newAlbum.artistName = fieldEntries["artist"]
                                    newAlbum.yearPublished = fieldEntries["year"]
                                    newAlbum.genre = fieldEntries["genre"]
                                    newAlbum.mediaType = fieldEntries["media"]
                                    println(newAlbum)
                                    db.libraryDao().insertAll(newAlbum)
                                    onDismiss()
                                },
                            ) {
                                Text("Save",onTextLayout = {})
                            }
                            TextButton(
                                interactionSource = remember { MutableInteractionSource() },
                                modifier = Modifier.padding(8.dp),
                                onClick = {
                                    onDismiss()
                                },
                            ) {
                                Text("Cancel",onTextLayout = {})
                            }

                        }

                    }
                }
            }
        }else{
            AlertDialog(
                title = {
                    Text(text = "Media Saver",onTextLayout = { })
                },
                text = {
                    Text(text = "Make a selection on whether to scan and save new media or use manual entry",onTextLayout = {})
                },
                icon = { Icon(Icons.Rounded.Star, contentDescription = "Localized description") },
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(
                        interactionSource = remember { MutableInteractionSource() },                        onClick = {
                            choice="manual"
                            onDismiss()
                        }
                    ) {
                        Text("Enter Manual",onTextLayout = {  })
                    }
                },
                dismissButton = {
                    TextButton(
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {
                            choice="scan"
                            startCamera()

                        }
                    ) {
                        Text("Scan",onTextLayout = { })
                    }
                }
            )
        }
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }
}