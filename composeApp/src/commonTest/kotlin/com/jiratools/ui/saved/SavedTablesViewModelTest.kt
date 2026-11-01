package com.jiratools.ui.saved

import com.jiratools.data.HttpClientFactory
import com.jiratools.data.JiraApiClient
import com.jiratools.data.JiraCredentials
import com.jiratools.data.SavedTableStore
import com.jiratools.data.TicketMapper
import com.jiratools.domain.BurndownSnapshot
import com.jiratools.domain.CatalogLoadResult
import com.jiratools.domain.ColumnKey
import com.jiratools.domain.ConnectionStatus
import com.jiratools.domain.DynamicStartTimeRecord
import com.jiratools.domain.JiraError
import com.jiratools.domain.SAVED_TABLES_SCHEMA_VERSION
import com.jiratools.domain.SavedTable
import com.jiratools.domain.SavedTableCatalog
import com.jiratools.domain.SavedTableChartField
import com.jiratools.domain.SavedTableChartType
import com.jiratools.domain.SortState
import com.jiratools.domain.TableNameValidation
import com.jiratools.domain.Ticket
import com.jiratools.domain.TrashedSavedTable
import com.jiratools.domain.userMessage
import com.jiratools.platform.FakeAppPaths
import com.jiratools.platform.FakeClock
import com.jiratools.platform.RecordingUrlOpener
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Trạng thái danh mục bảng đã lưu (Nguyên tắc III).
 *
 * Loạt test này bao phủ luồng lưu bảng mới của US1: nội dung bảng đúng bằng kết quả truy vấn, không
 * phát sinh request nào tới Jira, tên được kiểm tra trước khi ghi và mọi thao tác ghi bị chặn khi
 * danh mục không đọc được (FR-001..FR-003, FR-011, FR-013, FR-015, FR-018, FR-020).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SavedTablesViewModelTest {
    private val paths = FakeAppPaths()
    private val clock = FakeClock()
    private val opener = RecordingUrlOpener()
    private val requests = mutableListOf<String>()
    private val engine =
        MockEngine { request ->
            requests += request.url.toString()
            respond(content = "{}", status = HttpStatusCode.OK)
        }

    @Test
    fun dueDateSyncUsesSavedJiraTimeZoneAndOnlyOffersMismatchedRows() =
        runTest {
            val target = "2026-11-01"
            val ticket =
                Ticket(
                    key = "HSP-1",
                    summary = "Due date test",
                    status = "In Progress",
                    issueId = "90001",
                    dynamicDueAtMillis = Instant.parse("2026-11-02T00:30:00Z").toEpochMilliseconds(),
                )
            val table = savedTable("Bảng Due Date", jiraTimeZone = "America/Los_Angeles").copy(tickets = listOf(ticket))
            val viewModel = viewModelWithTables(listOf(table))

            assertEquals(target, viewModel.dueDateSyncTarget(table.id, ticket.key))

            val matched = ticket.copy(dueDate = target)
            val noDynamicDate = ticket.copy(key = "HSP-2", dynamicDueAtMillis = null)
            val invalidZoneTable =
                table.copy(id = "invalid-zone", jiraTimeZone = "Not/A_Zone", tickets = listOf(ticket))
            val viewModelWithRows =
                viewModelWithTables(
                    listOf(
                        table.copy(tickets = listOf(matched, noDynamicDate)),
                        invalidZoneTable,
                    ),
                )

            assertNull(viewModelWithRows.dueDateSyncTarget(table.id, matched.key))
            assertNull(viewModelWithRows.dueDateSyncTarget(table.id, noDynamicDate.key))
            assertNull(viewModelWithRows.dueDateSyncTarget(invalidZoneTable.id, ticket.key))
        }

    @Test
    fun updateDueDateWritesOnlyAfterJiraSuccessAndPersistsTheSnapshot() =
        runTest {
            var requestPath: String? = null
            var requestBody: String? = null
            val updateEngine =
                MockEngine { request ->
                    requestPath = request.url.encodedPath
                    requestBody = request.body.asText()
                    respond(content = "", status = HttpStatusCode.NoContent)
                }
            val ticket =
                Ticket(
                    key = "HSP-1",
                    summary = "Due date test",
                    status = "In Progress",
                    issueId = "90001",
                    dynamicDueAtMillis = Instant.parse("2026-11-02T00:30:00Z").toEpochMilliseconds(),
                )
            val table = savedTable("Bảng Due Date", jiraTimeZone = "America/Los_Angeles").copy(tickets = listOf(ticket))
            val viewModel = viewModelWithTables(listOf(table), engine = updateEngine)
            viewModel.onConnectionStatusChanged(
                ConnectionStatus.Connected(
                    baseUrl = BASE_URL,
                    email = "dev@congty.vn",
                    jiraTimeZone = "America/Los_Angeles",
                ),
            )

            viewModel.updateDueDate(table.id, ticket.key)

            assertEquals("/rest/api/3/issue/90001", requestPath)
            val fields =
                Json
                    .parseToJsonElement(requireNotNull(requestBody))
                    .jsonObject
                    .getValue("fields")
                    .jsonObject
            assertEquals("2026-11-01", fields.getValue("duedate").jsonPrimitive.content)
            assertEquals(
                "2026-11-01",
                tablesOf(viewModel)
                    .single()
                    .tickets
                    .single()
                    .dueDate,
            )
            assertEquals(
                ticket.dynamicDueAtMillis,
                tablesOf(viewModel)
                    .single()
                    .tickets
                    .single()
                    .dynamicDueAtMillis,
            )
            assertEquals(
                "2026-11-01",
                storedCatalog()
                    .tables
                    .single()
                    .tickets
                    .single()
                    .dueDate,
            )
            assertNull(viewModel.dueDateSyncTarget(table.id, ticket.key))
        }

    @Test
    fun updateDueDateRequiresConnectedMatchingSiteAndReportsLocalWriteFailure() =
        runTest {
            var requestsMade = 0
            val updateEngine =
                MockEngine {
                    requestsMade += 1
                    respond(content = "", status = HttpStatusCode.NoContent)
                }
            val ticket =
                Ticket(
                    key = "HSP-1",
                    summary = "Due date test",
                    status = "In Progress",
                    dynamicDueAtMillis = Instant.parse("2026-11-02T00:30:00Z").toEpochMilliseconds(),
                )
            val table = savedTable("Bảng Due Date", jiraTimeZone = "America/Los_Angeles").copy(tickets = listOf(ticket))
            val viewModel = viewModelWithTables(listOf(table), engine = updateEngine)

            viewModel.updateDueDate(table.id, ticket.key)
            assertEquals(0, requestsMade, "Không cập nhật khi chưa xác thực kết nối Jira")

            viewModel.onConnectionStatusChanged(
                ConnectionStatus.Connected(
                    baseUrl = "https://other.atlassian.net",
                    email = "dev@congty.vn",
                    jiraTimeZone = "America/Los_Angeles",
                ),
            )
            viewModel.updateDueDate(table.id, ticket.key)
            assertEquals(0, requestsMade, "Không cập nhật khi Jira site đang kết nối không khớp bảng")

            viewModel.onConnectionStatusChanged(
                ConnectionStatus.Connected(
                    baseUrl = BASE_URL,
                    email = "dev@congty.vn",
                    jiraTimeZone = "America/Los_Angeles",
                ),
            )
            paths.failOnWriteSavedTables = true
            viewModel.updateDueDate(table.id, ticket.key)

            assertEquals(1, requestsMade)
            assertContains(requireNotNull(viewModel.state.value.message), "Jira đã cập nhật")
            assertContains(requireNotNull(viewModel.state.value.message), "snapshot")
            assertEquals(
                "2026-11-01",
                tablesOf(viewModel)
                    .single()
                    .tickets
                    .single()
                    .dueDate,
            )
            assertNull(
                storedCatalog()
                    .tables
                    .single()
                    .tickets
                    .single()
                    .dueDate,
            )
        }

    @Test
    fun jiraFailureKeepsSnapshotAndLeavesRowRetryable() =
        runTest {
            val failingEngine =
                MockEngine {
                    respond(content = ERROR_BODY, status = HttpStatusCode.Forbidden, headers = JSON_HEADERS)
                }
            val ticket =
                Ticket(
                    key = "HSP-1",
                    summary = "Due date test",
                    status = "In Progress",
                    dynamicDueAtMillis = Instant.parse("2026-11-02T00:30:00Z").toEpochMilliseconds(),
                )
            val table = savedTable("Bảng Due Date", jiraTimeZone = "America/Los_Angeles").copy(tickets = listOf(ticket))
            val viewModel = viewModelWithTables(listOf(table), engine = failingEngine)
            viewModel.onConnectionStatusChanged(connected("America/Los_Angeles"))

            viewModel.updateDueDate(table.id, ticket.key)

            assertEquals(ticket, tablesOf(viewModel).single().tickets.single())
            assertEquals(
                ticket,
                storedCatalog()
                    .tables
                    .single()
                    .tickets
                    .single(),
            )
            assertEquals("2026-11-01", viewModel.dueDateSyncTarget(table.id, ticket.key))
            assertContains(requireNotNull(viewModel.state.value.message), "quyền")
        }

    @Test
    fun duplicateUpdateForOneIssueIsBlockedWhileAnotherIssueCanProceed() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            var requestsMade = 0
            val updateEngine =
                MockEngine {
                    requestsMade += 1
                    gate.await()
                    respond(content = "", status = HttpStatusCode.NoContent)
                }
            val dueMillis = Instant.parse("2026-11-02T00:30:00Z").toEpochMilliseconds()
            val first = Ticket(key = "HSP-1", summary = "First", status = "Open", dynamicDueAtMillis = dueMillis)
            val second = Ticket(key = "HSP-2", summary = "Second", status = "Open", dynamicDueAtMillis = dueMillis)
            val table =
                savedTable("Bảng Due Date", jiraTimeZone = "America/Los_Angeles")
                    .copy(tickets = listOf(first, second))
            val viewModel = viewModelWithTables(listOf(table), engine = updateEngine)
            viewModel.onConnectionStatusChanged(connected("America/Los_Angeles"))

            val firstUpdate =
                launch(start = CoroutineStart.UNDISPATCHED) { viewModel.updateDueDate(table.id, first.key) }
            val duplicateUpdate =
                launch(start = CoroutineStart.UNDISPATCHED) { viewModel.updateDueDate(table.id, first.key) }
            val secondIssueUpdate =
                launch(start = CoroutineStart.UNDISPATCHED) { viewModel.updateDueDate(table.id, second.key) }

            assertTrue(
                viewModel.state.value.dueDateSyncStates.values
                    .all { it.updating },
                "Issue khác được bắt đầu cập nhật trong lúc request đầu tiên đang chờ",
            )
            gate.complete(Unit)
            firstUpdate.join()
            duplicateUpdate.join()
            secondIssueUpdate.join()
            assertEquals(2, requestsMade, "Chỉ một request mỗi issue")
            assertEquals(listOf("2026-11-01", "2026-11-01"), tablesOf(viewModel).single().tickets.map { it.dueDate })
        }

    @Test
    fun saveTableStoresEverythingFromQueryResultWithoutNetworkCalls() =
        runTest {
            val viewModel = viewModel()

            val result =
                viewModel.saveTable(
                    name = "  Báo cáo tuần  ",
                    jql = JQL,
                    tickets = tickets(),
                    visibleColumns = setOf(ColumnKey.KEY, ColumnKey.SUMMARY),
                    jiraBaseUrl = BASE_URL,
                )

            assertEquals(TableNameValidation.Valid, result)
            assertTrue(requests.isEmpty(), "Lưu bảng không được gọi Jira (FR-018)")
            val state = viewModel.state.value
            val saved = state.tables.single()
            val stored = storedCatalog().tables.single()
            assertEquals("Báo cáo tuần", saved.name, "Tên phải được cắt khoảng trắng")
            assertEquals(JQL, saved.jql)
            assertEquals(BASE_URL, saved.jiraBaseUrl)
            assertEquals(clock.millis, saved.savedAtMillis)
            assertNull(saved.refreshedAtMillis)
            assertEquals(
                setOf(ColumnKey.KEY, ColumnKey.SUMMARY) + ColumnKey.schemaV3SavedColumns(),
                saved.visibleColumns,
            )
            assertEquals(listOf("HSP-1", "HSP-2"), saved.tickets.map { it.key })
            assertEquals(saved, stored, "Bảng đã ghi xuống file phải giống hệt bảng trong bộ nhớ")
        }

    @Test
    fun newSavedTableKeepsDueDateAndExistingTableColumnsRemainUnchanged() =
        runTest {
            val legacy = savedTable("Bảng cũ")
            val loadedViewModel = viewModelWithTables(listOf(legacy))

            assertEquals(setOf(ColumnKey.KEY, ColumnKey.SUMMARY), tablesOf(loadedViewModel).single().visibleColumns)

            val newViewModel = viewModel()
            newViewModel.saveTable("Bảng mới", JQL, tickets(), ColumnKey.all(), BASE_URL)

            assertTrue("DUE_DATE" in tablesOf(newViewModel).single().visibleColumns.map { it.name })
        }

    @Test
    fun saveTableAddsBaselineWithDoneAndUnknownStatusCategories() =
        runTest {
            val viewModel = viewModel()
            val snapshot =
                listOf(
                    Ticket(key = "HSP-1", summary = "Done", status = "Done", statusCategoryKey = "done"),
                    Ticket(key = "HSP-2", summary = "Open", status = "Open", statusCategoryKey = "new"),
                    Ticket(key = "HSP-3", summary = "Missing", status = "Unknown"),
                    Ticket(key = "HSP-4", summary = "Unexpected", status = "Custom", statusCategoryKey = "custom"),
                )

            viewModel.saveTable("Bảng baseline", JQL, snapshot, ColumnKey.all(), BASE_URL)

            val baseline = tablesOf(viewModel).single().burndownSnapshots.single()
            assertEquals(
                BurndownSnapshot(clock.millis, ticketCount = 4, remainingCount = 3, unknownCategoryCount = 2),
                baseline,
            )
            assertEquals(
                baseline,
                storedCatalog()
                    .tables
                    .single()
                    .burndownSnapshots
                    .single(),
            )
        }

    @Test
    fun saveTableAddsTimeColumnsAndTracksOnlyIndeterminateStatuses() =
        runTest {
            val viewModel = viewModel()
            val validStart = TicketMapper.parseUpdatedAtMillis(FIRST_UPDATED)!!
            val displayValue = "2026-10-31 09:00"
            val snapshot =
                listOf(
                    Ticket(
                        key = "HSP-1",
                        summary = "Custom status in progress",
                        status = "Doing",
                        updated = displayValue,
                        issueId = "90001",
                        statusCategoryKey = "indeterminate",
                        updatedAtMillis = validStart,
                    ),
                    Ticket(
                        key = "HSP-2",
                        summary = "Name looks in progress",
                        status = "In Progress",
                        updated = displayValue,
                        issueId = "90002",
                        statusCategoryKey = "new",
                        updatedAtMillis = validStart,
                    ),
                    Ticket(
                        key = "HSP-3",
                        summary = "Missing category",
                        status = "Doing",
                        updated = displayValue,
                        issueId = "90003",
                        updatedAtMillis = validStart,
                    ),
                    Ticket(
                        key = "HSP-4",
                        summary = "Missing issue ID",
                        status = "Doing",
                        updated = displayValue,
                        statusCategoryKey = "indeterminate",
                        updatedAtMillis = validStart,
                    ),
                    Ticket(
                        key = "HSP-5",
                        summary = "Missing updated timestamp",
                        status = "Doing",
                        updated = displayValue,
                        issueId = "90005",
                        statusCategoryKey = "indeterminate",
                    ),
                )

            viewModel.saveTable(
                name = "Báo cáo tuần",
                jql = JQL,
                tickets = snapshot,
                visibleColumns = setOf(ColumnKey.KEY, ColumnKey.SUMMARY),
                jiraBaseUrl = "$BASE_URL/projects/HSP",
            )

            val catalog = storedCatalog()
            val savedTable = catalog.tables.single()
            assertEquals(
                setOf(ColumnKey.KEY, ColumnKey.SUMMARY) + ColumnKey.schemaV3SavedColumns(),
                savedTable.visibleColumns,
            )
            assertEquals(
                listOf(DynamicStartTimeRecord(BASE_URL, "90001", validStart, displayValue)),
                catalog.dynamicStartTimes,
            )
            assertEquals(displayValue, savedTable.tickets[0].dynamicStartTime)
            assertEquals(validStart, savedTable.tickets[0].dynamicStartAtMillis)
            assertNull(savedTable.tickets[1].dynamicStartTime)
        }

    @Test
    fun missingIssueIdOrUpdatedCanBeRetriedOnLaterRefresh() =
        runTest {
            val engine =
                pagingEngine(
                    pages =
                        listOf(
                            searchPage(issueId = null, updated = FIRST_UPDATED),
                            searchPage(issueId = "90001", updated = null),
                            searchPage(issueId = "90001", updated = FIRST_UPDATED),
                        ),
                )
            val viewModel = viewModelWithSavedTables(count = 1, engine = engine)
            viewModel.onConnectionStatusChanged(connected())
            val tableId = tablesOf(viewModel).single().id

            viewModel.refresh(tableId)
            assertTrue(storedCatalog().dynamicStartTimes.isEmpty())

            viewModel.refresh(tableId)
            assertTrue(storedCatalog().dynamicStartTimes.isEmpty())

            viewModel.refresh(tableId)

            val record = storedCatalog().dynamicStartTimes.single()
            assertEquals("90001", record.issueId)
            assertEquals(TicketMapper.parseUpdatedAtMillis(FIRST_UPDATED), record.startedAtMillis)
            assertEquals("2026-10-31 09:00", record.displayValue)
        }

    @Test
    fun firstStartIsImmutableAndSharedAcrossTablesForTheNormalizedSite() =
        runTest {
            val ticket =
                Ticket(
                    key = "HSP-9",
                    summary = "Cùng một issue",
                    status = "To Do",
                    issueId = "90001",
                )
            val firstTable = savedTable("Bảng 1").copy(tickets = listOf(ticket))
            val secondTable =
                savedTable("Bảng 2", jiraBaseUrl = "congty.atlassian.net/projects/HSP")
                    .copy(tickets = listOf(ticket))
            val engine =
                pagingEngine(
                    pages =
                        listOf(
                            searchPage(updated = FIRST_UPDATED),
                            searchPage(updated = SECOND_UPDATED),
                        ),
                )
            val viewModel = viewModelWithTables(listOf(firstTable, secondTable), engine)
            viewModel.onConnectionStatusChanged(connected())
            val tableId = firstTable.id

            viewModel.refresh(tableId)

            val firstRecord = storedCatalog().dynamicStartTimes.single()
            assertEquals(BASE_URL, firstRecord.normalizedJiraBaseUrl)
            assertEquals("2026-10-31 09:00", firstRecord.displayValue)
            assertEquals(
                listOf("2026-10-31 09:00", "2026-10-31 09:00"),
                tablesOf(viewModel).map { it.tickets.single().dynamicStartTime },
            )

            viewModel.refresh(tableId)

            assertEquals(listOf(firstRecord), storedCatalog().dynamicStartTimes)
            assertEquals(
                listOf("2026-10-31 09:00", "2026-10-31 09:00"),
                tablesOf(viewModel).map { it.tickets.single().dynamicStartTime },
            )
        }

    @Test
    fun startLedgerSurvivesMissingIssuesTableDeletionAndReload() =
        runTest {
            val engine =
                pagingEngine(
                    pages =
                        listOf(
                            searchPage(),
                            """{"isLast":true,"issues":[]}""",
                        ),
                )
            val viewModel = viewModelWithSavedTables(count = 1, engine = engine)
            viewModel.onConnectionStatusChanged(connected())
            val tableId = tablesOf(viewModel).single().id

            viewModel.refresh(tableId)
            val record = storedCatalog().dynamicStartTimes.single()

            viewModel.refresh(tableId)
            assertTrue(tablesOf(viewModel).single().tickets.isEmpty())
            assertEquals(listOf(record), storedCatalog().dynamicStartTimes)

            viewModel.toggleColumn(tableId, ColumnKey.DYNAMIC_START_TIME)
            viewModel.renameTable(tableId, "Bảng đã đổi tên")
            assertEquals(listOf(record), storedCatalog().dynamicStartTimes)

            viewModel.moveTableToTrash(tableId)
            val storedAfterTrash = storedCatalog()
            assertTrue(storedAfterTrash.tables.isEmpty())
            assertEquals(
                tableId,
                storedAfterTrash.trashedTables
                    .single()
                    .table.id,
            )
            assertEquals(listOf(record), storedAfterTrash.dynamicStartTimes)

            val reloaded = viewModel()
            reloaded.load()
            assertTrue(tablesOf(reloaded).isEmpty())
            reloaded.saveTable(
                name = "Bảng sau khi tải lại",
                jql = JQL,
                tickets =
                    listOf(
                        Ticket(
                            key = "HSP-9",
                            summary = "Saved after reload",
                            status = "To Do",
                            issueId = "90001",
                        ),
                    ),
                visibleColumns = setOf(ColumnKey.KEY),
                jiraBaseUrl = BASE_URL,
            )

            val reloadedTable = tablesOf(reloaded).single()
            assertEquals("2026-10-31 09:00", reloadedTable.tickets.single().dynamicStartTime)
            assertEquals(listOf(record), storedCatalog().dynamicStartTimes)
        }

    @Test
    fun saveTableKeepsEveryTicketFieldOfTheQueryTable() =
        runTest {
            val viewModel = viewModel()

            viewModel.saveTable("Báo cáo tuần", JQL, tickets(), ColumnKey.all(), BASE_URL)

            assertEquals(tickets(), storedCatalog().tables.single().tickets, "Bảng lưu phải có mọi trường (FR-002)")
        }

    @Test
    fun saveTableWritesCatalogOncePerSave() =
        runTest {
            val viewModel = viewModel()

            viewModel.saveTable("Báo cáo tuần", JQL, tickets(), ColumnKey.all(), BASE_URL)

            assertEquals(1, paths.savedTablesWrites, "Mỗi lần lưu chỉ được ghi file một lần")
            val catalog = storedCatalog()
            assertEquals(SAVED_TABLES_SCHEMA_VERSION, catalog.schemaVersion)
            assertEquals(1, catalog.tables.size)
        }

    @Test
    fun saveTableRejectsBlankNameWithoutWriting() =
        runTest {
            val viewModel = viewModel()

            val result = viewModel.saveTable("   ", JQL, tickets(), ColumnKey.all(), BASE_URL)

            assertEquals(TableNameValidation.Empty, result)
            assertNull(paths.storedSavedTables(), "Tên rỗng thì không được ghi file")
            assertEquals(0, paths.savedTablesWrites)
            assertTrue(
                viewModel.state.value.tables
                    .isEmpty(),
            )
            assertNotNull(viewModel.state.value.message)
        }

    @Test
    fun saveTableRejectsDuplicateNameIgnoringCaseWithoutWriting() =
        runTest {
            val viewModel = viewModel()
            viewModel.saveTable("Báo cáo tuần", JQL, tickets(), ColumnKey.all(), BASE_URL)

            val result = viewModel.saveTable("báo CÁO TUẦN", JQL, tickets(), ColumnKey.all(), BASE_URL)

            assertEquals(TableNameValidation.Duplicate, result)
            assertEquals(1, storedCatalog().tables.size, "Bảng trùng tên không được thêm vào danh mục")
            assertFalse(viewModel.state.value.catalogUnreadable)
        }

    @Test
    fun saveTableRefusesWhenCatalogIsUnreadable() =
        runTest {
            paths.seedRawSavedTables(BROKEN_JSON)
            val viewModel = viewModel()
            viewModel.load()
            assertTrue(viewModel.state.value.catalogUnreadable)

            viewModel.saveTable("Báo cáo tuần", JQL, tickets(), ColumnKey.all(), BASE_URL)

            assertEquals(BROKEN_JSON, paths.storedSavedTables(), "File hỏng không được ghi đè")
            assertEquals(0, paths.savedTablesWrites, "Không được ghi khi danh mục không đọc được")
            assertTrue(
                viewModel.state.value.tables
                    .isEmpty(),
            )
            assertNotNull(viewModel.state.value.message, "Phải giải thích vì sao không lưu được (FR-020)")
        }

    @Test
    fun saveTableMessageMentionsTableName() =
        runTest {
            val viewModel = viewModel()

            viewModel.saveTable("Báo cáo tuần", JQL, tickets(), ColumnKey.all(), BASE_URL)

            assertContains(
                viewModel.state.value.message
                    .orEmpty(),
                "Báo cáo tuần",
            )
        }

    @Test
    fun numberedIdsAreUniqueWhenSavedInTheSameMillis() =
        runTest {
            val viewModel = viewModel()

            viewModel.saveTable("Bảng một", JQL, tickets(), ColumnKey.all(), BASE_URL)
            viewModel.saveTable("Bảng hai", JQL, tickets(), ColumnKey.all(), BASE_URL)

            val ids = storedCatalog().tables.map(SavedTable::id)
            assertEquals(ids.size, ids.toSet().size, "Hai bảng lưu cùng lúc phải có định danh khác nhau")
            assertEquals(listOf("${clock.millis}", "${clock.millis}-2"), ids)
        }

    @Test
    fun writeFailureIsReportedWithoutChangingTheList() =
        runTest {
            val viewModel = viewModel()
            paths.failOnWriteSavedTables = true

            viewModel.saveTable("Báo cáo tuần", JQL, tickets(), ColumnKey.all(), BASE_URL)

            assertTrue(tablesOf(viewModel).isEmpty(), "Ghi lỗi thì danh mục trong bộ nhớ không đổi")
            assertNotNull(viewModel.state.value.message, "Phải báo lý do không lưu được (FR-015)")
            assertNull(paths.storedSavedTables())
        }

    @Test
    fun loadWarnsWhenTheSavedFileIsUnreadable() =
        runTest {
            paths.seedRawSavedTables(BROKEN_JSON)
            val viewModel = viewModel()

            viewModel.load()

            val state = viewModel.state.value
            assertTrue(state.catalogUnreadable, "Dữ liệu hỏng phải bật cảnh báo (FR-020)")
            assertTrue(state.tables.isEmpty())
            assertContains(state.loadWarning.orEmpty(), paths.savedTablesFile(), message = "Cảnh báo nêu vị trí file")
        }

    @Test
    fun loadWarnsWhenSchemaVersionIsUnknown() =
        runTest {
            paths.seedRawSavedTables("""{"schemaVersion":${SAVED_TABLES_SCHEMA_VERSION + 1},"tables":[]}""")
            val viewModel = viewModel()

            viewModel.load()

            assertTrue(viewModel.state.value.catalogUnreadable)
        }

    @Test
    fun loadWithoutFileIsSilent() =
        runTest {
            val viewModel = viewModel()

            viewModel.load()

            val state = viewModel.state.value
            assertFalse(state.catalogUnreadable, "Chưa có file không phải là lỗi")
            assertNull(state.loadWarning)
            assertTrue(state.tables.isEmpty())
        }

    @Test
    fun retryLoadPicksUpARepairedFileAndClearsTheWarning() =
        runTest {
            paths.seedRawSavedTables(BROKEN_JSON)
            val viewModel = viewModel()
            viewModel.load()
            assertTrue(viewModel.state.value.catalogUnreadable)

            SavedTableStore(paths = paths, dispatcher = UnconfinedTestDispatcher())
                .save(SavedTableCatalog(tables = listOf(savedTable(name = "Bảng một"))))
            viewModel.retryLoad()

            val state = viewModel.state.value
            assertFalse(state.catalogUnreadable, "Đọc lại được thì phải tắt cảnh báo")
            assertNull(state.loadWarning)
            assertEquals(1, state.tables.size)
        }

    @Test
    fun selectingATableOpensItWithoutReadingTheFileAgain() =
        runTest {
            val viewModel = viewModelWithSavedTables(count = 1)
            val readsBefore = paths.savedTablesReads
            val id = tablesOf(viewModel).single().id

            viewModel.selectTable(id)

            val state = viewModel.state.value
            assertEquals(id, state.selectedId)
            assertEquals(readsBefore, paths.savedTablesReads, "Mở bảng chỉ dùng dữ liệu trong bộ nhớ (FR-006)")
        }

    @Test
    fun calendarOpensOnlyForASelectedTableAndClosingKeepsSortWithoutSideEffects() =
        runTest {
            val viewModel = viewModelWithSavedTables(count = 1)
            val table = tablesOf(viewModel).single()

            viewModel.openCalendar()

            assertFalse(viewModel.state.value.calendarOpen, "Không có bảng được chọn thì không mở calendar")

            viewModel.selectTable(table.id)
            viewModel.sortSavedTableBy(ColumnKey.SUMMARY)
            val writesBefore = paths.savedTablesWrites
            val requestsBefore = requests.size
            val catalogBefore = storedCatalog()
            clock.millis = 1_800_000_000_000

            viewModel.openCalendar()

            assertTrue(viewModel.state.value.calendarOpen)
            assertEquals(clock.millis, viewModel.state.value.calendarReferenceMillis)
            assertEquals(SortState(column = ColumnKey.SUMMARY), viewModel.state.value.savedTableSort)

            clock.millis += 1_000
            viewModel.openCalendar()
            assertEquals(1_800_000_000_000, viewModel.state.value.calendarReferenceMillis)

            viewModel.closeCalendar()

            assertFalse(viewModel.state.value.calendarOpen)
            assertNull(viewModel.state.value.calendarReferenceMillis)
            assertEquals(table.id, viewModel.state.value.selectedId)
            assertEquals(SortState(column = ColumnKey.SUMMARY), viewModel.state.value.savedTableSort)
            assertEquals(writesBefore, paths.savedTablesWrites, "Calendar không ghi thay đổi vào catalog")
            assertEquals(requestsBefore, requests.size, "Calendar không được gọi Jira")
            assertEquals(catalogBefore, storedCatalog())

            viewModel.openCalendar()
            assertEquals(clock.millis, viewModel.state.value.calendarReferenceMillis)
        }

    @Test
    fun chartOpensForSelectedTableAndPreservesSortWithoutSideEffects() =
        runTest {
            val viewModel = viewModelWithSavedTables(count = 1)
            val table = tablesOf(viewModel).single()
            val requestsBefore = requests.size
            val writesBefore = paths.savedTablesWrites

            viewModel.openCharts()
            assertFalse(viewModel.state.value.chartOpen, "Chart không mở nếu chưa chọn bảng")

            viewModel.selectTable(table.id)
            viewModel.sortSavedTableBy(ColumnKey.SUMMARY)
            viewModel.openCharts()

            assertTrue(viewModel.state.value.chartOpen)
            assertEquals(SavedTableChartType.PIE, viewModel.state.value.chartType)
            assertEquals(SavedTableChartField.STATUS, viewModel.state.value.chartField)
            assertEquals(SortState(column = ColumnKey.SUMMARY), viewModel.state.value.savedTableSort)
            viewModel.selectChartType(SavedTableChartType.BURNDOWN)
            viewModel.selectChartField(SavedTableChartField.PRIORITY)
            viewModel.closeCharts()
            assertFalse(viewModel.state.value.chartOpen)
            assertEquals(table.id, viewModel.state.value.selectedId)
            assertEquals(SavedTableChartType.BURNDOWN, viewModel.state.value.chartType)
            assertEquals(SavedTableChartField.PRIORITY, viewModel.state.value.chartField)
            assertEquals(SortState(column = ColumnKey.SUMMARY), viewModel.state.value.savedTableSort)
            assertEquals(writesBefore, paths.savedTablesWrites)
            assertEquals(requestsBefore, requests.size)
        }

    @Test
    fun selectingAnotherTableClosesCalendarAndResetsSavedTableSort() =
        runTest {
            val viewModel = viewModelWithSavedTables(count = 2)
            val (first, second) = tablesOf(viewModel)

            viewModel.selectTable(first.id)
            viewModel.sortSavedTableBy(ColumnKey.KEY)
            viewModel.openCalendar()
            assertTrue(viewModel.state.value.calendarOpen)

            viewModel.selectTable(second.id)

            assertEquals(second.id, viewModel.state.value.selectedId)
            assertFalse(viewModel.state.value.calendarOpen)
            assertNull(viewModel.state.value.calendarReferenceMillis)
            assertEquals(SortState(), viewModel.state.value.savedTableSort)
        }

    @Test
    fun clearSelectionRemovesTheOpenTable() =
        runTest {
            val viewModel = viewModelWithSavedTables(count = 1)
            viewModel.selectTable(tablesOf(viewModel).single().id)
            viewModel.openCalendar()
            assertTrue(viewModel.state.value.calendarOpen)

            viewModel.clearSelection()

            assertNull(viewModel.state.value.selectedId)
            assertFalse(viewModel.state.value.calendarOpen)
            assertNull(viewModel.state.value.calendarReferenceMillis)
        }

    @Test
    fun togglingAColumnUpdatesOnlyItsOwnTable() =
        runTest {
            val viewModel = viewModelWithSavedTables(count = 2)
            val first = tablesOf(viewModel).first()
            val second = tablesOf(viewModel).last()
            val writesBefore = paths.savedTablesWrites

            viewModel.toggleColumn(first.id, ColumnKey.STATUS)

            val state = viewModel.state.value
            assertEquals(setOf(ColumnKey.KEY, ColumnKey.SUMMARY, ColumnKey.STATUS), state.tables.first().visibleColumns)
            assertEquals(second.visibleColumns, state.tables.last().visibleColumns, "Bảng khác không được đổi cột")
            assertEquals(writesBefore + 1, paths.savedTablesWrites, "Đổi cột phải được lưu lại (FR-017)")
            assertEquals(
                setOf(ColumnKey.KEY, ColumnKey.SUMMARY, ColumnKey.STATUS),
                storedCatalog().tables.first().visibleColumns,
            )
        }

    @Test
    fun theKeyColumnCannotBeHidden() =
        runTest {
            val viewModel = viewModelWithSavedTables(count = 1)
            val id = tablesOf(viewModel).single().id
            val writesBefore = paths.savedTablesWrites

            viewModel.toggleColumn(id, ColumnKey.KEY)

            assertTrue(ColumnKey.KEY in tablesOf(viewModel).single().visibleColumns, "Cột Mã không tắt được")
            assertEquals(writesBefore, paths.savedTablesWrites, "Không đổi gì thì không cần ghi file")
        }

    @Test
    fun openTicketUsesTheBaseUrlSavedWithTheTable() =
        runTest {
            val viewModel = viewModelWithSavedTables(count = 1)
            val id = tablesOf(viewModel).single().id

            val message = viewModel.openTicket(tableId = id, ticketKey = "HSP-1")

            assertNull(message, "Mở thành công thì không có thông báo lỗi")
            assertEquals(listOf("$BASE_URL/browse/HSP-1"), opener.openedUrls)
        }

    @Test
    fun openTicketExplainsWhenTheSavedBaseUrlIsUnusable() =
        runTest {
            val viewModel = viewModelWithSavedTables(count = 1, jiraBaseUrl = "")
            val id = tablesOf(viewModel).single().id

            val message = viewModel.openTicket(tableId = id, ticketKey = "HSP-1")

            assertNotNull(message, "Địa chỉ Jira không hợp lệ phải được báo thay vì im lặng (FR-008)")
            assertTrue(opener.openedUrls.isEmpty())
        }

    @Test
    fun refreshIsRefusedWithoutAConnection() =
        runTest {
            val viewModel = viewModelWithSavedTables(count = 1)
            val id = tablesOf(viewModel).single().id

            viewModel.refresh(id)

            assertNull(viewModel.state.value.refreshingId)
            assertTrue(requests.isEmpty(), "Chưa kết nối thì không được gọi Jira (FR-023)")
            assertNotNull(viewModel.state.value.message)
            assertEquals(0, paths.savedTablesWrites)
        }

    @Test
    fun refreshReplacesEveryTicketFromAllPages() =
        runTest {
            val engine = pagingEngine(pages = listOf(PAGE_ONE, PAGE_TWO))
            val viewModel = viewModelWithSavedTables(count = 1, engine = engine)
            viewModel.onConnectionStatusChanged(connected())
            val before = tablesOf(viewModel).single()
            clock.millis = REFRESHED_AT

            viewModel.refresh(before.id)

            val after = tablesOf(viewModel).single()
            assertEquals(listOf("HSP-9", "HSP-10"), after.tickets.map { it.key }, "Phải thay thế toàn bộ (FR-021)")
            assertEquals(REFRESHED_AT, after.refreshedAtMillis)
            assertEquals(BASE_URL, after.jiraBaseUrl)
            assertEquals(before.name, after.name, "Làm mới không đổi tên bảng")
            assertEquals(before.visibleColumns, after.visibleColumns, "Làm mới không đổi cột hiển thị")
            assertEquals(
                listOf(BurndownSnapshot(REFRESHED_AT, ticketCount = 2, remainingCount = 2, unknownCategoryCount = 2)),
                after.burndownSnapshots,
            )
            assertEquals(1, paths.savedTablesWrites, "Chỉ ghi một lần sau khi đã đủ dữ liệu (FR-026)")
            assertEquals(2, requests.size, "Phải nạp tới trang cuối (FR-022)")
            assertNull(viewModel.state.value.refreshingId)
        }

    @Test
    fun refreshUsesTheNewestCloseOrResolveTransitionForRealDueDate() =
        runTest {
            val engine =
                MockEngine { request ->
                    if (request.url.encodedPath.endsWith("/search/jql")) {
                        respond(
                            content = searchPage(key = "HSP-9", issueId = "90001"),
                            status = HttpStatusCode.OK,
                            headers = JSON_HEADERS,
                        )
                    } else {
                        respond(
                            content =
                                """{"issueChangeLogs":[{"issueId":"90001","changeHistories":[{"created":"2026-01-01T10:00:00Z","items":[{"field":"status","toString":"Close"}]},{"created":"2026-01-03T10:00:00Z","items":[{"field":"status","toString":"Resolve"}]}]}]}""",
                            status = HttpStatusCode.OK,
                            headers = JSON_HEADERS,
                        )
                    }
                }
            val saved =
                savedTable("Bảng 1", jiraTimeZone = "UTC").copy(
                    tickets =
                        listOf(
                            Ticket(
                                key = "HSP-9",
                                summary = "HSP-9",
                                status = "In Progress",
                                issueId = "90001",
                            ),
                        ),
                )
            val viewModel = viewModelWithTables(listOf(saved), engine)
            viewModel.onConnectionStatusChanged(connected("UTC"))

            viewModel.refresh(saved.id)

            val ticket = tablesOf(viewModel).single().tickets.single()
            assertEquals("2026-01-03 10:00", ticket.realDueDate)
            assertEquals(
                kotlin.time.Instant
                    .parse("2026-01-03T10:00:00Z")
                    .toEpochMilliseconds(),
                ticket.realDueAtMillis,
            )
        }

    @Test
    fun refreshKeepsExistingRealDueDateAndLeavesNewIssuesBlankWhenChangelogFails() =
        runTest {
            val engine =
                MockEngine { request ->
                    if (request.url.encodedPath.endsWith("/search/jql")) {
                        respond(
                            content =
                                """{"isLast":true,"issues":[{"id":"90001","key":"HSP-9","fields":{"summary":"HSP-9","status":{"name":"Doing"}}},{"id":"90004","key":"HSP-11","fields":{"summary":"HSP-11","status":{"name":"Doing"}}},{"id":"90002","key":"HSP-10","fields":{"summary":"HSP-10","status":{"name":"Doing"}}}]}""",
                            status = HttpStatusCode.OK,
                            headers = JSON_HEADERS,
                        )
                    } else {
                        respond(content = "{}", status = HttpStatusCode.InternalServerError, headers = JSON_HEADERS)
                    }
                }
            val saved =
                savedTable("Bảng 1").copy(
                    tickets =
                        listOf(
                            Ticket(
                                key = "HSP-9",
                                summary = "HSP-9",
                                status = "In Progress",
                                issueId = "90001",
                                realDueDate = "2026-01-03 10:00",
                                realDueAtMillis =
                                    kotlin.time.Instant
                                        .parse(
                                            "2026-01-03T10:00:00Z",
                                        ).toEpochMilliseconds(),
                            ),
                            Ticket(
                                key = "HSP-11",
                                summary = "HSP-11",
                                status = "In Progress",
                                issueId = "90003",
                                realDueDate = "2026-01-04 10:00",
                                realDueAtMillis =
                                    kotlin.time.Instant
                                        .parse(
                                            "2026-01-04T10:00:00Z",
                                        ).toEpochMilliseconds(),
                            ),
                        ),
                )
            val viewModel = viewModelWithTables(listOf(saved), engine)
            viewModel.onConnectionStatusChanged(connected("UTC"))

            viewModel.refresh(saved.id)

            val refreshed = tablesOf(viewModel).single()
            assertEquals("2026-01-03 10:00", refreshed.tickets.first { it.key == "HSP-9" }.realDueDate)
            assertEquals("2026-01-04 10:00", refreshed.tickets.first { it.key == "HSP-11" }.realDueDate)
            assertNull(refreshed.tickets.first { it.key == "HSP-10" }.realDueDate)
            assertNotNull(refreshed.refreshedAtMillis)
            assertContains(
                viewModel.state.value.message
                    .orEmpty(),
                "Real Due Date chưa được cập nhật",
            )
        }

    @Test
    fun refreshIsAllowedWhenConnectionIsUnverified() =
        runTest {
            val engine = pagingEngine(pages = listOf(PAGE_TWO))
            val viewModel = viewModelWithSavedTables(count = 1, engine = engine)
            viewModel.onConnectionStatusChanged(
                ConnectionStatus.Unverified(baseUrl = BASE_URL, email = "dev@congty.vn"),
            )
            val before = tablesOf(viewModel).single()

            viewModel.refresh(before.id)

            assertEquals(listOf("HSP-10"), tablesOf(viewModel).single().tickets.map { it.key })
            assertEquals(1, requests.size, "Unverified must not block a Jira refresh")
            assertEquals(1, paths.savedTablesWrites)
            assertEquals("Đã làm mới Bảng 1: 1 ticket", viewModel.state.value.message)
        }

    @Test
    fun refreshKeepsOldDataWhenAPageFails() =
        runTest {
            val engine =
                pagingEngine(
                    pages = listOf(searchPage(isLast = false, nextPageToken = "page-2")),
                    failOnCall = 2,
                )
            val viewModel = viewModelWithSavedTables(count = 1, engine = engine)
            viewModel.onConnectionStatusChanged(connected())
            val before = tablesOf(viewModel).single()

            viewModel.refresh(before.id)

            val after = tablesOf(viewModel).single()
            assertEquals(before.tickets, after.tickets, "Lỗi thì dữ liệu cũ phải nguyên vẹn (FR-024)")
            assertEquals(before.burndownSnapshots, after.burndownSnapshots)
            assertNull(after.refreshedAtMillis)
            assertEquals(0, paths.savedTablesWrites, "Lỗi thì không ghi file (FR-024)")
            assertTrue(storedCatalog().dynamicStartTimes.isEmpty(), "Lỗi trang không được commit ledger")
            assertEquals(JiraError.RateLimited().userMessage(), viewModel.state.value.message)
            assertNull(viewModel.state.value.refreshingId)
        }

    @Test
    fun refreshReportsProgressWhileLoadingPages() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val engine = pagingEngine(pages = listOf(PAGE_ONE, PAGE_TWO), gateOnCall = 2, gate = gate)
            val viewModel = viewModelWithSavedTables(count = 1, engine = engine)
            viewModel.onConnectionStatusChanged(connected())
            val id = tablesOf(viewModel).single().id

            // Khởi động ngay (UNDISPATCHED) rồi chờ trang đầu nạp xong; trang thứ hai bị giữ lại ở cổng.
            val job = launch(start = CoroutineStart.UNDISPATCHED) { viewModel.refresh(id) }
            val inProgress = viewModel.state.first { it.refreshedCount > 0 }

            assertEquals(id, inProgress.refreshingId, "Đang làm mới thì phải biết là bảng nào")
            assertEquals(1, inProgress.refreshedCount, "Tiến trình cộng dồn theo từng trang (FR-025)")

            gate.complete(Unit)
            job.join()

            assertEquals(listOf("HSP-9", "HSP-10"), tablesOf(viewModel).single().tickets.map { it.key })
            assertNull(viewModel.state.value.refreshingId)
        }

    @Test
    fun cancellingARefreshKeepsTheStoredDataAndWritesNothing() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val engine =
                pagingEngine(
                    pages = listOf(searchPage(isLast = false, nextPageToken = "page-2"), PAGE_TWO),
                    gateOnCall = 2,
                    gate = gate,
                )
            val viewModel = viewModelWithSavedTables(count = 1, engine = engine)
            viewModel.onConnectionStatusChanged(connected())
            val before = tablesOf(viewModel).single()

            val job = launch(start = CoroutineStart.UNDISPATCHED) { viewModel.refresh(before.id) }
            assertEquals(before.id, viewModel.state.value.refreshingId)

            viewModel.cancelRefresh()
            gate.complete(Unit)
            job.join()

            assertNull(viewModel.state.value.refreshingId, "Dừng xong thì không còn bảng nào đang làm mới")
            assertEquals(before.tickets, tablesOf(viewModel).single().tickets, "Huỷ thì dữ liệu cũ nguyên vẹn (FR-025)")
            assertEquals(before.burndownSnapshots, tablesOf(viewModel).single().burndownSnapshots)
            assertEquals(0, paths.savedTablesWrites, "Huỷ thì không ghi gì (FR-025)")
            assertTrue(storedCatalog().dynamicStartTimes.isEmpty(), "Huỷ refresh không được commit ledger")
        }

    @Test
    fun failedCatalogWriteDoesNotCommitStartAndLaterRefreshCanRetry() =
        runTest {
            val engine =
                pagingEngine(
                    pages =
                        listOf(
                            searchPage(updated = FIRST_UPDATED),
                            searchPage(updated = SECOND_UPDATED),
                        ),
                )
            val viewModel = viewModelWithSavedTables(count = 1, engine = engine)
            viewModel.onConnectionStatusChanged(connected())
            val before = tablesOf(viewModel).single()
            paths.failOnWriteSavedTables = true

            viewModel.refresh(before.id)

            assertEquals(before.tickets, tablesOf(viewModel).single().tickets)
            assertEquals(before.burndownSnapshots, tablesOf(viewModel).single().burndownSnapshots)
            assertTrue(storedCatalog().dynamicStartTimes.isEmpty())
            assertNotNull(viewModel.state.value.message)

            paths.failOnWriteSavedTables = false
            viewModel.refresh(before.id)

            val record = storedCatalog().dynamicStartTimes.single()
            assertEquals(TicketMapper.parseUpdatedAtMillis(SECOND_UPDATED), record.startedAtMillis)
            assertEquals("2026-10-31 10:00", record.displayValue)
        }

    @Test
    fun saveTableStoresVerifiedTimezoneAndCalculatesDueDateFromTheRecordedStart() =
        runTest {
            val viewModel = viewModel()
            val startAtMillis = TicketMapper.parseUpdatedAtMillis(MONDAY_START_UPDATED)!!
            val startDisplay = TicketMapper.formatUpdated(MONDAY_START_UPDATED)
            viewModel.onConnectionStatusChanged(connected(timeZone = HO_CHI_MINH))

            viewModel.saveTable(
                name = "Lịch sprint",
                jql = JQL,
                tickets =
                    listOf(
                        Ticket(
                            key = "HSP-1",
                            summary = "Công việc bắt đầu trong sprint",
                            status = "Doing",
                            updated = startDisplay,
                            issueId = "90001",
                            statusCategoryKey = "indeterminate",
                            updatedAtMillis = startAtMillis,
                            originalExpectedTime = "1h",
                        ),
                    ),
                visibleColumns = setOf(ColumnKey.KEY),
                jiraBaseUrl = BASE_URL,
            )
            val table = tablesOf(viewModel).single()

            assertEquals(HO_CHI_MINH, table.jiraTimeZone)
            assertEquals(startDisplay, table.tickets.single().dynamicStartTime)
            assertEquals("2026-11-02 13:30", table.tickets.single().dynamicDueDate)
            assertEquals(
                TicketMapper.parseUpdatedAtMillis("2026-11-02T13:30:00.000+0700"),
                table.tickets.single().dynamicDueAtMillis,
            )

            val reloaded = viewModel()
            reloaded.load()
            val reloadedTable = tablesOf(reloaded).single()
            assertEquals(HO_CHI_MINH, reloadedTable.jiraTimeZone)
            assertEquals("2026-11-02 13:30", reloadedTable.tickets.single().dynamicDueDate)
        }

    @Test
    fun changedEstimateRecalculatesDueFromTheFirstStartAndUnverifiedKeepsSavedTimezone() =
        runTest {
            val startAtMillis = TicketMapper.parseUpdatedAtMillis(MONDAY_START_UPDATED)!!
            val startDisplay = TicketMapper.formatUpdated(MONDAY_START_UPDATED)
            val record = DynamicStartTimeRecord(BASE_URL, "90001", startAtMillis, startDisplay)
            val table =
                savedTable("Lịch sprint", jiraTimeZone = HO_CHI_MINH).copy(
                    tickets =
                        listOf(
                            Ticket(
                                key = "HSP-9",
                                summary = "Công việc đã có mốc",
                                status = "Doing",
                                updated = startDisplay,
                                issueId = "90001",
                                statusCategoryKey = "indeterminate",
                                updatedAtMillis = startAtMillis,
                                originalExpectedTime = "1h",
                                dynamicStartTime = startDisplay,
                                dynamicStartAtMillis = startAtMillis,
                                dynamicDueDate = "2026-11-02 13:30",
                            ),
                        ),
                )
            val engine =
                pagingEngine(
                    pages =
                        listOf(
                            searchPage(
                                updated = "2026-11-02T16:00:00.000+0700",
                                originalExpectedTime = "2h",
                            ),
                        ),
                )
            val viewModel = viewModelWithTables(listOf(table), engine, listOf(record))
            viewModel.onConnectionStatusChanged(ConnectionStatus.Unverified(BASE_URL, "dev@congty.vn"))

            viewModel.refresh(table.id)

            val refreshed = tablesOf(viewModel).single()
            assertEquals(HO_CHI_MINH, refreshed.jiraTimeZone)
            assertEquals(startDisplay, refreshed.tickets.single().dynamicStartTime)
            assertEquals(startAtMillis, refreshed.tickets.single().dynamicStartAtMillis)
            assertEquals("2h", refreshed.tickets.single().originalExpectedTime)
            assertEquals("2026-11-02 14:30", refreshed.tickets.single().dynamicDueDate)
            assertEquals(listOf(record), storedCatalog().dynamicStartTimes)
        }

    @Test
    fun missingOrInvalidEstimateAndStartLeaveDueDateBlankWithoutTimezoneWarning() =
        runTest {
            val viewModel = viewModel()
            val startAtMillis = TicketMapper.parseUpdatedAtMillis(MONDAY_START_UPDATED)!!
            val startDisplay = TicketMapper.formatUpdated(MONDAY_START_UPDATED)
            viewModel.onConnectionStatusChanged(connected(timeZone = HO_CHI_MINH))

            viewModel.saveTable(
                name = "Thiếu dữ liệu",
                jql = JQL,
                tickets =
                    listOf(
                        Ticket(
                            key = "HSP-1",
                            summary = "Thiếu start",
                            status = "Done",
                            originalExpectedTime = "1h",
                        ),
                        Ticket(
                            key = "HSP-2",
                            summary = "Thiếu estimate",
                            status = "Done",
                            dynamicStartTime = startDisplay,
                            dynamicStartAtMillis = startAtMillis,
                        ),
                        Ticket(
                            key = "HSP-3",
                            summary = "Estimate không hợp lệ",
                            status = "Done",
                            originalExpectedTime = "one hour",
                            dynamicStartTime = startDisplay,
                            dynamicStartAtMillis = startAtMillis,
                        ),
                    ),
                visibleColumns = setOf(ColumnKey.KEY),
                jiraBaseUrl = BASE_URL,
            )
            val table = tablesOf(viewModel).single()
            viewModel.selectTable(table.id)

            assertTrue(table.tickets.all { it.dynamicDueDate == null && it.dynamicDueAtMillis == null })
            assertTrue(
                table.tickets.any { ticket ->
                    ticket.summary == "Estimate không hợp lệ" &&
                        ticket.originalExpectedTime == "one hour"
                },
            )
            assertNull(viewModel.state.value.timeZoneWarning)
        }

    @Test
    fun missingOrInvalidTimezoneLeavesDueDateBlankAndReportsTheReason() =
        runTest {
            listOf(null, "Not/A_Real_Zone").forEachIndexed { index, timeZone ->
                val viewModel = viewModel()
                val startAtMillis = TicketMapper.parseUpdatedAtMillis(MONDAY_START_UPDATED)!!
                val startDisplay = TicketMapper.formatUpdated(MONDAY_START_UPDATED)
                viewModel.onConnectionStatusChanged(connected(timeZone = timeZone))
                viewModel.saveTable(
                    name = "Thiếu múi giờ $index",
                    jql = JQL,
                    tickets =
                        listOf(
                            Ticket(
                                key = "HSP-1",
                                summary = "Có start và estimate",
                                status = "Doing",
                                updated = startDisplay,
                                issueId = "90001",
                                statusCategoryKey = "indeterminate",
                                updatedAtMillis = startAtMillis,
                                originalExpectedTime = "1h",
                            ),
                        ),
                    visibleColumns = setOf(ColumnKey.KEY),
                    jiraBaseUrl = BASE_URL,
                )
                val table = tablesOf(viewModel).single()
                viewModel.selectTable(table.id)

                assertNull(table.tickets.single().dynamicDueDate)
                assertNull(table.tickets.single().dynamicDueAtMillis)
                assertNotNull(viewModel.state.value.timeZoneWarning)
            }
        }

    @Test
    fun renameTableKeepsIdentityAndData() =
        runTest {
            val history =
                listOf(
                    BurndownSnapshot(
                        recordedAtMillis = 1,
                        ticketCount = 3,
                        remainingCount = 2,
                        unknownCategoryCount = 1,
                    ),
                )
            val viewModel =
                viewModelWithTables(
                    listOf(
                        savedTable("Bảng 1").copy(burndownSnapshots = history),
                        savedTable("Bảng 2"),
                    ),
                )
            val target = tablesOf(viewModel).first()
            val writesBefore = paths.savedTablesWrites

            val result = viewModel.renameTable(tableId = target.id, newName = "  Báo cáo mới  ")

            assertEquals(TableNameValidation.Valid, result)
            val renamed = tablesOf(viewModel).first { it.id == target.id }
            assertEquals("Báo cáo mới", renamed.name)
            assertEquals(target.id, renamed.id, "Đổi tên không đổi định danh (FR-009)")
            assertEquals(target.tickets, renamed.tickets, "Đổi tên không đổi dữ liệu ticket")
            assertEquals(target.visibleColumns, renamed.visibleColumns)
            assertEquals(target.savedAtMillis, renamed.savedAtMillis)
            assertEquals(history, renamed.burndownSnapshots)
            assertEquals(writesBefore + 1, paths.savedTablesWrites)
            assertEquals("Báo cáo mới", storedCatalog().tables.first { it.id == target.id }.name)
        }

    @Test
    fun renameTableRejectsBlankAndDuplicateNamesWithoutWriting() =
        runTest {
            val viewModel = viewModelWithSavedTables(count = 2)
            val target = tablesOf(viewModel).first()
            val other = tablesOf(viewModel).last()
            val writesBefore = paths.savedTablesWrites

            val blank = viewModel.renameTable(tableId = target.id, newName = "   ")
            val duplicate = viewModel.renameTable(tableId = target.id, newName = other.name.uppercase())

            assertEquals(TableNameValidation.Empty, blank)
            assertEquals(TableNameValidation.Duplicate, duplicate)
            val keptName = tablesOf(viewModel).first { it.id == target.id }.name
            assertEquals(target.name, keptName, "Tên cũ giữ nguyên (FR-011)")
            assertEquals(writesBefore, paths.savedTablesWrites, "Tên không hợp lệ thì không ghi file")
        }

    @Test
    fun renameTableAcceptsItsOwnNameInAnyCase() =
        runTest {
            val viewModel = viewModelWithSavedTables(count = 1)
            val target = tablesOf(viewModel).single()

            val result = viewModel.renameTable(tableId = target.id, newName = "  ${target.name.uppercase()}  ")

            assertEquals(TableNameValidation.Valid, result, "Tên của chính bảng đó không bị coi là trùng")
            assertEquals(target.name.uppercase(), tablesOf(viewModel).single().name)
        }

    @Test
    fun moveTableToTrashPreservesOnlyTheTargetTableAndClearsItsSelection() =
        runTest {
            val targetHistory =
                listOf(
                    BurndownSnapshot(
                        recordedAtMillis = 1,
                        ticketCount = 1,
                        remainingCount = 1,
                        unknownCategoryCount = 1,
                    ),
                )
            val otherHistory =
                listOf(
                    BurndownSnapshot(
                        recordedAtMillis = 2,
                        ticketCount = 1,
                        remainingCount = 0,
                        unknownCategoryCount = 0,
                    ),
                )
            val viewModel =
                viewModelWithTables(
                    listOf(
                        savedTable("Bảng 1").copy(burndownSnapshots = targetHistory),
                        savedTable("Bảng 2").copy(burndownSnapshots = otherHistory),
                    ),
                )
            val target = tablesOf(viewModel).first()
            val other = tablesOf(viewModel).last()
            viewModel.selectTable(target.id)
            viewModel.openCalendar()
            val writesBefore = paths.savedTablesWrites

            viewModel.moveTableToTrash(target.id)

            assertEquals(listOf(other.id), tablesOf(viewModel).map { it.id })
            assertEquals(
                listOf(TrashedSavedTable(target, clock.millis)),
                viewModel.state.value.trashedTables,
            )
            assertNull(viewModel.state.value.selectedId, "Xoá bảng đang mở thì bỏ chọn (FR-010)")
            assertFalse(viewModel.state.value.calendarOpen)
            assertNull(viewModel.state.value.calendarReferenceMillis)
            assertEquals(writesBefore + 1, paths.savedTablesWrites)
            assertEquals(listOf(other.id), storedCatalog().tables.map { it.id })
            assertEquals(listOf(target), storedCatalog().trashedTables.map { it.table })
            assertEquals(otherHistory, storedCatalog().tables.single().burndownSnapshots)
        }

    @Test
    fun moveTableToTrashKeepsTheSelectionOfAnotherTable() =
        runTest {
            val viewModel = viewModelWithSavedTables(count = 2)
            val first = tablesOf(viewModel).first()
            val second = tablesOf(viewModel).last()
            viewModel.selectTable(second.id)

            viewModel.moveTableToTrash(first.id)

            assertEquals(second.id, viewModel.state.value.selectedId)
        }

    @Test
    fun moveTableToTrashWriteFailureKeepsCatalogAndUiState() =
        runTest {
            val viewModel = viewModelWithSavedTables(count = 1)
            val table = tablesOf(viewModel).single()
            viewModel.selectTable(table.id)
            val before = viewModel.state.value
            val storedBefore = paths.storedSavedTables()
            paths.failOnWriteSavedTables = true

            viewModel.moveTableToTrash(table.id)

            assertEquals(before.tables, viewModel.state.value.tables)
            assertEquals(before.selectedId, viewModel.state.value.selectedId)
            assertTrue(
                viewModel.state.value.trashedTables
                    .isEmpty(),
            )
            assertEquals(storedBefore, paths.storedSavedTables())
            assertNotNull(viewModel.state.value.message)
        }

    @Test
    fun restoreTableRequiresUniqueNameAndPreservesSavedPayload() =
        runTest {
            val snapshots = listOf(BurndownSnapshot(10, 2, 1, 0))
            val target = savedTable("Bảng trùng").copy(burndownSnapshots = snapshots)
            val active = savedTable("bảng trùng")
            val viewModel = viewModelWithTables(listOf(active))
            val catalogWithTrash =
                SavedTableCatalog(
                    tables = listOf(active),
                    dynamicStartTimes =
                        listOf(
                            DynamicStartTimeRecord(
                                normalizedJiraBaseUrl = BASE_URL,
                                issueId = "shared-1",
                                startedAtMillis = 1L,
                                displayValue = FIRST_UPDATED,
                            ),
                        ),
                    trashedTables = listOf(TrashedSavedTable(target, deletedAtMillis = 20L)),
                )
            SavedTableStore(paths = paths, dispatcher = UnconfinedTestDispatcher()).save(catalogWithTrash)
            viewModel.load()

            assertEquals(TableNameValidation.Duplicate, viewModel.restoreTable(target.id))
            assertEquals(listOf(active), viewModel.state.value.tables)
            assertEquals(
                target.id,
                viewModel.state.value.trashedTables
                    .single()
                    .table.id,
            )

            assertEquals(TableNameValidation.Valid, viewModel.restoreTable(target.id, "  Bảng khôi phục  "))

            val restored =
                viewModel.state.value.tables
                    .first { it.id == target.id }
            assertEquals("Bảng khôi phục", restored.name)
            assertEquals(target.copy(name = "Bảng khôi phục"), restored)
            assertTrue(
                viewModel.state.value.trashedTables
                    .isEmpty(),
            )
            assertEquals(catalogWithTrash.dynamicStartTimes, storedCatalog().dynamicStartTimes)
            assertEquals(listOf(active, restored), storedCatalog().tables)
        }

    @Test
    fun restoreTableWithoutNameConflictKeepsOriginalNameAndId() =
        runTest {
            val target = savedTable("Bảng có thể khôi phục")
            val trashed = TrashedSavedTable(target, deletedAtMillis = 20L)
            val viewModel = viewModelWithTables(emptyList())
            SavedTableStore(paths = paths, dispatcher = UnconfinedTestDispatcher())
                .save(SavedTableCatalog(trashedTables = listOf(trashed)))
            viewModel.load()

            assertEquals(TableNameValidation.Valid, viewModel.restoreTable(target.id))

            assertEquals(listOf(target), viewModel.state.value.tables)
            assertTrue(
                viewModel.state.value.trashedTables
                    .isEmpty(),
            )
            assertEquals(listOf(target), storedCatalog().tables)
        }

    @Test
    fun permanentlyDeleteTrashedTableKeepsOtherDataAndSharedLedger() =
        runTest {
            val first = savedTable("Bảng 1")
            val second = savedTable("Bảng 2")
            val dynamicStartTimes =
                listOf(
                    DynamicStartTimeRecord(
                        normalizedJiraBaseUrl = BASE_URL,
                        issueId = "shared-1",
                        startedAtMillis = 1L,
                        displayValue = FIRST_UPDATED,
                    ),
                )
            val viewModel = viewModelWithTables(listOf(first), dynamicStartTimes = dynamicStartTimes)
            val trashed = TrashedSavedTable(second, deletedAtMillis = 2L)
            SavedTableStore(paths = paths, dispatcher = UnconfinedTestDispatcher())
                .save(
                    SavedTableCatalog(
                        tables = listOf(first),
                        dynamicStartTimes = dynamicStartTimes,
                        trashedTables = listOf(trashed),
                    ),
                )
            viewModel.load()
            val writesBefore = paths.savedTablesWrites

            viewModel.permanentlyDeleteTrashedTable(second.id)

            assertEquals(listOf(first), viewModel.state.value.tables)
            assertTrue(
                viewModel.state.value.trashedTables
                    .isEmpty(),
            )
            assertEquals(listOf(first), storedCatalog().tables)
            assertTrue(storedCatalog().trashedTables.isEmpty())
            assertEquals(dynamicStartTimes, storedCatalog().dynamicStartTimes)
            assertEquals(writesBefore + 1, paths.savedTablesWrites)
        }

    @Test
    fun restoreAndPermanentDeleteWriteFailuresKeepTrashIntact() =
        runTest {
            val target = savedTable("Bảng thùng rác")
            val viewModel = viewModelWithTables(emptyList())
            SavedTableStore(paths = paths, dispatcher = UnconfinedTestDispatcher())
                .save(SavedTableCatalog(trashedTables = listOf(TrashedSavedTable(target, 1L))))
            viewModel.load()
            val storedBefore = paths.storedSavedTables()
            paths.failOnWriteSavedTables = true

            viewModel.restoreTable(target.id)
            assertTrue(
                viewModel.state.value.tables
                    .isEmpty(),
            )
            assertEquals(
                target.id,
                viewModel.state.value.trashedTables
                    .single()
                    .table.id,
            )
            assertEquals(storedBefore, paths.storedSavedTables())
            assertNotNull(viewModel.state.value.message)

            viewModel.permanentlyDeleteTrashedTable(target.id)
            assertEquals(
                target.id,
                viewModel.state.value.trashedTables
                    .single()
                    .table.id,
            )
            assertEquals(storedBefore, paths.storedSavedTables())
            assertNotNull(viewModel.state.value.message)
        }

    @Test
    fun renameAndMoveToTrashAreRefusedWhenTheCatalogIsUnreadable() =
        runTest {
            paths.seedRawSavedTables(BROKEN_JSON)
            val viewModel = viewModel()
            viewModel.load()
            assertTrue(viewModel.state.value.catalogUnreadable)

            viewModel.renameTable(tableId = "bất-kỳ", newName = "Tên mới")
            viewModel.moveTableToTrash("bất-kỳ")

            assertEquals(0, paths.savedTablesWrites, "Danh mục không đọc được thì không được ghi (FR-020)")
            assertEquals(BROKEN_JSON, paths.storedSavedTables(), "File hỏng phải được giữ nguyên")
        }

    /** Động cơ trả lần lượt các trang đã cho; có thể chèn lỗi hoặc giữ một trang lại để quan sát tiến trình. */
    private fun OutgoingContent.asText(): String =
        when (this) {
            is TextContent -> text
            is OutgoingContent.ByteArrayContent -> bytes().decodeToString()
            else -> ""
        }

    private fun pagingEngine(
        pages: List<String>,
        failOnCall: Int? = null,
        failureStatus: HttpStatusCode = HttpStatusCode.TooManyRequests,
        gateOnCall: Int? = null,
        gate: CompletableDeferred<Unit>? = null,
    ): MockEngine {
        var call = 0
        return MockEngine { request ->
            if (request.url.encodedPath.endsWith("/changelog/bulkfetch")) {
                respond(content = """{"issueChangeLogs":[]}""", status = HttpStatusCode.OK, headers = JSON_HEADERS)
            } else {
                call += 1
                requests += "search-$call"
                if (call == failOnCall) {
                    respond(content = ERROR_BODY, status = failureStatus, headers = JSON_HEADERS)
                } else {
                    if (call == gateOnCall) gate?.await()
                    val body = pages.getOrElse(call - 1) { pages.last() }
                    respond(content = body, status = HttpStatusCode.OK, headers = JSON_HEADERS)
                }
            }
        }
    }

    private fun connected(): ConnectionStatus = ConnectionStatus.Connected(baseUrl = BASE_URL, email = "dev@congty.vn")

    private fun connected(timeZone: String?): ConnectionStatus =
        ConnectionStatus.Connected(baseUrl = BASE_URL, email = "dev@congty.vn", jiraTimeZone = timeZone)

    private suspend fun viewModelWithSavedTables(
        count: Int,
        jiraBaseUrl: String = BASE_URL,
        jiraTimeZone: String? = null,
        engine: MockEngine = this.engine,
    ): SavedTablesViewModel =
        viewModelWithTables(
            tables = (1..count).map { index -> savedTable(name = "Bảng $index", jiraBaseUrl = jiraBaseUrl) },
            engine = engine,
        )

    private suspend fun viewModelWithTables(
        tables: List<SavedTable>,
        engine: MockEngine = this.engine,
        dynamicStartTimes: List<DynamicStartTimeRecord> = emptyList(),
    ): SavedTablesViewModel {
        SavedTableStore(paths = paths, dispatcher = UnconfinedTestDispatcher())
            .save(SavedTableCatalog(tables = tables, dynamicStartTimes = dynamicStartTimes))
        // Bộ đếm chỉ tính các lần ghi của kịch bản đang kiểm thử, không tính bước dựng dữ liệu ban đầu.
        paths.savedTablesWrites = 0
        return viewModel(engine = engine).also { it.load() }
    }

    private fun searchPage(
        key: String = "HSP-9",
        issueId: String? = "90001",
        statusName: String = "Doing",
        statusCategoryKey: String? = "indeterminate",
        updated: String? = FIRST_UPDATED,
        originalExpectedTime: String? = null,
        isLast: Boolean = true,
        nextPageToken: String? = null,
    ): String {
        val idField = issueId?.let { ",\"id\":\"$it\"" }.orEmpty()
        val categoryField = statusCategoryKey?.let { ",\"statusCategory\":{\"key\":\"$it\"}" }.orEmpty()
        val updatedField = updated?.let { ",\"updated\":\"$it\"" }.orEmpty()
        val estimateField = originalExpectedTime?.let { ",\"timetracking\":{\"originalEstimate\":\"$it\"}" }.orEmpty()
        val nextPageField = nextPageToken?.let { ",\"nextPageToken\":\"$it\"" }.orEmpty()
        val body =
            """
            {
              "isLast": $isLast$nextPageField,
              "issues": [{
                "key": "$key"$idField,
                "fields": {
                  "summary": "$key",
                  "status": {"name": "$statusName"$categoryField}$updatedField$estimateField
                }
              }]
            }
            """.trimIndent()
        return body
    }

    private fun savedTable(
        name: String,
        jiraBaseUrl: String = BASE_URL,
        jiraTimeZone: String? = null,
    ): SavedTable =
        SavedTable(
            id = "id-$name",
            name = name,
            jql = JQL,
            jiraBaseUrl = jiraBaseUrl,
            savedAtMillis = clock.millis,
            visibleColumns = setOf(ColumnKey.KEY, ColumnKey.SUMMARY),
            tickets = tickets(),
            jiraTimeZone = jiraTimeZone,
        )

    private fun tablesOf(viewModel: SavedTablesViewModel): List<SavedTable> {
        val state = viewModel.state.value
        return state.tables
    }

    private fun viewModel(engine: MockEngine = this.engine): SavedTablesViewModel =
        SavedTablesViewModel(
            store = SavedTableStore(paths = paths, dispatcher = UnconfinedTestDispatcher()),
            clock = clock,
            urlOpener = opener,
            apiClient =
                JiraApiClient(
                    client = HttpClientFactory.create(engine),
                    credentials = {
                        JiraCredentials(baseUrl = BASE_URL, email = "dev@congty.vn", token = TEST_TOKEN)
                    },
                ),
            dispatcher = UnconfinedTestDispatcher(),
        )

    private suspend fun storedCatalog(): SavedTableCatalog {
        val result = SavedTableStore(paths = paths, dispatcher = UnconfinedTestDispatcher()).load()
        return assertIs<CatalogLoadResult.Loaded>(result).catalog
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
            Ticket(
                key = "HSP-2",
                summary = "Thêm màn hình báo cáo",
                status = "Done",
                assignee = null,
                priority = null,
                updated = "2026-10-29T08:00:00.000+0700",
            ),
        )

    private companion object {
        const val JQL = "project = HSP ORDER BY created DESC"
        const val BASE_URL = "https://congty.atlassian.net"
        const val BROKEN_JSON = """{"schemaVersion":1,"tables":["""
        const val REFRESHED_AT = 1_700_000_900_000L
        const val FIRST_UPDATED = "2026-10-31T09:00:00.000+0700"
        const val SECOND_UPDATED = "2026-10-31T10:00:00.000+0700"
        const val MONDAY_START_UPDATED = "2026-11-02T11:30:00.000+0700"
        const val HO_CHI_MINH = "Asia/Ho_Chi_Minh"
        const val TEST_TOKEN = "api-token-dung-cho-kiem-thu"
        val JSON_HEADERS = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        const val ERROR_BODY = """{"errorMessages":["Too many requests"],"errors":{}}"""

        /** Trang đầu của lần làm mới: còn trang sau nên phải nạp tiếp (FR-022). */
        val PAGE_ONE =
            """
            {
              "isLast": false,
              "nextPageToken": "token-2",
              "issues": [
                {
                  "id": "90001",
                  "key": "HSP-9",
                  "fields": {
                    "summary": "Việc mới xuất hiện",
                    "status": {"id": "10000", "name": "To Do"},
                    "assignee": null,
                    "priority": null,
                    "updated": "2026-10-31T09:00:00.000+0700"
                  }
                }
              ]
            }
            """.trimIndent()

        /** Trang cuối của lần làm mới. */
        val PAGE_TWO =
            """
            {
              "isLast": true,
              "issues": [
                {
                  "id": "90002",
                  "key": "HSP-10",
                  "fields": {
                    "summary": "Việc mới thứ hai",
                    "status": {"id": "10001", "name": "Done"},
                    "assignee": {"displayName": "Tran Van B"},
                    "priority": {"id": "2", "name": "High"},
                    "updated": "2026-10-31T10:00:00.000+0700"
                  }
                }
              ]
            }
            """.trimIndent()
    }
}
