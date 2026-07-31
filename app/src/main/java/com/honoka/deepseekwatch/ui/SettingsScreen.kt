package com.honoka.deepseekwatch.ui

import android.graphics.Bitmap
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.rememberScalingLazyListState
import com.honoka.deepseekwatch.data.ApiKeyEntry
import com.honoka.deepseekwatch.data.BalanceRepository
import com.honoka.deepseekwatch.data.QrImportServer
import com.honoka.deepseekwatch.util.generateQrBitmap
import com.honoka.deepseekwatch.util.getLocalIpv4
import com.honoka.deepseekwatch.ui.theme.TerminalSurface
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun SettingsScreen(repository: BalanceRepository, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    val apiKeys by repository.apiKeys.collectAsState(initial = emptyList())
    val threshold by repository.lowBalanceThreshold.collectAsState(initial = 5.0)
    val intervalMs by repository.refreshIntervalMs.collectAsState(initial = 60_000L)

    var showKeyEditor by remember { mutableStateOf(apiKeys.isEmpty()) }
    var editingName by remember { mutableStateOf("") }
    var editingKey by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    // 扫码导入状态
    var importing by remember { mutableStateOf(false) }
    var importStatus by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var serverUrl by remember { mutableStateOf("") }
    var server by remember { mutableStateOf<QrImportServer?>(null) }
    val listState = rememberScalingLazyListState()

    fun mask(k: String) = if (k.length <= 8) k.take(3) + "***" else k.take(5) + "..." + k.takeLast(4)

    TerminalBackground(Modifier.fillMaxSize()) {
        Scaffold(timeText = { TimeText() }) {
            ScalingLazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .rotaryScroll(listState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ---- API Key 区块 ----
            item {
                Spacer(Modifier.height(12.dp))
                Text("API Key", style = MaterialTheme.typography.title3)
            }
            if (showKeyEditor) {
                item {
                    WearTextField(
                        value = editingName,
                        onValueChange = { editingName = it },
                        placeholder = "名称(可选)"
                    )
                }
                item {
                    WearTextField(
                        value = editingKey,
                        onValueChange = { editingKey = it },
                        placeholder = "sk-...",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
                item {
                    Text(
                        "手机上复制 Key 后长按输入框粘贴（剪贴板共享）",
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                item {
                    Button(
                        enabled = editingKey.isNotBlank() && !busy,
                        onClick = {
                            scope.launch {
                                busy = true
                                val name = editingName.trim().ifEmpty { "key-${System.currentTimeMillis() % 10000}" }
                                repository.saveKey(ApiKeyEntry(name, editingKey.trim()))
                                editingKey = ""
                                editingName = ""
                                showKeyEditor = false
                                busy = false
                                onSaved()
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("保存")
                    }
                }
            } else {
                apiKeys.forEach { entry ->
                    item {
                        CompactButton(onClick = {
                            scope.launch { repository.setCurrentKey(entry.name) }
                        }) {
                            Text(mask(entry.key))
                        }
                    }
                }
                item {
                    CompactButton(onClick = { showKeyEditor = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("添加 Key")
                    }
                }
            }
            if (apiKeys.size > 1) {
                item {
                    Text(
                        "点击上方 Key 可切换当前使用",
                        fontSize = 10.sp,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )
                }
            }

            // ---- 扫码导入 ----
            item {
                CompactButton(onClick = {
                    if (server == null) {
                        val ip = getLocalIpv4()
                        if (ip == null) {
                            importStatus = "error"
                        } else {
                            val token = Random.nextLong().toString(16)
                            var srv: QrImportServer? = null
                            srv = QrImportServer(token) { name, key ->
                                scope.launch {
                                    val n = name.trim().ifEmpty { "key-${System.currentTimeMillis() % 10000}" }
                                    repository.saveKey(ApiKeyEntry(n, key.trim()))
                                    importStatus = "success"
                                    importing = false
                                    srv?.stop()
                                    server = null
                                    onSaved()
                                }
                            }
                            srv?.start()
                            server = srv
                            serverUrl = "http://$ip:${srv?.listeningPort}/input?t=$token"
                            qrBitmap = generateQrBitmap(serverUrl)
                            importStatus = ""
                            importing = true
                        }
                    }
                }) {
                    Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("扫码导入 Key")
                }
            }
            if (importStatus == "error") {
                item {
                    Text(
                        "无法获取局域网 IP，请确认手表已连接 WiFi",
                        fontSize = 10.sp,
                        color = MaterialTheme.colors.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ---- 低余额提醒 ----
            item {
                Spacer(Modifier.height(16.dp))
                Text("低余额提醒", style = MaterialTheme.typography.title3)
            }
            item {
                WearTextField(
                    value = if (threshold == 0.0) "" else threshold.toString(),
                    onValueChange = {
                        val v = it.toDoubleOrNull()
                        if (v != null && v >= 0) {
                            scope.launch { repository.setThreshold(v) }
                        }
                    },
                    placeholder = "阈值(元)，0 关闭",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            item {
                Text(
                    "余额低于阈值时振动提醒，当前: ${if (threshold == 0.0) "关闭" else "¥$threshold"}",
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }

            // ---- 自动刷新 ----
            item {
                Spacer(Modifier.height(16.dp))
                Text("自动刷新间隔", style = MaterialTheme.typography.title3)
            }
            val options = listOf(30_000L to "30秒", 60_000L to "1分钟", 300_000L to "5分钟")
            options.forEach { (ms, label) ->
                item {
                    CompactButton(
                        onClick = { scope.launch { repository.setInterval(ms) } }
                    ) { Text(if (intervalMs == ms) "✓ $label" else label) }
                }
            }

            // ---- 关于 ----
            item {
                Spacer(Modifier.height(16.dp))
                Text("关于", style = MaterialTheme.typography.title3)
            }
            item {
                Text(
                    "余额来自 DeepSeek 官方 API。用量统计官方未开放，本应用通过本地余额快照估算趋势，仅供参考。",
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // ---- 二维码导入 Dialog（不允许点外部关闭，防止误触丢码） ----
    if (importing) {
        Dialog(onDismissRequest = { }) {
            val dialogListState = rememberScalingLazyListState()
            ScalingLazyColumn(
                state = dialogListState,
                modifier = Modifier.rotaryScroll(dialogListState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text("用手机扫码输入 Key", style = MaterialTheme.typography.title3)
                }
                qrBitmap?.let { bmp ->
                    item {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "导入二维码",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                    }
                }
                item {
                    Text(serverUrl.replace("http://", ""), fontSize = 10.sp, textAlign = TextAlign.Center)
                }
                item {
                    Text(
                        "手机连同一 WiFi，扫码后在网页输入 Key。\n导入成功后自动关闭。",
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )
                }
                item {
                    CompactButton(onClick = {
                        server?.stop()
                        server = null
                        importing = false
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("取消")
                    }
                }
            }
        }
    }
    }
}

/** Wear 风格输入框（Wear Compose Material 已移除 TextField，用 BasicTextField 自绘） */
@Composable
private fun WearTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        textStyle = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onSurface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalSurface)
            .padding(12.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, color = MaterialTheme.colors.onSurfaceVariant, fontSize = 12.sp)
                }
                inner()
            }
        }
    )
}
