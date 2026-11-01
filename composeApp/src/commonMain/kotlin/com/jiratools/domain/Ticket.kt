package com.jiratools.domain

import kotlinx.serialization.Serializable

/**
 * Một ticket Jira đã rút gọn về đúng các trường bảng cần hiển thị (data-model, mục 2).
 *
 * `assignee` và `priority` có thể null vì Jira cho phép ticket chưa gán người xử lý hoặc chưa có
 * độ ưu tiên; giao diện hiển thị ô trống trong trường hợp đó.
 *
 * `@Serializable` chỉ để lưu kết quả truy vấn xuống file cục bộ; luồng truy vấn của 001 không đổi.
 */
@Serializable
data class Ticket(
    val key: String,
    val summary: String,
    val status: String,
    val assignee: String? = null,
    val priority: String? = null,
    val updated: String = "",
    val issueId: String? = null,
    val statusCategoryKey: String? = null,
    val updatedAtMillis: Long? = null,
    val originalExpectedTime: String? = null,
    val dynamicStartTime: String? = null,
    val dynamicStartAtMillis: Long? = null,
    val dynamicDueDate: String? = null,
    val dynamicDueAtMillis: Long? = null,
    val realDueDate: String? = null,
    val realDueAtMillis: Long? = null,
    val dueDate: String? = null,
    val parentKey: String? = null,
    val hasKnownSubtasks: Boolean = false,
    val isSubtask: Boolean = false,
    val isParent: Boolean = false,
)

/** Một trang kết quả tìm kiếm (data-model, mục 3). */
data class QueryPage(
    val tickets: List<Ticket>,
    val nextPageToken: String? = null,
    val isLast: Boolean = true,
    val approximateTotal: Int? = null,
)
