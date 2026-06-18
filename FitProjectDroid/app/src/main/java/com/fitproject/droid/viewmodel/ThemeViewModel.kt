package com.fitproject.droid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fitproject.droid.data.ThemeRepository
import com.fitproject.droid.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ThemeRepository(application)

    val themeMode: StateFlow<AppThemeMode> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppThemeMode.DARK)

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }
}