package com.jiratools.ui.query

import com.jiratools.data.AppConfigStore
import com.jiratools.data.HttpClientFactory
import com.jiratools.data.JiraApiClient
import com.jiratools.data.JiraCredentials
import com.jiratools.domain.JiraError
import com.jiratools.domain.QueryState
import com.jiratools.platform.FakeAppPaths
import com.jiratools.platform.TestDispatcherProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class QueryViewModelTest {
    @Test
    fun loadSavedKhoiPhucTruyVanLanTruoc() =
        runTest {
            val paths = FakeAppPaths().apply { seedRawConfig("""{"schemaVersion":1,"lastJql":"project = HSP"}""") }
            val viewModel = viewModel(paths = paths)

            viewModel.loadSaved()

            assertEquals("project = HSP", viewModel.jql.value)
        }

    @Test
    fun runChuyenTuIdleQuaLoadingRoiSuccess() =
        runTest {
            val viewModel = viewModel(engine = routingEngine())
            val observed = mutableListOf<QueryState>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.toList(observed) }

            viewModel.run(jql = BASE_JQL)
            job.cancel()

            assertEquals(QueryState.Idle, observed.first())
            assertEquals(QueryState.Loading, observed[1])
            val success = assertIs<QueryState.Success>(observed.last())
            assertEquals(listOf("HSP-1", "HSP-2"), success.tickets.map { it.key })
            assertTrue(success.hasMore)
        }

    @Test
    fun ketQuaRongTraVeEmpty() =
        runTest {
            val viewModel = viewModel(engine = staticEngine(EMPTY_BODY))

            viewModel.run(jql = BASE_JQL)

            assertEquals(QueryState.Empty, viewModel.state.value)
        }

    @Test
    fun loiTruyVanGiuLaiJqlDaNhap() =
        runTest {
            val viewModel =
                viewModel(
                    engine = MockEngine { respond(INVALID_JQL_BODY, HttpStatusCode.BadRequest, JSON_HEADERS) },
                )

            viewModel.run(jql = "project = HSP abc")

            val error = assertIs<QueryState.Error>(viewModel.state.value)
            assertEquals("project = HSP abc", error.jql)
            assertIs<JiraError.InvalidJql>(error.error)
        }

    @Test
    fun jqlRongKhongGoiMang() =
        runTest {
            var calls = 0
            val viewModel =
                viewModel(
                    engine =
                        MockEngine {
                            calls++
                            respond(PAGE_BODY, HttpStatusCode.OK, JSON_HEADERS)
                        },
                )

            viewModel.run(jql = "   ")

            assertEquals(0, calls)
            val error = assertIs<QueryState.Error>(viewModel.state.value)
            assertEquals("   ", error.jql)
        }

    @Test
    fun loadMoreNoiThemMaKhongTrungLap() =
        runTest {
            var pageCalls = 0
            val engine =
                MockEngine { request ->
                    when {
                        request.url.encodedPath.endsWith(COUNT_PATH) -> countResponse()
                        pageCalls++ == 0 -> respond(PAGE_BODY, HttpStatusCode.OK, JSON_HEADERS)
                        else -> respond(PAGE_BODY_TWO, HttpStatusCode.OK, JSON_HEADERS)
                    }
                }
            val viewModel = viewModel(engine = engine)

            viewModel.run(jql = BASE_JQL)
            viewModel.loadMore()

            val success = assertIs<QueryState.Success>(viewModel.state.value)
            assertEquals(listOf("HSP-1", "HSP-2", "HSP-3"), success.tickets.map { it.key })
            assertEquals(false, success.hasMore)
        }

    @Test
    fun loadMoreDungLaiKhiHetTrang() =
        runTest {
            var searchCalls = 0
            val viewModel =
                viewModel(
                    engine =
                        MockEngine { request ->
                            if (request.url.encodedPath.endsWith(COUNT_PATH)) {
                                respond(COUNT_BODY, HttpStatusCode.OK, JSON_HEADERS)
                            } else {
                                searchCalls++
                                respond(LAST_PAGE_BODY, HttpStatusCode.OK, JSON_HEADERS)
                            }
                        },
                )

            viewModel.run(jql = BASE_JQL)
            viewModel.loadMore()

            assertEquals(1, searchCalls)
            val success = assertIs<QueryState.Success>(viewModel.state.value)
            assertEquals(false, success.hasMore)
        }

    @Test
    fun runLuuLastJqlVaoCauHinh() =
        runTest {
            val paths = FakeAppPaths()
            val viewModel = viewModel(paths = paths, engine = staticEngine(EMPTY_BODY))

            viewModel.run(jql = "project = HSP AND status = Done")

            val stored = paths.storedConfig()
            assertTrue(
                requireNotNull(stored).contains("project = HSP AND status = Done"),
                "cấu hình phải lưu lastJql, nhận được $stored",
            )
        }

    @Test
    fun soLuongUocTinhKhongChanViecHienThiKetQua() =
        runTest {
            val viewModel = viewModel(engine = routingEngine())
            val observed = mutableListOf<QueryState>()
            val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.toList(observed) }

            viewModel.run(jql = BASE_JQL)
            job.cancel()

            val successes = observed.filterIsInstance<QueryState.Success>()
            assertEquals(2, successes.size, "phải có trạng thái Success trước và sau khi đếm: $successes")
            assertNull(successes.first().approximateTotal)
            assertEquals(153, successes.last().approximateTotal)
        }

    @Test
    fun loiMangTraVeLoiMang() =
        runTest {
            val viewModel = viewModel(engine = MockEngine { respondError(HttpStatusCode.ServiceUnavailable) })

            viewModel.run(jql = BASE_JQL)

            assertIs<QueryState.Error>(viewModel.state.value)
        }

    @Test
    fun updateJqlChiDoiGiaTriHienThi() =
        runTest {
            var calls = 0
            val viewModel =
                viewModel(
                    engine =
                        MockEngine {
                            calls++
                            respond(EMPTY_BODY, HttpStatusCode.OK, JSON_HEADERS)
                        },
                )

            viewModel.updateJql(NEW_JQL)

            assertEquals(NEW_JQL, viewModel.jql.value)
            assertEquals(QueryState.Idle, viewModel.state.value)
            assertEquals(0, calls)
        }

    private fun routingEngine(): MockEngine =
        MockEngine { request ->
            if (request.url.encodedPath.endsWith(COUNT_PATH)) {
                respond(COUNT_BODY, HttpStatusCode.OK, JSON_HEADERS)
            } else {
                respond(PAGE_BODY, HttpStatusCode.OK, JSON_HEADERS)
            }
        }

    private fun MockRequestHandleScope.countResponse(): HttpResponseData =
        respond(COUNT_BODY, HttpStatusCode.OK, JSON_HEADERS)

    private fun staticEngine(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): MockEngine = MockEngine { respond(body, status, JSON_HEADERS) }

    private fun viewModel(
        paths: FakeAppPaths = FakeAppPaths(),
        engine: MockEngine = staticEngine(EMPTY_BODY),
    ): QueryViewModel {
        val dispatcher = TestDispatcherProvider().dispatcher
        return QueryViewModel(
            apiClient =
                JiraApiClient(
                    client = HttpClientFactory.create(engine),
                    credentials = { JiraCredentials(baseUrl = BASE_URL, email = "dev@congty.vn", token = "api-token") },
                ),
            configStore = AppConfigStore(paths = paths, dispatcher = dispatcher),
            dispatcher = dispatcher,
        )
    }

    private companion object {
        const val BASE_URL = "https://congty.atlassian.net"
        const val BASE_JQL = "project = HSP ORDER BY updated DESC"
        const val NEW_JQL = "project = HSP AND status != Done"
        const val COUNT_PATH = "/rest/api/3/search/approximate-count"
        val JSON_HEADERS = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

        const val UPDATED_ONE = "2026-10-30T08:15:00.000+0700"

        val INVALID_JQL_BODY =
            """{"errorMessages":["Error in the JQL Query: Expecting operator."],"errors":{}}"""

        val PAGE_BODY =
            """
            {
              "isLast": false,
              "nextPageToken": "token-2",
              "issues": [
                {
                  "key": "HSP-1",
                  "fields": {"summary": "Một", "status": {"name": "To Do"}, "updated": "$UPDATED_ONE"}
                },
                {"key": "HSP-2", "fields": {"summary": "Hai", "status": {"name": "Done"}}}
              ]
            }
            """.trimIndent()

        val PAGE_BODY_TWO =
            """
            {
              "isLast": true,
              "issues": [
                {"key": "HSP-2", "fields": {"summary": "Hai", "status": {"name": "Done"}}},
                {"key": "HSP-3", "fields": {"summary": "Ba", "status": {"name": "In Progress"}}}
              ]
            }
            """.trimIndent()

        val LAST_PAGE_BODY =
            """
            {
              "isLast": true,
              "issues": [
                {"key": "HSP-1", "fields": {"summary": "Một", "status": {"name": "To Do"}}}
              ]
            }
            """.trimIndent()

        val EMPTY_BODY = """{"isLast": true, "issues": []}"""
        val COUNT_BODY = """{"count": 153}"""
    }
}
