package com.jiratools.domain

/**
 * Thông tin kết nối đã lưu dùng cho giao diện Cài đặt, không bao gồm giá trị token.
 *
 * Chỉ để đọc — không có luồng nào ghi token từ đây trở lại kho bí mật.
 */
data class SavedConnectionInfo(
    val baseUrl: String = "",
    val email: String = "",
    val tokenSaved: Boolean = false,
) {
    val hasSavedConnection: Boolean
        get() = baseUrl.isNotBlank() || email.isNotBlank() || tokenSaved
}
