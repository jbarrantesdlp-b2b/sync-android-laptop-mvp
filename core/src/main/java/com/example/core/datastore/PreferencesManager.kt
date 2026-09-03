package com.example.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.model.ThemePack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("sync_prefs")

class PreferencesManager(private val context: Context) {
    companion object {
        val KEY_THEME = stringPreferencesKey("theme_pack")
        val KEY_FONT = stringPreferencesKey("font_family")
        val KEY_CONN = stringPreferencesKey("connection_status")
    }

    val themePackFlow: Flow<ThemePack> = context.dataStore.data
        .map { prefs ->
            val raw = prefs[KEY_THEME] ?: ThemePack.Minimal.name
            ThemePack.values().find { it.name == raw } ?: ThemePack.Minimal
        }

    val fontFamilyFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[KEY_FONT] ?: "Inter" }

    val connectionStatusFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[KEY_CONN] ?: "UNKNOWN" }

    suspend fun setTheme(theme: ThemePack) {
        context.dataStore.edit { prefs -> prefs[KEY_THEME] = theme.name }
    }

    suspend fun setFont(font: String) {
        context.dataStore.edit { prefs -> prefs[KEY_FONT] = font }
    }

    suspend fun setConnectionStatus(status: String) {
        context.dataStore.edit { prefs -> prefs[KEY_CONN] = status }
    }
}
