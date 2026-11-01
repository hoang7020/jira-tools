package com.jiratools.ui.query

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jiratools.domain.ColumnKey

/** Bề rộng làn dành cho thanh cuộn dọc ở mép phải vùng bảng. */
val TABLE_SCROLLBAR_LANE_WIDTH = 12.dp

private val FROZEN_KEY_WIDTH = 110.dp
private val SUMMARY_MIN_WIDTH = 400.dp
private val STATUS_WIDTH = 130.dp
private val ASSIGNEE_WIDTH = 150.dp
private val PRIORITY_WIDTH = 120.dp
private val UPDATED_WIDTH = 130.dp
private val DUE_DATE_WIDTH = 130.dp
private val ORIGINAL_EXPECTED_TIME_WIDTH = 170.dp
private val DYNAMIC_START_TIME_WIDTH = 170.dp
private val DYNAMIC_DUE_DATE_WIDTH = 170.dp
private val REAL_DUE_DATE_WIDTH = 170.dp

/** Bề rộng cố định của cột [column]; `when` đầy đủ nên thêm cột mới sẽ báo lỗi biên dịch. */
private fun fixedWidthOf(column: ColumnKey): Dp =
    when (column) {
        ColumnKey.KEY -> FROZEN_KEY_WIDTH
        ColumnKey.STATUS -> STATUS_WIDTH
        ColumnKey.ASSIGNEE -> ASSIGNEE_WIDTH
        ColumnKey.PRIORITY -> PRIORITY_WIDTH
        ColumnKey.UPDATED -> UPDATED_WIDTH
        ColumnKey.DUE_DATE -> DUE_DATE_WIDTH
        ColumnKey.ORIGINAL_EXPECTED_TIME -> ORIGINAL_EXPECTED_TIME_WIDTH
        ColumnKey.DYNAMIC_START_TIME -> DYNAMIC_START_TIME_WIDTH
        ColumnKey.DYNAMIC_DUE_DATE -> DYNAMIC_DUE_DATE_WIDTH
        ColumnKey.REAL_DUE_DATE -> REAL_DUE_DATE_WIDTH
        ColumnKey.SUMMARY -> error("cột Tiêu đề co giãn theo khung nhìn, không có bề rộng cố định")
    }

/**
 * Bố cục cột của bảng kết quả: bề rộng cột Mã (ngoài vùng cuộn ngang) cùng bề rộng các cột còn lại.
 */
data class TableColumnLayout(
    val keyWidth: Dp,
    val widths: Map<ColumnKey, Dp>,
    val scrollableContentWidth: Dp,
    val scrollableViewportWidth: Dp,
) {
    /** `true` khi tổng bề rộng các cột còn lại vượt khung nhìn và người dùng cần cuộn ngang (FR-008). */
    val needsHorizontalScroll: Boolean
        get() = scrollableContentWidth > scrollableViewportWidth

    /** Bề rộng hiển thị của [column]; cột Mã luôn nằm ngoài vùng cuộn ngang (FR-011). */
    fun widthOf(column: ColumnKey): Dp = if (column == ColumnKey.KEY) keyWidth else widths.getValue(column)
}

/**
 * Tính bố cục cột cho bảng kết quả.
 *
 * Cột Tiêu đề co giãn lấp đầy phần còn lại khi dư chỗ, nhưng không nhỏ hơn [SUMMARY_MIN_WIDTH] — nhờ đó
 * bảng hiển thị gọn khi vừa khung nhìn (FR-010) và chuyển sang cuộn ngang khi thiếu chỗ (FR-008).
 *
 * @param visibleColumns danh sách cột đang bật, theo thứ tự hiển thị.
 * @param availableWidth bề rộng vùng bảng (đã trừ đệm của vùng truy vấn).
 */
fun computeTableColumnLayout(
    visibleColumns: List<ColumnKey>,
    availableWidth: Dp,
): TableColumnLayout {
    val scrollableViewportWidth =
        (availableWidth - FROZEN_KEY_WIDTH - TABLE_SCROLLBAR_LANE_WIDTH).coerceAtLeast(0.dp)
    val scrollableColumns = visibleColumns.filter { it != ColumnKey.KEY }
    val fixedOthersWidth =
        scrollableColumns
            .filter { it != ColumnKey.SUMMARY }
            .fold(0.dp) { sum, column -> sum + fixedWidthOf(column) }

    val widths =
        scrollableColumns.associateWith { column ->
            if (column == ColumnKey.SUMMARY) {
                (scrollableViewportWidth - fixedOthersWidth).coerceAtLeast(SUMMARY_MIN_WIDTH)
            } else {
                fixedWidthOf(column)
            }
        }

    return TableColumnLayout(
        keyWidth = FROZEN_KEY_WIDTH,
        widths = widths,
        scrollableContentWidth = widths.values.fold(0.dp) { sum, width -> sum + width },
        scrollableViewportWidth = scrollableViewportWidth,
    )
}
