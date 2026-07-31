package com.honoka.deepseekwatch.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.focusable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.wear.compose.material.ScalingLazyListState
import kotlinx.coroutines.launch

/**
 * 表冠/数字表圈旋转滚动支持（Galaxy Watch 7 rotary 事件 → 列表滚动）
 *
 * 官方要求（developer.android.com/training/wearables/compose/rotary-input）：
 * onRotaryScrollEvent 处理的组件必须可聚焦并持有焦点，否则 rotary 事件不派发。
 */
@Composable
fun Modifier.rotaryScroll(listState: ScalingLazyListState): Modifier {
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    return this
        .onRotaryScrollEvent { event ->
            scope.launch { listState.scroll { scrollBy(event.verticalScrollPixels) } }
            true
        }
        .focusRequester(focusRequester)
        .focusable()
}
