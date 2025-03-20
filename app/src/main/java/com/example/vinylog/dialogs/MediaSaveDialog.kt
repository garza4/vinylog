package com.example.vinylog.dialogs
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.vinylog.objects.database.AppDb
import com.example.vinylog.objects.Album
import com.example.vinylog.util.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode

@SuppressLint("RememberReturnType")
@Composable
fun MediaSaveDialog(mediaType: String, onDismiss: () -> Unit, db: AppDb, barcodeScanner: BarcodeScanner) {
    val context = LocalContext.current
    var fieldEntries by remember { mutableStateOf(mapOf("artist" to "", "album" to "", "artist" to "", "songs" to "", "year" to "", "genre" to "", "media" to mediaType)) }
    var choice by remember { mutableStateOf("") }

    remember {
        barcodeScanner.setScanResultListener(object : BarcodeScanner.ScanResultListener {
            override fun onScanResult(barcode: String) {
                fieldEntries = fieldEntries + ("album" to barcode) // Example: use barcode as album name
                choice = "manual" // Switch to manual entry after scan
            }

            override fun onScanCancelled() {
                Log.d("MediaSaveDialog", "Scan cancelled")
                choice = "" // Go back to initial choice dialog
            }

            override fun onPermissionDenied() {
                Log.d("MediaSaveDialog", "Camera permission denied")
                choice = "" // Go back to initial choice dialog
            }
        })
    }

    when (choice) {
        "" -> AlertDialog(
            title = {
                Text(text = "Media Saver",onTextLayout = {})
            },
            text = {
                Text(text = "Make a selection on whether to scan and save new media or use manual entry",onTextLayout = {})
            },
            icon = { Icon(Icons.Rounded.Star, contentDescription = "Localized description") },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        choice = "manual"
                    }
                ) {
                    Text("Enter Manual",onTextLayout = {})
                }
            },
            dismissButton = {
                TextButton(
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        choice = "scan"
                        barcodeScanner.scanBarcode() // Trigger barcode scanning
                    }
                ) {
                    Text("Scan",onTextLayout = {})
                }
            }
        )

        "manual" -> {
            Dialog(onDismissRequest = { onDismiss() }) {
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
                    ) {
                        fieldEntries.keys.forEach { entry ->
                            TextField(
                                interactionSource = remember { MutableInteractionSource() },
                                value = fieldEntries[entry]!!,
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
                        ) {
                            TextButton(
                                interactionSource = remember { MutableInteractionSource() },
                                modifier = Modifier.padding(8.dp),
                                onClick = {
                                    val newAlbum = Album()
                                    newAlbum.albumName = fieldEntries["album"]
                                    newAlbum.artistName = fieldEntries["artist"]
                                    newAlbum.yearPublished = fieldEntries["year"]
                                    newAlbum.genre = fieldEntries["genre"]
                                    newAlbum.mediaType = fieldEntries["media"]
                                    println(newAlbum)
                                    db.libraryDao().insertAll(newAlbum)
                                    db.libraryDao().all
                                    onDismiss()
                                },
                            ) {
                                Text("Save",onTextLayout = {  })
                            }
                            TextButton(
                                interactionSource = remember { MutableInteractionSource() },
                                modifier = Modifier.padding(8.dp),
                                onClick = {
                                    onDismiss()
                                },
                            ) {
                                Text("Cancel",onTextLayout = {  })
                            }
                        }
                    }
                }
            }

        }
        "scan" -> {
            // This state is handled by the barcodeScanner callback
            // Optionally, show a "Scanning..." dialog or wait for the callback
            AlertDialog(
                title = { Text("Scanning...") },
                text = { Text("Please scan a barcode.") },
                onDismissRequest = { choice = "" },
                confirmButton = {
                    TextButton(onClick = { choice = "" }) { Text("Cancel") }
                }
            )
        }
    }
}

fun startCamera(context: Context) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

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
                context as LifecycleOwner, cameraSelector, preview)

        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }

    }, ContextCompat.getMainExecutor(context))
}
