package com.honoka.deepseekwatch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.rememberScalingLazyListState
import com.honoka.deepseekwatch.ui.theme.TerminalAmber
import com.honoka.deepseekwatch.ui.theme.TerminalGreen
import com.honoka.deepseekwatch.ui.theme.TerminalMono
import com.honoka.deepseekwatch.ui.theme.TerminalRed
import com.honoka.deepseekwatch.ui.theme.TerminalTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BalanceScreen(
    viewModel: BalanceViewModel,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberScalingLazyListState()

    TerminalBackground(Modifier.fillMaxSize()) {
        Scaffold(
            timeText = { TimeText() },
            modifier = Modifier.fillMaxSize()
        ) {
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .rotaryScroll(listState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(Modifier.height(10.dp))
                    // 终端标题行：等宽小字 + 青绿光标块
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            "DEEPSEEK·BALANCE",
                            fontSize = 12.sp,
                            fontFamily = TerminalMono,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colors.onSurfaceVariant
                        )
                        Box(
                            Modifier
                                .align(Alignment.CenterEnd)
                                .size(7.dp)
                                .background(TerminalTeal)
                        )
                    }
                }
                when (val s = state) {
                    is BalanceUiState.NoKey -> {
                        item {
                            Spacer(Modifier.height(24.dp))
                            Text(
                                "未配置 Key",
                                fontSize = 16.sp,
                                fontFamily = TerminalMono,
                                letterSpacing = 1.sp,
                                color = TerminalAmber,
                                textAlign = TextAlign.Center
                            )
                        }
                        item {
                            Spacer(Modifier.height(8.dp))
                            Chip(
                                onClick = onOpenSettings,
                                label = { Text("去设置") },
                                icon = { Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                colors = ChipDefaults.primaryChipColors()
                            )
                        }
                    }
                    is BalanceUiState.Loading -> item {
                        Spacer(Modifier.height(28.dp))
                        CircularProgressIndicator(modifier = Modifier.height(48.dp))
                        Spacer(Modifier.height(16.dp))
                    }
                    is BalanceUiState.Error -> {
                        item {
                            Spacer(Modifier.height(24.dp))
                            Text(
                                s.message,
                                fontSize = 13.sp,
                                fontFamily = TerminalMono,
                                color = TerminalRed,
                                textAlign = TextAlign.Center
                            )
                        }
                        item {
                            Spacer(Modifier.height(8.dp))
                            Chip(
                                onClick = { viewModel.refreshNow() },
                                label = { Text("重试") },
                                icon = { Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                colors = ChipDefaults.primaryChipColors()
                            )
                        }
                    }
                    is BalanceUiState.Success -> {
                        val info = s.balance.balanceInfos.firstOrNull()
                        if (info != null) {
                            item {
                                Spacer(Modifier.height(12.dp))
                                // 货币小字（等宽，上对齐）
                                Text(
                                    info.currency,
                                    fontSize = 13.sp,
                                    fontFamily = TerminalMono,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colors.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                            item {
                                // 余额大数字：JetBrains Mono Bold 40sp 青绿
                                Text(
                                    info.totalBalance,
                                    fontSize = 36.sp,
                                    fontFamily = TerminalMono,
                                    fontWeight = FontWeight.Bold,
                                    color = TerminalTeal,
                                    textAlign = TextAlign.Center,
                                    letterSpacing = 1.sp
                                )
                            }
                            item {
                                Spacer(Modifier.height(6.dp))
                                // 状态徽标：胶囊样式（半透明底色 + 圆角 + 文字）
                                val ok = s.balance.isAvailable
                                val statusColor = if (ok) TerminalGreen else TerminalRed
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(statusColor.copy(alpha = 0.18f))
                                        .border(1.dp, statusColor.copy(alpha = 0.6f), RoundedCornerShape(50))
                                        .padding(horizontal = 16.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (ok) "账户可用" else "账户不可用",
                                        fontSize = 11.sp,
                                        fontFamily = TerminalMono,
                                        letterSpacing = 1.sp,
                                        color = statusColor,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            item {
                                Spacer(Modifier.height(10.dp))
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 28.dp)
                                        .height(1.dp)
                                        .background(Color.White.copy(alpha = 0.08f))
                                )
                            }
                            item {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "GRANTED ${info.grantedBalance}  TOPUP ${info.toppedUpBalance}",
                                    fontSize = 10.sp,
                                    fontFamily = TerminalMono,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colors.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "SYNC ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(s.updatedAt))}",
                                fontSize = 9.sp,
                                fontFamily = TerminalMono,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colors.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                        item {
                            Spacer(Modifier.height(10.dp))
                            Chip(
                                onClick = { viewModel.refreshNow() },
                                label = { Text("刷新") },
                                icon = { Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                colors = ChipDefaults.primaryChipColors()
                            )
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(6.dp))
                    Chip(
                        onClick = onOpenHistory,
                        label = { Text("历史趋势") },
                        icon = { Icon(Icons.Filled.Timeline, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        colors = ChipDefaults.secondaryChipColors()
                    )
                }
                item {
                    Spacer(Modifier.height(6.dp))
                    Chip(
                        onClick = onOpenSettings,
                        label = { Text("设置") },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        colors = ChipDefaults.secondaryChipColors()
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
