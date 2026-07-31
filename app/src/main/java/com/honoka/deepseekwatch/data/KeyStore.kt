package com.honoka.deepseekwatch.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// 注意：DataStore 文件名必须全局唯一，不能用 "settings"（与 SettingsStore 冲突）
private val Context.keyDataStore by preferencesDataStore(name = "keys")

@Serializable
data class ApiKeyEntry(val name: String, val key: String)

class KeyStore(private val context: Context) {

    private val entriesKey = stringPreferencesKey("api_keys")
    private val currentNameKey = stringPreferencesKey("current_key_name")
    private val json = Json { ignoreUnknownKeys = true }

    private fun decodeEntries(raw: String?): List<ApiKeyEntry> = raw?.let {
        runCatching { json.decodeFromString<List<ApiKeyEntry>>(it) }.getOrNull()
    } ?: emptyList()

    val apiKeys: Flow<List<ApiKeyEntry>> = context.keyDataStore.data.map { prefs ->
        decodeEntries(prefs[entriesKey])
    }

    val currentKeyName: Flow<String> = context.keyDataStore.data.map { prefs ->
        prefs[currentNameKey] ?: ""
    }

    val currentApiKey: Flow<String> = context.keyDataStore.data.map { prefs ->
        val entries = decodeEntries(prefs[entriesKey])
        val current = prefs[currentNameKey] ?: ""
        entries.firstOrNull { it.name == current }?.key
            ?: entries.firstOrNull()?.key
            ?: ""
    }

    suspend fun currentApiKeyValue(): String = currentApiKey.first()

    suspend fun save(entry: ApiKeyEntry, makeCurrent: Boolean = true) {
        context.keyDataStore.edit { prefs ->
            val updated = decodeEntries(prefs[entriesKey]).filterNot { it.name == entry.name } + entry
            prefs[entriesKey] = json.encodeToString(updated)
            if (makeCurrent) prefs[currentNameKey] = entry.name
        }
    }

    suspend fun remove(name: String) {
        context.keyDataStore.edit { prefs ->
            val updated = decodeEntries(prefs[entriesKey]).filterNot { it.name == name }
            prefs[entriesKey] = json.encodeToString(updated)
            if (prefs[currentNameKey] == name) {
                prefs[currentNameKey] = updated.firstOrNull()?.name ?: ""
            }
        }
    }

    suspend fun setCurrent(name: String) {
        context.keyDataStore.edit { prefs -> prefs[currentNameKey] = name }
    }
}
