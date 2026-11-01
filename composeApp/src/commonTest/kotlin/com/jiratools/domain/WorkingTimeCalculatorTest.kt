package com.jiratools.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WorkingTimeCalculatorTest {
    @Test
    fun oneHourCrossingLunchResumesAtOnePm() {
        assertEquals(
            "2026-11-02 13:30",
            dueDate("2026-11-02T11:30", "1h"),
        )
    }

    @Test
    fun workdayAndWorkweekEstimatesUseEightAndFortyHours() {
        assertEquals(
            "2026-11-09 16:00",
            dueDate("2026-11-06T16:00", "1d"),
        )
        assertEquals(
            "2026-11-06 17:00",
            dueDate("2026-11-02T08:00", "1w"),
        )
    }

    @Test
    fun startsDuringLunchAfterHoursAndOnWeekendsMoveToTheNextSession() {
        assertEquals("2026-11-02 13:30", dueDate("2026-11-02T12:15", "30m"))
        assertEquals("2026-11-09 08:30", dueDate("2026-11-06T18:00", "30m"))
        assertEquals("2026-11-09 08:30", dueDate("2026-11-08T10:00", "30m"))
    }

    @Test
    fun zeroDurationPreservesTheExactStartInstantEvenOutsideWorkingHours() {
        val start = instantMillis("2026-11-07T12:15")

        assertEquals(start, WorkingTimeCalculator.addWorkingTime(start, WorkDuration.parse("0")!!, UTC))
        assertEquals("2026-11-07 12:15", WorkingTimeCalculator.formatInTimeZone(start, UTC))
    }

    @Test
    fun localWorkSessionsFollowTimezoneDstChanges() {
        assertEquals(
            "2026-03-09 09:00",
            dueDate(
                start = "2026-03-06T16:00",
                estimate = "2h",
                timeZoneId = "America/New_York",
            ),
        )
    }

    @Test
    fun invalidTimezoneReturnsNullForCalculationAndFormatting() {
        assertNull(
            WorkingTimeCalculator.addWorkingTime(
                startAtMillis = instantMillis("2026-11-02T08:00"),
                duration = WorkDuration.parse("1h")!!,
                timeZoneId = "Not/A_Real_Zone",
            ),
        )
        assertNull(WorkingTimeCalculator.formatInTimeZone(0L, "Not/A_Real_Zone"))
    }

    @Test
    fun mapsWorkingSessionBoundariesToFractionsOfTheWorkday() {
        assertFraction("2026-11-02T08:00", 0.0)
        assertFraction("2026-11-02T10:00", 0.25)
        assertFraction("2026-11-02T12:00", 0.5)
        assertFraction("2026-11-02T13:00", 0.5)
        assertFraction("2026-11-02T14:00", 0.625)
        assertFraction("2026-11-02T17:00", 1.0)
    }

    @Test
    fun doesNotMapLunchOutsideHoursOrWeekendToAWorkdayFraction() {
        assertNull(workdayFraction("2026-11-02T12:30"))
        assertNull(workdayFraction("2026-11-02T18:00"))
        assertNull(workdayFraction("2026-11-07T10:00"))
        assertNull(WorkingTimeCalculator.workdayFractionAt(0L, "Not/A_Real_Zone"))
    }

    @Test
    fun mapsLocalWorkSessionsCorrectlyAcrossTimezoneDstChanges() {
        assertEquals(
            0.25,
            WorkingTimeCalculator.workdayFractionAt(
                instantMillis("2026-03-09T10:00", "America/New_York"),
                "America/New_York",
            ),
        )
        assertEquals(
            0.625,
            WorkingTimeCalculator.workdayFractionAt(
                instantMillis("2026-03-09T14:00", "America/New_York"),
                "America/New_York",
            ),
        )
    }

    private fun dueDate(
        start: String,
        estimate: String,
        timeZoneId: String = UTC,
    ): String {
        val startAtMillis = instantMillis(start, timeZoneId)
        val duration = WorkDuration.parse(estimate)!!
        val dueAtMillis = WorkingTimeCalculator.addWorkingTime(startAtMillis, duration, timeZoneId)!!
        return WorkingTimeCalculator.formatInTimeZone(dueAtMillis, timeZoneId)!!
    }

    private fun assertFraction(
        localDateTime: String,
        expected: Double,
        timeZoneId: String = UTC,
    ) {
        assertEquals(expected, workdayFraction(localDateTime, timeZoneId))
    }

    private fun workdayFraction(
        localDateTime: String,
        timeZoneId: String = UTC,
    ): Double? =
        WorkingTimeCalculator.workdayFractionAt(
            instantMillis(localDateTime, timeZoneId),
            timeZoneId,
        )

    private fun instantMillis(
        localDateTime: String,
        timeZoneId: String = UTC,
    ): Long = LocalDateTime.parse(localDateTime).toInstant(TimeZone.of(timeZoneId)).toEpochMilliseconds()

    private companion object {
        const val UTC = "UTC"
    }
}
