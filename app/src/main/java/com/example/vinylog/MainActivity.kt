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
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.vinylog.activities.AlbumView
import com.example.vinylog.objects.Album
import com.example.vinylog.objects.MusicList
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

var ALBUM_LIST = MusicList(
    listOf(
        listOf(
            Album("A","","","",""),
            Album("B","","","",""),
            Album("C","","","",""),
        )
    )
)

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
            text = "Hello $name!",
            modifier = modifier
    )
    loadAlbumsFromFile();
}

@Composable
fun loadAlbumsFromFile(){
    Button(onClick = {}) {
        Text("Record List")
    }
    Column {
        ALBUM_LIST.musicList.forEach { collection ->
            collection.forEach {album ->
                Text(
                    text=album.toString()
                )
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