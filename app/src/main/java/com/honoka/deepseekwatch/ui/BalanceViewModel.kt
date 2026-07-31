package com.honoka.deepseekwatch.ui

import android.app.Application
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.honoka.deepseekwatch.data.BalanceInfo
import com.honoka.deepseekwatch.data.BalanceRepository
import com.honoka.deepseekwatch.data.BalanceResponse
import com.honoka.deepseekwatch.data.BalanceSnapshot
import com.honoka.deepseekwatch.data.DeepSeekApiException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface BalanceUiState {
    data object Loading : BalanceUiState
    data class Success(val balance: BalanceResponse, val updatedAt: Long) : BalanceUiState
    data class Error(val message: String) : BalanceUiState
    data object NoKey : BalanceUiState
}

class BalanceViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        /** 调试用：不调 API，直接返回假成功余额（模拟器验证成功态 UI） */
        @Volatile
        var fakeSuccessEnabled = false
    }

    val repository = BalanceRepository(app)

    private val _uiState = MutableStateFlow<BalanceUiState>(BalanceUiState.Loading)
    val uiState: StateFlow<BalanceUiState> = _uiState.asStateFlow()

    private val _history = MutableStateFlow<List<BalanceSnapshot>>(emptyList())
    val history: StateFlow<List<BalanceSnapshot>> = _history.asStateFlow()

    private var refreshJob: Job? = null
    private var currentKey: String = ""
    private var intervalMs: Long = 60_000L
    private var threshold: Double = 5.0

    init {
        viewModelScope.launch {
            repository.currentApiKey.collect { key ->
                currentKey = key
                restart()
            }
        }
        viewModelScope.launch {
            repository.refreshIntervalMs.collect { intervalMs = it; restart() }
        }
        viewModelScope.launch {
            repository.lowBalanceThreshold.collect { threshold = it }
        }
        viewModelScope.launch {
            repository.history.collect { _history.value = it }
        }
    }

    private fun restart() {
        refreshJob?.cancel()
        if (currentKey.isBlank()) {
            _uiState.value = BalanceUiState.NoKey
        } else {
            refreshJob = viewModelScope.launch {
                while (isActive) {
                    fetch(currentKey)
                    delay(intervalMs)
                }
            }
        }
    }

    fun refreshNow() {
        if (currentKey.isNotBlank()) viewModelScope.launch { fetch(currentKey) }
    }

    private suspend fun fetch(key: String) {
        _uiState.value = BalanceUiState.Loading
        _uiState.value = try {
            val resp = if (fakeSuccessEnabled) {
                BalanceResponse(
                    isAvailable = true,
                    balanceInfos = listOf(BalanceInfo("CNY", "10.06", "0.00", "10.06"))
                )
            } else {
                repository.fetchBalance(key)
            }
            repository.recordSnapshot(key)
            checkLowBalance(resp)
            BalanceUiState.Success(resp, System.currentTimeMillis())
        } catch (e: DeepSeekApiException) {
            if (e.code == 401) BalanceUiState.Error("API Key 无效或已过期 (401)")
            else BalanceUiState.Error("HTTP ${e.code}")
        } catch (e: Exception) {
            BalanceUiState.Error("网络错误，请检查手表联网")
        }
    }

    private fun checkLowBalance(resp: BalanceResponse) {
        if (threshold <= 0) return
        val total = resp.balanceInfos.firstOrNull()?.totalBalance?.toDoubleOrNull() ?: return
        if (total < threshold) {
            val vibrator = getApplication<Application>()
                .getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 120, 250), -1))
        }
    }
}
