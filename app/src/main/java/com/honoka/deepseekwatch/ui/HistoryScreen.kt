package com.honoka.deepseekwatch.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.rememberScalingLazyListState
import com.honoka.deepseekwatch.data.BalanceSnapshot
import com.honoka.deepseekwatch.ui.theme.TerminalAmber
import com.honoka.deepseekwatch.ui.theme.TerminalMono
import com.honoka.deepseekwatch.ui.theme.TerminalTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: BalanceViewModel, onBack: () -> Unit) {
    val history by viewModel.history.collectAsState()
    val listState = rememberScalingLazyListState()

    TerminalBackground(Modifier.fillMaxSize()) {
        Scaffold(timeText = { TimeText() }) {
            ScalingLazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .rotaryScroll(listState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "BALANCE·TREND",
                        fontSize = 13.sp,
                        fontFamily = TerminalMono,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                if (history.size >= 2) {
                    item {
                        Sparkline(
                            snapshots = history,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                    }
                    // Y 轴标注：余额最大/最小值
                    item {
                        Text(
                            "余额 MAX ¥${"%.2f".format(history.maxOf { it.b })} · MIN ¥${"%.2f".format(history.minOf { it.b })}",
                            fontSize = 10.sp,
                            fontFamily = TerminalMono,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    // 消耗与速率
                    item {
                        Spacer(Modifier.height(4.dp))
                        val spanHours = (history.last().t - history.first().t) / 3600_000.0
                        val consumed = history.first().b - history.last().b
                        val rate = if (spanHours > 0.05) consumed / spanHours else 0.0
                        Text(
                            "消耗 ¥${"%.2f".format(consumed)}" +
                                if (rate > 0) "（约 ¥${"%.3f".format(rate)}/小时）" else "",
                            fontSize = 11.sp,
                            fontFamily = TerminalMono,
                            letterSpacing = 0.5.sp,
                            color = TerminalAmber,
                            textAlign = TextAlign.Center
                        )
                    }
                    // X 轴标注：采样时间范围
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "采样 ${history.size} 次 · 跨度 ${"%.1f".format((history.last().t - history.first().t) / 3600_000.0)} 小时",
                            fontSize = 10.sp,
                            fontFamily = TerminalMono,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    item {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "时间 ${history.firstOrNull()?.let { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(it.t)) } ?: "--"} ~ " +
                                "${history.lastOrNull()?.let { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(it.t)) } ?: "--"}",
                            fontSize = 9.sp,
                            fontFamily = TerminalMono,
                            color = MaterialTheme.colors.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    item {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "暂无数据",
                            fontSize = 14.sp,
                            fontFamily = TerminalMono,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    item {
                        Text(
                            "打开 App 会自动采样余额",
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colors.onSurfaceVariant
                        )
                    }
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    Chip(
                        onClick = onBack,
                        label = { Text("返回") },
                        icon = { Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        colors = ChipDefaults.secondaryChipColors()
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

/** 带 XY 数值标注的趋势曲线：左侧 MAX/MIN（Y 轴）、右上最新值、底部起止时间（X 轴） */
@Composable
private fun Sparkline(snapshots: List<BalanceSnapshot>, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val values = snapshots.map { it.b }
    val labelStyle = TextStyle(
        fontFamily = TerminalMono,
        fontSize = 9.sp,
        color = Color(0xFF8B98A5)
    )
    val valueStyle = TextStyle(
        fontFamily = TerminalMono,
        fontSize = 10.sp,
        color = TerminalTeal
    )
    val timeFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    val t0Text = timeFormat.format(Date(snapshots.first().t))
    val t1Text = timeFormat.format(Date(snapshots.last().t))

    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val min = values.min()
        val max = values.max()
        val range = (max - min).coerceAtLeast(0.01)
        val leftPad = 44.dp.toPx()
        val rightPad = 40.dp.toPx()
        val bottomPad = 18.dp.toPx()
        val topPad = 8.dp.toPx()
        val plotW = size.width - leftPad - rightPad
        val plotH = size.height - topPad - bottomPad

        fun yOf(v: Double): Float = topPad + (plotH - ((v - min) / range * plotH)).toFloat()
        fun xOf(i: Int): Float = leftPad + if (values.size > 1) i * plotW / (values.size - 1) else 0f

        // 参考网格线（MAX / 中值 / MIN）
        val gridColor = Color.White.copy(alpha = 0.06f)
        listOf(max, (max + min) / 2, min).forEach { v ->
            drawLine(gridColor, Offset(leftPad, yOf(v)), Offset(size.width - rightPad, yOf(v)), 1f)
        }
        // 曲线
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = xOf(i)
            val y = yOf(v)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, TerminalTeal, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        // 起点（灰点）+ 终点（青点）
        drawCircle(
            Color(0xFF8B98A5), radius = 4.dp.toPx(),
            center = Offset(xOf(0), yOf(values.first()))
        )
        drawCircle(
            TerminalTeal, radius = 5.dp.toPx(),
            center = Offset(xOf(values.lastIndex), yOf(values.last()))
        )

        // Y 轴标注：MAX / MIN 数值（曲线左侧）
        drawText(
            textMeasurer, AnnotatedString("¥${"%.1f".format(max)}"),
            topLeft = Offset(2.dp.toPx(), yOf(max) - 7.dp.toPx()),
            style = labelStyle
        )
        drawText(
            textMeasurer, AnnotatedString("¥${"%.1f".format(min)}"),
            topLeft = Offset(2.dp.toPx(), yOf(min) + 2.dp.toPx()),
            style = labelStyle
        )
        // 最新余额值（曲线右上）
        drawText(
            textMeasurer, AnnotatedString("¥${"%.2f".format(values.last())}"),
            topLeft = Offset(size.width - rightPad + 4.dp.toPx(), yOf(values.last()) - 7.dp.toPx()),
            style = valueStyle
        )
        // X 轴标注：起止时间（底部）
        drawText(
            textMeasurer, AnnotatedString(t0Text),
            topLeft = Offset(leftPad, size.height - bottomPad + 3.dp.toPx()),
            style = labelStyle
        )
        drawText(
            textMeasurer, AnnotatedString(t1Text),
            topLeft = Offset(size.width - rightPad + 4.dp.toPx(), size.height - bottomPad + 3.dp.toPx()),
            style = labelStyle
        )
    }
}
