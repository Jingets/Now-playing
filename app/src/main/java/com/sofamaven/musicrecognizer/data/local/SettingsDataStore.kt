package com.sofamaven.musicrecognizer.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sofamaven.musicrecognizer.data.model.AppSettings
import com.sofamaven.musicrecognizer.data.model.AudioInputSource
import com.sofamaven.musicrecognizer.data.model.RecognitionMode
import com.sofamaven.musicrecognizer.data.model.RecognitionService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tv_music_recognizer_prefs")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val KEY_SERVICE = stringPreferencesKey("key_service")
        private val KEY_AUDD_API_KEY = stringPreferencesKey("key_audd_api_key")
        private val KEY_ACR_HOST = stringPreferencesKey("key_acr_host")
        private val KEY_ACR_ACCESS_KEY = stringPreferencesKey("key_acr_access_key")
        private val KEY_ACR_ACCESS_SECRET = stringPreferencesKey("key_acr_access_secret")
        private val KEY_INPUT_SOURCE = stringPreferencesKey("key_input_source")
        private val KEY_RECOGNITION_MODE = stringPreferencesKey("key_recognition_mode")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        val serviceStr = preferences[KEY_SERVICE] ?: RecognitionService.AUDD.name
        val selectedService = try {
            RecognitionService.valueOf(serviceStr)
        } catch (_: Exception) {
            RecognitionService.AUDD
        }

        val inputSourceStr = preferences[KEY_INPUT_SOURCE] ?: AudioInputSource.REMOTE_MIC.name
        val inputSource = try {
            AudioInputSource.valueOf(inputSourceStr)
        } catch (_: Exception) {
            AudioInputSource.REMOTE_MIC
        }

        val modeStr = preferences[KEY_RECOGNITION_MODE] ?: RecognitionMode.MANUAL.name
        val mode = try {
            RecognitionMode.valueOf(modeStr)
        } catch (_: Exception) {
            RecognitionMode.MANUAL
        }

        AppSettings(
            selectedService = selectedService,
            auddApiKey = preferences[KEY_AUDD_API_KEY] ?: "",
            acrHost = preferences[KEY_ACR_HOST] ?: "identify-eu-west-1.acrcloud.com",
            acrAccessKey = preferences[KEY_ACR_ACCESS_KEY] ?: "",
            acrAccessSecret = preferences[KEY_ACR_ACCESS_SECRET] ?: "",
            inputSource = inputSource,
            recognitionMode = mode
        )
    }

    suspend fun saveService(service: RecognitionService) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVICE] = service.name
        }
    }

    suspend fun saveAuddApiKey(apiKey: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUDD_API_KEY] = apiKey.trim()
        }
    }

    suspend fun saveAcrCredentials(host: String, accessKey: String, accessSecret: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACR_HOST] = host.trim()
            prefs[KEY_ACR_ACCESS_KEY] = accessKey.trim()
            prefs[KEY_ACR_ACCESS_SECRET] = accessSecret.trim()
        }
    }

    suspend fun saveInputSource(source: AudioInputSource) {
        context.dataStore.edit { prefs ->
            prefs[KEY_INPUT_SOURCE] = source.name
        }
    }

    suspend fun saveRecognitionMode(mode: RecognitionMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_RECOGNITION_MODE] = mode.name
        }
    }

    suspend fun updateAllSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVICE] = settings.selectedService.name
            prefs[KEY_AUDD_API_KEY] = settings.auddApiKey.trim()
            prefs[KEY_ACR_HOST] = settings.acrHost.trim()
            prefs[KEY_ACR_ACCESS_KEY] = settings.acrAccessKey.trim()
            prefs[KEY_ACR_ACCESS_SECRET] = settings.acrAccessSecret.trim()
            prefs[KEY_INPUT_SOURCE] = settings.inputSource.name
            prefs[KEY_RECOGNITION_MODE] = settings.recognitionMode.name
        }
    }
}
