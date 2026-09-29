package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "security_settings")

class SecurityPreferences(private val context: Context) {

    companion object {
        val MASTER_PIN_HASH = stringPreferencesKey("master_pin_hash")
        val BIOMETRICS_ENABLED = booleanPreferencesKey("biometrics_enabled")
        val AUTO_LOCK_TIMEOUT_MINUTES = intPreferencesKey("auto_lock_timeout_minutes")
        val GENERATOR_LENGTH = intPreferencesKey("generator_length")
        val GENERATOR_UPPERCASE = booleanPreferencesKey("generator_uppercase")
        val GENERATOR_LOWERCASE = booleanPreferencesKey("generator_lowercase")
        val GENERATOR_NUMBERS = booleanPreferencesKey("generator_numbers")
        val GENERATOR_SYMBOLS = booleanPreferencesKey("generator_symbols")
        val GENERATOR_EXCLUDE_SIMILAR = booleanPreferencesKey("generator_exclude_similar")
    }

    val masterPinHash: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[MASTER_PIN_HASH]
    }

    val biometricsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BIOMETRICS_ENABLED] ?: true
    }

    val generatorLength: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[GENERATOR_LENGTH] ?: 16
    }

    val generatorUppercase: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[GENERATOR_UPPERCASE] ?: true
    }

    val generatorLowercase: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[GENERATOR_LOWERCASE] ?: true
    }

    val generatorNumbers: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[GENERATOR_NUMBERS] ?: true
    }

    val generatorSymbols: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[GENERATOR_SYMBOLS] ?: true
    }

    val generatorExcludeSimilar: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[GENERATOR_EXCLUDE_SIMILAR] ?: false
    }

    suspend fun saveMasterPinHash(hash: String) {
        context.dataStore.edit { preferences ->
            preferences[MASTER_PIN_HASH] = hash
        }
    }

    suspend fun setBiometricsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRICS_ENABLED] = enabled
        }
    }

    suspend fun saveGeneratorOptions(
        length: Int,
        uppercase: Boolean,
        lowercase: Boolean,
        numbers: Boolean,
        symbols: Boolean,
        excludeSimilar: Boolean
    ) {
        context.dataStore.edit { preferences ->
            preferences[GENERATOR_LENGTH] = length
            preferences[GENERATOR_UPPERCASE] = uppercase
            preferences[GENERATOR_LOWERCASE] = lowercase
            preferences[GENERATOR_NUMBERS] = numbers
            preferences[GENERATOR_SYMBOLS] = symbols
            preferences[GENERATOR_EXCLUDE_SIMILAR] = excludeSimilar
        }
    }
}
