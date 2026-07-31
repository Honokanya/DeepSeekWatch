package com.honoka.deepseekwatch.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Typography
import com.honoka.deepseekwatch.R

// ---- 色板：Retro-futuristic 数据终端风（无紫/靛/洋红）----
val TerminalBg = Color(0xFF0A0E13)          // AMOLED 纯黑底
val TerminalSurface = Color(0xFF131A22)     // 次级面板
val TerminalTeal = Color(0xFF2DD4BF)        // 主 accent 青绿（余额）
val TerminalAmber = Color(0xFFF5A623)       // 辅助 accent 琥珀（警告/速率）
val TerminalGreen = Color(0xFF34D399)       // 成功
val TerminalRed = Color(0xFFF87171)         // 错误
val TerminalText = Color(0xFFE6EDF3)        // 主文字
val TerminalTextDim = Color(0xFF8B98A5)     // 次文字

// ---- 字体：JetBrains Mono（等宽终端感）----
val TerminalMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

private val TerminalColors = Colors(
    primary = TerminalTeal,
    primaryVariant = TerminalTeal,
    secondary = TerminalAmber,
    secondaryVariant = TerminalAmber,
    error = TerminalRed,
    onPrimary = Color(0xFF04211D),
    onSecondary = Color(0xFF2A1A00),
    onError = Color(0xFF2A0A0A),
    background = TerminalBg,
    onBackground = TerminalText,
    surface = TerminalSurface,
    onSurface = TerminalText,
    onSurfaceVariant = TerminalTextDim,
)

private val TerminalTypography = Typography(
    title1 = TextStyle(fontFamily = TerminalMono, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 0.5.sp),
    title2 = TextStyle(fontFamily = TerminalMono, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 0.5.sp),
    title3 = TextStyle(fontFamily = TerminalMono, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.5.sp),
    body1 = TextStyle(fontSize = 14.sp),
    body2 = TextStyle(fontSize = 12.sp),
    button = TextStyle(fontFamily = TerminalMono, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.5.sp),
    caption1 = TextStyle(fontFamily = TerminalMono, fontSize = 11.sp),
    caption2 = TextStyle(fontFamily = TerminalMono, fontSize = 10.sp),
)

@Composable
fun DeepSeekWatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = TerminalColors,
        typography = TerminalTypography,
        content = content
    )
}
