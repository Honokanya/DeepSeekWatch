package com.honoka.deepseekwatch.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class BalanceRepository(context: Context) {

    private val api = DeepSeekApi()
    private val keyStore = KeyStore(context.applicationContext)
    private val historyStore = HistoryStore(context.applicationContext)
    private val settingsStore = SettingsStore(context.applicationContext)

    val currentApiKey: Flow<String> = keyStore.currentApiKey
    val apiKeys: Flow<List<ApiKeyEntry>> = keyStore.apiKeys
    val history: Flow<List<BalanceSnapshot>> = historyStore.history
    val lowBalanceThreshold: Flow<Double> = settingsStore.lowBalanceThreshold
    val refreshIntervalMs: Flow<Long> = settingsStore.refreshIntervalMs

    suspend fun currentApiKeyValue(): String = keyStore.currentApiKeyValue()
    suspend fun fetchBalance(key: String): BalanceResponse = api.fetchBalance(key)
    suspend fun saveKey(entry: ApiKeyEntry, makeCurrent: Boolean = true) = keyStore.save(entry, makeCurrent)
    suspend fun removeKey(name: String) = keyStore.remove(name)
    suspend fun setCurrentKey(name: String) = keyStore.setCurrent(name)

    /** 拉取余额并追加本地快照（失败静默，不影响 UI） */
    suspend fun recordSnapshot(key: String) {
        try {
            val resp = api.fetchBalance(key)
            resp.balanceInfos.firstOrNull()?.totalBalance?.toDoubleOrNull()?.let { b ->
                historyStore.append(BalanceSnapshot(System.currentTimeMillis(), b))
            }
        } catch (_: Exception) { }
    }

    suspend fun setThreshold(value: Double) = settingsStore.setLowBalanceThreshold(value)
    suspend fun setInterval(value: Long) = settingsStore.setRefreshIntervalMs(value)

    /** 调试用：注入演示余额历史（模拟 16 个采样点，跨度约 7.5 小时） */
    suspend fun seedDemoHistory() {
        val now = System.currentTimeMillis()
        val base = 11.65
        val snapshots = (0..15).map { i ->
            BalanceSnapshot(
                t = now - (15 - i) * 30 * 60_000L,
                b = base - i * 0.11
            )
        }
        snapshots.forEach { historyStore.append(it) }
    }
}
