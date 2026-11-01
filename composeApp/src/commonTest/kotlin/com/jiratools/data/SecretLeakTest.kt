package com.jiratools.data

import com.jiratools.domain.ConnectionStatus
import com.jiratools.domain.JiraError
import com.jiratools.domain.userMessage
import com.jiratools.platform.FakeAppPaths
import com.jiratools.platform.FakeSecureStorage
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.util.encodeBase64
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Hồi quy chống rò rỉ bí mật: token chỉ được nằm trong kho bí mật của hệ điều hành, không bao giờ
 * xuất hiện trong file cấu hình hay thông báo lỗi (FR-003, T056).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SecretLeakTest {
    private val baseUrl = "https://congty.atlassian.net"

    private val email = "dev@congty.vn"

    private val token = "api-token-sieu-bi-mat"

    @Test
    fun fileCauHinhSauKhiKetNoiThanhCongKhongChuaToken() =
        runTest {
            val paths = FakeAppPaths()
            val storage = FakeSecureStorage()
            val repository = repository(engine = myselfEngine(), paths = paths, storage = storage)

            val status = repository.validate(baseUrl = baseUrl, email = email, token = token)

            assertEquals(ConnectionStatus.Connected(baseUrl = baseUrl, email = email), status)
            val content = paths.storedConfig()
            assertTrue(content != null && content.isNotBlank())
            assertFalse(content.contains(token), "File cấu hình không được chứa token")
            assertFalse(content.contains("Api-Token".lowercase()), "File cấu hình không được có khoá token")
            assertEquals(listOf(token), storage.savedTokens, "Token chỉ được lưu vào kho bí mật")
        }

    @Test
    fun fileCauHinhSauKhiChayTruyVanVanKhongChuaToken() =
        runTest {
            val paths = FakeAppPaths()
            val storage = FakeSecureStorage()
            val repository = repository(engine = myselfEngine(), paths = paths, storage = storage)
            repository.validate(baseUrl = baseUrl, email = email, token = token)

            val store = AppConfigStore(paths = paths, dispatcher = UnconfinedTestDispatcher())
            store.save(store.load().copy(lastJql = "project = HSP", baseUrl = baseUrl, email = email))

            val content = paths.storedConfig().orEmpty()
            assertFalse(content.contains(token), "Truy vấn gần nhất không được kéo theo token")
        }

    @Test
    fun loiXacThucKhongLoTokenTrongThongBao() =
        runTest {
            val paths = FakeAppPaths()
            val storage = FakeSecureStorage()
            val engine =
                MockEngine {
                    respond(
                        content = """{"errorMessages":["Client must be authenticated."],"errors":{}}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = JSON_HEADERS,
                    )
                }
            val repository = repository(engine = engine, paths = paths, storage = storage)

            val status = repository.validate(baseUrl = baseUrl, email = email, token = token)

            val invalid = status as ConnectionStatus.Invalid
            assertFalse(invalid.error.userMessage().contains(token))
            assertFalse(invalid.error.toString().contains(token))
        }

    @Test
    fun thongBaoCuaJiraErrorMapperKhongChuaChuoiBasic() {
        val body =
            ErrorCollectionDto(
                errorMessages = listOf("Basic ${"$email:$token".encodeBase64()} bị từ chối"),
            )

        val error = JiraErrorMapper.fromStatus(status = 400, body = body)

        val message = error.userMessage()
        assertFalse(message.contains(token), "Thông báo lỗi không được chứa token")
        assertTrue(message.contains("***"), "Giá trị xác thực phải được che bằng dấu ***")
    }

    @Test
    fun cheCaBearerTokenTrongChiTietLoi() {
        val redacted = JiraErrorMapper.redact("Gửi Bearer $token thất bại")

        assertEquals("Gửi Bearer *** thất bại", redacted)
    }

    @Test
    fun cheTokenTrongThongDiepCuaNgoaiLeMang() {
        val error = JiraErrorMapper.fromThrowable(IllegalStateException("header Authorization: Basic $token"))

        val detail = (error as JiraError.Unexpected).detail
        assertTrue(detail != null && detail.contains("***"))
        assertFalse(detail.contains(token))
    }

    @Test
    fun gioiHanDoDaiChiTietLoiDeKhongRoRiDuLieuDai() {
        val redacted = JiraErrorMapper.redact("x".repeat(2_000))

        assertEquals(500, redacted?.length)
    }

    @Test
    fun redactGiuNguyenNull() {
        assertNull(JiraErrorMapper.redact(null))
    }

    @Test
    fun thongDiepNgoaiLeKhongCoChuoiXacThucVanGiuNguyen() {
        val error = JiraErrorMapper.fromThrowable(IllegalStateException("mất kết nối"))

        assertEquals("mất kết nối", (error as JiraError.Unexpected).detail)
    }

    private fun myselfEngine(): MockEngine =
        MockEngine { request ->
            assertEquals("/rest/api/3/myself", request.url.encodedPath)
            respond(
                content = """{"accountId":"abc","displayName":"Nguyen Van A","active":true}""",
                status = HttpStatusCode.OK,
                headers = JSON_HEADERS,
            )
        }

    private fun repository(
        engine: MockEngine,
        paths: FakeAppPaths,
        storage: FakeSecureStorage,
    ) = ConnectionRepository(
        apiClient = JiraApiClient(client = HttpClientFactory.create(engine)),
        configStore = AppConfigStore(paths = paths, dispatcher = UnconfinedTestDispatcher()),
        secureStorage = storage,
        dispatcher = UnconfinedTestDispatcher(),
    )

    private companion object {
        val JSON_HEADERS = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }
}
