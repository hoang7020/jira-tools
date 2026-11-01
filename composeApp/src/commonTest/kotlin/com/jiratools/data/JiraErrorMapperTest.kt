package com.jiratools.data

import com.jiratools.domain.JiraError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class JiraErrorMapperTest {
    @Test
    fun status400TraVeInvalidJql() {
        val error =
            JiraErrorMapper.fromStatus(
                status = 400,
                body = ErrorCollectionDto(errorMessages = listOf("Error in the JQL Query: Expecting a field name.")),
            )

        assertEquals(
            "Error in the JQL Query: Expecting a field name.",
            assertIs<JiraError.InvalidJql>(error).message,
        )
    }

    @Test
    fun status401TraVeAuthFailed() {
        assertIs<JiraError.AuthFailed>(JiraErrorMapper.fromStatus(401, null))
    }

    @Test
    fun status403TraVePermissionDenied() {
        assertIs<JiraError.PermissionDenied>(JiraErrorMapper.fromStatus(403, null))
    }

    @Test
    fun status429TraVeRateLimitedKemRetryAfter() {
        val error = JiraErrorMapper.fromStatus(429, null, retryAfterSeconds = 30)

        assertEquals(30, assertIs<JiraError.RateLimited>(error).retryAfterSeconds)
    }

    @Test
    fun status429KhongCoRetryAfterVanHopLe() {
        val error = JiraErrorMapper.fromStatus(429, null, retryAfterSeconds = null)

        assertNull(assertIs<JiraError.RateLimited>(error).retryAfterSeconds)
    }

    @Test
    fun unknownHostTraVeInstanceNotFound() {
        val error = JiraErrorMapper.fromThrowable(UnknownHostException("sai-dia-chi.atlassian.net"))

        assertEquals("sai-dia-chi.atlassian.net", assertIs<JiraError.InstanceNotFound>(error).host)
    }

    @Test
    fun connectExceptionTraVeNetworkUnreachable() {
        assertIs<JiraError.NetworkUnreachable>(JiraErrorMapper.fromThrowable(ConnectException("refused")))
    }

    @Test
    fun ioExceptionTraVeNetworkUnreachable() {
        assertIs<JiraError.NetworkUnreachable>(JiraErrorMapper.fromThrowable(IOException("broken pipe")))
    }

    @Test
    fun hetThoiGianTraVeTimeout() {
        assertIs<JiraError.Timeout>(JiraErrorMapper.fromThrowable(HttpRequestTimeoutException("timeout")))
        assertIs<JiraError.Timeout>(JiraErrorMapper.fromThrowable(ConnectTimeoutException("connect timeout")))
    }

    @Test
    fun ngoaiLeLongTrongCauseTraVeDungLoai() {
        val wrapped = RuntimeException("outer", UnknownHostException("long-nhau.atlassian.net"))

        assertIs<JiraError.InstanceNotFound>(JiraErrorMapper.fromThrowable(wrapped))
    }

    @Test
    fun ngoaiLeKhacTraVeUnexpected() {
        val error = JiraErrorMapper.fromThrowable(IllegalStateException("loi la"))

        val unexpected = assertIs<JiraError.Unexpected>(error)
        assertNull(unexpected.status)
        assertEquals("loi la", unexpected.detail)
    }

    @Test
    fun statusKhongNhanDienTraVeUnexpectedKemStatus() {
        val error = JiraErrorMapper.fromStatus(503, ErrorCollectionDto(errorMessages = listOf("Service Unavailable")))

        val unexpected = assertIs<JiraError.Unexpected>(error)
        assertEquals(503, unexpected.status)
        assertEquals("Service Unavailable", unexpected.detail)
    }

    @Test
    fun thongBaoLoiKhongBaoGioChuaToken() {
        val token = "ATATT3xFfGF0SIEUBAOMAT"
        val body =
            ErrorCollectionDto(
                errorMessages =
                    listOf(
                        "Request failed with header Authorization: Basic dXNlckBleGFtcGxlLmNvbTpBVEFUVDN4RmZHRjA=",
                    ),
            )

        val error = JiraErrorMapper.fromStatus(500, body)
        val detail = assertIs<JiraError.Unexpected>(error).detail ?: fail("detail phai co gia tri")

        assertFalse(detail.contains(token), "thong bao loi khong duoc chua token")
        assertTrue(detail.contains("***"), "gia tri bi mat phai duoc che")
        assertFalse(detail.contains("dXNlckBleGFtcGxlLmNvbTpBVEFUVDN4RmZHRjA="))
    }

    @Test
    fun thongBaoLoiRongKhongGayLoi() {
        val error = JiraErrorMapper.fromStatus(500, ErrorCollectionDto())

        assertNull(assertIs<JiraError.Unexpected>(error).detail)
    }
}
