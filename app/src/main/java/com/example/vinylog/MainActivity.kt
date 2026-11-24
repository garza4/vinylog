package com.example.vinylog

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import com.example.vinylog.activities.AlbumView
import com.example.vinylog.activities.GameLibraryActivity
import com.example.vinylog.objects.Album
import com.example.vinylog.objects.MCollection
import com.example.vinylog.objects.MusicCollection
import com.example.vinylog.ui.Book
import com.example.vinylog.ui.CompactDisc
import com.example.vinylog.ui.GameCartridge
import com.example.vinylog.ui.MovieCase
import com.example.vinylog.ui.VinylRecord
import com.example.vinylog.ui.theme.VinyLogTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (!File(filesDir, "lib").exists()) {
            openFileOutput("lib", MODE_PRIVATE).use {
                it.write("{}".toByteArray())
            }
        }
        setContent {
            VinyLogTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val hole = MaterialTheme.colorScheme.background
                    Column(
                        Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Select Library",
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        Column(Modifier.weight(1f).fillMaxWidth()) {
                            Row(Modifier.weight(1f).fillMaxWidth()) {
                                LibraryChoice(
                                    label = "Vinyl",
                                    onClick = {
                                        startActivity(Intent(this@MainActivity, AlbumView::class.java).putExtra("mt", "Vinyl"))
                                    },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                ) {
                                    VinylRecord(holeColor = hole)
                                }
                                LibraryChoice(
                                    label = "CDs",
                                    onClick = {
                                        startActivity(Intent(this@MainActivity, AlbumView::class.java).putExtra("mt", "CDs"))
                                    },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                ) {
                                    CompactDisc(holeColor = hole)
                                }
                            }
                            Row(Modifier.weight(1f).fillMaxWidth()) {
                                LibraryChoice(
                                    label = "Movies",
                                    onClick = {
                                        startActivity(Intent(this@MainActivity, AlbumView::class.java).putExtra("mt", "Movies"))
                                    },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                ) {
                                    MovieCase()
                                }
                                LibraryChoice(
                                    label = "Video Games",
                                    onClick = {
                                        startActivity(Intent(this@MainActivity, GameLibraryActivity::class.java))
                                    },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                ) {
                                    GameCartridge()
                                }
                            }
                            Row(Modifier.weight(1f).fillMaxWidth()) {
                                LibraryChoice(
                                    label = "Books",
                                    onClick = {
                                        startActivity(Intent(this@MainActivity, AlbumView::class.java).putExtra("mt", "Books"))
                                    },
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                ) {
                                    Book()
                                }
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryChoice(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    art: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                art()
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        )
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