package com.example.vinylog.dialogs
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.vinylog.objects.database.AppDb
import com.example.vinylog.objects.Album
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode

@Composable
fun MediaSaveDialog(mediaType: String, onDismiss: () -> Unit, db: AppDb) {
    var choice by remember { mutableStateOf("") }
    var fieldEntries by remember { mutableStateOf(mapOf("artist" to "", "album" to "", "artist" to "", "songs" to "", "year" to "", "genre" to "", "media" to mediaType)) }
    if (choice.isEmpty()) {
        AlertDialog(
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
                        val options = BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(
                                Barcode.FORMAT_QR_CODE,
                                Barcode.FORMAT_AZTEC
                            )
                            .build()
                    }
                ) {
                    Text("Scan",onTextLayout = {})
                }
            }
        )
    } else if (choice == "manual") {
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
    } else {
        AlertDialog(
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
                        onDismiss()
                    }
                ) {
                    Text("Enter Manual",onTextLayout = {})
                }
            },
            dismissButton = {
                TextButton(
                    interactionSource = remember { MutableInteractionSource() },                    onClick = {
                        choice = "scan"
                    }
                ) {
                    Text("Scan",onTextLayout = {})
                }
            }
        )
    }
}