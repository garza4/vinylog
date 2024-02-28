package com.example.vinylog.activities

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val bundle = intent.extras
                    val myObj = bundle?.getSerializable("group") as? MCollection
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
}