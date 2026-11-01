package com.jiratools.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class SavedTableChartCalculatorTest {
    @Test
    fun groupsCategoryValuesAndKeepsMissingValuesVisible() {
        val tickets =
            listOf(
                ticket(status = "Open", assignee = "Lan", priority = "High"),
                ticket(status = "Open", assignee = "Lan", priority = "High"),
                ticket(status = "Done", assignee = null, priority = ""),
                ticket(status = "", assignee = "Minh", priority = "Low"),
            )

        assertEquals(
            listOf(CategoryCount("Done", 1), CategoryCount("Open", 2), CategoryCount("Chưa xác định", 1)),
            SavedTableChartCalculator.categoryCounts(tickets, SavedTableChartField.STATUS).counts,
        )
        assertEquals(
            listOf(CategoryCount("Lan", 2), CategoryCount("Minh", 1), CategoryCount("Chưa xác định", 1)),
            SavedTableChartCalculator.categoryCounts(tickets, SavedTableChartField.ASSIGNEE).counts,
        )
        assertEquals(
            listOf(CategoryCount("High", 2), CategoryCount("Low", 1), CategoryCount("Chưa xác định", 1)),
            SavedTableChartCalculator.categoryCounts(tickets, SavedTableChartField.PRIORITY).counts,
        )
        assertEquals(4, SavedTableChartCalculator.categoryCounts(tickets, SavedTableChartField.STATUS).totalCount)
    }

    @Test
    fun groupsUpdatedTicketsByJiraLocalDateAndReportsMissingDates() {
        val tickets =
            listOf(
                ticket(updatedAtMillis = Instant.parse("2026-12-01T10:00:00Z").toEpochMilliseconds()),
                ticket(updatedAtMillis = Instant.parse("2026-12-01T12:00:00Z").toEpochMilliseconds()),
                ticket(updatedAtMillis = Instant.parse("2026-12-03T08:00:00Z").toEpochMilliseconds()),
                ticket(updatedAtMillis = null),
            )

        val projection = SavedTableChartCalculator.updatedTicketsByDay(tickets, "UTC")

        assertEquals(listOf(2, 1), projection.points.map(UpdatedTicketsByDayPoint::ticketCount))
        assertEquals(listOf("2026-12-01", "2026-12-03"), projection.points.map { it.date.toString() })
        assertEquals(1, projection.missingUpdatedCount)
        assertEquals(false, projection.usedUtcFallback)
    }

    @Test
    fun invalidTimezoneUsesUtcAndIsReported() {
        val projection =
            SavedTableChartCalculator.updatedTicketsByDay(
                listOf(ticket(updatedAtMillis = 1_798_900_200_000L)),
                "Not/A-Timezone",
            )

        assertEquals(1, projection.points.size)
        assertTrue(projection.usedUtcFallback)
    }

    @Test
    fun burndownUsesSnapshotCountsInRecordedOrderAndRetainsUnknownCounts() {
        val snapshots =
            listOf(
                BurndownSnapshot(20, 3, 2, 1),
                BurndownSnapshot(10, 4, 3, 0),
            )

        val projection = SavedTableChartCalculator.burndown(snapshots)

        assertEquals(listOf(20L, 10L), projection.points.map(BurndownPoint::recordedAtMillis))
        assertEquals(listOf(2, 3), projection.points.map(BurndownPoint::remainingCount))
        assertEquals(listOf(1, 0), projection.points.map(BurndownPoint::unknownCategoryCount))
    }

    private fun ticket(
        status: String = "Open",
        assignee: String? = "Lan",
        priority: String? = "High",
        updatedAtMillis: Long? = null,
    ) = Ticket(
        key = "HSP-1",
        summary = "Ticket",
        status = status,
        assignee = assignee,
        priority = priority,
        updatedAtMillis = updatedAtMillis,
    )
}
