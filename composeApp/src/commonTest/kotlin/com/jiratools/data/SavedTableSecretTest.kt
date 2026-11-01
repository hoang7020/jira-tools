package com.jiratools.data

import com.jiratools.domain.ColumnKey
import com.jiratools.domain.ConnectionStatus
import com.jiratools.domain.Ticket
import com.jiratools.platform.FakeAppPaths
import com.jiratools.platform.FakeClock
import com.jiratools.platform.FakeSecureStorage
import com.jiratools.platform.RecordingUrlOpener
import com.jiratools.ui.saved.SavedTablesViewModel
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Hồi quy chống rò rỉ bí mật cho bảng đã lưu: token chỉ nằm trong kho bí mật của hệ điều hành, không
 * bao giờ xuất hiện trong `saved-tables.json` hay `config.json` (FR-016, SC-008).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SavedTableSecretTest {
    private val baseUrl = "https://congty.atlassian.net"

    private val email = "dev@congty.vn"

    private val token = "api-token-sieu-bi-mat"

    @Test
    fun savedCatalogNeverContainsTheApiToken() =
        runTest {
            val paths = FakeAppPaths()
            val storage = FakeSecureStorage()
            val viewModel = viewModel(paths = paths, storage = storage)
            connect(paths = paths, storage = storage)

            viewModel.saveTable(
                name = "Báo cáo tuần",
                jql = JQL,
                tickets = tickets(),
                visibleColumns = ColumnKey.all(),
                jiraBaseUrl = baseUrl,
            )

            val content = paths.storedSavedTables().orEmpty()
            assertTrue(content.isNotBlank(), "Bảng phải được ghi xuống file")
            assertFalse(content.contains(token), "File bảng đã lưu không được chứa token")
            assertFalse(content.contains("token", ignoreCase = true), "File bảng đã lưu không được có khoá token")
            assertFalse(content.contains("apiToken", ignoreCase = true), "File bảng đã lưu không được có khoá apiToken")
            assertEquals(listOf(token), storage.savedTokens, "Token chỉ được lưu vào kho bí mật")
        }

    @Test
    fun savingATableKeepsTheConfigFileFreeOfTheToken() =
        runTest {
            val paths = FakeAppPaths()
            val storage = FakeSecureStorage()
            val viewModel = viewModel(paths = paths, storage = storage)
            connect(paths = paths, storage = storage)
            val configBeforeSaving = paths.storedConfig()

            viewModel.saveTable(
                name = "Báo cáo tuần",
                jql = JQL,
                tickets = tickets(),
                visibleColumns = ColumnKey.all(),
                jiraBaseUrl = baseUrl,
            )

            assertEquals(configBeforeSaving, paths.storedConfig(), "Lưu bảng không được ghi vào file cấu hình")
            assertFalse(paths.storedConfig().orEmpty().contains(token), "File cấu hình không được chứa token")
        }

    private fun viewModel(
        paths: FakeAppPaths,
        storage: FakeSecureStorage,
    ): SavedTablesViewModel =
        SavedTablesViewModel(
            store = SavedTableStore(paths = paths, dispatcher = UnconfinedTestDispatcher()),
            clock = FakeClock(),
            urlOpener = RecordingUrlOpener(),
            apiClient =
                JiraApiClient(
                    client = HttpClientFactory.create(myselfEngine()),
                    credentials = {
                        JiraCredentials(
                            baseUrl = baseUrl,
                            email = email,
                            token = storage.loadToken().orEmpty(),
                        )
                    },
                ),
            dispatcher = UnconfinedTestDispatcher(),
        )

    private suspend fun connect(
        paths: FakeAppPaths,
        storage: FakeSecureStorage,
    ) {
        val repository =
            ConnectionRepository(
                apiClient = JiraApiClient(client = HttpClientFactory.create(myselfEngine())),
                configStore = AppConfigStore(paths = paths, dispatcher = UnconfinedTestDispatcher()),
                secureStorage = storage,
                dispatcher = UnconfinedTestDispatcher(),
            )

        val status = repository.validate(baseUrl = baseUrl, email = email, token = token)

        assertEquals(ConnectionStatus.Connected(baseUrl = baseUrl, email = email), status)
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

    private fun tickets(): List<Ticket> =
        listOf(
            Ticket(
                key = "HSP-1",
                summary = "Sửa lỗi đăng nhập",
                status = "In Progress",
                assignee = "Nguyen Van A",
                priority = "High",
                updated = "2026-10-30T10:15:00.000+0700",
            ),
        )

    private companion object {
        const val JQL = "project = HSP ORDER BY created DESC"
        val JSON_HEADERS = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }
}
