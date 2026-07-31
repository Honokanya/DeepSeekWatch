package com.honoka.deepseekwatch

import android.Manifest
import android.os.Bundle
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.honoka.deepseekwatch.ui.BalanceScreen
import com.honoka.deepseekwatch.ui.BalanceViewModel
import com.honoka.deepseekwatch.ui.HistoryScreen
import com.honoka.deepseekwatch.ui.SettingsScreen
import com.honoka.deepseekwatch.ui.theme.DeepSeekWatchTheme
import com.honoka.deepseekwatch.work.BalanceCheckWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 调试入口：adb shell am start -n com.honoka.deepseekwatch/.MainActivity --ez debug_seed_history true
        if (intent?.getBooleanExtra("debug_seed_history", false) == true) {
            val repo = com.honoka.deepseekwatch.data.BalanceRepository(applicationContext)
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { repo.seedDemoHistory() }
        }
        // 调试入口：adb shell am start -n com.honoka.deepseekwatch/.MainActivity --ez debug_fake_success true
        if (intent?.getBooleanExtra("debug_fake_success", false) == true) {
            BalanceViewModel.fakeSuccessEnabled = true
        }
        // 调试入口：adb shell am start -n com.honoka.deepseekwatch/.MainActivity --ez debug_check_now true
        if (intent?.getBooleanExtra("debug_check_now", false) == true) {
            WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<BalanceCheckWorker>().build())
        }
        scheduleBalanceCheck()
        requestNotificationPermission()
        setContent { DeepSeekWatchApp() }
    }

    /** 后台周期检查：每小时拉取余额，低余额时震动+通知，并同步表盘小组件 */
    private fun scheduleBalanceCheck() {
        val request = PeriodicWorkRequestBuilder<BalanceCheckWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "balance-check", ExistingPeriodicWorkPolicy.UPDATE, request
        )
    }

    /** Wear OS 5 (API 33+) 通知需要运行时授权 */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }
}

object Routes {
    const val BALANCE = "balance"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
}

@Composable
fun DeepSeekWatchApp(viewModel: BalanceViewModel = viewModel()) {
    DeepSeekWatchTheme {
        val navController = rememberSwipeDismissableNavController()
        val context = LocalContext.current
        val repository = remember { viewModel.repository }

        SwipeDismissableNavHost(
            navController = navController,
            startDestination = Routes.BALANCE
        ) {
            composable(Routes.BALANCE) {
                BalanceScreen(
                    viewModel = viewModel,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenHistory = { navController.navigate(Routes.HISTORY) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    repository = repository,
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
