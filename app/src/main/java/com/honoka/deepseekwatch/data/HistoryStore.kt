package com.honoka.deepseekwatch.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BalanceSnapshot(val t: Long, val b: Double)

private val Context.historyDataStore by preferencesDataStore(name = "history")

class HistoryStore(private val context: Context) {

    private val historyKey = stringPreferencesKey("balance_history")
    private val json = Json { ignoreUnknownKeys = true }
    private val maxEntries = 200

    val history: Flow<List<BalanceSnapshot>> = context.historyDataStore.data.map { prefs ->
        prefs[historyKey]?.let {
            runCatching { json.decodeFromString<List<BalanceSnapshot>>(it) }.getOrNull()
        } ?: emptyList()
    }

    suspend fun append(snapshot: BalanceSnapshot) {
        context.historyDataStore.edit { prefs ->
            val current = prefs[historyKey]?.let {
                runCatching { json.decodeFromString<List<BalanceSnapshot>>(it) }.getOrNull()
            } ?: emptyList()
            prefs[historyKey] = json.encodeToString((current + snapshot).takeLast(maxEntries))
        }
    }
}
