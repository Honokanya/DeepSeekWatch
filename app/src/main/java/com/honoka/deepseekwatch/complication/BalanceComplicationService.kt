package com.honoka.deepseekwatch.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.complications.ComplicationProviderService
import androidx.wear.complications.ComplicationRequest
import androidx.wear.complications.data.ComplicationData
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.PlainComplicationText
import androidx.wear.complications.data.ShortTextComplicationData
import com.honoka.deepseekwatch.MainActivity
import com.honoka.deepseekwatch.data.BalanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 表盘"复杂"小部件：SHORT_TEXT 显示当前余额（如 CNY 110.00）。
 * 未配置 Key 或请求失败时显示 "--"；点击打开 App。
 */
class BalanceComplicationService : ComplicationProviderService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repository: BalanceRepository by lazy { BalanceRepository(this) }

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        scope.launch {
            val key = repository.currentApiKeyValue()
            val text: String = if (key.isBlank()) {
                "--"
            } else {
                try {
                    val resp = repository.fetchBalance(key)
                    resp.balanceInfos.firstOrNull()
                        ?.let { "${it.currency} ${it.totalBalance}" }
                        ?: "--"
                } catch (_: Exception) {
                    "--"
                }
            }

            val launchIntent = Intent(this@BalanceComplicationService, MainActivity::class.java)
            val tapAction = PendingIntent.getActivity(
                this@BalanceComplicationService, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val data = ShortTextComplicationData.Builder(
                PlainComplicationText.Builder(text).build(),
                PlainComplicationText.Builder("DeepSeek 余额").build()
            )
                .setTitle(PlainComplicationText.Builder("DeepSeek").build())
                .setTapAction(tapAction)
                .build()

            runCatching { listener.onComplicationData(data) }
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return ShortTextComplicationData.Builder(
            PlainComplicationText.Builder("¥0.00").build(),
            PlainComplicationText.Builder("DeepSeek 余额").build()
        )
            .setTitle(PlainComplicationText.Builder("DeepSeek").build())
            .build()
    }
}
