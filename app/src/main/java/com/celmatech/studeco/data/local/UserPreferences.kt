package com.celmatech.studeco.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "studeco_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // Encrypted storage for sensitive tokens (extra hardening)
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "studeco_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        private val KEY_FEED_FILTER = stringPreferencesKey("feed_filter")
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    val isDarkTheme: Flow<Boolean> = dataStore.data.map { it[KEY_DARK_THEME] ?: false }

    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { it[KEY_DARK_THEME] = enabled }
    }

    val feedFilter: Flow<String> = dataStore.data.map { it[KEY_FEED_FILTER] ?: "institution" }

    suspend fun setFeedFilter(filter: String) {
        dataStore.edit { it[KEY_FEED_FILTER] = filter }
    }

    // Secure session helpers (Supabase SDK also manages its own session)
    fun saveSecureString(key: String, value: String) {
        encryptedPrefs.edit().putString(key, value).apply()
    }

    fun getSecureString(key: String): String? = encryptedPrefs.getString(key, null)

    fun clearSecure() {
        encryptedPrefs.edit().clear().apply()
    }
}
