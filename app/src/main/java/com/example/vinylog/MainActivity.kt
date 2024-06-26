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
import androidx.room.Room
import com.example.vinylog.activities.AlbumView
import com.example.vinylog.objects.Album
import com.example.vinylog.objects.MCollection
import com.example.vinylog.objects.MusicCollection
import com.example.vinylog.objects.database.AppDb
import com.example.vinylog.ui.theme.VinyLogTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDb::class.java, "library"
        ).allowMainThreadQueries().build()
        super.onCreate(savedInstanceState)
        if(!File(filesDir, "lib").exists()){
            openFileOutput("lib", MODE_PRIVATE).use {
                it.write("{}".toByteArray())
            }
        }else{
            println("file exists")
            createLibStructure(db)
        }
        setContent {
            VinyLogTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column {
                        //TODO pass as parcel to second activity
                        val media = db.libraryDao().byMedia
                        val albums = db.libraryDao().all

                        val colls = createCollections(albums,media)
                        println("collection size " + albums.size)

                        colls.collect.forEach{collection ->
                            collection.forEach{group ->
                                Button(onClick = {
                                    val intent = Intent(this@MainActivity,AlbumView::class.java)
                                    with(intent){
                                        putExtra("group",group)
                                        putExtra("mt",group.collectionName)
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

fun createLibStructure(db: AppDb){
    val media = db.libraryDao().all
    println(media);
}

fun createCollections(albums:List<Album>, mediaTypes:List<String>):MusicCollection{
    //need a collection for each kind of collection
    val medias = mutableListOf<MCollection>()
    mediaTypes.forEach{media ->
        run {
            medias.add(MCollection(media, albums.filter { al -> al.mediaType == media }))
        }
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