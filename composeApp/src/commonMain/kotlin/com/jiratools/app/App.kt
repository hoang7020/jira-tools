package com.jiratools.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jiratools.domain.ColumnKey
import com.jiratools.domain.ConnectionStatus
import com.jiratools.domain.QueryState
import com.jiratools.domain.SavedTable
import com.jiratools.domain.SortState
import com.jiratools.domain.TableNameValidation
import com.jiratools.domain.Ticket
import com.jiratools.domain.errorMessage
import com.jiratools.ui.connection.ConnectionViewModel
import com.jiratools.ui.connection.connectionStatusText
import com.jiratools.ui.layout.AppDestination
import com.jiratools.ui.layout.AppNavigation
import com.jiratools.ui.query.QueryInput
import com.jiratools.ui.query.TicketTable
import com.jiratools.ui.saved.DeleteTableDialog
import com.jiratools.ui.saved.PermanentlyDeleteTrashedTableDialog
import com.jiratools.ui.saved.RenameTableDialog
import com.jiratools.ui.saved.RestoreTableDialog
import com.jiratools.ui.saved.SaveTableDialog
import com.jiratools.ui.saved.SavedMessageBanner
import com.jiratools.ui.saved.SavedTableTrashArea
import com.jiratools.ui.saved.SavedTablesArea
import com.jiratools.ui.saved.defaultTableName
import com.jiratools.ui.settings.SettingsScreen
import com.jiratools.ui.theme.AppTheme
import com.jiratools.ui.theme.AppearanceViewModel
import kotlinx.coroutines.launch

/**
 * Khung ứng dụng giữ trạng thái truy vấn, bảng đã lưu và kết nối khi người dùng chuyển màn hình.
 */
@Composable
fun App(container: AppContainer) {
    val appearanceViewModel = remember { AppearanceViewModel(configStore = container.configStore) }
    val themeMode by appearanceViewModel.themeMode.collectAsState()
    AppTheme(themeMode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val connectionViewModel = remember { ConnectionViewModel(repository = container.connectionRepository) }
            val queryViewModel = remember { container.queryViewModel }
            val savedTablesViewModel = remember { container.savedTablesViewModel }
            val status by connectionViewModel.status.collectAsState()
            val draft by connectionViewModel.draft.collectAsState()
            val hasSavedConnection by connectionViewModel.hasSavedConnection.collectAsState()
            val tokenSaved by connectionViewModel.tokenSaved.collectAsState()
            val notice by connectionViewModel.notice.collectAsState()
            val appearanceNotice by appearanceViewModel.notice.collectAsState()
            val queryState by queryViewModel.state.collectAsState()
            val jql by queryViewModel.jql.collectAsState()
            val loadingMore by queryViewModel.loadingMore.collectAsState()
            val visibleColumns by queryViewModel.visibleColumns.collectAsState()
            val sort by queryViewModel.sort.collectAsState()
            val savedState by savedTablesViewModel.state.collectAsState()
            var openError by remember { mutableStateOf<String?>(null) }
            var saveDialog by remember { mutableStateOf<SaveDialogState?>(null) }
            var renameDialog by remember { mutableStateOf<RenameDialogState?>(null) }
            var deleteDialog by remember { mutableStateOf<SavedTable?>(null) }
            var restoreDialog by remember { mutableStateOf<RestoreDialogState?>(null) }
            var permanentDeleteDialog by remember { mutableStateOf<SavedTable?>(null) }
            var destination by remember { mutableStateOf(AppDestination.QUERY) }
            var navigationCollapsed by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                appearanceViewModel.loadSaved()
                connectionViewModel.loadSaved()
                launch { connectionViewModel.verifySaved() }
                queryViewModel.loadSaved()
                savedTablesViewModel.load()
            }

            LaunchedEffect(status) {
                savedTablesViewModel.onConnectionStatusChanged(status)
            }

            Row(modifier = Modifier.fillMaxSize()) {
                AppNavigation(
                    destination = destination,
                    collapsed = navigationCollapsed,
                    onDestinationSelected = { destination = it },
                    onToggleCollapsed = { navigationCollapsed = !navigationCollapsed },
                )
                VerticalDivider()
                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                    ConnectionStatusBar(status = status)
                    HorizontalDivider()
                    when (destination) {
                        AppDestination.QUERY ->
                            QueryArea(
                                status = status,
                                jql = jql,
                                state = queryState,
                                loadingMore = loadingMore,
                                onJqlChange = { value -> queryViewModel.updateJql(value) },
                                onRun = { scope.launch { queryViewModel.run(jql = jql) } },
                                onRetry = { scope.launch { queryViewModel.retry() } },
                                onLoadMore = { scope.launch { queryViewModel.loadMore() } },
                                onToggleColumn = { column -> scope.launch { queryViewModel.toggleColumn(column) } },
                                onSortBy = { column -> scope.launch { queryViewModel.sortBy(column) } },
                                onOpenTicket = { ticket ->
                                    scope.launch { openError = container.ticketOpener.open(ticket) }
                                },
                                openError = openError,
                                onDismissOpenError = { openError = null },
                                columns = ColumnKey.queryColumns().filter { it in visibleColumns },
                                availableColumns = ColumnKey.queryColumns(),
                                sort = sort,
                                onSaveTable = { saveDialog = SaveDialogState() },
                                savedMessage = savedState.message,
                                onDismissSavedMessage = { savedTablesViewModel.clearMessage() },
                            )
                        AppDestination.SAVED_TABLES ->
                            // Bảng đã lưu xem được không cần kết nối Jira nên không phụ thuộc `status` (FR-004).
                            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                SavedTablesArea(
                                    state = savedState,
                                    clock = container.appClock,
                                    connected = status !is ConnectionStatus.Unconfigured,
                                    onRetryLoad = { scope.launch { savedTablesViewModel.retryLoad() } },
                                    onSelectTable = { id -> savedTablesViewModel.selectTable(id) },
                                    onRename = { table ->
                                        renameDialog = RenameDialogState(tableId = table.id, name = table.name)
                                    },
                                    onDelete = { table -> deleteDialog = table },
                                    onRefresh = { tableId -> scope.launch { savedTablesViewModel.refresh(tableId) } },
                                    onCancelRefresh = { savedTablesViewModel.cancelRefresh() },
                                    onToggleColumn = { tableId, column ->
                                        scope.launch { savedTablesViewModel.toggleColumn(tableId, column) }
                                    },
                                    onOpenCalendar = { savedTablesViewModel.openCalendar() },
                                    onCloseCalendar = { savedTablesViewModel.closeCalendar() },
                                    onOpenCharts = { savedTablesViewModel.openCharts() },
                                    onCloseCharts = { savedTablesViewModel.closeCharts() },
                                    onChartTypeChange = { savedTablesViewModel.selectChartType(it) },
                                    onChartFieldChange = { savedTablesViewModel.selectChartField(it) },
                                    onSortSavedTableBy = { column -> savedTablesViewModel.sortSavedTableBy(column) },
                                    onOpenTicket = { tableId, ticketKey ->
                                        scope.launch { openError = savedTablesViewModel.openTicket(tableId, ticketKey) }
                                    },
                                    onUpdateDueDate = { tableId, ticketKey ->
                                        scope.launch { savedTablesViewModel.updateDueDate(tableId, ticketKey) }
                                    },
                                    onDismissMessage = { savedTablesViewModel.clearMessage() },
                                )
                                OpenErrorBanner(message = openError, onDismiss = { openError = null })
                            }
                        AppDestination.TRASH ->
                            SavedTableTrashArea(
                                state = savedState,
                                clock = container.appClock,
                                onRetryLoad = { scope.launch { savedTablesViewModel.retryLoad() } },
                                onRestore = { trashed ->
                                    scope.launch {
                                        val validation = savedTablesViewModel.restoreTable(trashed.table.id)
                                        if (validation == TableNameValidation.Duplicate) {
                                            restoreDialog =
                                                RestoreDialogState(
                                                    tableId = trashed.table.id,
                                                    name = trashed.table.name,
                                                    error = validation.errorMessage(),
                                                )
                                        }
                                    }
                                },
                                onPermanentlyDelete = { trashed -> permanentDeleteDialog = trashed.table },
                                onDismissMessage = { savedTablesViewModel.clearMessage() },
                            )
                        AppDestination.SETTINGS ->
                            SettingsScreen(
                                status = status,
                                draft = draft,
                                hasSavedConnection = hasSavedConnection,
                                tokenSaved = tokenSaved,
                                notice = notice,
                                onDraftChange = connectionViewModel::updateDraft,
                                onValidate = { baseUrl, email, token ->
                                    scope.launch {
                                        connectionViewModel.validate(baseUrl = baseUrl, email = email, token = token)
                                    }
                                },
                                onDisconnect = { scope.launch { connectionViewModel.disconnect() } },
                                onDismissNotice = connectionViewModel::clearNotice,
                                themeMode = themeMode,
                                appearanceNotice = appearanceNotice,
                                onThemeModeChange = { mode -> scope.launch { appearanceViewModel.setThemeMode(mode) } },
                                onDismissAppearanceNotice = appearanceViewModel::clearNotice,
                                modifier = Modifier.fillMaxSize(),
                            )
                    }
                }
            }

            val dialog = saveDialog
            if (dialog != null) {
                val connected = status as? ConnectionStatus.Connected
                SaveTableDialog(
                    defaultName = defaultTableName(container.appClock),
                    error = dialog.error,
                    onConfirm = { name ->
                        scope.launch {
                            val validation =
                                savedTablesViewModel.saveTable(
                                    name = name,
                                    jql = jql,
                                    tickets = (queryState as? QueryState.Success)?.tickets.orEmpty(),
                                    visibleColumns = visibleColumns,
                                    jiraBaseUrl = connected?.baseUrl.orEmpty(),
                                )
                            saveDialog =
                                if (validation == TableNameValidation.Valid) {
                                    null
                                } else {
                                    dialog.copy(error = validation.errorMessage())
                                }
                        }
                    },
                    onDismiss = { saveDialog = null },
                )
            }

            val rename = renameDialog
            if (rename != null) {
                RenameTableDialog(
                    currentName = rename.name,
                    error = rename.error,
                    onConfirm = { newName ->
                        scope.launch {
                            val validation =
                                savedTablesViewModel.renameTable(tableId = rename.tableId, newName = newName)
                            renameDialog =
                                if (validation == TableNameValidation.Valid) {
                                    null
                                } else {
                                    rename.copy(error = validation.errorMessage())
                                }
                        }
                    },
                    onDismiss = { renameDialog = null },
                )
            }

            val deleting = deleteDialog
            if (deleting != null) {
                DeleteTableDialog(
                    tableName = deleting.name,
                    onConfirm = {
                        scope.launch { savedTablesViewModel.moveTableToTrash(deleting.id) }
                        deleteDialog = null
                    },
                    onDismiss = { deleteDialog = null },
                )
            }

            val restoring = restoreDialog
            if (restoring != null) {
                RestoreTableDialog(
                    currentName = restoring.name,
                    error = restoring.error,
                    onConfirm = { name ->
                        scope.launch {
                            val validation = savedTablesViewModel.restoreTable(restoring.tableId, name)
                            restoreDialog =
                                if (validation == TableNameValidation.Valid) {
                                    null
                                } else {
                                    restoring.copy(error = validation.errorMessage())
                                }
                        }
                    },
                    onDismiss = { restoreDialog = null },
                )
            }

            val permanentlyDeleting = permanentDeleteDialog
            if (permanentlyDeleting != null) {
                PermanentlyDeleteTrashedTableDialog(
                    tableName = permanentlyDeleting.name,
                    onConfirm = {
                        scope.launch { savedTablesViewModel.permanentlyDeleteTrashedTable(permanentlyDeleting.id) }
                        permanentDeleteDialog = null
                    },
                    onDismiss = { permanentDeleteDialog = null },
                )
            }
        }
    }
}

/** Trạng thái cục bộ của hộp thoại đổi tên: bảng đang đổi, tên hiện tại và lỗi gần nhất (FR-009, FR-011). */
private data class RenameDialogState(
    val tableId: String,
    val name: String,
    val error: String? = null,
)

private data class RestoreDialogState(
    val tableId: String,
    val name: String,
    val error: String? = null,
)

/** Trạng thái cục bộ của hộp thoại lưu bảng: đang mở và lỗi tên bảng gần nhất (FR-001, FR-011). */
private data class SaveDialogState(
    val error: String? = null,
)

/** Dòng trạng thái kết nối luôn hiển thị ở phía trên nội dung chính. */
@Composable
private fun ConnectionStatusBar(status: ConnectionStatus) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = connectionStatusText(status),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QueryArea(
    status: ConnectionStatus,
    jql: String,
    state: QueryState,
    loadingMore: Boolean,
    onJqlChange: (String) -> Unit,
    onRun: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onToggleColumn: (ColumnKey) -> Unit,
    onSortBy: (ColumnKey) -> Unit,
    onOpenTicket: (Ticket) -> Unit,
    openError: String?,
    onDismissOpenError: () -> Unit,
    columns: List<ColumnKey>,
    availableColumns: List<ColumnKey>,
    sort: SortState,
    onSaveTable: () -> Unit,
    savedMessage: String?,
    onDismissSavedMessage: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SaveTableHeader(
            state = state,
            status = status,
            savedMessage = savedMessage,
            onSaveTable = onSaveTable,
            onDismissSavedMessage = onDismissSavedMessage,
        )
        if (status !is ConnectionStatus.Unconfigured) {
            QueryInput(
                jql = jql,
                onJqlChange = onJqlChange,
                onRun = onRun,
                running = state is QueryState.Loading,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            HorizontalDivider()
            TicketTable(
                state = state,
                columns = columns,
                availableColumns = availableColumns,
                sort = sort,
                loadingMore = loadingMore,
                onRetry = onRetry,
                onLoadMore = onLoadMore,
                onToggleColumn = onToggleColumn,
                onSortBy = onSortBy,
                onOpenTicket = onOpenTicket,
                modifier = Modifier.padding(top = 8.dp),
            )
            OpenErrorBanner(message = openError, onDismiss = onDismissOpenError)
        } else {
            Text(
                text = "Vui lòng mở Cài đặt để cấu hình kết nối Jira.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/**
 * Hàng tiêu đề "Kết quả truy vấn" kèm nút lưu bảng và thông báo thao tác lưu (FR-012, FR-015).
 *
 * Nút chỉ bật khi đã có kết quả truy vấn để lưu; khi bị vô hiệu luôn kèm một dòng giải thích ngắn.
 */
@Composable
private fun SaveTableHeader(
    state: QueryState,
    status: ConnectionStatus,
    savedMessage: String?,
    onSaveTable: () -> Unit,
    onDismissSavedMessage: () -> Unit,
) {
    val connected = status is ConnectionStatus.Connected
    val hint = saveHint(state = state, connected = connected)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Kết quả truy vấn", style = MaterialTheme.typography.titleMedium)
            Box(modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onSaveTable, enabled = hint == null) { Text("Lưu bảng") }
        }
        if (hint != null && connected) {
            // Khi chưa kết nối, khu vực bên dưới đã có dòng giải thích riêng nên không nhắc lại.
            Text(text = hint, style = MaterialTheme.typography.bodySmall)
        }
        SavedMessageBanner(message = savedMessage, onDismiss = onDismissSavedMessage)
    }
}

/** Lý do nút "Lưu bảng" chưa dùng được; `null` khi đã có kết quả để lưu (FR-012). */
private fun saveHint(
    state: QueryState,
    connected: Boolean,
): String? =
    when {
        !connected -> NOT_CONNECTED_HINT
        state is QueryState.Success && state.tickets.isNotEmpty() -> null
        state is QueryState.Loading -> "Đang chạy truy vấn, chờ kết quả để lưu thành bảng."
        state is QueryState.Empty -> "Truy vấn không có ticket nào để lưu."
        state is QueryState.Error -> "Truy vấn đang lỗi nên chưa lưu được bảng."
        else -> "Chạy truy vấn rồi lưu kết quả thành một bảng riêng."
    }

private const val NOT_CONNECTED_HINT = "Cần kết nối Jira thành công để lưu bảng."

/** Thông báo lỗi mở ticket: không chặn luồng, người dùng vẫn giữ nguyên bảng kết quả (FR-013). */
@Composable
private fun OpenErrorBanner(
    message: String?,
    onDismiss: () -> Unit,
) {
    if (message == null) return
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = onDismiss) { Text("Đóng") }
    }
}
