package com.example.vinylog.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.vinylog.activities.AlbumViewModel

class AlbumViewModelFactory(private val application: android.app.Application, private val mediaType : String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlbumViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlbumViewModel(application,mediaType) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}