package com.minicut.timer.ui.util

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.minicut.timer.MiniCutApplication
import com.minicut.timer.data.repository.MiniCutRepository

val Context.miniCutRepository: MiniCutRepository
    get() = (applicationContext as MiniCutApplication).container.repository

inline fun <VM : ViewModel> miniCutViewModelFactory(
    crossinline create: () -> VM,
): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
    }
