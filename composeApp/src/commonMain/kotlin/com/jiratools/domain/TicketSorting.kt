package com.jiratools.domain

import kotlinx.datetime.LocalDate

/**
 * Sắp xếp ticket của bảng đã lưu ngay trong bộ nhớ (FR-007).
 *
 * Dữ liệu đã lưu là một ảnh chụp nên thứ tự do ứng dụng quyết định, không phát sinh truy vấn tới
 * Jira. So sánh theo chuỗi không phân biệt hoa/thường; `assignee`/`priority` null được coi là chuỗi
 * rỗng nhờ đó ticket chưa gán người xử lý luôn đứng đầu khi sắp tăng dần. Hàm thuần: danh sách đầu
 * vào không bị đổi thứ tự, và thứ tự đã lưu được giữ nguyên giữa các phần tử bằng nhau.
 */
fun sortTickets(
    tickets: List<Ticket>,
    sort: SortState,
): List<Ticket> {
    val column = sort.column ?: return tickets
    val byColumn =
        when (column) {
            ColumnKey.ORIGINAL_EXPECTED_TIME ->
                compareBy<Ticket> { WorkDuration.parse(it.originalExpectedTime.orEmpty()) }
            ColumnKey.DYNAMIC_START_TIME -> compareBy { it.dynamicStartAtMillis }
            ColumnKey.DYNAMIC_DUE_DATE -> compareBy { it.dynamicDueAtMillis }
            ColumnKey.REAL_DUE_DATE -> compareBy { it.realDueAtMillis }
            ColumnKey.DUE_DATE -> compareBy { it.dueDate?.let(::parseCalendarDate) }
            else -> compareBy<Ticket> { it.sortValue(column).lowercase() }
        }
    val grouped = groupQueryResultTickets(tickets)
    val groups = ticketGroups(grouped)
    val comparator = if (sort.ascending) byColumn else byColumn.reversed()
    return groups.sortedWith(compareBy(comparator) { it.first() }).flatten()
}

private fun ticketGroups(tickets: List<Ticket>): List<List<Ticket>> {
    val groups = mutableListOf<List<Ticket>>()
    var index = 0
    while (index < tickets.size) {
        val ticket = tickets[index]
        if (ticket.isParent) {
            val children = tickets.drop(index + 1).takeWhile { it.parentKey == ticket.key && it.isSubtask }
            groups += listOf(ticket) + children
            index += children.size + 1
        } else if (!ticket.isSubtask ||
            ticket.parentKey == null ||
            tickets.none { it.key == ticket.parentKey && it.isParent }
        ) {
            groups += listOf(ticket)
            index++
        } else {
            index++
        }
    }
    return groups
}

/**
 * Chiều sắp xếp mới khi người dùng bấm vào tiêu đề cột: bấm lại cùng cột thì đảo chiều, cột mới thì
 * bắt đầu từ tăng dần (FR-007).
 */
fun SortState.toggledBy(column: ColumnKey): SortState =
    if (this.column == column) {
        copy(ascending = !ascending)
    } else {
        SortState(column = column, ascending = true)
    }

/** Giá trị dùng để so sánh của một ticket theo cột đang sắp xếp (cùng nguồn với cột hiển thị). */
private fun Ticket.sortValue(column: ColumnKey): String =
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

private fun parseCalendarDate(value: String): LocalDate? =
    try {
        LocalDate.parse(value)
    } catch (_: IllegalArgumentException) {
        null
    }
