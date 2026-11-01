package com.jiratools.ui.connection

import com.jiratools.domain.ConnectionStatus
import com.jiratools.domain.JiraError
import com.jiratools.domain.userMessage

/** Câu chữ ngắn cho dòng trạng thái trên màn hình chính. */
fun connectionStatusText(status: ConnectionStatus): String =
    when (status) {
        ConnectionStatus.Unconfigured -> "Chưa cấu hình kết nối Jira."
        ConnectionStatus.Validating -> "Đang kiểm tra kết nối tới Jira…"
        is ConnectionStatus.Unverified -> "Chưa xác minh được kết nối Jira — kiểm tra mạng."
        is ConnectionStatus.Connected -> "Đã kết nối Jira."
        is ConnectionStatus.Invalid -> "${status.error.connectionStatusReason()} — mở Cài đặt."
    }

private fun JiraError.connectionStatusReason(): String =
    when (this) {
        is JiraError.InvalidJql -> "Jira từ chối yêu cầu"
        JiraError.AuthFailed -> "Email hoặc API token không đúng"
        JiraError.PermissionDenied -> "Tài khoản không đủ quyền"
        is JiraError.RateLimited -> "Jira đang giới hạn truy cập"
        is JiraError.InstanceNotFound -> "Không tìm thấy máy chủ Jira"
        JiraError.NetworkUnreachable -> "Không kết nối được tới Jira"
        JiraError.Timeout -> "Jira phản hồi quá chậm"
        is JiraError.Unexpected -> "Lỗi kết nối không mong đợi"
    }

/** Câu chữ cho màn hình Cài đặt, kèm hướng dẫn thay token nếu Jira từ chối. */
fun connectionStatusDetail(status: ConnectionStatus): String =
    when (status) {
        ConnectionStatus.Unconfigured -> "Nhập địa chỉ Jira, email và API token để bắt đầu."
        ConnectionStatus.Validating -> "Đang kiểm tra kết nối tới Jira…"
        is ConnectionStatus.Unverified ->
            "Đã có thông tin kết nối trên máy nhưng chưa xác minh được. Bạn vẫn có thể chạy truy vấn."
        is ConnectionStatus.Connected -> "Kết nối thành công tới ${status.baseUrl} bằng tài khoản ${status.email}."
        is ConnectionStatus.Invalid -> "${status.error.userMessage()} Mở Cài đặt để nhập lại token."
    }
