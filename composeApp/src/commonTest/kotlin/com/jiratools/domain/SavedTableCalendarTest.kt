package com.jiratools.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class SavedTableCalendarTest {
    @Test
    fun projectsOverdueSegmentStartingTheDayAfterDynamicDueDate() {
        val projection =
            projectSavedTableCalendar(
                tickets =
                    listOf(
                        ticket("HSP-LATE", "2026-01-10T08:00:00Z", "2026-01-16T08:00:00Z")
                            .copy(realDueAtMillis = millis("2026-01-20T08:00:00Z")),
                    ),
                timeZoneId = "UTC",
                nowMillis = millis("2026-01-15T12:00:00Z"),
            )

        val row = projection.rows.single()
        assertEquals(33L, row.overdueStartDayOffset)
        assertEquals(4L, row.overdueDurationDays)
        assertFalse(row.overdueContinuesBeforeWindow)
        assertFalse(row.overdueContinuesAfterWindow)
    }

    @Test
    fun reprojectsTicketRangesWithinAnExplicitlyExpandedDateRange() {
        val projection =
            projectSavedTableCalendar(
                tickets = listOf(ticket("HSP-FUTURE", "2027-01-04T08:00:00Z", "2027-01-05T10:00:00Z")),
                timeZoneId = "UTC",
                nowMillis = millis("2026-11-01T12:00:00Z"),
                dateRange = CalendarDateRange(LocalDate(2027, 1, 1), LocalDate(2027, 2, 1)),
            )
        val row = projection.rows.single()

        assertEquals(LocalDate(2027, 1, 1), projection.windowStart)
        assertEquals(LocalDate(2027, 2, 1), projection.windowEnd)
        assertEquals(3L, row.visibleStartDayOffset)
        assertEquals(2L, row.visibleDurationDays)
        assertEquals(
            listOf(
                CalendarWorkSegment(3L, 0.0, 1.0),
                CalendarWorkSegment(4L, 0.0, 0.25),
            ),
            row.expectedSegments,
        )
    }

    @Test
    fun keepsTheDefaultCalendarRangeWhenAnExplicitRangeIsInvalid() {
        val projection =
            projectSavedTableCalendar(
                tickets = emptyList(),
                timeZoneId = "UTC",
                nowMillis = millis("2026-11-01T12:00:00Z"),
                dateRange = CalendarDateRange(LocalDate(2026, 12, 1), LocalDate(2026, 11, 1)),
            )

        assertEquals(LocalDate(2026, 10, 1), projection.windowStart)
        assertEquals(LocalDate(2027, 2, 1), projection.windowEnd)
        assertTrue(projection.timeZoneValid)
    }

    @Test
    fun projectsExpectedTimelineByWorkingTimeAndMergesAcrossLunch() {
        val projection =
            projectSavedTableCalendar(
                tickets =
                    listOf(
                        ticket("HSP-HALF", "2026-11-02T08:00:00Z", "2026-11-02T12:00:00Z"),
                        ticket("HSP-LUNCH", "2026-11-02T10:00:00Z", "2026-11-02T14:00:00Z"),
                    ),
                timeZoneId = "UTC",
                nowMillis = millis("2026-11-02T12:00:00Z"),
            )
        val windowStart = checkNotNull(projection.windowStart)
        val dayOffset = LocalDate(2026, 11, 2).toEpochDays() - windowStart.toEpochDays()
        val rows = projection.rows.associateBy { it.ticketKey }

        assertEquals(
            listOf(CalendarWorkSegment(dayOffset, 0.0, 0.5)),
            rows.getValue("HSP-HALF").expectedSegments,
        )
        assertEquals(
            listOf(CalendarWorkSegment(dayOffset, 0.25, 0.625)),
            rows.getValue("HSP-LUNCH").expectedSegments,
        )
    }

    @Test
    fun projectsAcrossWorkingDaysWithoutDrawingOnWeekendColumns() {
        val projection =
            projectSavedTableCalendar(
                tickets = listOf(ticket("HSP-WEEKEND", "2026-11-06T11:00:00Z", "2026-11-09T14:00:00Z")),
                timeZoneId = "UTC",
                nowMillis = millis("2026-11-02T12:00:00Z"),
            )
        val windowStart = checkNotNull(projection.windowStart)
        val fridayOffset = LocalDate(2026, 11, 6).toEpochDays() - windowStart.toEpochDays()
        val mondayOffset = LocalDate(2026, 11, 9).toEpochDays() - windowStart.toEpochDays()

        assertEquals(
            listOf(
                CalendarWorkSegment(fridayOffset, 0.375, 1.0),
                CalendarWorkSegment(mondayOffset, 0.0, 0.625),
            ),
            projection.rows.single().expectedSegments,
        )
        assertEquals(3L, mondayOffset - fridayOffset)
    }

    @Test
    fun projectsInTheSavedTimezoneAndClipsToTheVisibleWindow() {
        val timezoneProjection =
            projectSavedTableCalendar(
                tickets = listOf(ticket("HSP-TZ", "2026-11-02T03:00:00Z", "2026-11-02T07:00:00Z")),
                timeZoneId = "Asia/Ho_Chi_Minh",
                nowMillis = millis("2026-11-02T05:00:00Z"),
            )
        val timezoneOffset =
            LocalDate(2026, 11, 2).toEpochDays() - checkNotNull(timezoneProjection.windowStart).toEpochDays()
        assertEquals(
            listOf(CalendarWorkSegment(timezoneOffset, 0.25, 0.625)),
            timezoneProjection.rows.single().expectedSegments,
        )

        val clippedProjection =
            projectSavedTableCalendar(
                tickets = listOf(ticket("HSP-CLIP", "2026-10-01T08:00:00Z", "2027-03-16T17:00:00Z")),
                timeZoneId = "UTC",
                nowMillis = millis("2026-11-15T12:00:00Z"),
            )
        val clippedRow = clippedProjection.rows.single()

        assertEquals(0L, clippedRow.expectedSegments.first().dayOffset)
        assertEquals(0.0, clippedRow.expectedSegments.first().startFraction)
        assertEquals(1.0, clippedRow.expectedSegments.first().endFraction)
        assertTrue(clippedRow.continuesBeforeWindow)
        assertTrue(clippedRow.continuesAfterWindow)
    }

    @Test
    fun projectsOverdueHoursAsWorkingSegmentsAndIgnoresInvalidExpectedRanges() {
        val projection =
            projectSavedTableCalendar(
                tickets =
                    listOf(
                        ticket("HSP-LATE", "2026-11-02T08:00:00Z", "2026-11-05T10:00:00Z")
                            .copy(realDueAtMillis = millis("2026-11-06T10:00:00Z")),
                        ticket("HSP-ZERO", "2026-11-02T08:00:00Z", "2026-11-02T08:00:00Z"),
                        ticket("HSP-REVERSED", "2026-11-02T15:00:00Z", "2026-11-02T10:00:00Z"),
                        ticket("HSP-INVALID", "2026-11-02T08:00:00Z", "2026-11-04T08:00:00Z")
                            .copy(dynamicStartAtMillis = Long.MAX_VALUE),
                    ),
                timeZoneId = "UTC",
                nowMillis = millis("2026-11-02T12:00:00Z"),
            )
        val windowStart = checkNotNull(projection.windowStart)
        val overdueOffset = LocalDate(2026, 11, 6).toEpochDays() - windowStart.toEpochDays()
        val rows = projection.rows.associateBy { it.ticketKey }

        assertEquals(
            listOf(CalendarWorkSegment(overdueOffset, 0.0, 0.25)),
            rows.getValue("HSP-LATE").overdueSegments,
        )
        assertTrue(rows.getValue("HSP-ZERO").expectedSegments.isEmpty())
        assertTrue(rows.getValue("HSP-REVERSED").expectedSegments.isEmpty())
        assertTrue(rows.getValue("HSP-INVALID").expectedSegments.isEmpty())
    }

    @Test
    fun doesNotProjectOverdueSegmentWhenRealDueDateIsNotAfterDynamicDueDate() {
        val projection =
            projectSavedTableCalendar(
                tickets =
                    listOf(
                        ticket("HSP-ONTIME", "2026-01-10T08:00:00Z", "2026-01-16T08:00:00Z")
                            .copy(realDueAtMillis = millis("2026-01-16T20:00:00Z")),
                        ticket("HSP-EARLY", "2026-01-10T08:00:00Z", "2026-01-16T08:00:00Z")
                            .copy(realDueAtMillis = millis("2026-01-15T20:00:00Z")),
                    ),
                timeZoneId = "UTC",
                nowMillis = millis("2026-01-15T12:00:00Z"),
            )

        assertTrue(projection.rows.all { it.overdueStartDayOffset == null && it.overdueDurationDays == null })
    }

    @Test
    fun projectsOverdueRangeEvenWhenTheExpectedTimelineHasNoStartDate() {
        val projection =
            projectSavedTableCalendar(
                tickets =
                    listOf(
                        ticket("HSP-NO-START", null, "2026-01-16T08:00:00Z")
                            .copy(realDueAtMillis = millis("2026-01-20T08:00:00Z")),
                    ),
                timeZoneId = "UTC",
                nowMillis = millis("2026-01-15T12:00:00Z"),
            )

        val row = projection.rows.single()
        assertEquals(SavedTableCalendarIssue.MISSING_START, row.issue)
        assertEquals(33L, row.overdueStartDayOffset)
        assertEquals(4L, row.overdueDurationDays)
    }

    @Test
    fun usesTheSavedTableTimezoneForTodayAndTicketDates() {
        val projection =
            projectSavedTableCalendar(
                tickets =
                    listOf(
                        ticket(
                            key = "HSP-1",
                            start = "2026-01-01T20:00:00Z",
                            due = "2026-01-02T03:00:00Z",
                        ),
                    ),
                timeZoneId = "Asia/Ho_Chi_Minh",
                nowMillis = millis("2026-03-31T18:00:00Z"),
            )

        assertEquals(LocalDate(2026, 4, 1), projection.todayDate)
        assertEquals(LocalDate(2026, 3, 1), projection.windowStart)
        assertEquals(LocalDate(2026, 7, 1), projection.windowEnd)
        assertEquals(LocalDate(2026, 1, 2), projection.rows.single().startDate)
        assertEquals(LocalDate(2026, 1, 2), projection.rows.single().dueDate)
        assertEquals(SavedTableCalendarOutsideWindow.BEFORE, projection.rows.single().outsideWindow)
        assertNull(projection.rows.single().visibleStartDayOffset)
        assertNull(projection.rows.single().visibleDurationDays)
    }

    @Test
    fun clampsCalendarMonthBoundariesAndHandlesLeapYears() {
        val nonLeapYear =
            projectSavedTableCalendar(
                tickets = emptyList(),
                timeZoneId = "UTC",
                nowMillis = millis("2026-03-31T12:00:00Z"),
            )
        val leapYear =
            projectSavedTableCalendar(
                tickets = emptyList(),
                timeZoneId = "UTC",
                nowMillis = millis("2024-03-31T12:00:00Z"),
            )
        val yearBoundary =
            projectSavedTableCalendar(
                tickets = emptyList(),
                timeZoneId = "UTC",
                nowMillis = millis("2026-11-30T12:00:00Z"),
            )

        assertEquals(LocalDate(2026, 2, 28), nonLeapYear.windowStart)
        assertEquals(LocalDate(2026, 6, 30), nonLeapYear.windowEnd)
        assertEquals(LocalDate(2024, 2, 29), leapYear.windowStart)
        assertEquals(LocalDate(2024, 6, 30), leapYear.windowEnd)
        assertEquals(LocalDate(2026, 10, 30), yearBoundary.windowStart)
        assertEquals(LocalDate(2027, 2, 28), yearBoundary.windowEnd)
    }

    @Test
    fun clipsIntersectingRangesToTheInclusiveWindow() {
        val projection =
            projectSavedTableCalendar(
                tickets =
                    listOf(
                        ticket("HSP-IN", "2026-01-01T12:00:00Z", "2026-01-04T12:00:00Z"),
                        ticket("HSP-LEFT", "2025-12-01T12:00:00Z", "2025-12-20T12:00:00Z"),
                        ticket("HSP-RIGHT", "2026-04-12T12:00:00Z", "2026-04-20T12:00:00Z"),
                        ticket("HSP-BOTH", "2025-12-01T12:00:00Z", "2026-05-01T12:00:00Z"),
                        ticket("HSP-END", "2026-04-15T12:00:00Z", "2026-04-15T12:00:00Z"),
                    ),
                timeZoneId = "UTC",
                nowMillis = millis("2026-01-15T12:00:00Z"),
            )

        val windowStart = LocalDate(2025, 12, 15)
        val windowEnd = LocalDate(2026, 4, 15)
        val rows = projection.rows.associateBy { it.ticketKey }

        assertEquals(windowStart, projection.windowStart)
        assertEquals(windowEnd, projection.windowEnd)
        assertEquals(122L, windowEnd.toEpochDays() - windowStart.toEpochDays() + 1L)

        val inside = rows.getValue("HSP-IN")
        assertEquals(LocalDate(2026, 1, 1).toEpochDays() - windowStart.toEpochDays(), inside.visibleStartDayOffset)
        assertEquals(4L, inside.visibleDurationDays)
        assertFalse(inside.continuesBeforeWindow)
        assertFalse(inside.continuesAfterWindow)
        assertNull(inside.outsideWindow)

        val left = rows.getValue("HSP-LEFT")
        assertEquals(0L, left.visibleStartDayOffset)
        assertEquals(6L, left.visibleDurationDays)
        assertTrue(left.continuesBeforeWindow)
        assertFalse(left.continuesAfterWindow)
        assertNull(left.outsideWindow)

        val right = rows.getValue("HSP-RIGHT")
        assertEquals(LocalDate(2026, 4, 12).toEpochDays() - windowStart.toEpochDays(), right.visibleStartDayOffset)
        assertEquals(4L, right.visibleDurationDays)
        assertFalse(right.continuesBeforeWindow)
        assertTrue(right.continuesAfterWindow)

        val both = rows.getValue("HSP-BOTH")
        assertEquals(0L, both.visibleStartDayOffset)
        assertEquals(122L, both.visibleDurationDays)
        assertTrue(both.continuesBeforeWindow)
        assertTrue(both.continuesAfterWindow)

        val sameDay = rows.getValue("HSP-END")
        assertEquals(LocalDate(2026, 4, 15).toEpochDays() - windowStart.toEpochDays(), sameDay.visibleStartDayOffset)
        assertEquals(1L, sameDay.visibleDurationDays)
    }

    @Test
    fun countsInclusiveCalendarDaysAcrossDaylightSavingTransitions() {
        val projection =
            projectSavedTableCalendar(
                tickets = listOf(ticket("HSP-DST", "2026-03-28T23:30:00Z", "2026-03-29T22:30:00Z")),
                timeZoneId = "Europe/Berlin",
                nowMillis = millis("2026-03-10T12:00:00Z"),
            )

        val row = projection.rows.single()
        assertEquals(LocalDate(2026, 3, 29), row.startDate)
        assertEquals(LocalDate(2026, 3, 30), row.dueDate)
        assertEquals(2L, row.visibleDurationDays)
    }

    @Test
    fun keepsValidRangesOutsideTheWindowAsRowsWithTheirDirection() {
        val projection =
            projectSavedTableCalendar(
                tickets =
                    listOf(
                        ticket("HSP-BEFORE", "2025-11-01T12:00:00Z", "2025-12-14T12:00:00Z"),
                        ticket("HSP-AFTER", "2026-04-16T12:00:00Z", "2026-05-01T12:00:00Z"),
                    ),
                timeZoneId = "UTC",
                nowMillis = millis("2026-01-15T12:00:00Z"),
            )

        assertTrue(projection.hasValidRanges)
        assertEquals(listOf("HSP-BEFORE", "HSP-AFTER"), projection.rows.map { it.ticketKey })
        assertEquals(
            listOf(SavedTableCalendarOutsideWindow.BEFORE, SavedTableCalendarOutsideWindow.AFTER),
            projection.rows.map { it.outsideWindow },
        )
        assertTrue(projection.rows.all { it.visibleStartDayOffset == null && it.visibleDurationDays == null })
    }

    @Test
    fun preservesTicketOrderAndExplainsMissingReversedAndOutOfWindowRanges() {
        val projection =
            projectSavedTableCalendar(
                tickets =
                    listOf(
                        ticket("HSP-1", null, "2026-01-02T08:00:00Z"),
                        ticket("HSP-2", "2026-01-02T08:00:00Z", null),
                        ticket("HSP-3", null, null),
                        ticket("HSP-4", "2026-01-03T08:00:00Z", "2026-01-02T08:00:00Z"),
                        ticket("HSP-5", "2026-01-01T08:00:00Z", "2026-01-04T08:00:00Z")
                            .copy(dynamicStartAtMillis = Long.MAX_VALUE),
                        ticket("HSP-6", "2026-01-01T08:00:00Z", "2026-01-04T08:00:00Z")
                            .copy(dynamicDueAtMillis = Long.MAX_VALUE),
                        ticket("HSP-7", null, null)
                            .copy(dynamicStartAtMillis = Long.MAX_VALUE, dynamicDueAtMillis = Long.MAX_VALUE),
                        ticket("HSP-8", "2026-01-01T08:00:00Z", "2026-01-04T08:00:00Z"),
                    ),
                timeZoneId = "UTC",
                nowMillis = millis("2026-01-01T12:00:00Z"),
            )

        assertEquals(
            listOf("HSP-1", "HSP-2", "HSP-3", "HSP-4", "HSP-5", "HSP-6", "HSP-7", "HSP-8"),
            projection.rows.map { it.ticketKey },
        )
        assertEquals(
            listOf(
                SavedTableCalendarIssue.MISSING_START,
                SavedTableCalendarIssue.MISSING_DUE,
                SavedTableCalendarIssue.MISSING_BOTH,
                SavedTableCalendarIssue.DUE_BEFORE_START,
                SavedTableCalendarIssue.DUE_BEFORE_START,
                SavedTableCalendarIssue.NONE,
                SavedTableCalendarIssue.NONE,
                SavedTableCalendarIssue.NONE,
            ),
            projection.rows.map { it.issue },
        )
        assertTrue(projection.hasValidRanges)
        assertEquals(LocalDate(2025, 12, 1), projection.windowStart)
        assertEquals(LocalDate(2026, 4, 1), projection.windowEnd)
        assertNull(projection.rows[0].visibleStartDayOffset)
        assertNull(projection.rows[3].visibleDurationDays)
        assertNull(projection.rows[4].visibleDurationDays)
        assertEquals(31L, projection.rows[5].visibleStartDayOffset)
        assertEquals(91L, projection.rows[5].visibleDurationDays)
        assertTrue(projection.rows[5].continuesAfterWindow)
        assertEquals(SavedTableCalendarOutsideWindow.AFTER, projection.rows[6].outsideWindow)
        assertEquals(4L, projection.rows[7].visibleDurationDays)
        assertTrue(projection.rows.take(5).all { it.expectedSegments.isEmpty() })
        assertTrue(projection.rows[6].expectedSegments.isEmpty())
    }

    @Test
    fun validTimezoneAlwaysBuildsWindowEvenWhenThereAreNoValidRanges() {
        val emptyProjection =
            projectSavedTableCalendar(
                tickets = emptyList(),
                timeZoneId = "UTC",
                nowMillis = millis("2026-01-15T12:00:00Z"),
            )
        val missingDatesProjection =
            projectSavedTableCalendar(
                tickets = listOf(ticket("HSP-1", null, null)),
                timeZoneId = "UTC",
                nowMillis = millis("2026-01-15T12:00:00Z"),
            )

        assertTrue(emptyProjection.timeZoneValid)
        assertFalse(emptyProjection.hasValidRanges)
        assertEquals(LocalDate(2025, 12, 15), emptyProjection.windowStart)
        assertEquals(LocalDate(2026, 4, 15), emptyProjection.windowEnd)
        assertTrue(emptyProjection.rows.isEmpty())

        assertTrue(missingDatesProjection.timeZoneValid)
        assertFalse(missingDatesProjection.hasValidRanges)
        assertEquals(LocalDate(2025, 12, 15), missingDatesProjection.windowStart)
        assertEquals(LocalDate(2026, 4, 15), missingDatesProjection.windowEnd)
        assertEquals(SavedTableCalendarIssue.MISSING_BOTH, missingDatesProjection.rows.single().issue)
        assertNull(missingDatesProjection.rows.single().visibleStartDayOffset)
    }

    @Test
    fun invalidTimezoneKeepsRowsButCannotBuildAWindow() {
        val invalidTimezone =
            projectSavedTableCalendar(
                tickets = listOf(ticket("HSP-1", "2026-01-01T08:00:00Z", "2026-01-02T08:00:00Z")),
                timeZoneId = "Not/A_Real_Zone",
                nowMillis = millis("2026-01-15T12:00:00Z"),
            )
        val missingTimezone =
            projectSavedTableCalendar(
                tickets = listOf(ticket("HSP-2", "2026-01-01T08:00:00Z", "2026-01-02T08:00:00Z")),
                timeZoneId = null,
                nowMillis = millis("2026-01-15T12:00:00Z"),
            )

        assertFalse(invalidTimezone.timeZoneValid)
        assertEquals(SavedTableCalendarIssue.INVALID_TIME_ZONE, invalidTimezone.rows.single().issue)
        assertNull(invalidTimezone.todayDate)
        assertNull(invalidTimezone.windowStart)
        assertNull(invalidTimezone.windowEnd)

        assertFalse(missingTimezone.timeZoneValid)
        assertEquals(SavedTableCalendarIssue.INVALID_TIME_ZONE, missingTimezone.rows.single().issue)
        assertNull(missingTimezone.windowStart)
        assertNull(missingTimezone.windowEnd)
    }

    private fun ticket(
        key: String,
        start: String?,
        due: String?,
    ) = Ticket(
        key = key,
        summary = key,
        status = "In Progress",
        dynamicStartAtMillis = start?.let(::millis),
        dynamicDueAtMillis = due?.let(::millis),
    )

    private fun millis(instant: String): Long = Instant.parse(instant).toEpochMilliseconds()
}
