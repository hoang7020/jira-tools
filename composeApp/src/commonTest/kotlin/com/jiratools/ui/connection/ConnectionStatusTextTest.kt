package com.jiratools.ui.connection

import com.jiratools.domain.ConnectionStatus
import com.jiratools.domain.JiraError
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ConnectionStatusTextTest {
    private val baseUrl = "https://jira.example.com"
    private val email = "person@example.com"
    private val token = "secret-token"

    @Test
    fun statusTextIsShortAndNeverShowsConnectionDetails() {
        val statuses =
            listOf(
                ConnectionStatus.Unconfigured to "Chưa cấu hình kết nối Jira.",
                ConnectionStatus.Validating to "Đang kiểm tra kết nối tới Jira…",
                ConnectionStatus.Unverified(baseUrl, email) to
                    "Chưa xác minh được kết nối Jira — kiểm tra mạng.",
                ConnectionStatus.Connected(baseUrl, email) to "Đã kết nối Jira.",
                ConnectionStatus.Invalid(JiraError.AuthFailed) to "Email hoặc API token không đúng — mở Cài đặt.",
            )

        statuses.forEach { (status, expected) ->
            val text = connectionStatusText(status)

            assertEquals(expected, text)
            assertFalse(text.contains(baseUrl), "Status text must not contain a Jira address")
            assertFalse(text.contains(email), "Status text must not contain an email")
            assertFalse(text.contains(token), "Status text must not contain a token")
        }
    }

    @Test
    fun invalidStatusShowsShortReasonWithoutConnectionDetailsOrErrorDetails() {
        val statuses =
            listOf(
                ConnectionStatus.Invalid(JiraError.InstanceNotFound(baseUrl)) to
                    "Không tìm thấy máy chủ Jira — mở Cài đặt.",
                ConnectionStatus.Invalid(JiraError.Unexpected(detail = "$baseUrl $email $token")) to
                    "Lỗi kết nối không mong đợi — mở Cài đặt.",
            )

        statuses.forEach { (status, expected) ->
            val text = connectionStatusText(status)

            assertEquals(expected, text)
            assertFalse(text.contains(baseUrl))
            assertFalse(text.contains(email))
            assertFalse(text.contains(token))
        }
    }

    @Test
    fun statusDetailProvidesSettingsGuidanceWithoutExposingTheToken() {
        assertEquals(
            "Nhập địa chỉ Jira, email và API token để bắt đầu.",
            connectionStatusDetail(ConnectionStatus.Unconfigured),
        )
        assertEquals(
            "Đang kiểm tra kết nối tới Jira…",
            connectionStatusDetail(ConnectionStatus.Validating),
        )
        assertEquals(
            "Đã có thông tin kết nối trên máy nhưng chưa xác minh được. Bạn vẫn có thể chạy truy vấn.",
            connectionStatusDetail(ConnectionStatus.Unverified(baseUrl, email)),
        )

        val connected = connectionStatusDetail(ConnectionStatus.Connected(baseUrl, email))
        assertContains(connected, baseUrl)
        assertContains(connected, email)
        assertFalse(connected.contains(token))

        val invalid = connectionStatusDetail(ConnectionStatus.Invalid(JiraError.AuthFailed))
        assertContains(invalid, "Email hoặc API token không đúng.")
        assertContains(invalid, "Cài đặt")
        assertFalse(invalid.contains(token))
        assertFalse(invalid.contains(baseUrl))
        assertFalse(invalid.contains(email))
    }
}
