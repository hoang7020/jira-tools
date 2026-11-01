package com.jiratools.ui.query

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.clipScrollableContainer
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiratools.domain.ColumnKey
import com.jiratools.domain.QueryState
import com.jiratools.domain.SortState
import com.jiratools.domain.Ticket
import com.jiratools.domain.queryTreeBranch
import com.jiratools.domain.userMessage
import com.jiratools.platform.TableHorizontalScrollbar
import com.jiratools.platform.TableVerticalScrollbar

private const val CELL_MAX_LINES = 1
private const val SORT_ASCENDING = " \u25b2"
private const val SORT_DESCENDING = " \u25bc"

data class TicketRowActionState(
    val targetDate: String,
    val updating: Boolean = false,
    val error: String? = null,
)

/**
 * Bảng ticket hiển thị theo danh sách cột đang bật.
 *
 * Mọi trạng thái của [QueryState] đều có phản hồi trực quan để người dùng biết chuyện gì đang
 * xảy ra (FR-009, FR-011).
 */
@Composable
fun TicketTable(
    state: QueryState,
    columns: List<ColumnKey>,
    availableColumns: List<ColumnKey>,
    sort: SortState,
    loadingMore: Boolean,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onToggleColumn: (ColumnKey) -> Unit,
    onSortBy: (ColumnKey) -> Unit,
    onOpenTicket: (Ticket) -> Unit,
    rowActions: Map<String, TicketRowActionState> = emptyMap(),
    onRowAction: ((Ticket) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        val rowsState = remember { LazyListState() }
        Box(modifier = Modifier.weight(1f)) {
            when (state) {
                QueryState.Idle -> Hint("Nhập truy vấn JQL để bắt đầu.")
                QueryState.Loading -> LoadingView()
                QueryState.Empty -> Hint("Không có ticket nào khớp truy vấn.")
                is QueryState.Error -> QueryErrorView(state = state, onRetry = onRetry)
                is QueryState.Success ->
                    SuccessTable(
                        state = state,
                        columns = columns,
                        availableColumns = availableColumns,
                        sort = sort,
                        rowsState = rowsState,
                        onToggleColumn = onToggleColumn,
                        onSortBy = onSortBy,
                        onOpenTicket = onOpenTicket,
                        rowActions = rowActions,
                        onRowAction = onRowAction,
                    )
            }
        }
        if (state is QueryState.Success) {
            Footer(
                state = state,
                loadingMore = loadingMore,
                onLoadMore = onLoadMore,
            )
        }
    }
}

@Composable
private fun SuccessTable(
    state: QueryState.Success,
    columns: List<ColumnKey>,
    availableColumns: List<ColumnKey>,
    sort: SortState,
    rowsState: LazyListState,
    onToggleColumn: (ColumnKey) -> Unit,
    onSortBy: (ColumnKey) -> Unit,
    onOpenTicket: (Ticket) -> Unit,
    rowActions: Map<String, TicketRowActionState>,
    onRowAction: ((Ticket) -> Unit)?,
) {
    // Vào composition ở mỗi truy vấn mới (khi đó trạng thái trước là Loading) nên danh sách bắt đầu
    // từ đầu; "Tải thêm" giữ nguyên vị trí cuộn vì bảng không rời composition (FR-014).
    LaunchedEffect(Unit) { rowsState.scrollToItem(0) }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val layout = computeTableColumnLayout(visibleColumns = columns, availableWidth = maxWidth)

        // Hàng tiêu đề và mọi hàng dữ liệu chia sẻ cùng một trạng thái cuộn ngang nên luôn thẳng hàng;
        // cột Mã nằm ngoài vùng cuộn nên luôn đứng yên (FR-009, FR-011).
        val horizontalScroll = rememberScrollState()

        Column(modifier = Modifier.fillMaxWidth()) {
            TableToolbar(
                approximateTotal = state.approximateTotal,
                availableColumns = availableColumns,
                visibleColumns = columns,
                onToggleColumn = onToggleColumn,
            )
            HeaderRow(
                columns = columns,
                sort = sort,
                layout = layout,
                horizontalScroll = horizontalScroll,
                onSortBy = onSortBy,
            )
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(
                    state = rowsState,
                    modifier = Modifier.fillMaxHeight().weight(1f),
                ) {
                    itemsIndexed(state.tickets, key = { _, ticket -> ticket.key }) { _, ticket ->
                        TicketRow(
                            ticket = ticket,
                            treeBranch = queryTreeBranch(state.tickets, ticket),
                            columns = columns,
                            layout = layout,
                            horizontalScroll = horizontalScroll,
                            onOpen = { onOpenTicket(ticket) },
                            actionState = rowActions[ticket.key],
                            onRowAction = onRowAction?.let { action -> { action(ticket) } },
                            reserveActionSpace = onRowAction != null && ColumnKey.DUE_DATE in columns,
                        )
                        HorizontalDivider()
                    }
                }
                // Làn dành riêng cho thanh cuộn dọc: giữ bề rộng ổn định cho bảng, chỉ vẽ thanh cuộn khi
                // danh sách thực sự vượt khung nhìn (US2 #6).
                Box(modifier = Modifier.fillMaxHeight().width(TABLE_SCROLLBAR_LANE_WIDTH)) {
                    if (rowsState.canScrollBackward || rowsState.canScrollForward) {
                        TableVerticalScrollbar(state = rowsState, modifier = Modifier.fillMaxHeight())
                    }
                }
            }
            if (layout.needsHorizontalScroll) {
                // Thanh cuộn ngang trải đúng vùng cột trượt (bỏ qua cột Mã và làn thanh cuộn dọc) — FR-008.
                TableHorizontalScrollbar(
                    state = horizontalScroll,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = layout.keyWidth, end = TABLE_SCROLLBAR_LANE_WIDTH),
                )
            }
        }
    }
}

private fun approximateLabel(total: Int?): String = total?.let { "Khoảng $it kết quả" } ?: "Kết quả truy vấn"

@Composable
private fun TableToolbar(
    approximateTotal: Int?,
    availableColumns: List<ColumnKey>,
    visibleColumns: List<ColumnKey>,
    onToggleColumn: (ColumnKey) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = approximateLabel(approximateTotal), style = MaterialTheme.typography.bodySmall)
        Box(modifier = Modifier.weight(1f))
        ColumnChooser(
            availableColumns = availableColumns,
            visibleColumns = visibleColumns,
            onToggleColumn = onToggleColumn,
        )
    }
}

/** Menu bật/tắt cột; cột `Mã` luôn hiển thị nên bị khoá (FR-011). */
@Composable
private fun ColumnChooser(
    availableColumns: List<ColumnKey>,
    visibleColumns: List<ColumnKey>,
    onToggleColumn: (ColumnKey) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) { Text("Cột hiển thị") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            availableColumns.forEach { column ->
                val locked = column == ColumnKey.KEY
                DropdownMenuItem(
                    text = { Text(column.label) },
                    onClick = {
                        onToggleColumn(column)
                        expanded = false
                    },
                    enabled = !locked,
                    leadingIcon = {
                        Checkbox(
                            checked = column in visibleColumns,
                            onCheckedChange = null,
                            enabled = !locked,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun HeaderRow(
    columns: List<ColumnKey>,
    sort: SortState,
    layout: TableColumnLayout,
    horizontalScroll: ScrollState,
    onSortBy: (ColumnKey) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (ColumnKey.KEY in columns) {
            HeaderCell(
                column = ColumnKey.KEY,
                width = layout.keyWidth,
                ascending = if (sort.column == ColumnKey.KEY) sort.ascending else null,
                onSort = { onSortBy(ColumnKey.KEY) },
            )
        }
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .clipScrollableContainer(Orientation.Horizontal),
        ) {
            Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                columns.filter { it != ColumnKey.KEY }.forEach { column ->
                    HeaderCell(
                        column = column,
                        width = layout.widthOf(column),
                        ascending = if (sort.column == column) sort.ascending else null,
                        onSort = { onSortBy(column) },
                    )
                }
            }
        }
    }
}

/** Tiêu đề cột bấm được để sắp xếp; mũi tên cho biết chiều đang áp dụng (FR-012). */
@Composable
private fun HeaderCell(
    column: ColumnKey,
    width: Dp,
    ascending: Boolean?,
    onSort: () -> Unit,
) {
    val indicator =
        when (ascending) {
            null -> ""
            true -> SORT_ASCENDING
            false -> SORT_DESCENDING
        }
    Text(
        text = column.label + indicator,
        modifier =
            Modifier
                .width(width)
                .clickable(onClick = onSort)
                .padding(horizontal = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (ascending == null) FontWeight.SemiBold else FontWeight.Bold,
        maxLines = CELL_MAX_LINES,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun LoadingView() {
    Centered {
        CircularProgressIndicator()
        Text(
            text = "Đang chạy truy vấn...",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Hàng ticket bấm được để mở ticket trong Jira; con trỏ đổi thành bàn tay để gợi ý (FR-013). */
@Composable
private fun TicketRow(
    ticket: Ticket,
    treeBranch: com.jiratools.domain.QueryTreeBranch,
    columns: List<ColumnKey>,
    layout: TableColumnLayout,
    horizontalScroll: ScrollState,
    onOpen: () -> Unit,
    actionState: TicketRowActionState?,
    onRowAction: (() -> Unit)?,
    reserveActionSpace: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(if (ticket.isParent) Color.Gray.copy(alpha = 0.16f) else Color.Transparent)
                .then(if (reserveActionSpace) Modifier.height(48.dp) else Modifier)
                .clickable(onClick = onOpen)
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (ColumnKey.KEY in columns) {
            Text(
                text = listOfNotNull(treeBranch.marker, ticket.value(ColumnKey.KEY)).joinToString(" "),
                modifier = Modifier.width(layout.keyWidth).padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                maxLines = CELL_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .clipScrollableContainer(Orientation.Horizontal),
        ) {
            Row(
                modifier = Modifier.fillMaxHeight().horizontalScroll(horizontalScroll),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                columns.filter { it != ColumnKey.KEY }.forEach { column ->
                    val actionEnabled = column == ColumnKey.DUE_DATE && onRowAction != null
                    if (actionEnabled) {
                        Box(
                            modifier =
                                Modifier
                                    .width(layout.widthOf(column))
                                    .fillMaxHeight()
                                    .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                text = ticket.value(column),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = CELL_MAX_LINES,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Box(
                                modifier = Modifier.align(Alignment.BottomStart),
                            ) {
                                when {
                                    actionState?.updating == true ->
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    actionState != null ->
                                        Text(
                                            text = "${actionState.targetDate} · Cập nhật",
                                            modifier =
                                                Modifier
                                                    .clickable(role = Role.Button, onClick = checkNotNull(onRowAction))
                                                    .pointerHoverIcon(PointerIcon.Hand)
                                                    .semantics {
                                                        contentDescription =
                                                            "Cập nhật Due Date thành ${actionState.targetDate}"
                                                    },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 9.sp,
                                            maxLines = CELL_MAX_LINES,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    else -> Box(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier =
                                Modifier
                                    .width(layout.widthOf(column))
                                    .fillMaxHeight()
                                    .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                text = ticket.value(column),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = CELL_MAX_LINES,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Ticket.value(column: ColumnKey): String =
    when (column) {
        ColumnKey.KEY -> key
        ColumnKey.SUMMARY -> summary
        ColumnKey.STATUS -> status
        ColumnKey.ASSIGNEE -> assignee.orEmpty()
        ColumnKey.PRIORITY -> priority.orEmpty()
        ColumnKey.UPDATED -> updated
        ColumnKey.DUE_DATE -> dueDate.orEmpty()
        ColumnKey.ORIGINAL_EXPECTED_TIME -> originalExpectedTime.orEmpty()
        ColumnKey.DYNAMIC_START_TIME -> dynamicStartTime.orEmpty()
        ColumnKey.DYNAMIC_DUE_DATE -> dynamicDueDate.orEmpty()
        ColumnKey.REAL_DUE_DATE -> realDueDate.orEmpty()
    }

@Composable
private fun QueryErrorView(
    state: QueryState.Error,
    onRetry: () -> Unit,
) {
    Centered {
        Text(
            text = state.error.userMessage(),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry) { Text("Thử lại") }
    }
}

@Composable
private fun Footer(
    state: QueryState.Success,
    loadingMore: Boolean,
    onLoadMore: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loadingMore) {
            CircularProgressIndicator()
        } else if (state.hasMore) {
            OutlinedButton(onClick = onLoadMore) { Text("Tải thêm") }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Centered {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}
