package com.github.s3rgeym.hh_resume_automate.ui.home

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.s3rgeym.hh_resume_automate.api.ApiClient

class HomeViewModelFactory(
    private val context: Context,
    private val client: ApiClient,
    private val sharedPrefs: SharedPreferences,
    private val snackbarHostState: SnackbarHostState,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(context.applicationContext, client, sharedPrefs, snackbarHostState) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}