package com.jiratools.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CalendarDateRangeTest {
    @Test
    fun createsTheExistingInitialCalendarWindow() {
        assertEquals(
            CalendarDateRange(LocalDate(2026, 10, 15), LocalDate(2027, 2, 15)),
            initialCalendarDateRange(LocalDate(2026, 11, 15)),
        )
    }

    @Test
    fun expandsBothEdgesByOneMonthWithoutGapsOrDuplicateDays() {
        val range = CalendarDateRange(LocalDate(2026, 10, 15), LocalDate(2027, 2, 15))

        val before = range.expandedBefore()
        val after = range.expandedAfter()

        assertEquals(LocalDate(2026, 9, 15), before?.startDate)
        assertEquals(range.endDate, before?.endDate)
        assertEquals(30L, before?.daysAddedBefore(range))
        assertEquals(LocalDate(2026, 10, 15), after?.startDate)
        assertEquals(LocalDate(2027, 3, 15), after?.endDate)
        assertEquals(124L, range.dayCount)
        assertEquals(152L, after?.dayCount)
    }

    @Test
    fun clampsMonthExpansionAtLeapMonthBoundaries() {
        val range = CalendarDateRange(LocalDate(2024, 3, 31), LocalDate(2024, 5, 31))

        assertEquals(LocalDate(2024, 2, 29), range.expandedBefore()?.startDate)
        assertEquals(LocalDate(2024, 6, 30), range.expandedAfter()?.endDate)
    }

    @Test
    fun refusesInvalidOrUnrepresentableRanges() {
        val invalid = CalendarDateRange(LocalDate(2026, 2, 1), LocalDate(2026, 1, 1))
        val earliest = CalendarDateRange(LocalDate(-999_999, 1, 1), LocalDate(-999_998, 1, 1))
        val latest = CalendarDateRange(LocalDate(999_998, 12, 1), LocalDate(999_999, 12, 31))

        assertNull(invalid.dayCount)
        assertNull(invalid.expandedBefore())
        assertNull(earliest.expandedBefore())
        assertNull(latest.expandedAfter())
    }
}
