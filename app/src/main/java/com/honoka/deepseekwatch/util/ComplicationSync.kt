package com.honoka.deepseekwatch.util

import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.honoka.deepseekwatch.complication.BalanceComplicationService

/**
 * 主动通知系统刷新表盘小组件。
 *
 * 实现说明：Wear OS 4+ 系统固件内置 androidx.wear.complications.ComplicationManager，
 * 但该 API 未随任何可用的 compileOnly 库发布（wearable 2.9.0 / complications alpha 均无），
 * 因此采用运行时反射调用——设备上有该类则生效，没有则静默跳过（由系统周期兜底）。
 */
object ComplicationSync {

    private const val TAG = "ComplicationSync"

    fun pushUpdate(context: Context) {
        try {
            val cls = Class.forName("androidx.wear.complications.ComplicationManager")
            val ctor = cls.getConstructor(Context::class.java)
            val manager = ctor.newInstance(context)
            val method = cls.getMethod("notifyComplicationUpdate", ComponentName::class.java)
            method.invoke(
                manager,
                ComponentName(context, BalanceComplicationService::class.java)
            )
        } catch (e: Exception) {
            Log.w(TAG, "notifyComplicationUpdate unavailable or failed: ${e.message}")
        }
    }
}
