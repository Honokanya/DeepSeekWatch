package com.honoka.deepseekwatch.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private val lowBalanceThresholdKey = doublePreferencesKey("low_balance_threshold")
    private val refreshIntervalKey = longPreferencesKey("refresh_interval_ms")

    val lowBalanceThreshold: Flow<Double> = context.settingsDataStore.data.map { prefs ->
        prefs[lowBalanceThresholdKey] ?: 5.0
    }

    val refreshIntervalMs: Flow<Long> = context.settingsDataStore.data.map { prefs ->
        prefs[refreshIntervalKey] ?: 60_000L
    }

    suspend fun setLowBalanceThreshold(value: Double) {
        context.settingsDataStore.edit { it[lowBalanceThresholdKey] = value }
    }

    suspend fun setRefreshIntervalMs(value: Long) {
        context.settingsDataStore.edit { it[refreshIntervalKey] = value }
    }
}
