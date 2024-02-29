package com.example.vinylog.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.vinylog.objects.MCollection
import com.example.vinylog.objects.database.AppDb
import com.example.vinylog.ui.theme.VinyLogTheme

class AlbumView : ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(
            applicationContext,
            AppDb::class.java, "library"
        ).build()
        setContent {
            VinyLogTheme {
                var showDialog by remember { mutableStateOf(false) }
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val bundle = intent.extras
                    val myObj = bundle?.getSerializable("group") as? MCollection
                    if(showDialog){
                        DialogExamples(onDismiss = { showDialog = false })
                    }

                    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 30.dp) ){
                        myObj?.albums?.forEach{album ->
                            item(span = {
                                GridItemSpan(maxLineSpan)
                            }){
                                Card(){
                                    Text(text = album.toString())
                                }
                            }
                        }
                    }
                    Button(onClick = {
                        //your onclick code here
                        println("click")
                        showDialog=true;
                    }) {
                        Text(text = "Add to library")
                    }
                }
            }
        }
    }

    @Composable
    fun FilledButtonExample(onClick: () -> Unit) {
        Button(onClick = { onClick() }) {
            Text("Filled")
        }
    }

    @Composable
    fun DialogExamples(onDismiss:()->Unit) {
        var choice by remember { mutableStateOf("scan") }
        AlertDialog(
            title = {
                Text(text = "Media Saver")
            },
            text = {
                Text(text = "Make a selection on whether to scan and save new media or use manual entry")
            },
            icon = { Icon(Icons.Rounded.Star, contentDescription = "Localized description") },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismiss()
                    }
                ) {
                    Text("Enter Manual")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        choice="scan"
                        onDismiss()
                    }
                ) {
                    Text("Scan")
                }
            }
        )
    }
}