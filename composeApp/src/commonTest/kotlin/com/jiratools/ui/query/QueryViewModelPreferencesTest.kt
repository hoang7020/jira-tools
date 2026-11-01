package com.jiratools.ui.query

import com.jiratools.data.AppConfigStore
import com.jiratools.data.HttpClientFactory
import com.jiratools.data.JiraApiClient
import com.jiratools.data.JiraCredentials
import com.jiratools.domain.ColumnKey
import com.jiratools.domain.SortState
import com.jiratools.platform.FakeAppPaths
import com.jiratools.platform.TestDispatcherProvider
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Kiểm thử tuỳ biến bảng: bật/tắt cột và sắp xếp phía server (FR-011, FR-012).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QueryViewModelPreferencesTest {
    @Test
    fun macDinhHienThiDuSauCot() =
        runTest {
            val viewModel = viewModel()

            assertEquals(ColumnKey.all(), viewModel.visibleColumns.value)
            val visibleNames = viewModel.visibleColumns.value.map { it.name }
            assertTrue("ORIGINAL_EXPECTED_TIME" in visibleNames)
            assertFalse("DYNAMIC_START_TIME" in visibleNames)
            assertFalse("DYNAMIC_DUE_DATE" in visibleNames)
        }

    @Test
    fun tatCotDuocGhiNgayVaoCauHinh() =
        runTest {
            val paths = FakeAppPaths()
            val viewModel = viewModel(paths = paths)

            viewModel.toggleColumn(ColumnKey.PRIORITY)

            assertTrue(ColumnKey.PRIORITY !in viewModel.visibleColumns.value)
            val saved = AppConfigStore(paths = paths, dispatcher = TestDispatcherProvider().dispatcher).load()
            assertTrue(ColumnKey.PRIORITY !in saved.visibleColumns)
        }

    @Test
    fun batLaiCotDuocGhiNgayVaoCauHinh() =
        runTest {
            val paths =
                FakeAppPaths().apply {
                    seedRawConfig(
                        """{"schemaVersion":1,"visibleColumns":["KEY","SUMMARY"]}""",
                    )
                }
            val viewModel = viewModel(paths = paths)
            viewModel.loadSaved()

            viewModel.toggleColumn(ColumnKey.STATUS)

            assertTrue(ColumnKey.STATUS in viewModel.visibleColumns.value)
            val saved = AppConfigStore(paths = paths, dispatcher = TestDispatcherProvider().dispatcher).load()
            assertTrue(ColumnKey.STATUS in saved.visibleColumns)
        }

    @Test
    fun khongTheTatHetCot() =
        runTest {
            val paths = FakeAppPaths().apply { seedRawConfig("""{"schemaVersion":1,"visibleColumns":["KEY"]}""") }
            val viewModel = viewModel(paths = paths)
            viewModel.loadSaved()

            viewModel.toggleColumn(ColumnKey.KEY)

            assertEquals(setOf(ColumnKey.KEY, ColumnKey.ORIGINAL_EXPECTED_TIME), viewModel.visibleColumns.value)
        }

    @Test
    fun luonGiuCotMaKhiTatTatCaCotKhac() =
        runTest {
            val paths =
                FakeAppPaths().apply {
                    seedRawConfig(
                        """{"schemaVersion":1,"visibleColumns":["KEY","SUMMARY"]}""",
                    )
                }
            val viewModel = viewModel(paths = paths)
            viewModel.loadSaved()

            viewModel.toggleColumn(ColumnKey.SUMMARY)

            assertEquals(setOf(ColumnKey.KEY, ColumnKey.ORIGINAL_EXPECTED_TIME), viewModel.visibleColumns.value)
        }

    @Test
    fun sapXepLanDauTheoChieuTangDan() =
        runTest {
            val paths = FakeAppPaths()
            val viewModel = viewModel(paths = paths)

            viewModel.sortBy(ColumnKey.UPDATED)

            assertEquals(SortState(column = ColumnKey.UPDATED, ascending = true), viewModel.sort.value)
            val saved = AppConfigStore(paths = paths, dispatcher = TestDispatcherProvider().dispatcher).load()
            assertEquals(SortState(column = ColumnKey.UPDATED, ascending = true), saved.sort)
        }

    @Test
    fun bamCungCotDoiChieuSapXep() =
        runTest {
            val viewModel = viewModel()

            viewModel.sortBy(ColumnKey.UPDATED)
            viewModel.sortBy(ColumnKey.UPDATED)

            assertEquals(SortState(column = ColumnKey.UPDATED, ascending = false), viewModel.sort.value)
        }

    @Test
    fun chonCotKhacChayLaiTruyVanVoiJqlDaVietLai() =
        runTest {
            val paths = FakeAppPaths()
            val viewModel = viewModel(paths = paths)
            viewModel.run(jql = BASE_JQL)

            viewModel.sortBy(ColumnKey.STATUS)

            val saved = AppConfigStore(paths = paths, dispatcher = TestDispatcherProvider().dispatcher).load()
            assertEquals("project = HSP ORDER BY status ASC", saved.lastJql)
        }

    @Test
    fun khoiDongKhoiPhucCotVaSapXepDaLuu() =
        runTest {
            val paths =
                FakeAppPaths().apply {
                    seedRawConfig(
                        """
                        {"schemaVersion":1,"visibleColumns":["KEY","SUMMARY"],
                        "sort":{"column":"STATUS","ascending":false}}
                        """.trimIndent(),
                    )
                }
            val viewModel = viewModel(paths = paths)

            viewModel.loadSaved()

            assertEquals(
                setOf(ColumnKey.KEY, ColumnKey.SUMMARY, ColumnKey.valueOf("ORIGINAL_EXPECTED_TIME")),
                viewModel.visibleColumns.value,
            )
            assertEquals(SortState(column = ColumnKey.STATUS, ascending = false), viewModel.sort.value)
        }

    @Test
    fun sapXepChayLaiTruyVanBangJqlHienTai() =
        runTest {
            var searchCalls = 0
            val viewModel =
                viewModel(
                    engine =
                        MockEngine { request ->
                            if (request.url.encodedPath.endsWith(COUNT_PATH)) {
                                respond(EMPTY_BODY, HttpStatusCode.OK, JSON_HEADERS)
                            } else {
                                searchCalls++
                                respond(EMPTY_BODY, HttpStatusCode.OK, JSON_HEADERS)
                            }
                        },
                )
            viewModel.run(jql = BASE_JQL)
            val afterRun = searchCalls

            viewModel.sortBy(ColumnKey.UPDATED)

            assertEquals(afterRun + 1, searchCalls)
        }

    private fun viewModel(
        paths: FakeAppPaths = FakeAppPaths(),
        engine: MockEngine = MockEngine { respond(EMPTY_BODY, HttpStatusCode.OK, JSON_HEADERS) },
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
        const val BASE_JQL = "project = HSP"
        const val COUNT_PATH = "/rest/api/3/search/approximate-count"
        const val EMPTY_BODY = """{"isLast": true, "issues": []}"""
        val JSON_HEADERS = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }
}
