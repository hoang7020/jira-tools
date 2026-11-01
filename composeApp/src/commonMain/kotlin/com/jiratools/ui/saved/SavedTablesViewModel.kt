package com.jiratools.ui.saved

import com.jiratools.data.ApiResult
import com.jiratools.data.JiraApiClient
import com.jiratools.data.SavedTableStore
import com.jiratools.data.StatusChangelogMapper
import com.jiratools.domain.BaseUrlResult
import com.jiratools.domain.BurndownSnapshot
import com.jiratools.domain.CatalogLoadResult
import com.jiratools.domain.ColumnKey
import com.jiratools.domain.ConnectionStatus
import com.jiratools.domain.DynamicStartTimeRecord
import com.jiratools.domain.SavedTable
import com.jiratools.domain.SavedTableCatalog
import com.jiratools.domain.SavedTableChartField
import com.jiratools.domain.SavedTableChartType
import com.jiratools.domain.SortState
import com.jiratools.domain.TableNameValidation
import com.jiratools.domain.Ticket
import com.jiratools.domain.TrashedSavedTable
import com.jiratools.domain.WorkDuration
import com.jiratools.domain.WorkingTimeCalculator
import com.jiratools.domain.browseUrl
import com.jiratools.domain.errorMessage
import com.jiratools.domain.newTableId
import com.jiratools.domain.normalizeBaseUrl
import com.jiratools.domain.normalizeTableName
import com.jiratools.domain.toggledBy
import com.jiratools.domain.userMessage
import com.jiratools.domain.validateTableName
import com.jiratools.platform.AppClock
import com.jiratools.platform.UrlOpener
import com.jiratools.platform.ioDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant

data class DueDateSyncKey(
    val tableId: String,
    val ticketKey: String,
)

data class DueDateSyncState(
    val targetDate: String,
    val updating: Boolean = false,
    val error: String? = null,
)

/**
 * Trạng thái màn hình "Bảng đã lưu" (data-model, mục 6).
 *
 * Mọi quy tắc nghiệp vụ nằm ngoài Compose: danh mục, bảng đang mở, tiến trình làm mới và thông báo
 * thao tác gần nhất đều được phơi qua `StateFlow` để giao diện chỉ render (Nguyên tắc III).
 */
data class SavedTablesUiState(
    val tables: List<SavedTable> = emptyList(),
    val trashedTables: List<TrashedSavedTable> = emptyList(),
    val selectedId: String? = null,
    val catalogUnreadable: Boolean = false,
    val loadWarning: String? = null,
    val timeZoneWarning: String? = null,
    val refreshingId: String? = null,
    val refreshedCount: Int = 0,
    val message: String? = null,
    val calendarOpen: Boolean = false,
    val calendarReferenceMillis: Long? = null,
    val chartOpen: Boolean = false,
    val chartType: SavedTableChartType = SavedTableChartType.PIE,
    val chartField: SavedTableChartField = SavedTableChartField.STATUS,
    val savedTableSort: SortState = SortState(),
    val dueDateSyncStates: Map<DueDateSyncKey, DueDateSyncState> = emptyMap(),
)

/**
 * Quản lý danh mục bảng kết quả đã lưu: nạp, lưu mới, làm mới và ghi lại các thay đổi cục bộ.
 *
 * Mọi thao tác đều là `suspend` để tầng giao diện tự quản phạm vi coroutine, đồng nhất với
 * `QueryViewModel` và `ConnectionViewModel` hiện có. Khi danh mục không đọc được, ViewModel từ chối
 * mọi thao tác ghi để file hỏng không bị ghi đè (FR-020).
 */
class SavedTablesViewModel(
    private val store: SavedTableStore,
    private val clock: AppClock,
    private val urlOpener: UrlOpener,
    private val apiClient: JiraApiClient,
    private val dispatcher: CoroutineDispatcher = ioDispatcher,
) {
    private val mutableState = MutableStateFlow(SavedTablesUiState())

    private var dynamicStartTimes: List<DynamicStartTimeRecord> = emptyList()
    private var connection: ConnectionStatus = ConnectionStatus.Unconfigured
    private var refreshJob: Job? = null
    private val dueDatePersistenceMutex = Mutex()

    val state: StateFlow<SavedTablesUiState> = mutableState.asStateFlow()

    /** Ghi nhận trạng thái kết nối mới nhất để biết có được phép làm mới hay không (FR-023). */
    fun onConnectionStatusChanged(status: ConnectionStatus) {
        connection = status
    }

    /** Nạp danh mục từ file; file chưa có là trạng thái bình thường, file hỏng thì cảnh báo (FR-004). */
    suspend fun load() {
        val current = mutableState.value
        when (val result = withContext(dispatcher) { store.load() }) {
            is CatalogLoadResult.Loaded -> {
                dynamicStartTimes = result.catalog.dynamicStartTimes
                mutableState.value =
                    current.copy(
                        tables = result.catalog.tables,
                        trashedTables = result.catalog.trashedTables,
                        catalogUnreadable = false,
                        loadWarning = null,
                        timeZoneWarning = timeZoneWarningFor(current.selectedId, result.catalog.tables),
                        dueDateSyncStates = dueDateSyncStates(result.catalog.tables),
                    )
            }
            CatalogLoadResult.Empty -> {
                dynamicStartTimes = emptyList()
                mutableState.value =
                    current.copy(
                        tables = emptyList(),
                        trashedTables = emptyList(),
                        catalogUnreadable = false,
                        loadWarning = null,
                        timeZoneWarning = null,
                        dueDateSyncStates = emptyMap(),
                    )
            }
            is CatalogLoadResult.Unreadable -> {
                dynamicStartTimes = emptyList()
                mutableState.value =
                    current.copy(
                        tables = emptyList(),
                        trashedTables = emptyList(),
                        catalogUnreadable = true,
                        loadWarning = unreadableWarning(result.detail),
                        timeZoneWarning = null,
                        dueDateSyncStates = emptyMap(),
                    )
            }
        }
    }

    /**
     * Lưu kết quả truy vấn đang hiển thị thành một bảng riêng, không gọi Jira (FR-001, FR-002, FR-018).
     *
     * @return kết quả kiểm tra tên: [TableNameValidation.Valid] khi tên dùng được và thao tác đã
     * được thực hiện; [TableNameValidation.Empty] hoặc [TableNameValidation.Duplicate] khi tên chưa
     * dùng được. Kết quả thật của thao tác (thành công hay lý do không ghi được) nằm ở
     * [SavedTablesUiState.message].
     */
    suspend fun saveTable(
        name: String,
        jql: String,
        tickets: List<Ticket>,
        visibleColumns: Set<ColumnKey>,
        jiraBaseUrl: String,
    ): TableNameValidation {
        val current = mutableState.value
        val validation = validateTableName(raw = name, otherNames = current.tables.map(SavedTable::name))
        if (validation != TableNameValidation.Valid) {
            mutableState.value = current.copy(message = validationMessage(validation))
            return validation
        }
        if (current.catalogUnreadable) {
            mutableState.value = current.copy(message = UNREADABLE_WRITE_MESSAGE)
            return validation
        }
        val now = clock.nowMillis()
        val baseUrlResult = normalizeBaseUrl(jiraBaseUrl)
        val normalizedBaseUrl = (baseUrlResult as? BaseUrlResult.Valid)?.baseUrl
        val updatedStartTimes =
            addFirstObservationRecords(
                existing = dynamicStartTimes,
                tickets = tickets,
                normalizedBaseUrl = normalizedBaseUrl,
            )
        val table =
            SavedTable(
                id =
                    newTableId(
                        nowMillis = now,
                        existingIds = current.tables.map(SavedTable::id) + current.trashedTables.map { it.table.id },
                    ),
                name = normalizeTableName(name),
                jql = jql,
                jiraBaseUrl = jiraBaseUrl,
                savedAtMillis = now,
                visibleColumns = visibleColumns + ColumnKey.KEY + ColumnKey.schemaV3SavedColumns(),
                tickets = tickets,
                jiraTimeZone = (connection as? ConnectionStatus.Connected)?.jiraTimeZone,
                burndownSnapshots = listOf(createBurndownSnapshot(tickets, now)),
            )
        val updatedTables = materializeDynamicTimes(current.tables + table, updatedStartTimes)
        val invalidBaseUrl = (baseUrlResult as? BaseUrlResult.Invalid)?.message
        val trackingWarning =
            if (invalidBaseUrl != null && tickets.any { it.statusCategoryKey == IN_PROGRESS_CATEGORY }) {
                "Không ghi nhận được Dynamic Start Time: $invalidBaseUrl"
            } else {
                null
            }
        return write(
            current = current,
            tables = updatedTables,
            dynamicStartTimes = updatedStartTimes,
            successMessage =
                trackingWarning?.let { "Đã lưu bảng ${table.name}. $it" } ?: "Đã lưu bảng ${table.name}",
        )
    }

    /** Ghi danh mục qua đúng một điểm ghi và phản ánh kết quả vào [SavedTablesUiState.message]. */
    private suspend fun write(
        current: SavedTablesUiState,
        tables: List<SavedTable>,
        dynamicStartTimes: List<DynamicStartTimeRecord>,
        successMessage: String?,
        trashedTables: List<TrashedSavedTable> = current.trashedTables,
        stateAfterWrite: SavedTablesUiState = current,
    ): TableNameValidation {
        try {
            withContext(dispatcher) {
                store.save(
                    SavedTableCatalog(
                        tables = tables,
                        dynamicStartTimes = dynamicStartTimes,
                        trashedTables = trashedTables,
                    ),
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            mutableState.value = current.copy(message = writeFailureMessage(failure))
            return TableNameValidation.Valid
        }
        this.dynamicStartTimes = dynamicStartTimes
        mutableState.value =
            stateAfterWrite.copy(
                tables = tables,
                trashedTables = trashedTables,
                message = successMessage,
                timeZoneWarning = timeZoneWarningFor(stateAfterWrite.selectedId, tables),
                dueDateSyncStates = dueDateSyncStates(tables, stateAfterWrite.dueDateSyncStates),
            )
        return TableNameValidation.Valid
    }

    /** Xoá thông báo thao tác gần nhất khi người dùng đã đọc xong (FR-015). */
    fun clearMessage() {
        mutableState.value = mutableState.value.copy(message = null)
    }

    /** Đọc lại danh mục sau khi người dùng xử lý file hỏng (FR-020). */
    suspend fun retryLoad() {
        load()
    }

    /** Mở một bảng để xem chi tiết; chỉ dùng dữ liệu đã nạp trong bộ nhớ (FR-006). */
    fun selectTable(id: String) {
        val current = mutableState.value
        val selectionChanged = current.selectedId != id
        mutableState.value =
            current.copy(
                selectedId = id,
                timeZoneWarning = timeZoneWarningFor(id, current.tables),
                calendarOpen = current.calendarOpen && !selectionChanged,
                calendarReferenceMillis = if (selectionChanged) null else current.calendarReferenceMillis,
                chartOpen = current.chartOpen && !selectionChanged,
                savedTableSort = if (selectionChanged) SortState() else current.savedTableSort,
            )
    }

    /** Bỏ chọn bảng đang mở, ví dụ sau khi xoá chính bảng đó (FR-010). */
    fun clearSelection() {
        mutableState.value =
            mutableState.value.copy(
                selectedId = null,
                timeZoneWarning = null,
                calendarOpen = false,
                calendarReferenceMillis = null,
                chartOpen = false,
                savedTableSort = SortState(),
            )
    }

    /** Mở calendar chỉ đọc cho bảng hiện đang chọn, nếu bảng đó còn trong snapshot đã nạp. */
    fun openCalendar() {
        val current = mutableState.value
        if (current.tables.none { it.id == current.selectedId }) return
        if (current.calendarOpen && current.calendarReferenceMillis != null) return
        mutableState.value =
            current.copy(
                calendarOpen = true,
                calendarReferenceMillis = clock.nowMillis(),
                chartOpen = false,
            )
    }

    /** Trở về chi tiết bảng đang chọn mà vẫn giữ thứ tự sắp xếp hiện tại. */
    fun closeCalendar() {
        mutableState.value = mutableState.value.copy(calendarOpen = false, calendarReferenceMillis = null)
    }

    /** Mở chart cho đúng bảng đang chọn mà không ghi catalog hoặc gọi Jira. */
    fun openCharts() {
        val current = mutableState.value
        if (current.tables.none { it.id == current.selectedId }) return
        mutableState.value = current.copy(chartOpen = true, calendarOpen = false, calendarReferenceMillis = null)
    }

    /** Trở về chi tiết bảng đang chọn. */
    fun closeCharts() {
        mutableState.value = mutableState.value.copy(chartOpen = false)
    }

    fun selectChartType(type: SavedTableChartType) {
        mutableState.value = mutableState.value.copy(chartType = type)
    }

    fun selectChartField(field: SavedTableChartField) {
        mutableState.value = mutableState.value.copy(chartField = field)
    }

    /** Cập nhật thứ tự bảng trong state màn hình; không ghi sort vào catalog. */
    fun sortSavedTableBy(column: ColumnKey) {
        val current = mutableState.value
        mutableState.value = current.copy(savedTableSort = current.savedTableSort.toggledBy(column))
    }

    /**
     * Bật/tắt một cột của đúng bảng đó rồi ghi lại danh mục (FR-017).
     *
     * Cột Mã luôn hiển thị nên không tắt được; khi danh mục không đọc được thì không ghi gì.
     */
    suspend fun toggleColumn(
        tableId: String,
        column: ColumnKey,
    ) {
        if (column == ColumnKey.KEY) return
        val current = mutableState.value
        val table = current.tables.firstOrNull { it.id == tableId } ?: return
        if (current.catalogUnreadable) {
            mutableState.value = current.copy(message = UNREADABLE_WRITE_MESSAGE)
            return
        }
        val columns =
            if (column in
                table.visibleColumns
            ) {
                table.visibleColumns - column
            } else {
                table.visibleColumns + column
            }
        val updated = current.tables.map { if (it.id == tableId) table.copy(visibleColumns = columns) else it }
        write(
            current = current,
            tables = updated,
            dynamicStartTimes = dynamicStartTimes,
            successMessage = null,
        )
    }

    /**
     * Mở ticket của bảng đã lưu bằng trình duyệt, dùng địa chỉ Jira đã lưu cùng bảng đó (FR-008).
     *
     * @return `null` khi đã mở thành công, ngược lại là thông báo lỗi cho người dùng.
     */
    suspend fun openTicket(
        tableId: String,
        ticketKey: String,
    ): String? {
        val tables = mutableState.value.tables
        val table = tables.firstOrNull { it.id == tableId } ?: return NO_TABLE_MESSAGE
        val url = browseUrl(baseUrl = table.jiraBaseUrl, key = ticketKey) ?: return NO_ADDRESS_MESSAGE
        return try {
            if (urlOpener.open(url)) null else cannotOpenMessage(url)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            cannotOpenMessage(url)
        }
    }

    /** Ngày đích chỉ có khi issue có Dynamic Due Date hợp lệ và Due Date hiện tại chưa khớp. */
    fun dueDateSyncTarget(
        tableId: String,
        ticketKey: String,
    ): String? = mutableState.value.dueDateSyncStates[DueDateSyncKey(tableId, ticketKey)]?.targetDate

    /** Gửi cập nhật theo thao tác người dùng và chỉ ghi snapshot sau khi Jira trả thành công. */
    suspend fun updateDueDate(
        tableId: String,
        ticketKey: String,
    ) {
        val current = mutableState.value
        val table = current.tables.firstOrNull { it.id == tableId } ?: return
        val key = DueDateSyncKey(tableId, ticketKey)
        val syncState = current.dueDateSyncStates[key] ?: return
        if (syncState.updating) return
        if (current.catalogUnreadable) {
            mutableState.value = current.copy(message = UNREADABLE_WRITE_MESSAGE)
            return
        }
        val connected = connection as? ConnectionStatus.Connected
        val connectedBaseUrl = connected?.baseUrl?.let(::normalizeBaseUrl)
        val tableBaseUrl = normalizeBaseUrl(table.jiraBaseUrl)
        if (
            connected == null ||
            connectedBaseUrl !is BaseUrlResult.Valid ||
            tableBaseUrl !is BaseUrlResult.Valid ||
            connectedBaseUrl.baseUrl != tableBaseUrl.baseUrl
        ) {
            mutableState.value = current.copy(message = DUE_DATE_CONNECTION_MESSAGE)
            return
        }
        val ticket = table.tickets.firstOrNull { it.key == ticketKey } ?: return
        val issueIdOrKey = ticket.issueId?.trim()?.takeIf(String::isNotEmpty) ?: ticket.key.trim()
        if (issueIdOrKey.isEmpty()) return

        mutableState.value =
            current.copy(
                dueDateSyncStates =
                    current.dueDateSyncStates +
                        (key to syncState.copy(updating = true, error = null)),
                message = null,
            )
        val result =
            try {
                withContext(dispatcher) {
                    apiClient.updateDueDate(issueIdOrKey = issueIdOrKey, dueDate = syncState.targetDate)
                }
            } catch (cancellation: CancellationException) {
                mutableState.value =
                    mutableState.value.copy(
                        dueDateSyncStates =
                            mutableState.value.dueDateSyncStates +
                                (key to syncState.copy(updating = false)),
                    )
                throw cancellation
            }
        when (result) {
            is ApiResult.Failure -> {
                val message = result.error.userMessage()
                mutableState.value =
                    mutableState.value.copy(
                        dueDateSyncStates =
                            mutableState.value.dueDateSyncStates +
                                (key to syncState.copy(updating = false, error = message)),
                        message = message,
                    )
            }
            is ApiResult.Success -> persistDueDateSync(tableId, ticketKey, syncState.targetDate)
        }
    }

    private suspend fun persistDueDateSync(
        tableId: String,
        ticketKey: String,
        targetDate: String,
    ) {
        dueDatePersistenceMutex.withLock {
            val current = mutableState.value
            val updatedTables =
                current.tables.map { table ->
                    if (table.id != tableId) {
                        table
                    } else {
                        table.copy(
                            tickets =
                                table.tickets.map { ticket ->
                                    if (ticket.key == ticketKey) ticket.copy(dueDate = targetDate) else ticket
                                },
                        )
                    }
                }
            try {
                withContext(dispatcher) {
                    store.save(
                        SavedTableCatalog(
                            tables = updatedTables,
                            dynamicStartTimes = dynamicStartTimes,
                            trashedTables = current.trashedTables,
                        ),
                    )
                }
                mutableState.value =
                    current.copy(
                        tables = updatedTables,
                        dueDateSyncStates =
                            dueDateSyncStates(
                                updatedTables,
                                current.dueDateSyncStates - DueDateSyncKey(tableId, ticketKey),
                            ),
                        message = "Đã cập nhật Due Date cho $ticketKey.",
                    )
            } catch (cancellation: CancellationException) {
                mutableState.value =
                    current.copy(
                        tables = updatedTables,
                        dueDateSyncStates =
                            dueDateSyncStates(
                                updatedTables,
                                current.dueDateSyncStates - DueDateSyncKey(tableId, ticketKey),
                            ),
                        message =
                            "Jira đã cập nhật Due Date cho $ticketKey nhưng thao tác lưu snapshot bị hủy.",
                    )
                throw cancellation
            } catch (failure: Throwable) {
                mutableState.value =
                    current.copy(
                        tables = updatedTables,
                        dueDateSyncStates =
                            dueDateSyncStates(
                                updatedTables,
                                current.dueDateSyncStates - DueDateSyncKey(tableId, ticketKey),
                            ),
                        message =
                            "Jira đã cập nhật Due Date cho $ticketKey nhưng chưa lưu được snapshot: " +
                                (failure.message ?: "lỗi không xác định") +
                                ".",
                    )
            }
        }
    }

    /**
     * Làm mới bảng đã lưu: nạp lại **toàn bộ** kết quả hiện tại của đúng JQL đã lưu rồi thay thế dữ
     * liệu cũ (FR-021, FR-022).
     *
     * Chỉ chạy khi đang kết nối Jira; mọi thất bại ở bất kỳ trang nào đều giữ nguyên dữ liệu cũ và
     * không ghi file (FR-023, FR-024). Người dùng có thể dừng giữa đường bằng [cancelRefresh] (FR-025).
     */
    suspend fun refresh(tableId: String) {
        val current = mutableState.value
        val table = current.tables.firstOrNull { it.id == tableId } ?: return
        if (connection == ConnectionStatus.Unconfigured) {
            mutableState.value = current.copy(message = NOT_CONNECTED_REFRESH_MESSAGE)
            return
        }
        val baseUrl =
            when (val status = connection) {
                is ConnectionStatus.Connected -> status.baseUrl
                is ConnectionStatus.Unverified -> status.baseUrl
                else -> table.jiraBaseUrl
            }
        val jiraTimeZone =
            when (val status = connection) {
                is ConnectionStatus.Connected -> status.jiraTimeZone
                is ConnectionStatus.Unverified -> table.jiraTimeZone
                else -> table.jiraTimeZone
            }
        if (current.catalogUnreadable) {
            mutableState.value = current.copy(message = UNREADABLE_WRITE_MESSAGE)
            return
        }
        refreshJob = currentCoroutineContext()[Job]
        mutableState.value = current.copy(refreshingId = tableId, refreshedCount = 0, message = null)
        try {
            loadEveryPage(table = table, baseUrl = baseUrl, jiraTimeZone = jiraTimeZone)
        } catch (cancellation: CancellationException) {
            mutableState.value = mutableState.value.copy(message = REFRESH_CANCELLED_MESSAGE)
            throw cancellation
        } finally {
            refreshJob = null
            mutableState.value = mutableState.value.copy(refreshingId = null, refreshedCount = 0)
        }
    }

    /** Dừng lần làm mới đang chạy, nếu có (FR-025). */
    fun cancelRefresh() {
        refreshJob?.cancel()
    }

    /** Nạp lần lượt mọi trang rồi thay thế dữ liệu; chỉ ghi file khi đã có đủ kết quả (FR-021, FR-022). */
    private suspend fun loadEveryPage(
        table: SavedTable,
        baseUrl: String,
        jiraTimeZone: String?,
    ) {
        val collected = mutableListOf<Ticket>()
        var pageToken: String? = null
        while (true) {
            val result = withContext(dispatcher) { apiClient.searchJql(jql = table.jql, nextPageToken = pageToken) }
            val page =
                when (result) {
                    is ApiResult.Success -> result.value
                    is ApiResult.Failure -> {
                        mutableState.value = mutableState.value.copy(message = result.error.userMessage())
                        return
                    }
                }
            collected.addAll(page.tickets)
            mutableState.value = mutableState.value.copy(refreshedCount = collected.size)
            val next = page.nextPageToken
            if (page.isLast || next == null) break
            pageToken = next
        }
        val current = mutableState.value.copy(refreshingId = null, refreshedCount = 0)
        val issueIds = collected.mapNotNull { it.issueId?.trim()?.takeIf(String::isNotEmpty) }.distinct()
        val changelogResult =
            if (issueIds.isEmpty()) {
                ApiResult.Success(emptyList())
            } else {
                withContext(dispatcher) { apiClient.bulkStatusChangelogs(issueIds) }
            }
        val (ticketsWithRealDue, changelogWarning) =
            when (changelogResult) {
                is ApiResult.Success -> {
                    val realDueTimes = StatusChangelogMapper.latestRealDueAtMillis(changelogResult.value)
                    collected.map { ticket ->
                        val atMillis =
                            ticket.issueId
                                ?.trim()
                                ?.takeIf(String::isNotEmpty)
                                ?.let(realDueTimes::get)
                        ticket.copy(
                            realDueAtMillis = atMillis,
                            realDueDate =
                                atMillis?.let { value ->
                                    jiraTimeZone?.let { zone ->
                                        WorkingTimeCalculator.formatInTimeZone(value, zone)
                                    }
                                },
                        )
                    } to null
                }
                is ApiResult.Failure -> {
                    val previousSite = (normalizeBaseUrl(table.jiraBaseUrl) as? BaseUrlResult.Valid)?.baseUrl
                    val currentSite = (normalizeBaseUrl(baseUrl) as? BaseUrlResult.Valid)?.baseUrl
                    val sameJiraSite = previousSite != null && previousSite == currentSite
                    val oldTickets =
                        if (sameJiraSite) {
                            table.tickets.associateBy { it.issueId?.trim()?.takeIf(String::isNotEmpty) }
                        } else {
                            emptyMap()
                        }
                    collected.map { ticket ->
                        val previous =
                            ticket.issueId
                                ?.trim()
                                ?.takeIf(String::isNotEmpty)
                                ?.let(oldTickets::get)
                                ?: table.tickets.firstOrNull { it.key == ticket.key }.takeIf { sameJiraSite }
                        ticket.copy(
                            realDueDate = previous?.realDueDate,
                            realDueAtMillis = previous?.realDueAtMillis,
                        )
                    } to "Real Due Date chưa được cập nhật: ${changelogResult.error.userMessage()}"
                }
            }
        val baseUrlResult = normalizeBaseUrl(baseUrl)
        val normalizedBaseUrl = (baseUrlResult as? BaseUrlResult.Valid)?.baseUrl
        val updatedStartTimes =
            addFirstObservationRecords(
                existing = dynamicStartTimes,
                tickets = ticketsWithRealDue,
                normalizedBaseUrl = normalizedBaseUrl,
            )
        val refreshedAtMillis = clock.nowMillis()
        val refreshed =
            table.copy(
                tickets = ticketsWithRealDue,
                refreshedAtMillis = refreshedAtMillis,
                jiraBaseUrl = baseUrl,
                jiraTimeZone = jiraTimeZone,
            )
        val refreshedWithSnapshot =
            refreshed.copy(
                burndownSnapshots =
                    table.burndownSnapshots +
                        createBurndownSnapshot(ticketsWithRealDue, refreshedAtMillis),
            )
        val updated =
            materializeDynamicTimes(
                current.tables.map { if (it.id == table.id) refreshedWithSnapshot else it },
                updatedStartTimes,
            )
        val invalidBaseUrl = (baseUrlResult as? BaseUrlResult.Invalid)?.message
        val trackingWarning =
            if (invalidBaseUrl != null && collected.any { it.statusCategoryKey == IN_PROGRESS_CATEGORY }) {
                "Không ghi nhận được Dynamic Start Time: $invalidBaseUrl"
            } else {
                null
            }
        val refreshMessage = "Đã làm mới ${table.name}: ${collected.size} ticket"
        val message =
            listOfNotNull(trackingWarning, changelogWarning).fold(refreshMessage) { currentMessage, warning ->
                "$currentMessage. $warning"
            }
        write(
            current = current,
            tables = updated,
            dynamicStartTimes = updatedStartTimes,
            successMessage = message,
        )
    }

    /**
     * Đổi tên một bảng đã lưu; định danh, dữ liệu và cột hiển thị của bảng không đổi (FR-009, FR-011).
     *
     * @return kết quả kiểm tra tên mới; tên của chính bảng đang đổi không bị coi là trùng.
     */
    suspend fun renameTable(
        tableId: String,
        newName: String,
    ): TableNameValidation {
        val current = mutableState.value
        val table = current.tables.firstOrNull { it.id == tableId } ?: return TableNameValidation.Valid
        val otherNames = current.tables.filter { it.id != tableId }.map(SavedTable::name)
        val validation = validateTableName(raw = newName, otherNames = otherNames)
        if (validation != TableNameValidation.Valid) {
            mutableState.value = current.copy(message = validationMessage(validation))
            return validation
        }
        if (current.catalogUnreadable) {
            mutableState.value = current.copy(message = UNREADABLE_WRITE_MESSAGE)
            return validation
        }
        val renamed = table.copy(name = normalizeTableName(newName))
        val updated = current.tables.map { if (it.id == tableId) renamed else it }
        return write(
            current = current,
            tables = updated,
            dynamicStartTimes = dynamicStartTimes,
            successMessage = "Đã đổi tên bảng thành ${renamed.name}",
        )
    }

    /**
     * Xoá một bảng đã lưu sau khi giao diện đã xác nhận (FR-010).
     *
     * Bảng đang mở bị xoá thì phần chi tiết quay về hướng dẫn chọn bảng; các bảng khác không đổi.
     */
    suspend fun moveTableToTrash(tableId: String) {
        val current = mutableState.value
        val table = current.tables.firstOrNull { it.id == tableId } ?: return
        if (current.catalogUnreadable) {
            mutableState.value = current.copy(message = UNREADABLE_WRITE_MESSAGE)
            return
        }
        val updated = current.tables.filterNot { it.id == tableId }
        val trashed =
            current.trashedTables +
                TrashedSavedTable(
                    table = table,
                    deletedAtMillis = clock.nowMillis(),
                )
        val selectedId = current.selectedId.takeIf { it != tableId }
        val selectionChanged = selectedId != current.selectedId
        val stateAfterWrite =
            current.copy(
                selectedId = selectedId,
                timeZoneWarning = timeZoneWarningFor(selectedId, updated),
                calendarOpen = current.calendarOpen && !selectionChanged,
                calendarReferenceMillis = if (selectionChanged) null else current.calendarReferenceMillis,
                chartOpen = current.chartOpen && !selectionChanged,
                savedTableSort = if (selectionChanged) SortState() else current.savedTableSort,
                refreshingId = current.refreshingId.takeIf { it != tableId },
                refreshedCount = if (current.refreshingId == tableId) 0 else current.refreshedCount,
            )
        if (current.refreshingId == tableId) refreshJob?.cancel()
        write(
            current = current,
            tables = updated,
            dynamicStartTimes = dynamicStartTimes,
            successMessage = "Đã chuyển bảng ${table.name} vào thùng rác",
            trashedTables = trashed,
            stateAfterWrite = stateAfterWrite,
        )
    }

    /**
     * Khôi phục một bảng trong thùng rác. Khi [name] không được cung cấp và tên cũ đang dùng, trả
     * [TableNameValidation.Duplicate] mà không ghi catalog để giao diện yêu cầu tên thay thế.
     */
    suspend fun restoreTable(
        tableId: String,
        name: String? = null,
    ): TableNameValidation {
        val current = mutableState.value
        val trashed = current.trashedTables.firstOrNull { it.table.id == tableId } ?: return TableNameValidation.Valid
        if (current.catalogUnreadable) {
            mutableState.value = current.copy(message = UNREADABLE_WRITE_MESSAGE)
            return TableNameValidation.Valid
        }
        val restoredName = name ?: trashed.table.name
        val validation = validateTableName(restoredName, current.tables.map(SavedTable::name))
        if (validation != TableNameValidation.Valid) return validation

        val restoredTable = trashed.table.copy(name = normalizeTableName(restoredName))
        write(
            current = current,
            tables = current.tables + restoredTable,
            dynamicStartTimes = dynamicStartTimes,
            successMessage = "Đã khôi phục bảng ${restoredTable.name}",
            trashedTables = current.trashedTables.filterNot { it.table.id == tableId },
        )
        return TableNameValidation.Valid
    }

    /** Xoá vĩnh viễn một mục đã được người dùng xác nhận ở giao diện. */
    suspend fun permanentlyDeleteTrashedTable(tableId: String) {
        val current = mutableState.value
        if (current.trashedTables.none { it.table.id == tableId }) return
        if (current.catalogUnreadable) {
            mutableState.value = current.copy(message = UNREADABLE_WRITE_MESSAGE)
            return
        }
        val tableName =
            current.trashedTables
                .first { it.table.id == tableId }
                .table.name
        write(
            current = current,
            tables = current.tables,
            dynamicStartTimes = dynamicStartTimes,
            successMessage = "Đã xoá vĩnh viễn bảng $tableName",
            trashedTables = current.trashedTables.filterNot { it.table.id == tableId },
        )
    }

    private fun addFirstObservationRecords(
        existing: List<DynamicStartTimeRecord>,
        tickets: List<Ticket>,
        normalizedBaseUrl: String?,
    ): List<DynamicStartTimeRecord> {
        if (normalizedBaseUrl == null) return existing
        val records = existing.toMutableList()
        val existingKeys = existing.mapTo(mutableSetOf()) { it.normalizedJiraBaseUrl to it.issueId }
        tickets.forEach { ticket ->
            if (ticket.statusCategoryKey != IN_PROGRESS_CATEGORY) return@forEach
            val issueId = ticket.issueId?.trim()?.takeIf(String::isNotEmpty) ?: return@forEach
            val startedAtMillis = ticket.updatedAtMillis ?: return@forEach
            val key = normalizedBaseUrl to issueId
            if (existingKeys.add(key)) {
                records +=
                    DynamicStartTimeRecord(
                        normalizedJiraBaseUrl = normalizedBaseUrl,
                        issueId = issueId,
                        startedAtMillis = startedAtMillis,
                        displayValue = ticket.updated,
                    )
            }
        }
        return records
    }

    private fun createBurndownSnapshot(
        tickets: List<Ticket>,
        recordedAtMillis: Long,
    ): BurndownSnapshot {
        val categories = tickets.map { it.statusCategoryKey?.trim()?.lowercase() }
        val remainingCount = categories.count { it != DONE_CATEGORY }
        val unknownCategoryCount = categories.count { it !in KNOWN_STATUS_CATEGORIES }
        return BurndownSnapshot(
            recordedAtMillis = recordedAtMillis,
            ticketCount = tickets.size,
            remainingCount = remainingCount,
            unknownCategoryCount = unknownCategoryCount,
        )
    }

    private fun materializeDynamicTimes(
        tables: List<SavedTable>,
        records: List<DynamicStartTimeRecord>,
    ): List<SavedTable> =
        tables.map { table ->
            val normalizedBaseUrl = (normalizeBaseUrl(table.jiraBaseUrl) as? BaseUrlResult.Valid)?.baseUrl
            val recordsByIssueId = mutableMapOf<String, DynamicStartTimeRecord>()
            records.forEach { record ->
                if (
                    normalizedBaseUrl != null &&
                    record.normalizedJiraBaseUrl == normalizedBaseUrl &&
                    record.issueId !in recordsByIssueId
                ) {
                    recordsByIssueId[record.issueId] = record
                }
            }
            table.copy(
                tickets =
                    table.tickets.map { ticket ->
                        val issueId = ticket.issueId?.trim()
                        val record = issueId?.let(recordsByIssueId::get)
                        val withStart =
                            if (record == null) {
                                ticket
                            } else {
                                ticket.copy(
                                    dynamicStartTime = record.displayValue,
                                    dynamicStartAtMillis = record.startedAtMillis,
                                )
                            }
                        calculateDynamicDueDate(withStart, table.jiraTimeZone)
                    },
            )
        }

    private fun calculateDynamicDueDate(
        ticket: Ticket,
        timeZoneId: String?,
    ): Ticket {
        val duration = WorkDuration.parse(ticket.originalExpectedTime.orEmpty())
        val startAtMillis = ticket.dynamicStartAtMillis
        val dueAtMillis =
            if (duration != null && startAtMillis != null && !timeZoneId.isNullOrBlank()) {
                WorkingTimeCalculator.addWorkingTime(startAtMillis, duration, timeZoneId)
            } else {
                null
            }
        val dueDate = dueAtMillis?.let { WorkingTimeCalculator.formatInTimeZone(it, timeZoneId.orEmpty()) }
        return ticket.copy(
            dynamicDueDate = dueDate,
            dynamicDueAtMillis = dueAtMillis.takeIf { dueDate != null },
        )
    }

    private fun timeZoneWarningFor(
        selectedId: String?,
        tables: List<SavedTable>,
    ): String? {
        val table = tables.firstOrNull { it.id == selectedId } ?: return null
        val timeZoneId = table.jiraTimeZone
        return when {
            timeZoneId.isNullOrBlank() ->
                "Không có múi giờ Jira cho bảng “${table.name}”; Dynamic Due Date sẽ để trống."
            !WorkingTimeCalculator.isValidTimeZone(timeZoneId) ->
                "Múi giờ Jira “$timeZoneId” của bảng “${table.name}” không hợp lệ; Dynamic Due Date sẽ để trống."
            else -> null
        }
    }

    private fun dueDateSyncStates(
        tables: List<SavedTable>,
        previous: Map<DueDateSyncKey, DueDateSyncState> = emptyMap(),
    ): Map<DueDateSyncKey, DueDateSyncState> =
        buildMap {
            for (table in tables) {
                for (ticket in table.tickets) {
                    val targetDate = targetDueDate(table, ticket) ?: continue
                    if (ticket.dueDate == targetDate) continue
                    val key = DueDateSyncKey(table.id, ticket.key)
                    val existing = previous[key]?.takeIf { it.targetDate == targetDate }
                    put(key, existing ?: DueDateSyncState(targetDate))
                }
            }
        }

    private fun targetDueDate(
        table: SavedTable,
        ticket: Ticket,
    ): String? {
        val timeZoneId = table.jiraTimeZone?.takeIf(String::isNotBlank) ?: return null
        if (!WorkingTimeCalculator.isValidTimeZone(timeZoneId)) return null
        val millis = ticket.dynamicDueAtMillis ?: return null
        return try {
            Instant
                .fromEpochMilliseconds(millis)
                .toLocalDateTime(TimeZone.of(timeZoneId))
                .date
                .toString()
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: kotlinx.datetime.DateTimeArithmeticException) {
            null
        }
    }

    private fun validationMessage(validation: TableNameValidation): String = validation.errorMessage().orEmpty()

    private fun unreadableWarning(detail: String?): String {
        val base = "Không đọc được dữ liệu bảng đã lưu tại ${store.filePath()}."
        return detail?.let { "$base Lý do: $it" } ?: base
    }

    private fun writeFailureMessage(failure: Throwable): String =
        "Không ghi được bảng đã lưu: ${failure.message ?: "lỗi không xác định"}."

    /** Lời nhắn khi không mở được trình duyệt, kèm địa chỉ để người dùng mở thủ công (FR-008). */
    private fun cannotOpenMessage(url: String): String = "Không mở được trình duyệt. Bạn có thể mở thủ công: $url"

    private companion object {
        const val IN_PROGRESS_CATEGORY = "indeterminate"
        const val DONE_CATEGORY = "done"
        val KNOWN_STATUS_CATEGORIES = setOf("new", IN_PROGRESS_CATEGORY, DONE_CATEGORY)
        const val UNREADABLE_WRITE_MESSAGE =
            "Chưa lưu được vì dữ liệu bảng đã lưu không đọc được. Vui lòng xử lý cảnh báo rồi thử lại."
        const val DUE_DATE_CONNECTION_MESSAGE =
            "Cần kết nối thành công tới đúng Jira site của bảng đã lưu trước khi cập nhật Due Date."
        const val NOT_CONNECTED_REFRESH_MESSAGE = "Cần kết nối Jira thành công trước khi làm mới bảng."
        const val REFRESH_CANCELLED_MESSAGE = "Đã dừng làm mới bảng. Dữ liệu cũ vẫn được giữ nguyên."
        const val NO_TABLE_MESSAGE = "Không tìm thấy bảng đã lưu."
        const val NO_ADDRESS_MESSAGE = "Chưa có địa chỉ Jira hợp lệ cho bảng này."
    }
}
