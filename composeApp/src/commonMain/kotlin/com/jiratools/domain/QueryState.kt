package com.jiratools.domain

/**
 * Trạng thái của khu vực truy vấn, phơi từ `QueryViewModel` qua `StateFlow` (Nguyên tắc III).
 *
 * `Error` luôn mang theo `jql` đã nhập để người dùng sửa lại và chạy tiếp (FR-009).
 */
sealed interface QueryState {
    /** Chưa chạy truy vấn nào trong phiên này. */
    data object Idle : QueryState

    /** Đang tải trang đầu tiên (FR-010). */
    data object Loading : QueryState

    /** Có kết quả; `hasMore` cho biết còn trang tiếp để tải thêm (FR-007). */
    data class Success(
        val tickets: List<Ticket>,
        val hasMore: Boolean = false,
        val approximateTotal: Int? = null,
    ) : QueryState

    /** Truy vấn hợp lệ nhưng không có ticket nào (FR-008). */
    data object Empty : QueryState

    /** Truy vấn thất bại với lỗi đã phân loại (FR-009). */
    data class Error(
        val error: QueryError,
        val jql: String,
    ) : QueryState
}
