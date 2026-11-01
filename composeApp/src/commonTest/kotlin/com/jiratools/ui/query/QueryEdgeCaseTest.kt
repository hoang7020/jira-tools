package com.jiratools.ui.query

import com.jiratools.data.AppConfigStore
import com.jiratools.data.HttpClientFactory
import com.jiratools.data.IssueDto
import com.jiratools.data.JiraApiClient
import com.jiratools.data.JiraCredentials
import com.jiratools.data.SearchJqlRequest
import com.jiratools.data.SearchJqlResponse
import com.jiratools.data.TicketMapper
import com.jiratools.domain.QueryState
import com.jiratools.domain.userMessage
import com.jiratools.platform.FakeAppPaths
import com.jiratools.platform.TestDispatcherProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Các trường hợp biên của luồng truy vấn: dữ liệu Unicode, tiêu đề dài, trang rỗng và cảnh báo
 * của Jira (T057).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QueryEdgeCaseTest {
    @Test
    fun jqlTiengVietDuocGuiNguyenVenVaHienThiDuocKetQua() =
        runTest {
            var sentJql: String? = null
            val viewModel =
                viewModel(
                    engine =
                        MockEngine { request ->
                            if (request.url.encodedPath.endsWith(COUNT_PATH)) {
                                respond(COUNT_BODY, HttpStatusCode.OK, JSON_HEADERS)
                            } else {
                                sentJql = request.body.asText()
                                respond(VIETNAMESE_PAGE_BODY, HttpStatusCode.OK, JSON_HEADERS)
                            }
                        },
                )

            viewModel.run(jql = VIETNAMESE_JQL)

            val sent = Json.decodeFromString<SearchJqlRequest>(requireNotNull(sentJql))
            assertEquals(VIETNAMESE_JQL, sent.jql, "JQL tiếng Việt phải được gửi nguyên vẹn, không bị đổi dấu")
            val success = assertIs<QueryState.Success>(viewModel.state.value)
            assertEquals("Sửa lỗi đăng nhập bằng tiếng Việt", success.tickets.single().summary)
        }

    @Test
    fun tieuDeRatDaiDuocGiuNguyenKhongBiCat() =
        runTest {
            val longSummary = "T".repeat(500)
            val viewModel =
                viewModel(
                    engine =
                        pageEngine(
                            """{"isLast":true,"issues":[{"key":"HSP-1","fields":{"summary":"$longSummary"}}]}""",
                        ),
                )

            viewModel.run(jql = BASE_JQL)

            val success = assertIs<QueryState.Success>(viewModel.state.value)
            assertEquals(
                500,
                success.tickets
                    .single()
                    .summary.length,
            )
        }

    @Test
    fun truyVanKhongCoKetQuaTraVeTrangThaiRong() =
        runTest {
            val viewModel = viewModel(engine = pageEngine("""{"isLast":true,"issues":[]}"""))

            viewModel.run(jql = BASE_JQL)

            assertEquals(QueryState.Empty, viewModel.state.value)
        }

    @Test
    fun mangRongKemIsLastFalseVanKhongBaoConTrang() =
        runTest {
            val viewModel = viewModel(engine = pageEngine("""{"isLast":false,"nextPageToken":"","issues":[]}"""))

            viewModel.run(jql = BASE_JQL)

            assertEquals(QueryState.Empty, viewModel.state.value)
        }

    @Test
    fun nextPageTokenRongCoiNhuTrangCuoi() =
        runTest {
            val page =
                TicketMapper.toPage(
                    SearchJqlResponse(
                        issues = listOf(IssueDto(key = "HSP-1", fields = null)),
                        nextPageToken = "",
                        isLast = null,
                    ),
                )

            assertTrue(page.isLast)
            assertNull(page.nextPageToken)
        }

    @Test
    fun canhBaoCuaJiraKhongLamHongBang() =
        runTest {
            val viewModel =
                viewModel(
                    engine =
                        pageEngine(
                            """
                            {
                              "isLast": true,
                              "warnings": ["Trường không tồn tại sẽ bị bỏ qua"],
                              "names": {"summary": "Summary"},
                              "issues": [{"key":"HSP-1","fields":{"summary":"Vẫn hiển thị"}}]
                            }
                            """.trimIndent(),
                        ),
                )

            viewModel.run(jql = BASE_JQL)

            val success = assertIs<QueryState.Success>(viewModel.state.value)
            assertEquals("Vẫn hiển thị", success.tickets.single().summary)
        }

    @Test
    fun truongLaTrongJsonCuaJiraKhongLamVoBoChuyenDoi() =
        runTest {
            val viewModel =
                viewModel(
                    engine =
                        pageEngine(
                            """
                            {
                              "isLast": true,
                              "schemaVersion": "mới",
                              "issues": [{"key":"HSP-1","unknown":[1,2],"fields":{"summary":"Ổn"}}]
                            }
                            """.trimIndent(),
                        ),
                )

            viewModel.run(jql = BASE_JQL)

            val success = assertIs<QueryState.Success>(viewModel.state.value)
            assertEquals("Ổn", success.tickets.single().summary)
        }

    @Test
    fun issueThieuKeyBiBoQuaThayViLamHongCaTrang() =
        runTest {
            val viewModel =
                viewModel(
                    engine =
                        pageEngine(
                            """
                            {
                              "isLast": true,
                              "issues": [
                                {"fields":{"summary":"Không có mã"}},
                                {"key":"HSP-2","fields":{"summary":"Có mã"}}
                              ]
                            }
                            """.trimIndent(),
                        ),
                )

            viewModel.run(jql = BASE_JQL)

            val success = assertIs<QueryState.Success>(viewModel.state.value)
            assertEquals(listOf("HSP-2"), success.tickets.map { it.key })
        }

    @Test
    fun jqlChiCoKhoangTrangBiCoiLaRong() =
        runTest {
            val viewModel = viewModel()

            viewModel.run(jql = "   ")

            val error = assertIs<QueryState.Error>(viewModel.state.value)
            assertFalse(error.error.userMessage().isBlank())
        }

    @Test
    fun maTicketTiengVietVanDuocDinhDangLaiThoiGian() {
        assertEquals("2026-10-30 08:15", TicketMapper.formatUpdated("2026-10-30T08:15:00.000+0700"))
        assertEquals("", TicketMapper.formatUpdated(null))
        assertEquals("", TicketMapper.formatUpdated(""))
        assertEquals("không rõ", TicketMapper.formatUpdated("không rõ"))
    }

    @Test
    fun tiepCanTicketThu250ChiVoiHaiLanTaiThem() =
        runTest {
            var searchCalls = 0
            val pages = listOf(page(1, 100, "t2"), page(101, 200, "t3"), page(201, 250, null))
            val viewModel =
                viewModel(
                    engine =
                        MockEngine { request ->
                            if (request.url.encodedPath.endsWith(COUNT_PATH)) {
                                respond("""{"count": 250}""", HttpStatusCode.OK, JSON_HEADERS)
                            } else {
                                val body = pages[searchCalls.coerceAtMost(pages.lastIndex)]
                                searchCalls += 1
                                respond(body, HttpStatusCode.OK, JSON_HEADERS)
                            }
                        },
                )

            viewModel.run(jql = BASE_JQL)
            viewModel.loadMore()
            viewModel.loadMore()

            val success = assertIs<QueryState.Success>(viewModel.state.value)
            assertEquals(250, success.tickets.size, "Phải tiếp cận được ticket thứ 250")
            assertEquals("HSP-250", success.tickets.last().key)
            assertEquals(
                250,
                success.tickets
                    .map { it.key }
                    .toSet()
                    .size,
                "Không được trùng dòng",
            )
            assertFalse(success.hasMore)
            assertEquals(3, searchCalls, "Chỉ 1 truy vấn ban đầu + 2 lần Tải thêm, không chạy lại truy vấn")
        }

    /** Trang kết quả Jira với `size` ticket bắt đầu từ `from`; `token` null nghĩa là trang cuối. */
    private fun page(
        from: Int,
        to: Int,
        token: String?,
    ): String {
        val issues = (from..to).joinToString(",") { """{"key":"HSP-$it","fields":{"summary":"Ticket $it"}}""" }
        val isLast = token == null
        val nextToken = token?.let { ""","nextPageToken":"$it"""" }.orEmpty()
        return """{"isLast":$isLast$nextToken,"issues":[$issues]}"""
    }

    private fun OutgoingContent.asText(): String =
        when (this) {
            is TextContent -> text
            is OutgoingContent.ByteArrayContent -> bytes().decodeToString()
            else -> ""
        }

    private fun pageEngine(body: String): MockEngine =
        MockEngine { request ->
            if (request.url.encodedPath.endsWith(COUNT_PATH)) {
                respond(COUNT_BODY, HttpStatusCode.OK, JSON_HEADERS)
            } else {
                respond(body, HttpStatusCode.OK, JSON_HEADERS)
            }
        }

    private fun viewModel(
        paths: FakeAppPaths = FakeAppPaths(),
        engine: MockEngine = pageEngine(EMPTY_BODY),
    ): QueryViewModel {
        val dispatcher = TestDispatcherProvider().dispatcher
        return QueryViewModel(
            apiClient =
                JiraApiClient(
                    client = HttpClientFactory.create(engine),
                    credentials = { JiraCredentials(baseUrl = BASE_URL, email = "dev@congty.vn", token = TOKEN) },
                ),
            configStore = AppConfigStore(paths = paths, dispatcher = dispatcher),
            dispatcher = dispatcher,
        )
    }

    private companion object {
        const val BASE_URL = "https://congty.atlassian.net"
        const val TOKEN = "api-token"
        const val BASE_JQL = "project = HSP"
        const val COUNT_PATH = "/rest/api/3/search/approximate-count"
        const val EMPTY_BODY = """{"isLast": true, "issues": []}"""
        const val COUNT_BODY = """{"count": 1}"""
        const val VIETNAMESE_JQL = "project = HSP AND summary ~ \"Sửa lỗi đăng nhập\" ORDER BY updated DESC"
        const val VIETNAMESE_PAGE_BODY =
            """{"isLast":true,"issues":[{"key":"HSP-1","fields":{"summary":"Sửa lỗi đăng nhập bằng tiếng Việt"}}]}"""
        val JSON_HEADERS = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }
}
