package com.jiratools.domain

import kotlinx.datetime.DateTimeArithmeticException
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Read-only calendar data projected from the currently selected saved-table snapshot. */
data class SavedTableCalendarProjection(
    val todayDate: LocalDate?,
    val windowStart: LocalDate?,
    val windowEnd: LocalDate?,
    val hasValidRanges: Boolean,
    val rows: List<SavedTableCalendarRow>,
    val timeZoneValid: Boolean,
)

/** A ticket row retains its place even when its date range cannot be drawn. */
data class SavedTableCalendarRow(
    val ticketKey: String,
    val startDate: LocalDate?,
    val dueDate: LocalDate?,
    val realDueDate: LocalDate? = null,
    val expectedSegments: List<CalendarWorkSegment> = emptyList(),
    val overdueSegments: List<CalendarWorkSegment> = emptyList(),
    val visibleStartDayOffset: Long?,
    val visibleDurationDays: Long?,
    val overdueStartDayOffset: Long? = null,
    val overdueDurationDays: Long? = null,
    val overdueContinuesBeforeWindow: Boolean = false,
    val overdueContinuesAfterWindow: Boolean = false,
    val overdueOutsideWindow: SavedTableCalendarOutsideWindow? = null,
    val continuesBeforeWindow: Boolean = false,
    val continuesAfterWindow: Boolean = false,
    val outsideWindow: SavedTableCalendarOutsideWindow? = null,
    val issue: SavedTableCalendarIssue,
)

data class CalendarWorkSegment(
    val dayOffset: Long,
    val startFraction: Double,
    val endFraction: Double,
)

data class CalendarDateRange(
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    val dayCount: Long?
        get() {
            if (startDate > endDate) return null
            return endDate.toEpochDays() - startDate.toEpochDays() + 1L
        }

    fun expandedBefore(): CalendarDateRange? {
        if (dayCount == null) return null
        val newStart = startDate.plusMonthsOrNull(-1) ?: return null
        return if (newStart < startDate) copy(startDate = newStart) else null
    }

    fun expandedAfter(): CalendarDateRange? {
        if (dayCount == null) return null
        val newEnd = endDate.plusMonthsOrNull(1) ?: return null
        return if (newEnd > endDate) copy(endDate = newEnd) else null
    }

    fun daysAddedBefore(previous: CalendarDateRange): Long? {
        if (endDate != previous.endDate || startDate >= previous.startDate) return null
        val addedDays = previous.startDate.toEpochDays() - startDate.toEpochDays()
        return addedDays.takeIf { it > 0L && dayCount != null }
    }
}

fun initialCalendarDateRange(todayDate: LocalDate): CalendarDateRange? {
    val startDate = todayDate.plusMonthsOrNull(-1) ?: return null
    val endDate = todayDate.plusMonthsOrNull(3) ?: return null
    return CalendarDateRange(startDate, endDate)
}

enum class SavedTableCalendarOutsideWindow {
    BEFORE,
    AFTER,
}

enum class SavedTableCalendarIssue {
    NONE,
    MISSING_START,
    MISSING_DUE,
    MISSING_BOTH,
    INVALID_START,
    INVALID_DUE,
    INVALID_BOTH,
    INVALID_TIME_ZONE,
    DUE_BEFORE_START,
}

/**
 * Projects ticket instants to inclusive Jira-local calendar days without changing the saved data.
 *
 * The calendar window is based on the opening-time snapshot, not on the ticket ranges, so empty and
 * invalid-range tables still have a stable date axis.
 */
fun projectSavedTableCalendar(
    tickets: List<Ticket>,
    timeZoneId: String?,
    nowMillis: Long,
    dateRange: CalendarDateRange? = null,
): SavedTableCalendarProjection {
    val timeZone = timeZoneId?.let(::timeZoneOrNull)
    if (timeZone == null) {
        return SavedTableCalendarProjection(
            todayDate = null,
            windowStart = null,
            windowEnd = null,
            hasValidRanges = false,
            rows =
                tickets.map { ticket ->
                    SavedTableCalendarRow(
                        ticketKey = ticket.key,
                        startDate = null,
                        dueDate = null,
                        realDueDate = null,
                        visibleStartDayOffset = null,
                        visibleDurationDays = null,
                        issue = SavedTableCalendarIssue.INVALID_TIME_ZONE,
                    )
                },
            timeZoneValid = false,
        )
    }

    val todayDate = nowMillis.toLocalDateOrNull(timeZone)
    val rows =
        tickets.map { ticket ->
            val startDate = ticket.dynamicStartAtMillis?.toLocalDateOrNull(timeZone)
            val dueDate = ticket.dynamicDueAtMillis?.toLocalDateOrNull(timeZone)
            val realDueDate = ticket.realDueAtMillis?.toLocalDateOrNull(timeZone)
            val issue =
                when {
                    ticket.dynamicStartAtMillis == null && ticket.dynamicDueAtMillis == null ->
                        SavedTableCalendarIssue.MISSING_BOTH
                    ticket.dynamicStartAtMillis == null -> SavedTableCalendarIssue.MISSING_START
                    ticket.dynamicDueAtMillis == null -> SavedTableCalendarIssue.MISSING_DUE
                    startDate == null && dueDate == null -> SavedTableCalendarIssue.INVALID_BOTH
                    startDate == null -> SavedTableCalendarIssue.INVALID_START
                    dueDate == null -> SavedTableCalendarIssue.INVALID_DUE
                    checkNotNull(ticket.dynamicDueAtMillis) < checkNotNull(ticket.dynamicStartAtMillis) ->
                        SavedTableCalendarIssue.DUE_BEFORE_START
                    else -> SavedTableCalendarIssue.NONE
                }
            SavedTableCalendarRow(
                ticketKey = ticket.key,
                startDate = startDate,
                dueDate = dueDate,
                realDueDate = realDueDate,
                visibleStartDayOffset = null,
                visibleDurationDays = null,
                issue = issue,
            )
        }

    val hasValidRanges = rows.any { it.issue == SavedTableCalendarIssue.NONE }
    if (todayDate == null) {
        return SavedTableCalendarProjection(
            todayDate = null,
            windowStart = null,
            windowEnd = null,
            hasValidRanges = hasValidRanges,
            rows = rows,
            timeZoneValid = true,
        )
    }

    val range =
        dateRange?.takeIf { it.dayCount != null }
            ?: initialCalendarDateRange(todayDate)
            ?: return SavedTableCalendarProjection(
                todayDate = todayDate,
                windowStart = null,
                windowEnd = null,
                hasValidRanges = hasValidRanges,
                rows = rows,
                timeZoneValid = true,
            )
    val windowStart = range.startDate
    val windowEnd = range.endDate
    val projectedRows =
        rows.mapIndexed { index, row ->
            val ticket = tickets[index]
            val visibleRange =
                if (row.issue == SavedTableCalendarIssue.NONE) {
                    projectVisibleRange(
                        row = row,
                        startDate = checkNotNull(row.startDate),
                        endDate = checkNotNull(row.dueDate),
                        windowStart = windowStart,
                        windowEnd = windowEnd,
                        isOverdue = false,
                    )
                } else {
                    row
                }
            val expectedSegments =
                if (row.issue == SavedTableCalendarIssue.NONE) {
                    projectWorkSegments(
                        startAtMillis = checkNotNull(ticket.dynamicStartAtMillis),
                        endAtMillis = checkNotNull(ticket.dynamicDueAtMillis),
                        windowStart = windowStart,
                        windowEnd = windowEnd,
                        timeZone = timeZone,
                    )
                } else {
                    emptyList()
                }
            val dueDate = row.dueDate
            val realDueDate = row.realDueDate
            val realDueAtMillis = ticket.realDueAtMillis
            if (dueDate == null || realDueDate == null || realDueAtMillis == null || realDueDate <= dueDate) {
                visibleRange.copy(expectedSegments = expectedSegments)
            } else {
                val overdueStart =
                    try {
                        dueDate.plus(1, DateTimeUnit.DAY)
                    } catch (_: DateTimeArithmeticException) {
                        null
                    }
                if (overdueStart == null) {
                    visibleRange.copy(expectedSegments = expectedSegments)
                } else {
                    val overdueRange =
                        projectVisibleRange(
                            row = visibleRange,
                            startDate = overdueStart,
                            endDate = realDueDate,
                            windowStart = windowStart,
                            windowEnd = windowEnd,
                            isOverdue = true,
                        )
                    val overdueStartAtMillis =
                        try {
                            overdueStart.atTime(MIDNIGHT).toInstant(timeZone).toEpochMilliseconds()
                        } catch (_: IllegalArgumentException) {
                            null
                        } catch (_: DateTimeArithmeticException) {
                            null
                        }
                    overdueRange.copy(
                        expectedSegments = expectedSegments,
                        overdueSegments =
                            overdueStartAtMillis?.let {
                                projectWorkSegments(
                                    startAtMillis = it,
                                    endAtMillis = realDueAtMillis,
                                    windowStart = windowStart,
                                    windowEnd = windowEnd,
                                    timeZone = timeZone,
                                )
                            } ?: emptyList(),
                    )
                }
            }
        }

    return SavedTableCalendarProjection(
        todayDate = todayDate,
        windowStart = windowStart,
        windowEnd = windowEnd,
        hasValidRanges = hasValidRanges,
        rows = projectedRows,
        timeZoneValid = true,
    )
}

private fun projectWorkSegments(
    startAtMillis: Long,
    endAtMillis: Long,
    windowStart: LocalDate,
    windowEnd: LocalDate,
    timeZone: TimeZone,
): List<CalendarWorkSegment> {
    if (endAtMillis <= startAtMillis) return emptyList()
    val segments = mutableListOf<CalendarWorkSegment>()
    var date = windowStart
    while (date <= windowEnd) {
        for (session in WorkingTimeCalculator.sessionsFor(date, timeZone)) {
            val visibleStart = maxOf(startAtMillis, session.startAtMillis)
            val visibleEnd = minOf(endAtMillis, session.endAtMillis)
            if (visibleEnd > visibleStart) {
                val startFraction = WorkingTimeCalculator.workdayFractionAt(visibleStart, timeZone) ?: continue
                val endFraction = WorkingTimeCalculator.workdayFractionAt(visibleEnd, timeZone) ?: continue
                val dayOffset = date.toEpochDays() - windowStart.toEpochDays()
                val previous = segments.lastOrNull()
                if (previous != null && previous.dayOffset == dayOffset && previous.endFraction == startFraction) {
                    segments[segments.lastIndex] = previous.copy(endFraction = endFraction)
                } else {
                    segments += CalendarWorkSegment(dayOffset, startFraction, endFraction)
                }
            }
        }
        if (date == windowEnd) break
        date =
            try {
                date.plus(1, DateTimeUnit.DAY)
            } catch (_: DateTimeArithmeticException) {
                break
            }
    }
    return segments
}

private fun projectVisibleRange(
    row: SavedTableCalendarRow,
    startDate: LocalDate,
    endDate: LocalDate,
    windowStart: LocalDate,
    windowEnd: LocalDate,
    isOverdue: Boolean,
): SavedTableCalendarRow {
    val outsideWindow =
        when {
            endDate < windowStart -> SavedTableCalendarOutsideWindow.BEFORE
            startDate > windowEnd -> SavedTableCalendarOutsideWindow.AFTER
            else -> null
        }
    if (outsideWindow != null) {
        return if (isOverdue) {
            row.copy(
                overdueOutsideWindow = outsideWindow,
            )
        } else {
            row.copy(outsideWindow = outsideWindow)
        }
    }
    val visibleStart = maxOf(startDate, windowStart)
    val visibleEnd = minOf(endDate, windowEnd)
    val offset = visibleStart.toEpochDays() - windowStart.toEpochDays()
    val duration = visibleEnd.toEpochDays() - visibleStart.toEpochDays() + 1L
    return if (isOverdue) {
        row.copy(
            overdueStartDayOffset = offset,
            overdueDurationDays = duration,
            overdueContinuesBeforeWindow = startDate < windowStart,
            overdueContinuesAfterWindow = endDate > windowEnd,
        )
    } else {
        row.copy(
            outsideWindow = null,
            visibleStartDayOffset = offset,
            visibleDurationDays = duration,
            continuesBeforeWindow = startDate < windowStart,
            continuesAfterWindow = endDate > windowEnd,
        )
    }
}

private fun LocalDate.plusMonthsOrNull(months: Int): LocalDate? =
    try {
        val targetMonth = YearMonth(year, month).plus(months, DateTimeUnit.MONTH)
        if (targetMonth.year !in MIN_SUPPORTED_YEAR..MAX_SUPPORTED_YEAR) {
            null
        } else {
            LocalDate(targetMonth.year, targetMonth.month.ordinal + 1, day.coerceAtMost(targetMonth.numberOfDays))
        }
    } catch (_: DateTimeArithmeticException) {
        null
    }

private fun timeZoneOrNull(timeZoneId: String): TimeZone? =
    try {
        TimeZone.of(timeZoneId)
    } catch (_: IllegalArgumentException) {
        null
    }

private val MIDNIGHT = LocalTime(0, 0)
private const val MIN_SUPPORTED_YEAR = -999_999
private const val MAX_SUPPORTED_YEAR = 999_999

private fun Long.toLocalDateOrNull(timeZone: TimeZone): LocalDate? =
    try {
        Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone).date
    } catch (_: DateTimeArithmeticException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
