package com.jiratools.ui.query

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jiratools.domain.ColumnKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Bố cục cột của bảng kết quả: cột Mã luôn cố định ngoài vùng cuộn ngang, cột Tiêu đề co giãn khi dư
 * chỗ và giữ tối thiểu khi thiếu chỗ (FR-008, FR-010, FR-011).
 */
class TableColumnLayoutTest {
    private val allColumns: List<ColumnKey> = ColumnKey.entries.filter { it != ColumnKey.REAL_DUE_DATE }

    /** Bề rộng vùng bảng khi cửa sổ ở kích thước tối thiểu 1024 dp trừ 32 dp đệm hai bên. */
    private val tableWidthAtMinimumWindow = 992.dp

    /** Cửa sổ rộng hơn tổng chiều rộng các cột saved table. */
    private val tableWidthOnWideWindow = 1800.dp

    @Test
    fun keyColumnKeepsDesignWidthForEveryColumnSet() {
        val columnSets =
            listOf(
                listOf(ColumnKey.KEY),
                listOf(ColumnKey.KEY, ColumnKey.SUMMARY),
                allColumns,
            )

        columnSets.forEach { columns ->
            val layout = computeTableColumnLayout(visibleColumns = columns, availableWidth = tableWidthOnWideWindow)

            assertEquals(
                expected = KEY_WIDTH_IN_TEST,
                actual = layout.widthOf(ColumnKey.KEY),
                message = "cột Mã phải giữ nguyên bề rộng cho tập cột $columns (FR-011)",
            )
        }
    }

    @Test
    fun onlyKeyColumnFitsWithoutHorizontalScroll() {
        val layout = computeTableColumnLayout(visibleColumns = listOf(ColumnKey.KEY), availableWidth = 992.dp)

        assertEquals(0.dp, layout.scrollableContentWidth)
        assertFalse(layout.needsHorizontalScroll)
    }

    @Test
    fun allColumnsNeedHorizontalScrollAtMinimumWindowWidth() {
        val layout = computeTableColumnLayout(visibleColumns = allColumns, availableWidth = tableWidthAtMinimumWindow)

        val expectedViewport = tableWidthAtMinimumWindow - layout.keyWidth - TABLE_SCROLLBAR_LANE_WIDTH

        assertEquals(expectedViewport, layout.scrollableViewportWidth)
        assertTrue(
            layout.needsHorizontalScroll,
            "đủ sáu cột ở cửa sổ tối thiểu phải cần cuộn ngang (US3, SC-006)",
        )
    }

    @Test
    fun allColumnsFitWithoutHorizontalScrollOnWideWindow() {
        val layout = computeTableColumnLayout(visibleColumns = allColumns, availableWidth = tableWidthOnWideWindow)

        assertFalse(layout.needsHorizontalScroll)
        assertEquals(
            layout.scrollableViewportWidth,
            layout.scrollableContentWidth,
            "khi dư chỗ, tổng bề rộng nội dung phải vừa đúng khung nhìn (không thừa khoảng trắng)",
        )
    }

    @Test
    fun summaryColumnAbsorbsSlackWhenColumnsFit() {
        val layout = computeTableColumnLayout(visibleColumns = allColumns, availableWidth = tableWidthOnWideWindow)
        val fixedOthers =
            sumOf(
                layout,
                ColumnKey.STATUS,
                ColumnKey.ASSIGNEE,
                ColumnKey.PRIORITY,
                ColumnKey.UPDATED,
                ColumnKey.DUE_DATE,
                ColumnKey.ORIGINAL_EXPECTED_TIME,
                ColumnKey.DYNAMIC_START_TIME,
                ColumnKey.DYNAMIC_DUE_DATE,
            )

        assertEquals(
            expected = layout.scrollableViewportWidth - fixedOthers,
            actual = layout.widthOf(ColumnKey.SUMMARY),
            message = "cột Tiêu đề phải lấp đầy phần còn lại khi các cột vừa khung nhìn (FR-010)",
        )
        assertTrue(layout.widthOf(ColumnKey.SUMMARY) > SUMMARY_MIN_WIDTH_IN_TEST)
    }

    @Test
    fun summaryColumnKeepsMinimumWidthWhenSpaceIsTight() {
        val layout = computeTableColumnLayout(visibleColumns = allColumns, availableWidth = tableWidthAtMinimumWindow)
        val fixedOthers =
            sumOf(
                layout,
                ColumnKey.STATUS,
                ColumnKey.ASSIGNEE,
                ColumnKey.PRIORITY,
                ColumnKey.UPDATED,
                ColumnKey.DUE_DATE,
                ColumnKey.ORIGINAL_EXPECTED_TIME,
                ColumnKey.DYNAMIC_START_TIME,
                ColumnKey.DYNAMIC_DUE_DATE,
            )

        assertEquals(SUMMARY_MIN_WIDTH_IN_TEST, layout.widthOf(ColumnKey.SUMMARY))
        assertEquals(fixedOthers + layout.widthOf(ColumnKey.SUMMARY), layout.scrollableContentWidth)
    }

    @Test
    fun scrollableContentWidthEqualsSumOfVisibleColumnWidths() {
        val layout = computeTableColumnLayout(visibleColumns = allColumns, availableWidth = tableWidthOnWideWindow)

        assertEquals(
            expected = layout.widths.values.fold(0.dp) { sum, width -> sum + width },
            actual = layout.scrollableContentWidth,
        )
        assertEquals(allColumns.size - 1, layout.widths.size, "cột Mã không nằm trong vùng cuộn ngang")
    }

    @Test
    fun narrowColumnSetFitsWithoutHorizontalScrollOnWideWindow() {
        val layout =
            computeTableColumnLayout(
                visibleColumns = listOf(ColumnKey.KEY, ColumnKey.SUMMARY, ColumnKey.STATUS),
                availableWidth = tableWidthOnWideWindow,
            )

        assertFalse(layout.needsHorizontalScroll)
        assertEquals(layout.scrollableViewportWidth, layout.scrollableContentWidth)
    }

    @Test
    fun exposesLabelsAndWidthsForNewTimeColumns() {
        val expectedLabels =
            mapOf(
                "ORIGINAL_EXPECTED_TIME" to "Original Expected Time",
                "DYNAMIC_START_TIME" to "Dynamic Start Time",
                "DYNAMIC_DUE_DATE" to "Dynamic Due Date",
                "REAL_DUE_DATE" to "Real Due Date",
                "DUE_DATE" to "Due Date",
            )
        val columns = ColumnKey.entries.filter { it.name in expectedLabels.keys }
        val actualLabels = columns.associate { it.name to it.label }
        val layout = computeTableColumnLayout(visibleColumns = ColumnKey.entries.toList(), availableWidth = 1200.dp)

        assertEquals(expectedLabels, actualLabels)
        assertEquals(expectedLabels.size, columns.size)
        columns.forEach { column ->
            assertTrue(layout.widthOf(column) > 0.dp, "${column.name} must have a positive width")
        }
    }

    private fun sumOf(
        layout: TableColumnLayout,
        vararg columns: ColumnKey,
    ): Dp = columns.fold(0.dp) { sum, column -> sum + layout.widthOf(column) }

    private companion object {
        val KEY_WIDTH_IN_TEST = 110.dp
        val SUMMARY_MIN_WIDTH_IN_TEST = 400.dp
    }
}
