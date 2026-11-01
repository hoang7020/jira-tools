package com.jiratools.domain

/**
 * Các cột có thể hiển thị trong bảng ticket.
 *
 * `label` là nhãn tiếng Việt hiển thị trên tiêu đề cột, `jqlField` là tên trường dùng khi
 * sinh mệnh đề `ORDER BY` phía Jira.
 */
enum class ColumnKey(
    val label: String,
    val jqlField: String?,
) {
    KEY("Mã", "key"),
    SUMMARY("Tiêu đề", "summary"),
    STATUS("Trạng thái", "status"),
    ASSIGNEE("Người xử lý", "assignee"),
    PRIORITY("Độ ưu tiên", "priority"),
    UPDATED("Cập nhật", "updated"),
    DUE_DATE("Due Date", "duedate"),
    ORIGINAL_EXPECTED_TIME("Original Expected Time", "originalEstimate"),
    DYNAMIC_START_TIME("Dynamic Start Time", null),
    DYNAMIC_DUE_DATE("Dynamic Due Date", null),
    REAL_DUE_DATE("Real Due Date", null),
    ;

    companion object {
        /** Các cột query được phép cấu hình; Dynamic columns chỉ thuộc saved tables. */
        fun queryColumns(): List<ColumnKey> = entries.filter { it !in savedOnlyColumns }

        /** Toàn bộ cột khả dụng cho saved tables. */
        fun savedTableColumns(): Set<ColumnKey> = entries.toSet()

        /** Các cột được bổ sung khi nâng saved-table catalog từ schema v1 lên v2. */
        fun schemaV2SavedColumns(): Set<ColumnKey> = setOf(ORIGINAL_EXPECTED_TIME, DYNAMIC_START_TIME, DYNAMIC_DUE_DATE)

        /** Các cột saved-only mới nhất được bổ sung khi lưu bảng. */
        fun schemaV3SavedColumns(): Set<ColumnKey> = schemaV2SavedColumns() + REAL_DUE_DATE

        /** Query columns mặc định cho [UserPreferences.visibleColumns]. */
        fun all(): Set<ColumnKey> = queryColumns().toSet()

        /** Bộ cột mặc định được lưu trong schema cấu hình 3, trước khi có Due Date. */
        fun schemaV3QueryColumns(): Set<ColumnKey> =
            setOf(KEY, SUMMARY, STATUS, ASSIGNEE, PRIORITY, UPDATED, ORIGINAL_EXPECTED_TIME)

        private val savedOnlyColumns = setOf(DYNAMIC_START_TIME, DYNAMIC_DUE_DATE, REAL_DUE_DATE)
    }
}
