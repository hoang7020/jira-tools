package com.jiratools.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Sắp xếp dữ liệu bảng đã lưu trong bộ nhớ (FR-007).
 *
 * Quy tắc: so sánh chuỗi không phân biệt hoa/thường, `assignee`/`priority` null coi như chuỗi rỗng,
 * sắp xếp ổn định, và không phát sinh truy vấn tới Jira.
 */
class TicketSortingTest {
    @Test
    fun sortingKeepsParentAndSubtasksTogether() {
        val parentA =
            Ticket(
                key = "A-1",
                summary = "Alpha",
                status = "Open",
                isParent = true,
            )
        val childA =
            Ticket(
                key = "A-2",
                summary = "Zulu",
                status = "Open",
                parentKey = "A-1",
                isSubtask = true,
            )
        val independent = Ticket(key = "B-1", summary = "Beta", status = "Open")

        val sorted =
            sortTickets(
                listOf(childA, independent, parentA),
                SortState(ColumnKey.SUMMARY, ascending = true),
            )

        assertEquals(listOf("A-1", "A-2", "B-1"), sorted.map { it.key })
    }

    @Test
    fun columnNullKeepsTheStoredOrder() {
        val tickets = sample()

        val sorted = sortTickets(tickets, SortState(column = null))

        assertEquals(tickets, sorted)
    }

    @Test
    fun sortsByKeyInBothDirections() {
        assertEquals(KEYS_ASCENDING, keysOf(ColumnKey.KEY, ascending = true))
        assertEquals(KEYS_ASCENDING.reversed(), keysOf(ColumnKey.KEY, ascending = false))
    }

    @Test
    fun sortsBySummaryInBothDirectionsIgnoringCase() {
        assertEquals(listOf("HSP-1", "HSP-2", "HSP-3"), keysOf(ColumnKey.SUMMARY, ascending = true))
        assertEquals(listOf("HSP-3", "HSP-2", "HSP-1"), keysOf(ColumnKey.SUMMARY, ascending = false))
    }

    @Test
    fun sortsByStatusInBothDirections() {
        assertEquals(listOf("HSP-2", "HSP-1", "HSP-3"), keysOf(ColumnKey.STATUS, ascending = true))
        assertEquals(listOf("HSP-3", "HSP-1", "HSP-2"), keysOf(ColumnKey.STATUS, ascending = false))
    }

    @Test
    fun sortsByAssigneeTreatingNullAsEmpty() {
        assertEquals(listOf("HSP-2", "HSP-3", "HSP-1"), keysOf(ColumnKey.ASSIGNEE, ascending = true))
        assertEquals(listOf("HSP-1", "HSP-3", "HSP-2"), keysOf(ColumnKey.ASSIGNEE, ascending = false))
    }

    @Test
    fun sortsByPriorityTreatingNullAsEmpty() {
        assertEquals(listOf("HSP-2", "HSP-1", "HSP-3"), keysOf(ColumnKey.PRIORITY, ascending = true))
        assertEquals(listOf("HSP-3", "HSP-1", "HSP-2"), keysOf(ColumnKey.PRIORITY, ascending = false))
    }

    @Test
    fun sortsByUpdatedInBothDirections() {
        assertEquals(listOf("HSP-1", "HSP-2", "HSP-3"), keysOf(ColumnKey.UPDATED, ascending = true))
        assertEquals(listOf("HSP-3", "HSP-2", "HSP-1"), keysOf(ColumnKey.UPDATED, ascending = false))
    }

    @Test
    fun keepsStoredOrderWhenValuesAreEqual() {
        val first = ticket(key = "HSP-9", summary = "Báo cáo tuần")
        val second = ticket(key = "HSP-8", summary = "báo cáo TUẦN")

        val sorted = sortTickets(listOf(first, second), SortState(ColumnKey.SUMMARY, ascending = true))

        assertEquals(listOf("HSP-9", "HSP-8"), sorted.map { it.key }, "Sắp xếp phải ổn định")
    }

    @Test
    fun togglingASortColumnFlipsDirectionAndStartsNewColumnsAscending() {
        assertEquals(SortState(ColumnKey.STATUS, ascending = true), SortState().toggledBy(ColumnKey.STATUS))
        assertEquals(
            SortState(ColumnKey.STATUS, ascending = false),
            SortState(ColumnKey.STATUS, ascending = true).toggledBy(ColumnKey.STATUS),
        )
        assertEquals(
            SortState(ColumnKey.SUMMARY, ascending = true),
            SortState(ColumnKey.STATUS, ascending = false).toggledBy(ColumnKey.SUMMARY),
        )
    }

    @Test
    fun doesNotChangeTheStoredListItself() {
        val tickets = sample()
        val before = tickets.map { it.key }

        sortTickets(tickets, SortState(ColumnKey.SUMMARY, ascending = false))

        assertEquals(before, tickets.map { it.key }, "Danh sách đã lưu không được đổi thứ tự")
    }

    @Test
    fun sortsOriginalExpectedTimeByParsedDurationAndKeepsMissingValuesFirstAscending() {
        val column = ColumnKey.valueOf("ORIGINAL_EXPECTED_TIME")
        val tickets =
            listOf(
                ticket("HSP-1").copy(originalExpectedTime = "1h"),
                ticket("HSP-2").copy(originalExpectedTime = "15m"),
                ticket("HSP-3").copy(originalExpectedTime = "unrecognized"),
                ticket("HSP-4"),
            )

        assertEquals(
            listOf("HSP-3", "HSP-4", "HSP-2", "HSP-1"),
            sortTickets(tickets, SortState(column, ascending = true)).map { it.key },
        )
    }

    @Test
    fun sortsDynamicTimesByInstantRatherThanFormattedText() {
        val startColumn = ColumnKey.valueOf("DYNAMIC_START_TIME")
        val tickets =
            listOf(
                ticket("HSP-1").copy(dynamicStartTime = "2026-10-30 08:00", dynamicStartAtMillis = 2L),
                ticket("HSP-2"),
                ticket("HSP-3").copy(dynamicStartTime = "2026-10-29 08:00", dynamicStartAtMillis = 1L),
                ticket("HSP-4").copy(dynamicStartTime = "display-only"),
            )

        assertEquals(
            listOf("HSP-2", "HSP-4", "HSP-3", "HSP-1"),
            sortTickets(tickets, SortState(startColumn, ascending = true)).map { it.key },
        )
    }

    @Test
    fun sortsDynamicDueDatesByInstantAndKeepsMissingValuesFirstAscending() {
        val dueDateColumn = ColumnKey.valueOf("DYNAMIC_DUE_DATE")
        val tickets =
            listOf(
                ticket("HSP-1").copy(dynamicDueDate = "2026-10-29 08:00", dynamicDueAtMillis = 2L),
                ticket("HSP-2").copy(dynamicDueDate = "2026-10-30 08:00", dynamicDueAtMillis = 1L),
                ticket("HSP-3"),
                ticket("HSP-4").copy(dynamicDueDate = "invalid"),
            )

        assertEquals(
            listOf("HSP-3", "HSP-4", "HSP-2", "HSP-1"),
            sortTickets(tickets, SortState(dueDateColumn, ascending = true)).map { it.key },
        )
    }

    @Test
    fun sortsDueDatesAsCalendarDatesAndKeepsMissingValuesFirst() {
        val dueDateColumn = ColumnKey.valueOf("DUE_DATE")
        val tickets =
            listOf(
                ticket("HSP-1").copy(dueDate = "2026-11-10"),
                ticket("HSP-2").copy(dueDate = "2026-02-05"),
                ticket("HSP-3"),
                ticket("HSP-4").copy(dueDate = "invalid"),
            )

        assertEquals(
            listOf("HSP-3", "HSP-4", "HSP-2", "HSP-1"),
            sortTickets(tickets, SortState(dueDateColumn, ascending = true)).map { it.key },
        )
        assertEquals(
            listOf("HSP-1", "HSP-2", "HSP-3", "HSP-4"),
            sortTickets(tickets, SortState(dueDateColumn, ascending = false)).map { it.key },
        )
    }

    private fun keysOf(
        column: ColumnKey,
        ascending: Boolean,
    ): List<String> = sortTickets(sample(), SortState(column = column, ascending = ascending)).map { it.key }

    private fun sample(): List<Ticket> =
        listOf(
            ticket(
                key = "HSP-2",
                summary = "beta",
                status = "Done",
                assignee = null,
                priority = null,
                updated = "2026-10-30T10:00:00.000+0700",
            ),
            ticket(
                key = "HSP-1",
                summary = "Alpha",
                status = "In Progress",
                assignee = "Bình",
                priority = "High",
                updated = "2026-10-29T10:00:00.000+0700",
            ),
            ticket(
                key = "HSP-3",
                summary = "gamma",
                status = "To Do",
                assignee = "an",
                priority = "Low",
                updated = "2026-10-31T10:00:00.000+0700",
            ),
        )

    private fun ticket(
        key: String,
        summary: String = "việc",
        status: String = "To Do",
        assignee: String? = null,
        priority: String? = null,
        updated: String = "2026-10-30T10:00:00.000+0700",
    ): Ticket =
        Ticket(
            key = key,
            summary = summary,
            status = status,
            assignee = assignee,
            priority = priority,
            updated = updated,
        )

    private companion object {
        val KEYS_ASCENDING = listOf("HSP-1", "HSP-2", "HSP-3")
    }
}
