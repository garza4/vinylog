package com.example.vinylog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.vinylog.objects.Album
import com.example.vinylog.objects.MCollection
import com.example.vinylog.objects.MusicCollection
import com.example.vinylog.ui.theme.VinyLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        val albumIntent = Intent(this, AlbumView::class.java)
        setContent {
            VinyLogTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Greeting("Android")
                }
            }
        }
    }
}

var ALBUM_LIST = MusicCollection(
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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    LoadAlbumsFromFile();
}

@Composable
fun LoadAlbumsFromFile(){
    Column {
        ALBUM_LIST.collect.forEach{collection ->
            collection.forEach{group ->
                Button(onClick = { /*TODO*/ }) {
                    Text(text = group.collectionName)
                }
                LazyColumn{
                    group.albums.forEach{album ->
                        item {
                            Text(text = album.toString())
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VinyLogTheme {
        Greeting("Android")
    }
}