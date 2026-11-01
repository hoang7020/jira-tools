package com.jiratools.platform

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val SCROLLBAR_THICKNESS = 8.dp
private val SCROLLBAR_MIN_LENGTH = 24.dp
private const val SCROLLBAR_HOVER_MILLIS = 300

/** Thanh cuộn dọc của bảng kết quả (widget chuẩn của Compose Desktop, không deprecated). */
@Composable
actual fun TableVerticalScrollbar(
    state: LazyListState,
    modifier: Modifier,
) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(state),
        modifier = modifier,
        style = tableScrollbarStyle(),
    )
}

/** Thanh cuộn ngang của vùng cột trượt; dùng chung [state] với mọi hàng để luôn đồng bộ. */
@Composable
actual fun TableHorizontalScrollbar(
    state: ScrollState,
    modifier: Modifier,
) {
    HorizontalScrollbar(
        adapter = rememberScrollbarAdapter(state),
        modifier = modifier,
        style = tableScrollbarStyle(),
    )
}

@Composable
private fun tableScrollbarStyle(): ScrollbarStyle {
    val outline = MaterialTheme.colorScheme.outline
    return remember(outline) {
        ScrollbarStyle(
            minimalHeight = SCROLLBAR_MIN_LENGTH,
            thickness = SCROLLBAR_THICKNESS,
            shape = RoundedCornerShape(SCROLLBAR_THICKNESS / 2),
            hoverDurationMillis = SCROLLBAR_HOVER_MILLIS,
            unhoverColor = outline.copy(alpha = 0.4f),
            hoverColor = outline,
        )
    }
}
