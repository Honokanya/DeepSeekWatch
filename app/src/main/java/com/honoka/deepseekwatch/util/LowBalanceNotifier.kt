package com.honoka.deepseekwatch.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.honoka.deepseekwatch.R

/**
 * 低余额警告：马达震动 + 系统通知。
 * App 前台（ViewModel）与后台周期检查（Worker）共用，保证两处行为一致。
 */
object LowBalanceNotifier {

    const val CHANNEL_ID = "low_balance"
    private const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "余额预警", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "DeepSeek 余额低于预警值时提醒"
                    }
                )
            }
        }
    }

    fun notify(context: Context, balance: Double, threshold: Double) {
        ensureChannel(context)
        // 马达震动（两段式）
        (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
            ?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 120, 250), -1))
        // 系统通知（Wear OS 上会显示在通知流并伴随提示）
        val content = String.format("当前余额 ¥%.2f，已低于预警值 ¥%.2f", balance, threshold)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("DeepSeek 余额不足")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }
}
