package com.jiratools.domain

import kotlinx.serialization.Serializable

/** Phiên bản lược đồ file cấu hình; tăng khi cấu trúc JSON thay đổi không tương thích. */
const val CURRENT_SCHEMA_VERSION: Int = 4

/** Giao diện màu được chọn trong Cài đặt. */
@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/** Trạng thái sắp xếp hiện tại của bảng ticket. */
@Serializable
data class SortState(
    val column: ColumnKey? = null,
    val ascending: Boolean = true,
)

/** Tuỳ chọn người dùng được lưu giữa các lần chạy (FR-010, FR-012). */
@Serializable
data class UserPreferences(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val baseUrl: String? = null,
    val email: String? = null,
    val visibleColumns: Set<ColumnKey> = ColumnKey.all(),
    val lastJql: String? = null,
    val sort: SortState? = null,
)
