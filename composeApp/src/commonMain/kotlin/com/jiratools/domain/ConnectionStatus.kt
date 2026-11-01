package com.jiratools.domain

/** Trạng thái kết nối Jira hiển thị trên vùng cấu hình. */
sealed interface ConnectionStatus {
    /** Chưa có thông tin kết nối nào được lưu. */
    data object Unconfigured : ConnectionStatus

    /** Đang kiểm tra kết nối tới Jira. */
    data object Validating : ConnectionStatus

    /** Đã có thông tin kết nối trên máy nhưng chưa xác minh được (mới mở ứng dụng hoặc lỗi mạng). */
    data class Unverified(
        val baseUrl: String,
        val email: String,
    ) : ConnectionStatus

    /** Đã kết nối thành công và thông tin đã được lưu. */
    data class Connected(
        val baseUrl: String,
        val email: String,
        val jiraTimeZone: String? = null,
    ) : ConnectionStatus

    /** Kết nối thất bại kèm nguyên nhân đã thân thiện hoá. */
    data class Invalid(
        val error: ConnectionError,
    ) : ConnectionStatus
}
