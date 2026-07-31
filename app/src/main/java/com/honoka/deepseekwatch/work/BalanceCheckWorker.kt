package com.honoka.deepseekwatch.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.honoka.deepseekwatch.data.BalanceInfo
import com.honoka.deepseekwatch.data.BalanceRepository
import com.honoka.deepseekwatch.data.BalanceResponse
import com.honoka.deepseekwatch.ui.BalanceViewModel
import com.honoka.deepseekwatch.util.ComplicationSync
import com.honoka.deepseekwatch.util.LowBalanceNotifier

/**
 * 后台周期余额检查（WorkManager，每小时）：
 * 1. 拉取最新余额（即使不打开 App）
 * 2. 低于预警阈值 → 震动手表 + 系统通知
 * 3. 同步刷新表盘小组件
 */
class BalanceCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = BalanceRepository(applicationContext)
        val key = repo.currentApiKeyValue()
        if (key.isBlank()) return Result.success()

        return try {
            val resp = if (BalanceViewModel.fakeSuccessEnabled) {
                BalanceResponse(
                    isAvailable = true,
                    balanceInfos = listOf(BalanceInfo("CNY", "3.00", "0.00", "3.00"))
                )
            } else {
                repo.fetchBalance(key)
            }
            repo.recordSnapshot(key)

            val total = resp.balanceInfos.firstOrNull()?.totalBalance?.toDoubleOrNull()
            val threshold = repo.lowBalanceThresholdValue()
            if (total != null && threshold > 0 && total < threshold) {
                LowBalanceNotifier.notify(applicationContext, total, threshold)
            }

            ComplicationSync.pushUpdate(applicationContext)
            Result.success()
        } catch (_: Exception) {
            // 网络失败不重试堆积（WorkManager retry 会指数退避），等下个周期
            Result.success()
        }
    }
}
