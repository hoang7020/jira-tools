package com.jiratools.domain

import kotlinx.serialization.Serializable

/** Phiên bản lược đồ file danh mục bảng đã lưu; tăng khi cấu trúc JSON thay đổi không tương thích. */
const val SAVED_TABLES_SCHEMA_VERSION: Int = 5

/**
 * Một bảng kết quả truy vấn được lưu cục bộ (data-model, mục 1).
 *
 * Bảng là ảnh chụp tại thời điểm lưu: nội dung chỉ đổi khi người dùng lưu bảng mới hoặc làm mới
 * chính bảng đó, không tham chiếu sống tới Jira. Không có trường bí mật nào ở đây — token chỉ nằm
 * trong kho bí mật của hệ điều hành (FR-016).
 */
@Serializable
data class SavedTable(
    val id: String,
    val name: String,
    val jql: String,
    val jiraBaseUrl: String,
    val savedAtMillis: Long,
    val refreshedAtMillis: Long? = null,
    val visibleColumns: Set<ColumnKey> = ColumnKey.savedTableColumns(),
    val tickets: List<Ticket> = emptyList(),
    val jiraTimeZone: String? = null,
    val burndownSnapshots: List<BurndownSnapshot> = emptyList(),
)

/** Mốc tổng hợp lịch sử số ticket còn lại trong một bảng đã lưu. */
@Serializable
data class BurndownSnapshot(
    val recordedAtMillis: Long,
    val ticketCount: Int,
    val remainingCount: Int,
    val unknownCategoryCount: Int,
) {
    init {
        require(ticketCount >= 0) { "ticketCount must be non-negative" }
        require(remainingCount in 0..ticketCount) { "remainingCount must be between zero and ticketCount" }
        require(unknownCategoryCount in 0..remainingCount) {
            "unknownCategoryCount must be between zero and remainingCount"
        }
    }
}

/** Mốc Dynamic Start Time dùng chung cho một issue tại một Jira site. */
@Serializable
data class DynamicStartTimeRecord(
    val normalizedJiraBaseUrl: String,
    val issueId: String,
    val startedAtMillis: Long,
    val displayValue: String,
)

/** Bảng đã chuyển vào thùng rác, giữ nguyên payload để có thể khôi phục đầy đủ. */
@Serializable
data class TrashedSavedTable(
    val table: SavedTable,
    val deletedAtMillis: Long,
)

/** Danh mục bảng đã lưu, tương ứng đúng một file `saved-tables.json` dưới ổ đĩa (data-model, mục 2). */
@Serializable
data class SavedTableCatalog(
    val schemaVersion: Int = SAVED_TABLES_SCHEMA_VERSION,
    val tables: List<SavedTable> = emptyList(),
    val dynamicStartTimes: List<DynamicStartTimeRecord> = emptyList(),
    val trashedTables: List<TrashedSavedTable> = emptyList(),
)

/**
 * Kết quả đọc danh mục (data-model, mục 3).
 *
 * Phân biệt [Empty] với [Unreadable] là điều kiện để cảnh báo dữ liệu hỏng hoạt động đúng: chỉ
 * [Unreadable] mới sinh cảnh báo và chặn mọi thao tác ghi, nhờ đó file hỏng không bị ghi đè.
 */
sealed interface CatalogLoadResult {
    /** Đọc thành công và đúng phiên bản lược đồ. */
    data class Loaded(
        val catalog: SavedTableCatalog,
    ) : CatalogLoadResult

    /** Chưa có file dữ liệu — trạng thái bình thường của người dùng mới. */
    data object Empty : CatalogLoadResult

    /** File có nhưng không đọc được (JSON hỏng, sai kiểu, khác phiên bản lược đồ, lỗi đọc). */
    data class Unreadable(
        val detail: String? = null,
    ) : CatalogLoadResult
}
