package com.jiratools.domain

import kotlinx.datetime.DateTimeArithmeticException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

enum class SavedTableChartField {
    STATUS,
    ASSIGNEE,
    PRIORITY,
}

enum class SavedTableChartType {
    PIE,
    COLUMN,
    LINE,
    BURNDOWN,
}

data class CategoryCount(
    val label: String,
    val ticketCount: Int,
)

data class CategoryChartProjection(
    val field: SavedTableChartField,
    val counts: List<CategoryCount>,
    val totalCount: Int,
)

data class UpdatedTicketsByDayPoint(
    val date: LocalDate,
    val ticketCount: Int,
)

data class UpdatedTicketsByDayProjection(
    val points: List<UpdatedTicketsByDayPoint>,
    val missingUpdatedCount: Int,
    val usedUtcFallback: Boolean,
)

data class BurndownPoint(
    val recordedAtMillis: Long,
    val ticketCount: Int,
    val remainingCount: Int,
    val unknownCategoryCount: Int,
)

data class BurndownProjection(
    val points: List<BurndownPoint>,
)

/** Pure projections for charts rendered from a saved table's local snapshot. */
object SavedTableChartCalculator {
    private const val UNKNOWN_CATEGORY = "Chưa xác định"

    fun categoryCounts(
        tickets: List<Ticket>,
        field: SavedTableChartField,
    ): CategoryChartProjection {
        val counts =
            tickets
                .groupingBy { ticket ->
                    val value =
                        when (field) {
                            SavedTableChartField.STATUS -> ticket.status
                            SavedTableChartField.ASSIGNEE -> ticket.assignee
                            SavedTableChartField.PRIORITY -> ticket.priority
                        }
                    value.displayCategory()
                }.eachCount()
                .map { (label, count) -> CategoryCount(label, count) }
                .sortedWith(compareBy<CategoryCount> { it.label == UNKNOWN_CATEGORY }.thenBy { it.label })
        return CategoryChartProjection(field = field, counts = counts, totalCount = tickets.size)
    }

    fun updatedTicketsByDay(
        tickets: List<Ticket>,
        timeZoneId: String?,
    ): UpdatedTicketsByDayProjection {
        val parsedTimeZone = timeZoneId?.takeIf(String::isNotBlank)?.let { runCatching { TimeZone.of(it) }.getOrNull() }
        val timeZone = parsedTimeZone ?: TimeZone.UTC
        val datedTickets =
            tickets.mapNotNull { ticket ->
                ticket.updatedAtMillis?.let { timestamp ->
                    try {
                        Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(timeZone).date
                    } catch (_: DateTimeArithmeticException) {
                        null
                    }
                }
            }
        val points =
            datedTickets
                .groupingBy { it }
                .eachCount()
                .map { (date, count) -> UpdatedTicketsByDayPoint(date, count) }
                .sortedBy { it.date.toEpochDays() }
        return UpdatedTicketsByDayProjection(
            points = points,
            missingUpdatedCount = tickets.size - datedTickets.size,
            usedUtcFallback = parsedTimeZone == null,
        )
    }

    fun burndown(snapshots: List<BurndownSnapshot>): BurndownProjection =
        BurndownProjection(
            points =
                snapshots.map { snapshot ->
                    BurndownPoint(
                        recordedAtMillis = snapshot.recordedAtMillis,
                        ticketCount = snapshot.ticketCount,
                        remainingCount = snapshot.remainingCount,
                        unknownCategoryCount = snapshot.unknownCategoryCount,
                    )
                },
        )

    private fun String?.displayCategory(): String = this?.trim()?.takeIf(String::isNotEmpty) ?: UNKNOWN_CATEGORY
}
