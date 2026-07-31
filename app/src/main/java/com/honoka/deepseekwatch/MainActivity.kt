package com.honoka.deepseekwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
        setContent { DeepSeekWatchApp() }
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
