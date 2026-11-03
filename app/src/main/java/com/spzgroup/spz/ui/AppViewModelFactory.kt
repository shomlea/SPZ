package com.spzgroup.spz.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.spzgroup.spz.data.AppRepository
import com.spzgroup.spz.ui.home.HomeViewModel
import com.spzgroup.spz.ui.library.LibraryViewModel
import com.spzgroup.spz.ui.player.PlayerViewModel

// Lets every ViewModel receive the repository in its constructor.
class AppViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel = when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(repository)
            modelClass.isAssignableFrom(PlayerViewModel::class.java) -> PlayerViewModel(repository)
            modelClass.isAssignableFrom(LibraryViewModel::class.java) -> LibraryViewModel(repository)
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
        return viewModel as T
    }
}