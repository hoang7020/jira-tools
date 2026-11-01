package com.jiratools.data

import com.jiratools.domain.CatalogLoadResult
import com.jiratools.domain.ColumnKey
import com.jiratools.domain.SAVED_TABLES_SCHEMA_VERSION
import com.jiratools.domain.SavedTable
import com.jiratools.domain.SavedTableCatalog
import com.jiratools.domain.Ticket
import com.jiratools.domain.TrashedSavedTable
import com.jiratools.platform.FakeAppPaths
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Kiểm thử kho danh mục bảng đã lưu: phân biệt "chưa có dữ liệu" với "dữ liệu hỏng", giữ nguyên
 * mọi trường khi ghi rồi đọc lại, và không bao giờ ghi đè file hỏng (FR-004, FR-020, FR-016).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SavedTableStoreTest {
    private val paths = FakeAppPaths()
    private val store = SavedTableStore(paths = paths, dispatcher = UnconfinedTestDispatcher())

    @Test
    fun loadReturnsEmptyWhenFileDoesNotExist() =
        runTest {
            val result = store.load()

            assertIs<CatalogLoadResult.Empty>(result)
            assertNull(paths.storedSavedTables(), "Đọc file chưa tồn tại không được tạo file mới")
        }

    @Test
    fun saveThenLoadKeepsEveryFieldAndSchemaVersion() =
        runTest {
            val catalog =
                SavedTableCatalog(
                    tables = listOf(savedTable(refreshedAtMillis = null), savedTable(refreshedAtMillis = REFRESHED_AT)),
                )

            store.save(catalog)
            val loaded = store.load()

            val result = assertIs<CatalogLoadResult.Loaded>(loaded)
            val table = result.catalog.tables.first()
            assertEquals(SAVED_TABLES_SCHEMA_VERSION, result.catalog.schemaVersion)
            assertEquals(catalog.tables, result.catalog.tables)
            assertEquals("Báo cáo tuần", table.name)
            assertEquals(JQL, table.jql)
            assertEquals(BASE_URL, table.jiraBaseUrl)
            assertEquals(listOf("HSP-1", "HSP-2"), table.tickets.map { it.key })
            assertEquals(setOf(ColumnKey.KEY, ColumnKey.SUMMARY), table.visibleColumns)
            assertNull(table.refreshedAtMillis, "Bảng chưa làm mới phải giữ null")
            assertEquals(REFRESHED_AT, result.catalog.tables[1].refreshedAtMillis)
        }

    @Test
    fun saveThenLoadKeepsTrashEntries() =
        runTest {
            val trashed =
                TrashedSavedTable(
                    table = savedTable(refreshedAtMillis = REFRESHED_AT),
                    deletedAtMillis = SAVED_AT + 100,
                )
            val catalog = SavedTableCatalog(trashedTables = listOf(trashed))

            store.save(catalog)

            val loaded = assertIs<CatalogLoadResult.Loaded>(store.load()).catalog
            assertEquals(SAVED_TABLES_SCHEMA_VERSION, loaded.schemaVersion)
            assertEquals(listOf(trashed), loaded.trashedTables)
        }

    @Test
    fun loadReturnsUnreadableForBrokenJson() =
        runTest {
            paths.seedRawSavedTables("""{"schemaVersion":1,"tables":[""")

            val result = store.load()

            assertIs<CatalogLoadResult.Unreadable>(result)
        }

    @Test
    fun loadReturnsUnreadableForWrongTypes() =
        runTest {
            paths.seedRawSavedTables("""{"schemaVersion":1,"tables":"không phải danh sách"}""")

            val result = store.load()

            assertIs<CatalogLoadResult.Unreadable>(result)
        }

    @Test
    fun loadReturnsUnreadableForUnknownSchemaVersion() =
        runTest {
            paths.seedRawSavedTables("""{"schemaVersion":99,"tables":[]}""")

            val result = store.load()

            val unreadable = assertIs<CatalogLoadResult.Unreadable>(result)
            assertContains(unreadable.detail.orEmpty(), "99", message = "Lý do nên nêu phiên bản lược đồ gặp phải")
        }

    @Test
    fun loadReturnsUnreadableWhenReadingFails() =
        runTest {
            paths.seedRawSavedTables("""{"schemaVersion":1,"tables":[]}""")
            paths.failOnReadSavedTables = true

            val result = store.load()

            assertIs<CatalogLoadResult.Unreadable>(result)
        }

    @Test
    fun unreadableFileIsNeitherDeletedNorOverwritten() =
        runTest {
            val broken = """{"schemaVersion":1,"tables":["""
            paths.seedRawSavedTables(broken)

            store.load()

            assertEquals(broken, paths.storedSavedTables(), "File hỏng phải được giữ nguyên để người dùng xử lý")
        }

    @Test
    fun savePropagatesWriteFailure() =
        runTest {
            paths.failOnWriteSavedTables = true

            assertFailsWith<IllegalStateException> {
                store.save(SavedTableCatalog(tables = listOf(savedTable(refreshedAtMillis = null))))
            }
        }

    @Test
    fun filePathIsReportedForWarningMessage() =
        runTest {
            assertTrue(store.filePath().endsWith("saved-tables.json"))
            assertContains(store.filePath(), paths.savedTablesFile())
        }

    @Test
    fun migratesSchemaV1AndKeepsSavedDataAndLegacyTicketFields() =
        runTest {
            val legacyJson =
                """
                {
                  "schemaVersion": 1,
                  "tables": [
                    {
                      "id": "legacy",
                      "name": "Bảng cũ",
                      "jql": "project = HSP",
                      "jiraBaseUrl": "https://congty.atlassian.net",
                      "savedAtMillis": 1700000000000,
                      "visibleColumns": ["KEY", "SUMMARY"],
                      "tickets": [
                        {
                          "key": "HSP-1",
                          "summary": "Giữ snapshot cũ",
                          "status": "Done",
                          "updated": "2026-10-30T08:15:00.000+0700"
                        }
                      ]
                    }
                  ]
                }
                """.trimIndent()
            paths.seedRawSavedTables(legacyJson)

            val result = store.load()

            val catalog = assertIs<CatalogLoadResult.Loaded>(result).catalog
            val table = catalog.tables.single()
            val ticket = table.tickets.single()
            assertEquals(SAVED_TABLES_SCHEMA_VERSION, catalog.schemaVersion)
            assertEquals("Bảng cũ", table.name)
            assertEquals("project = HSP", table.jql)
            assertEquals("Giữ snapshot cũ", ticket.summary)
            assertEquals(
                setOf(
                    "KEY",
                    "SUMMARY",
                    "ORIGINAL_EXPECTED_TIME",
                    "DYNAMIC_START_TIME",
                    "DYNAMIC_DUE_DATE",
                    "REAL_DUE_DATE",
                ),
                table.visibleColumns.mapTo(mutableSetOf()) { it.name },
            )
            assertNull(table.jiraTimeZone)
            assertNull(ticket.issueId)
            assertNull(ticket.originalExpectedTime)
            assertNull(ticket.dynamicStartAtMillis)
            assertNull(ticket.realDueAtMillis)
            assertTrue(catalog.dynamicStartTimes.isEmpty())
            assertTrue(table.burndownSnapshots.isEmpty())
            assertEquals(legacyJson, paths.storedSavedTables(), "Migration khi load không được ghi file")
        }

    @Test
    fun migratesSchemaV2AndKeepsRealDueDateColumnWithEmptyHistory() =
        runTest {
            paths.seedRawSavedTables(
                """
                {
                  "schemaVersion": 2,
                  "tables": [{
                    "id": "legacy-v2",
                    "name": "Bảng v2",
                    "jql": "project = HSP",
                    "jiraBaseUrl": "https://congty.atlassian.net",
                    "savedAtMillis": 1700000000000,
                    "visibleColumns": ["KEY", "SUMMARY"],
                    "tickets": [{"key": "HSP-1", "summary": "Giữ ticket", "status": "Doing"}]
                  }]
                }
                """.trimIndent(),
            )

            val catalog = assertIs<CatalogLoadResult.Loaded>(store.load()).catalog
            val table = catalog.tables.single()

            assertEquals(SAVED_TABLES_SCHEMA_VERSION, catalog.schemaVersion)
            assertTrue(ColumnKey.REAL_DUE_DATE in table.visibleColumns)
            assertEquals("HSP-1", table.tickets.single().key)
            assertTrue(table.burndownSnapshots.isEmpty())
        }

    @Test
    fun migratesSchemaV3WithSavedTicketsAndEmptyBurndownHistory() =
        runTest {
            paths.seedRawSavedTables(
                """
                {
                  "schemaVersion": 3,
                  "tables": [{
                    "id": "legacy-v3",
                    "name": "Bảng v3",
                    "jql": "project = HSP",
                    "jiraBaseUrl": "https://congty.atlassian.net",
                    "savedAtMillis": 1700000000000,
                    "tickets": [{
                      "key": "HSP-1",
                      "summary": "Giữ ticket",
                      "status": "Doing",
                      "statusCategoryKey": "indeterminate"
                    }]
                  }]
                }
                """.trimIndent(),
            )

            val catalog = assertIs<CatalogLoadResult.Loaded>(store.load()).catalog
            val table = catalog.tables.single()

            assertEquals(SAVED_TABLES_SCHEMA_VERSION, catalog.schemaVersion)
            assertEquals(listOf("HSP-1"), table.tickets.map(Ticket::key))
            assertTrue(table.burndownSnapshots.isEmpty())
        }

    @Test
    fun migratesSchemaV4WithEmptyTrashAndKeepsSharedDynamicTimes() =
        runTest {
            paths.seedRawSavedTables(
                """
                {
                  "schemaVersion": 4,
                  "tables": [{
                    "id": "legacy-v4",
                    "name": "Bảng v4",
                    "jql": "project = HSP",
                    "jiraBaseUrl": "https://congty.atlassian.net",
                    "savedAtMillis": 1700000000000,
                    "tickets": [{"key": "HSP-1", "summary": "Giữ ticket", "status": "Doing"}]
                  }],
                  "dynamicStartTimes": [{
                    "normalizedJiraBaseUrl": "https://congty.atlassian.net",
                    "issueId": "10001",
                    "startedAtMillis": 1700000000000,
                    "displayValue": "2026-10-30T08:00:00.000+0700"
                  }]
                }
                """.trimIndent(),
            )

            val catalog = assertIs<CatalogLoadResult.Loaded>(store.load()).catalog

            assertEquals(SAVED_TABLES_SCHEMA_VERSION, catalog.schemaVersion)
            assertEquals("legacy-v4", catalog.tables.single().id)
            assertEquals("10001", catalog.dynamicStartTimes.single().issueId)
            assertTrue(catalog.trashedTables.isEmpty())
        }

    private fun savedTable(refreshedAtMillis: Long?): SavedTable =
        SavedTable(
            id = "table-$refreshedAtMillis",
            name = "Báo cáo tuần",
            jql = JQL,
            jiraBaseUrl = BASE_URL,
            savedAtMillis = SAVED_AT,
            refreshedAtMillis = refreshedAtMillis,
            visibleColumns = setOf(ColumnKey.KEY, ColumnKey.SUMMARY),
            tickets =
                listOf(
                    Ticket(
                        key = "HSP-1",
                        summary = "Sửa lỗi đăng nhập",
                        status = "In Progress",
                        assignee = null,
                        priority = "High",
                        updated = "2026-10-30T10:15:00.000+0700",
                    ),
                    Ticket(
                        key = "HSP-2",
                        summary = "Thêm màn hình báo cáo",
                        status = "Done",
                        assignee = "Nguyen Van A",
                        priority = null,
                        updated = "2026-10-29T08:00:00.000+0700",
                    ),
                ),
        )

    private companion object {
        const val JQL = "project = HSP ORDER BY created DESC"
        const val BASE_URL = "https://congty.atlassian.net"
        const val SAVED_AT = 1_700_000_000_000
        const val REFRESHED_AT = 1_700_000_500_000
    }
}
