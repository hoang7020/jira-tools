package com.jiratools.ui.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jiratools.domain.ColumnKey
import com.jiratools.domain.QueryState
import com.jiratools.domain.SavedTable
import com.jiratools.domain.SavedTableChartField
import com.jiratools.domain.SavedTableChartType
import com.jiratools.domain.SortState
import com.jiratools.domain.projectSavedTableCalendar
import com.jiratools.domain.sortTickets
import com.jiratools.platform.AppClock
import com.jiratools.ui.query.TicketRowActionState
import com.jiratools.ui.query.TicketTable
import kotlinx.coroutines.delay

private val LIST_WIDTH = 280.dp
private const val SINGLE_LINE = 1

/**
 * Màn hình "Bảng đã lưu": danh sách bảng ở cột trái và chi tiết bảng đang chọn ở cột phải.
 *
 * Toàn bộ dữ liệu đến từ [SavedTablesUiState] đã nạp trong bộ nhớ nên màn hình này xem được cả khi
 * không có kết nối Jira (FR-004, FR-006). Lưới dữ liệu tái dùng `TicketTable` của màn hình truy vấn
 * bằng cách ánh xạ bảng đã lưu sang `QueryState.Success`, nhờ đó có sẵn cuộn, cột Mã cố định, sắp
 * xếp và ẩn/hiện cột mà không sửa thành phần dùng chung (FR-007, FR-017, FR-019).
 */
@Composable
fun SavedTablesArea(
    state: SavedTablesUiState,
    clock: AppClock,
    connected: Boolean,
    onRetryLoad: () -> Unit,
    onSelectTable: (String) -> Unit,
    onRename: (SavedTable) -> Unit,
    onDelete: (SavedTable) -> Unit,
    onRefresh: (String) -> Unit,
    onCancelRefresh: () -> Unit,
    onToggleColumn: (String, ColumnKey) -> Unit,
    onOpenCalendar: () -> Unit,
    onCloseCalendar: () -> Unit,
    onOpenCharts: () -> Unit,
    onCloseCharts: () -> Unit,
    onChartTypeChange: (SavedTableChartType) -> Unit,
    onChartFieldChange: (SavedTableChartField) -> Unit,
    onSortSavedTableBy: (ColumnKey) -> Unit,
    onOpenTicket: (String, String) -> Unit,
    onUpdateDueDate: (String, String) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentMillis by remember { mutableLongStateOf(clock.nowMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            currentMillis = clock.nowMillis()
        }
    }
    Column(modifier = modifier.fillMaxSize()) {
        SavedMessageBanner(message = state.message, onDismiss = onDismissMessage)
        TimeZoneWarningBanner(warning = state.timeZoneWarning)
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            SavedTableList(
                state = state,
                clock = clock,
                onRetryLoad = onRetryLoad,
                onSelectTable = onSelectTable,
                onRename = onRename,
                onDelete = onDelete,
            )
            VerticalDivider()
            Box(modifier = Modifier.fillMaxHeight().weight(1f)) {
                val selected = state.tables.firstOrNull { it.id == state.selectedId }
                if (selected == null) {
                    Hint("Chọn một bảng ở cột bên trái để xem dữ liệu đã lưu.")
                } else {
                    SavedTableDetail(
                        table = selected,
                        clock = clock,
                        connected = connected,
                        refreshing = state.refreshingId == selected.id,
                        refreshRunning = state.refreshingId != null,
                        refreshedCount = state.refreshedCount,
                        onRefresh = { onRefresh(selected.id) },
                        onCancelRefresh = onCancelRefresh,
                        onToggleColumn = { column -> onToggleColumn(selected.id, column) },
                        sort = state.savedTableSort,
                        calendarOpen = state.calendarOpen,
                        calendarReferenceMillis = state.calendarReferenceMillis,
                        chartOpen = state.chartOpen,
                        chartType = state.chartType,
                        chartField = state.chartField,
                        onOpenCalendar = onOpenCalendar,
                        onCloseCalendar = onCloseCalendar,
                        onOpenCharts = onOpenCharts,
                        onCloseCharts = onCloseCharts,
                        onChartTypeChange = onChartTypeChange,
                        onChartFieldChange = onChartFieldChange,
                        onSortBy = onSortSavedTableBy,
                        onOpenTicket = { ticketKey -> onOpenTicket(selected.id, ticketKey) },
                        rowActions =
                            state.dueDateSyncStates
                                .filterKeys { it.tableId == selected.id }
                                .mapKeys { it.key.ticketKey }
                                .mapValues { (_, sync) ->
                                    TicketRowActionState(
                                        targetDate = sync.targetDate,
                                        updating = sync.updating,
                                        error = sync.error,
                                    )
                                },
                        onUpdateDueDate = { ticketKey -> onUpdateDueDate(selected.id, ticketKey) },
                        nowAtMillis = currentMillis,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeZoneWarningBanner(warning: String?) {
    if (warning == null) return
    Text(
        text = warning,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun SavedTableList(
    state: SavedTablesUiState,
    clock: AppClock,
    onRetryLoad: () -> Unit,
    onSelectTable: (String) -> Unit,
    onRename: (SavedTable) -> Unit,
    onDelete: (SavedTable) -> Unit,
) {
    Column(modifier = Modifier.width(LIST_WIDTH).fillMaxHeight().padding(8.dp)) {
        Text(text = "Bảng đã lưu", style = MaterialTheme.typography.titleMedium)
        val warning = state.loadWarning
        if (warning != null) {
            UnreadableBanner(warning = warning, onRetry = onRetryLoad)
        }
        if (state.tables.isEmpty()) {
            Text(
                text = "Chưa có bảng nào được lưu.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            LazyColumn {
                items(state.tables, key = { it.id }) { table ->
                    SavedTableListItem(
                        table = table,
                        selected = table.id == state.selectedId,
                        clock = clock,
                        onClick = { onSelectTable(table.id) },
                        onRename = { onRename(table) },
                        onDelete = { onDelete(table) },
                    )
                }
            }
        }
    }
}

/** Một mục trong danh sách: tên, số ticket, thời điểm lưu, JQL rút gọn và thao tác đổi tên/xoá (FR-005, FR-009, FR-010). */
@Composable
private fun SavedTableListItem(
    table: SavedTable,
    selected: Boolean,
    clock: AppClock,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val background =
        if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(background, MaterialTheme.shapes.medium)
                .clickable(onClick = onClick)
                .padding(8.dp),
    ) {
        Text(
            text = table.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = SINGLE_LINE,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${table.tickets.size} ticket • Lưu: ${clock.format(table.savedAtMillis)}",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = table.jql,
            style = MaterialTheme.typography.bodySmall,
            maxLines = SINGLE_LINE,
            overflow = TextOverflow.Ellipsis,
        )
        Row {
            TextButton(onClick = onRename) { Text("Đổi tên") }
            TextButton(onClick = onDelete) { Text("Xoá") }
        }
    }
}

@Composable
private fun SavedTableDetail(
    table: SavedTable,
    clock: AppClock,
    connected: Boolean,
    refreshing: Boolean,
    refreshRunning: Boolean,
    refreshedCount: Int,
    onRefresh: () -> Unit,
    onCancelRefresh: () -> Unit,
    onToggleColumn: (ColumnKey) -> Unit,
    sort: SortState,
    calendarOpen: Boolean,
    calendarReferenceMillis: Long?,
    chartOpen: Boolean,
    chartType: SavedTableChartType,
    chartField: SavedTableChartField,
    onOpenCalendar: () -> Unit,
    onCloseCalendar: () -> Unit,
    onOpenCharts: () -> Unit,
    onCloseCharts: () -> Unit,
    onChartTypeChange: (SavedTableChartType) -> Unit,
    onChartFieldChange: (SavedTableChartField) -> Unit,
    onSortBy: (ColumnKey) -> Unit,
    onOpenTicket: (String) -> Unit,
    rowActions: Map<String, TicketRowActionState>,
    onUpdateDueDate: (String) -> Unit,
    nowAtMillis: Long,
) {
    val sorted = remember(table.tickets, sort) { sortTickets(table.tickets, sort) }
    val calendarProjection =
        remember(sorted, table.jiraTimeZone, calendarReferenceMillis) {
            calendarReferenceMillis?.let { referenceMillis ->
                projectSavedTableCalendar(
                    tickets = sorted,
                    timeZoneId = table.jiraTimeZone,
                    nowMillis = referenceMillis,
                )
            }
        }
    val columns = ColumnKey.entries.filter { it in table.visibleColumns }

    if (chartOpen) {
        SavedTableChartsArea(
            table = table,
            chartType = chartType,
            chartField = chartField,
            clock = clock,
            onChartTypeChange = onChartTypeChange,
            onChartFieldChange = onChartFieldChange,
            onBack = onCloseCharts,
            modifier = Modifier.fillMaxSize(),
        )
    } else if (calendarOpen) {
        val projection = calendarProjection
        if (projection != null) {
            SavedTableCalendarArea(
                tableName = table.name,
                timeZoneId = table.jiraTimeZone,
                tickets = sorted,
                calendarReferenceMillis = checkNotNull(calendarReferenceMillis),
                projection = projection,
                onBack = onCloseCalendar,
                onOpenTicket = onOpenTicket,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Không xác định được thời điểm mở lịch. Hãy quay lại và mở lịch lần nữa.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = onCloseCalendar) { Text("Quay lại") }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = table.name, style = MaterialTheme.typography.titleMedium)
                Box(modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    OutlinedButton(onClick = onOpenCharts) { Text("Đồ thị") }
                    OutlinedButton(onClick = onOpenCalendar) { Text("Mở lịch") }
                    RefreshAction(
                        connected = connected,
                        refreshing = refreshing,
                        refreshRunning = refreshRunning,
                        onRefresh = onRefresh,
                        onCancelRefresh = onCancelRefresh,
                    )
                }
            }
            if (refreshing) {
                Text(
                    text = "Đang nạp … $refreshedCount ticket",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else if (!connected) {
                Text(
                    text = "Cần kết nối Jira thành công để làm mới bảng.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(text = metadata(table = table, clock = clock), style = MaterialTheme.typography.bodySmall)
            Text(
                text = table.jql,
                style = MaterialTheme.typography.bodySmall,
                maxLines = SINGLE_LINE,
                overflow = TextOverflow.Ellipsis,
            )
            TicketTable(
                state = QueryState.Success(tickets = sorted, hasMore = false, approximateTotal = null),
                columns = columns,
                availableColumns = ColumnKey.entries,
                sort = sort,
                loadingMore = false,
                onRetry = {},
                onLoadMore = {},
                onToggleColumn = onToggleColumn,
                onSortBy = onSortBy,
                onOpenTicket = { ticket -> onOpenTicket(ticket.key) },
                rowActions = rowActions,
                onRowAction = { ticket -> onUpdateDueDate(ticket.key) },
                highlightOverdueRows = true,
                nowAtMillis = nowAtMillis,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private fun metadata(
    table: SavedTable,
    clock: AppClock,
): String {
    val refreshed = table.refreshedAtMillis?.let { " • Làm mới: ${clock.format(it)}" }.orEmpty()
    return "${table.tickets.size} ticket • Lưu: ${clock.format(table.savedAtMillis)}$refreshed"
}

/**
 * Nút "Làm mới" kèm tiến trình và nút "Dừng" (FR-023, FR-025).
 *
 * Khi chưa kết nối Jira, nút bị vô hiệu kèm dòng giải thích; trong lúc làm mới, dữ liệu cũ vẫn hiển
 * thị bên dưới nên người dùng không mất ngữ cảnh.
 */
@Composable
private fun RefreshAction(
    connected: Boolean,
    refreshing: Boolean,
    refreshRunning: Boolean,
    onRefresh: () -> Unit,
    onCancelRefresh: () -> Unit,
) {
    if (refreshing) {
        OutlinedButton(onClick = onCancelRefresh) { Text("Dừng") }
    } else {
        OutlinedButton(onClick = onRefresh, enabled = connected && !refreshRunning) { Text("Làm mới") }
    }
}

/** Cảnh báo dữ liệu không đọc được kèm đường dẫn file và nút thử lại (FR-020). */
@Composable
private fun UnreadableBanner(
    warning: String,
    onRetry: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            text = warning,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = onRetry) { Text("Thử lại") }
    }
}

/** Thông báo kết quả thao tác gần nhất; dùng chung cho cả hai chế độ xem (FR-015). */
@Composable
internal fun SavedMessageBanner(
    message: String?,
    onDismiss: () -> Unit,
) {
    if (message == null) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        TextButton(onClick = onDismiss) { Text("Đóng") }
    }
}

@Composable
private fun Hint(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}
