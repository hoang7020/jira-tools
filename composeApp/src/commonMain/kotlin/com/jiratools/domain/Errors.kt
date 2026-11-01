package com.jiratools.domain

/**
 * Tập lỗi duy nhất dùng chung cho kết nối và truy vấn.
 *
 * Gộp hai luồng lỗi vào một kiểu giúp UI có đúng một nơi chuyển lỗi thành thông báo tiếng Việt,
 * đồng thời giữ nguyên nhân gốc để kiểm thử (Nguyên tắc V - đơn giản hoá).
 */
sealed interface JiraError {
    /** JQL bị Jira từ chối (HTTP 400) hoặc JQL rỗng do người dùng nhập. */
    data class InvalidJql(
        val message: String,
    ) : JiraError

    /** Sai email hoặc API token (HTTP 401). */
    data object AuthFailed : JiraError

    /** Token hợp lệ nhưng không đủ quyền (HTTP 403). */
    data object PermissionDenied : JiraError

    /** Bị giới hạn tần suất (HTTP 429). */
    data class RateLimited(
        val retryAfterSeconds: Int? = null,
    ) : JiraError

    /** Không phân giải được tên miền Jira. */
    data class InstanceNotFound(
        val host: String,
    ) : JiraError

    /** Không kết nối được tới Jira. */
    data object NetworkUnreachable : JiraError

    /** Jira phản hồi quá chậm. */
    data object Timeout : JiraError

    /** Lỗi còn lại; `status` và `detail` có thể không có. */
    data class Unexpected(
        val status: Int? = null,
        val detail: String? = null,
    ) : JiraError
}

typealias ConnectionError = JiraError

typealias QueryError = JiraError

/** Thông báo tiếng Việt hiển thị cho người dùng, không chứa thông tin bí mật (FR-003). */
fun JiraError.userMessage(): String =
    when (this) {
        is JiraError.InvalidJql -> message
        JiraError.AuthFailed -> "Email hoặc API token không đúng."
        JiraError.PermissionDenied -> "Tài khoản không có quyền thực hiện thao tác này trên Jira."
        is JiraError.RateLimited ->
            retryAfterSeconds
                ?.let { "Jira đang giới hạn truy cập, vui lòng thử lại sau $it giây." }
                ?: "Jira đang giới hạn truy cập, vui lòng thử lại sau."
        is JiraError.InstanceNotFound -> "Không tìm thấy Jira tại $host. Kiểm tra lại địa chỉ."
        JiraError.NetworkUnreachable -> "Không kết nối được tới Jira. Kiểm tra lại kết nối mạng."
        JiraError.Timeout -> "Jira phản hồi quá lâu. Vui lòng thử lại."
        is JiraError.Unexpected ->
            detail
                ?: status?.let { "Jira trả về lỗi không mong đợi (mã $it)." }
                ?: "Đã xảy ra lỗi không mong đợi."
    }
