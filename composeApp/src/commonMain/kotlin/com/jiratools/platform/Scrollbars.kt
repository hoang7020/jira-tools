package com.jiratools.platform

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Thanh cuộn dọc cho danh sách ticket: dùng widget theo quy ước của hệ điều hành/Compose Desktop.
 *
 * Đây là ranh giới nền tảng (Nguyên tắc I): widget thanh cuộn chỉ tồn tại trên Desktop nên phần dùng
 * chung chỉ khai báo `expect`, còn `actual` nằm trong source set JVM.
 */
@Composable
expect fun TableVerticalScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
)

/**
 * Thanh cuộn ngang cho vùng các cột trượt (không tính cột mã ticket đứng yên).
 *
 * Nhận trực tiếp [ScrollState] vì mọi hàng dữ liệu và hàng tiêu đề chia sẻ cùng một trạng thái cuộn.
 */
@Composable
expect fun TableHorizontalScrollbar(
    state: ScrollState,
    modifier: Modifier = Modifier,
)
