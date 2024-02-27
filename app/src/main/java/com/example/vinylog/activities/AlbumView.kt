package com.example.vinylog.activities

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.example.vinylog.objects.MCollection
import com.example.vinylog.ui.theme.VinyLogTheme

class AlbumView : ComponentActivity(){
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VinyLogTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val bundle:MCollection? = intent.getParcelableExtra("group");
                    LazyColumn{
                        bundle?.albums?.forEach{album ->
                            item {
                                Text(text = album.toString())
                            }
                        }
                    }
                }
            }
        }
    }
}