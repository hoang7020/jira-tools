package com.jiratools.domain

/** Kết quả chuẩn hoá địa chỉ Jira do người dùng nhập. */
sealed interface BaseUrlResult {
    data class Valid(
        val baseUrl: String,
    ) : BaseUrlResult

    data class Invalid(
        val message: String,
    ) : BaseUrlResult
}

private val SUPPORTED_SCHEMES = setOf("http", "https")

private val SCHEME_SEPARATOR = "://"

private val HOST_PATTERN =
    Regex(
        "^[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?(\\.[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?)+$",
    )

/**
 * Chuẩn hoá địa chỉ Jira Cloud về dạng `scheme://host[:port]` để ghép với các endpoint REST.
 *
 * Quy tắc: cắt khoảng trắng hai đầu; thêm `https://` khi thiếu scheme; giữ nguyên `http://`
 * nếu người dùng chủ động nhập; bỏ đường dẫn, truy vấn, mảnh neo và dấu `/` ở cuối.
 *
 * Không bao giờ ném ngoại lệ: đầu vào không hợp lệ trả về [BaseUrlResult.Invalid].
 */
fun normalizeBaseUrl(raw: String): BaseUrlResult {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return BaseUrlResult.Invalid("Vui lòng nhập địa chỉ Jira.")

    val withScheme = if (trimmed.contains(SCHEME_SEPARATOR)) trimmed else "https://$trimmed"
    val schemeEnd = withScheme.indexOf(SCHEME_SEPARATOR)
    val scheme = withScheme.substring(0, schemeEnd).lowercase()
    if (scheme !in SUPPORTED_SCHEMES) {
        return BaseUrlResult.Invalid("Địa chỉ Jira chỉ hỗ trợ http hoặc https.")
    }

    val authority =
        withScheme
            .substring(schemeEnd + SCHEME_SEPARATOR.length)
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
    if (authority.isEmpty()) return BaseUrlResult.Invalid("Địa chỉ Jira không hợp lệ.")

    val host = authority.substringBefore(':')
    val port = authority.substringAfter(':', missingDelimiterValue = "")
    if (!HOST_PATTERN.matches(host)) return BaseUrlResult.Invalid("Địa chỉ Jira không hợp lệ.")
    if (port.isNotEmpty() && port.toIntOrNull() !in 1..65_535) {
        return BaseUrlResult.Invalid("Cổng của địa chỉ Jira không hợp lệ.")
    }

    return BaseUrlResult.Valid("$scheme://$host${if (port.isEmpty()) "" else ":$port"}")
}
