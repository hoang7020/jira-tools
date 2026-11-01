package com.jiratools.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant

/** Performs work-calendar arithmetic in a saved table's Jira account timezone. */
object WorkingTimeCalculator {
    private val workdayStart = LocalTime(8, 0)
    private val lunchStart = LocalTime(12, 0)
    private val lunchEnd = LocalTime(13, 0)
    private val workdayEnd = LocalTime(17, 0)

    fun isValidTimeZone(timeZoneId: String): Boolean = timeZoneOrNull(timeZoneId) != null

    /** Returns the elapsed working-time fraction within a weekday, or null outside a session. */
    fun workdayFractionAt(
        atMillis: Long,
        timeZoneId: String,
    ): Double? {
        val timeZone = timeZoneOrNull(timeZoneId) ?: return null
        return workdayFractionAt(atMillis, timeZone)
    }

    internal fun workdayFractionAt(
        atMillis: Long,
        timeZone: TimeZone,
    ): Double? =
        try {
            val local = Instant.fromEpochMilliseconds(atMillis).toLocalDateTime(timeZone)
            val time = local.time
            if (!local.date.isWorkday()) {
                null
            } else {
                when {
                    time < workdayStart || time > workdayEnd -> null
                    time > lunchStart && time < lunchEnd -> null
                    time <= lunchStart -> (time.nanosecondsSince(workdayStart)).toDouble() / WORKDAY_NANOSECONDS
                    else ->
                        (
                            FOUR_HOURS_NANOSECONDS + time.nanosecondsSince(lunchEnd)
                        ).toDouble() / WORKDAY_NANOSECONDS
                }
            }
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: kotlinx.datetime.DateTimeArithmeticException) {
            null
        }

    internal fun sessionsFor(
        date: LocalDate,
        timeZone: TimeZone,
    ): List<WorkingTimeSession> {
        if (!date.isWorkday()) return emptyList()
        return WORK_SESSIONS.mapNotNull { (start, end) ->
            try {
                WorkingTimeSession(
                    startAtMillis = date.atTime(start).toInstant(timeZone).toEpochMilliseconds(),
                    endAtMillis = date.atTime(end).toInstant(timeZone).toEpochMilliseconds(),
                )
            } catch (_: IllegalArgumentException) {
                null
            } catch (_: kotlinx.datetime.DateTimeArithmeticException) {
                null
            }
        }
    }

    /**
     * Adds work time in weekday sessions 08:00-12:00 and 13:00-17:00.
     *
     * Positive durations starting outside a session are advanced to the next session. A zero
     * duration preserves the exact starting instant.
     */
    fun addWorkingTime(
        startAtMillis: Long,
        duration: WorkDuration,
        timeZoneId: String,
    ): Long? {
        val timeZone = timeZoneOrNull(timeZoneId) ?: return null
        if (duration.nanoseconds == 0L) return startAtMillis

        return try {
            var localStart = Instant.fromEpochMilliseconds(startAtMillis).toLocalDateTime(timeZone)
            localStart = normalizeStart(localStart)
            var current = localStart.toInstant(timeZone)
            var remaining = duration.nanoseconds

            while (remaining > 0L) {
                val sessionEnd = endOfSession(localStart).toInstant(timeZone)
                val available = (sessionEnd - current).inWholeNanoseconds
                if (available <= 0L) {
                    localStart = nextSession(localStart)
                    current = localStart.toInstant(timeZone)
                    continue
                }
                if (remaining <= available) {
                    return (current + remaining.nanoseconds).toEpochMilliseconds()
                }
                remaining -= available
                localStart = nextSession(localStart)
                current = localStart.toInstant(timeZone)
            }
            current.toEpochMilliseconds()
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /** Formats a Jira-local timestamp as `yyyy-MM-dd HH:mm`, or null for an invalid timezone. */
    fun formatInTimeZone(
        millis: Long,
        timeZoneId: String,
    ): String? {
        val timeZone = timeZoneOrNull(timeZoneId) ?: return null
        return try {
            val local = Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone)
            "${local.date} ${local.hour.toTwoDigits()}:${local.minute.toTwoDigits()}"
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun timeZoneOrNull(timeZoneId: String): TimeZone? =
        try {
            TimeZone.of(timeZoneId)
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun normalizeStart(local: LocalDateTime): LocalDateTime {
        val date = local.date
        if (!date.isWorkday()) return nextWorkingDay(date).atTime(workdayStart)
        return when {
            local.time < workdayStart -> date.atTime(workdayStart)
            local.time < lunchStart -> local
            local.time < lunchEnd -> date.atTime(lunchEnd)
            local.time < workdayEnd -> local
            else -> nextWorkingDay(date).atTime(workdayStart)
        }
    }

    private fun endOfSession(local: LocalDateTime): LocalDateTime =
        local.date.atTime(if (local.time < lunchStart) lunchStart else workdayEnd)

    private fun nextSession(local: LocalDateTime): LocalDateTime =
        if (local.time < lunchStart) {
            local.date.atTime(lunchEnd)
        } else {
            nextWorkingDay(local.date).atTime(workdayStart)
        }

    private fun LocalDate.isWorkday(): Boolean = dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY

    private fun LocalTime.nanosecondsSince(start: LocalTime): Long =
        nanosecondsSinceMidnight() - start.nanosecondsSinceMidnight()

    private fun LocalTime.nanosecondsSinceMidnight(): Long =
        (((hour * 60L + minute) * 60L + second) * 1_000_000_000L) + nanosecond

    private fun nextWorkingDay(date: LocalDate): LocalDate {
        var next = date.plus(1, DateTimeUnit.DAY)
        while (!next.isWorkday()) {
            next = next.plus(1, DateTimeUnit.DAY)
        }
        return next
    }

    private fun Int.toTwoDigits(): String = toString().padStart(2, '0')

    private val WORK_SESSIONS =
        listOf(
            workdayStart to lunchStart,
            lunchEnd to workdayEnd,
        )

    private const val FOUR_HOURS_NANOSECONDS = 14_400_000_000_000L
    private const val WORKDAY_NANOSECONDS = 28_800_000_000_000.0
}

internal data class WorkingTimeSession(
    val startAtMillis: Long,
    val endAtMillis: Long,
)
