package com.jiratools.domain

/**
 * Dẫn xuất địa chỉ mở một ticket trong Jira từ địa chỉ đã lưu và mã ticket (FR-013).
 *
 * Địa chỉ được chuẩn hoá lại trước khi ghép để không sinh ra dấu `/` trùng và luôn có scheme.
 * Trả về `null` khi chưa có địa chỉ hợp lệ hoặc mã ticket rỗng, nhờ đó tầng gọi hiển thị thông
 * báo thay vì mở một địa chỉ sai.
 */
fun browseUrl(
    baseUrl: String,
    key: String,
): String? {
    val normalized =
        when (val result = normalizeBaseUrl(baseUrl)) {
            is BaseUrlResult.Valid -> result.baseUrl
            is BaseUrlResult.Invalid -> return null
        }
    val ticketKey = key.trim()
    if (ticketKey.isEmpty()) return null
    return "$normalized/browse/$ticketKey"
}
