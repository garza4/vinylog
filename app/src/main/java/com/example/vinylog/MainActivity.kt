package com.example.vinylog

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import com.example.vinylog.activities.AlbumView
import com.example.vinylog.objects.Album
import com.example.vinylog.objects.MCollection
import com.example.vinylog.objects.MusicCollection
import com.example.vinylog.ui.theme.VinyLogTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!File(filesDir, "lib").exists()) {
            openFileOutput("lib", MODE_PRIVATE).use {
                it.write("{}".toByteArray())
            }
        }
        setContent {
            VinyLogTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Select Media Type",
                                    style = MaterialTheme.typography.headlineLarge,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Divider()
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        startActivity(Intent(this@MainActivity, AlbumView::class.java).putExtra("mt", "Vinyl"))
                                    },
                                    interactionSource = remember { MutableInteractionSource() }

                                ) {
                                    Text(text = "Vinyl", onTextLayout = {})
                                }
                                Divider()
                                TextButton(
                                    onClick = {
                                        startActivity(Intent(this@MainActivity, AlbumView::class.java).putExtra("mt", "CDs"))
                                    },
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    Text(text = "CDs", onTextLayout = {})
                                }
                                Divider()
                                TextButton(
                                    onClick = {
                                        startActivity(Intent(this@MainActivity, AlbumView::class.java).putExtra("mt", "Movies"))
                                    },
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    Text(text = "Movies", onTextLayout = {})
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun createCollections(albums:LiveData<List<Album>>, mediaTypes:LiveData<List<String>>):MusicCollection{
    //need a collection for each kind of collection
    val medias = mutableListOf<MCollection>()
    mediaTypes.value?.forEach{media : String ->
        run {
            medias.add(MCollection(media, albums.value?.filter { al -> al.mediaType == media } ?: emptyList()))        }
    }
    return MusicCollection(
        listOf(
            medias
        )
    )
}

val ALBUM_LIST = MusicCollection(
    listOf(
        listOf(
            MCollection("Vinyl",listOf(
                Album("A","","","","","Vinyl"),
                Album("B","","","","","Vinyl"),
                Album("C","","","","","Vinyl"))

            ),
            MCollection("CDs",listOf(
                Album("D","","","","","CD"),
                Album("E","","","","","CD"),
                Album("F","","","","","CD"))

            ),
            MCollection("Movies",listOf(
                Album("G","","","","","DVD"),
                Album("H","","","","","DVD"),
                Album("I","","","","","DVD"))

            ))
    )
);