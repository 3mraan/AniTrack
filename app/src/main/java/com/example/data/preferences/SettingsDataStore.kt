package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "anitrack_settings")

data class UserSettings(
    val isDarkTheme: Boolean = true,
    val episodeReminders: Boolean = true,
    val releaseNotifications: Boolean = true,
    val defaultSort: String = "Recently Updated",
    val defaultViewGrid: Boolean = true,
    val autoStatusSuggestions: Boolean = true,
    val lastBackupLabel: String = "Synced Today",
    val recentSearches: List<String> = emptyList(),
    val tutorialCompleted: Boolean = false,
    val languageTag: String = "" // empty = follow device system language on first entry
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val EPISODE_REMINDERS = booleanPreferencesKey("episode_reminders")
        val RELEASE_NOTIFICATIONS = booleanPreferencesKey("release_notifications")
        val DEFAULT_SORT = stringPreferencesKey("default_sort")
        val DEFAULT_VIEW_GRID = booleanPreferencesKey("default_view_grid")
        val AUTO_STATUS_SUGGESTIONS = booleanPreferencesKey("auto_status_suggestions")
        val LAST_BACKUP_LABEL = stringPreferencesKey("last_backup_label")
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches")
        val TUTORIAL_COMPLETED = booleanPreferencesKey("tutorial_completed")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val searchesRaw = prefs[Keys.RECENT_SEARCHES] ?: ""
        UserSettings(
            isDarkTheme = prefs[Keys.IS_DARK_THEME] ?: true,
            episodeReminders = prefs[Keys.EPISODE_REMINDERS] ?: true,
            releaseNotifications = prefs[Keys.RELEASE_NOTIFICATIONS] ?: true,
            defaultSort = prefs[Keys.DEFAULT_SORT] ?: "Recently Updated",
            defaultViewGrid = prefs[Keys.DEFAULT_VIEW_GRID] ?: true,
            autoStatusSuggestions = prefs[Keys.AUTO_STATUS_SUGGESTIONS] ?: true,
            lastBackupLabel = prefs[Keys.LAST_BACKUP_LABEL] ?: "Synced Today",
            recentSearches = searchesRaw.split("|").map { it.trim() }.filter { it.isNotEmpty() },
            tutorialCompleted = prefs[Keys.TUTORIAL_COMPLETED] ?: false,
            languageTag = prefs[Keys.LANGUAGE_TAG] ?: ""
        )
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { it[Keys.IS_DARK_THEME] = isDark }
    }

    suspend fun setEpisodeReminders(enabled: Boolean) {
        context.dataStore.edit { it[Keys.EPISODE_REMINDERS] = enabled }
    }

    suspend fun setReleaseNotifications(enabled: Boolean) {
        context.dataStore.edit { it[Keys.RELEASE_NOTIFICATIONS] = enabled }
    }

    suspend fun setDefaultSort(sort: String) {
        context.dataStore.edit { it[Keys.DEFAULT_SORT] = sort }
    }

    suspend fun setDefaultViewGrid(isGrid: Boolean) {
        context.dataStore.edit { it[Keys.DEFAULT_VIEW_GRID] = isGrid }
    }

    suspend fun setAutoStatusSuggestions(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_STATUS_SUGGESTIONS] = enabled }
    }

    suspend fun updateBackupLabel(label: String) {
        context.dataStore.edit { it[Keys.LAST_BACKUP_LABEL] = label }
    }

    suspend fun addRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.length < 2) return
        context.dataStore.edit { prefs ->
            val current = (prefs[Keys.RECENT_SEARCHES] ?: "")
                .split("|").map { it.trim() }.filter { it.isNotEmpty() }
            val updated = (listOf(clean) + current.filterNot { it.equals(clean, ignoreCase = true) }).take(5)
            prefs[Keys.RECENT_SEARCHES] = updated.joinToString("|")
        }
    }

    suspend fun setTutorialCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.TUTORIAL_COMPLETED] = completed }
    }

    suspend fun setLanguageTag(tag: String) {
        context.dataStore.edit { it[Keys.LANGUAGE_TAG] = tag }
    }
}
