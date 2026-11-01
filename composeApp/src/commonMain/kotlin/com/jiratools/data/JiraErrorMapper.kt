package com.jiratools.data

import com.jiratools.domain.JiraError

/**
 * Chuyển lỗi HTTP và lỗi mạng thành [JiraError].
 *
 * Việc phân loại lỗi mạng dựa trên tên lớp của ngoại lệ (đi xuyên chuỗi `cause`) vì mã dùng
 * chung không được phép tham chiếu các lớp `java.net.*`. Nhờ đó toàn bộ logic phân loại nằm
 * trong `commonMain` và kiểm thử được bằng lớp ngoại lệ giả.
 */
object JiraErrorMapper {
    private const val MAX_DETAIL_LENGTH = 500

    private const val MAX_CAUSE_DEPTH = 8

    private val TIMEOUT_TYPES =
        setOf(
            "HttpRequestTimeoutException",
            "ConnectTimeoutException",
            "SocketTimeoutException",
        )

    private val HOST_NOT_FOUND_TYPES =
        setOf(
            "UnknownHostException",
            "UnresolvedAddressException",
        )

    private val UNREACHABLE_TYPES =
        setOf(
            "ConnectException",
            "NoRouteToHostException",
            "SocketException",
            "IOException",
        )

    private val SECRET_PATTERN = Regex("(?i)(basic|bearer)\\s+[A-Za-z0-9+/=._~-]+")

    fun fromStatus(
        status: Int,
        body: ErrorCollectionDto?,
        retryAfterSeconds: Int? = null,
    ): JiraError =
        when (status) {
            400 -> JiraError.InvalidJql(jiraMessage(body) ?: "Jira từ chối truy vấn JQL (mã 400).")
            401 -> JiraError.AuthFailed
            403 -> JiraError.PermissionDenied
            429 -> JiraError.RateLimited(retryAfterSeconds)
            else -> JiraError.Unexpected(status, jiraMessage(body))
        }

    fun fromThrowable(throwable: Throwable): JiraError {
        val causes = throwable.causeChain().toList()
        val hostNotFound = causes.firstOrNull { it::class.simpleName in HOST_NOT_FOUND_TYPES }
        if (hostNotFound != null) {
            return JiraError.InstanceNotFound(hostNotFound.message.orEmpty())
        }
        if (causes.any { it::class.simpleName in TIMEOUT_TYPES }) return JiraError.Timeout
        if (causes.any { it::class.simpleName in UNREACHABLE_TYPES }) return JiraError.NetworkUnreachable
        return JiraError.Unexpected(detail = redact(throwable.message))
    }

    /** Che mọi giá trị Basic/Bearer và giới hạn độ dài để token không lọt vào log hay UI (FR-003). */
    fun redact(text: String?): String? {
        val redacted = text?.replace(SECRET_PATTERN) { match -> "${match.groupValues[1]} ***" }
        return redacted?.take(MAX_DETAIL_LENGTH)
    }

    private fun jiraMessage(body: ErrorCollectionDto?): String? {
        val parts =
            (body?.errorMessages.orEmpty() + body?.errors?.values.orEmpty())
                .filter { it.isNotBlank() }
        if (parts.isEmpty()) return null
        return redact(parts.joinToString(" "))?.ifBlank { null }
    }

    private fun Throwable.causeChain(): Sequence<Throwable> =
        generateSequence(this) { it.cause }.distinct().take(MAX_CAUSE_DEPTH)
}
