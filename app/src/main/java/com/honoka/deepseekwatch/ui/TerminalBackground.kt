package com.honoka.deepseekwatch.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.honoka.deepseekwatch.ui.theme.TerminalBg
import com.honoka.deepseekwatch.ui.theme.TerminalTeal

/** 数据终端背景：纯黑底 + 细网格纹理 + 顶部青绿微光 */
@Composable
fun TerminalBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier = modifier.background(TerminalBg)) {
        Canvas(Modifier.fillMaxSize()) {
            val step = 26.dp.toPx()
            val gridColor = Color.White.copy(alpha = 0.035f)
            var x = 0f
            while (x <= size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += step
            }
            // 顶部青绿微光（径向渐变）
            val glowCenter = Offset(size.width * 0.5f, size.height * 0.16f)
            val glowRadius = size.width * 0.78f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(TerminalTeal.copy(alpha = 0.10f), Color.Transparent),
                    center = glowCenter,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = glowCenter
            )
        }
        content()
    }
}
