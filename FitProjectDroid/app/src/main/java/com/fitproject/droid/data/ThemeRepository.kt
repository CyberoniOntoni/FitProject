package com.fitproject.droid.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fitproject.droid.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore("app_theme")

class ThemeRepository(private val context: Context) {
    private val key = stringPreferencesKey("theme_mode")

    val themeMode: Flow<AppThemeMode> = context.themeDataStore.data.map { prefs ->
        when (prefs[key]) {
            AppThemeMode.LIGHT.name -> AppThemeMode.LIGHT
            else -> AppThemeMode.DARK
        }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.themeDataStore.edit { it[key] = mode.name }
    }
}