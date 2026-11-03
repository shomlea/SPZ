package com.spzgroup.spz.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.spzgroup.spz.data.AppRepository
import com.spzgroup.spz.ui.home.HomeViewModel
import com.spzgroup.spz.ui.library.LibraryViewModel
import com.spzgroup.spz.ui.player.PlayerViewModel
import com.spzgroup.spz.ui.reader.AudioPlayerViewModel
import com.spzgroup.spz.ui.reader.ReaderViewModel

// Lets every ViewModel receive the repository in its constructor.
class AppViewModelFactory(
    private val repository: AppRepository,
    private val appContext: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel = when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(repository)
            modelClass.isAssignableFrom(PlayerViewModel::class.java) -> PlayerViewModel(repository)
            modelClass.isAssignableFrom(LibraryViewModel::class.java) -> LibraryViewModel(repository)
            modelClass.isAssignableFrom(ReaderViewModel::class.java) -> ReaderViewModel(repository)
            modelClass.isAssignableFrom(AudioPlayerViewModel::class.java) ->
                AudioPlayerViewModel(repository, appContext)
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
        return viewModel as T
    }
}