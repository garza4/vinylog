package com.example.vinylog

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.example.vinylog.activities.AlbumView
import com.example.vinylog.objects.Album
import com.example.vinylog.objects.MCollection
import com.example.vinylog.objects.MusicCollection
import com.example.vinylog.ui.theme.VinyLogTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(!File(filesDir, "lib").exists()){
            openFileOutput("lib", MODE_PRIVATE).use {
                it.write("{}".toByteArray())
            }
        }else{
            println("file exists")
        }
        setContent {
            VinyLogTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column {
                        //TODO pass as parcel to second activity
                        ALBUM_LIST.collect.forEach{collection ->
                            collection.forEach{group ->
                                Button(onClick = {
                                    val intent = Intent(this@MainActivity,AlbumView::class.java)
                                    with(intent){
                                        putExtra("group",group)
                                    }
                                    startActivity(intent)

                                }) {
                                    Text(text = group.collectionName)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

val ALBUM_LIST = MusicCollection(
    listOf(
        listOf(
            MCollection("Vinyl",listOf(
                Album("A","","","",""),
                Album("B","","","",""),
                Album("C","","","",""))

            ),
            MCollection("CDs",listOf(
                Album("D","","","",""),
                Album("E","","","",""),
                Album("F","","","",""))

            ),
            MCollection("Movies",listOf(
                Album("G","","","",""),
                Album("H","","","",""),
                Album("I","","","",""))

            ))
    )
);