package com.jiratools.ui.saved

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jiratools.domain.CalendarDateRange
import com.jiratools.domain.CalendarWorkSegment
import com.jiratools.domain.SavedTableCalendarIssue
import com.jiratools.domain.SavedTableCalendarOutsideWindow
import com.jiratools.domain.SavedTableCalendarProjection
import com.jiratools.domain.SavedTableCalendarRow
import com.jiratools.domain.Ticket
import com.jiratools.domain.projectSavedTableCalendar
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

private val TICKET_COLUMN_WIDTH = 176.dp
private val DAY_COLUMN_WIDTH = 104.dp
private val HEADER_HEIGHT = 64.dp
private val ROW_HEIGHT = 48.dp
private const val RANGE_EXPANSION_THRESHOLD_DAYS = 10

private data class CalendarViewportAnchor(
    val date: LocalDate,
    val offsetPx: Int,
)

private data class CalendarViewportState(
    val firstVisible: Int,
    val lastVisible: Int,
    val range: CalendarDateRange?,
    val pendingAnchor: CalendarViewportAnchor?,
)

private fun Modifier.calendarHorizontalInput(state: LazyListState): Modifier =
    pointerInput(state) {
        coroutineScope {
            val scrollScope = this
            detectHorizontalDragGestures { change, dragAmount ->
                change.consume()
                scrollScope.launch { state.scrollBy(-dragAmount) }
            }
        }
    }.calendarHorizontalWheelInput(state)

private fun Modifier.calendarHorizontalWheelInput(state: LazyListState): Modifier =
    pointerInput(state) {
        coroutineScope {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Scroll) {
                        val horizontalDelta =
                            event.changes
                                .firstOrNull()
                                ?.scrollDelta
                                ?.x ?: 0f
                        if (horizontalDelta != 0f) {
                            event.changes.forEach { it.consume() }
                            launch { state.scrollBy(horizontalDelta) }
                        }
                    }
                }
            }
        }
    }

/**
 * Read-only calendar for a saved table. The date axis scrolls independently while ticket rows remain
 * vertically lazy and their fixed key column stays visible.
 */
@Composable
fun SavedTableCalendarArea(
    tableName: String,
    timeZoneId: String?,
    tickets: List<Ticket>,
    calendarReferenceMillis: Long,
    projection: SavedTableCalendarProjection,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialRange =
        remember(projection.windowStart, projection.windowEnd) {
            val start = projection.windowStart
            val end = projection.windowEnd
            if (start != null && end != null) CalendarDateRange(start, end) else null
        }
    var dateRange by remember(tickets, timeZoneId, calendarReferenceMillis) { mutableStateOf(initialRange) }
    var pendingAnchor by remember(tickets, timeZoneId, calendarReferenceMillis) {
        mutableStateOf<CalendarViewportAnchor?>(null)
    }
    val currentProjection =
        remember(tickets, timeZoneId, calendarReferenceMillis, dateRange, projection) {
            dateRange?.let { range ->
                projectSavedTableCalendar(
                    tickets = tickets,
                    timeZoneId = timeZoneId,
                    nowMillis = calendarReferenceMillis,
                    dateRange = range,
                )
            } ?: projection
        }
    val windowStart = currentProjection.windowStart
    val windowEnd = currentProjection.windowEnd
    val dayCountLong = dateRange?.dayCount ?: 0L
    val dateRangeTooLarge = dayCountLong > Int.MAX_VALUE
    val dayCount = if (dayCountLong in 1..Int.MAX_VALUE.toLong()) dayCountLong.toInt() else 0
    val todayIndex =
        if (dateRange != null && currentProjection.todayDate != null) {
            val daysFromWindowStart =
                (currentProjection.todayDate.toEpochDays() - checkNotNull(dateRange).startDate.toEpochDays())
                    .toInt()
            daysFromWindowStart.coerceIn(0, (dayCount - 1).coerceAtLeast(0))
        } else {
            0
        }
    val axisState = rememberLazyListState(initialFirstVisibleItemIndex = todayIndex)
    LaunchedEffect(tickets, timeZoneId, calendarReferenceMillis, initialRange) {
        if (dayCount > 0) axisState.scrollToItem(todayIndex)
    }
    LaunchedEffect(dateRange, pendingAnchor) {
        val anchor = pendingAnchor ?: return@LaunchedEffect
        val range = dateRange ?: return@LaunchedEffect
        val anchorIndex = anchor.date.toEpochDays() - range.startDate.toEpochDays()
        if (anchorIndex in 0 until (range.dayCount ?: 0L)) {
            axisState.scrollToItem(anchorIndex.toInt(), anchor.offsetPx)
        }
        pendingAnchor = null
    }
    LaunchedEffect(axisState) {
        var previousRange: CalendarDateRange? = null
        var previousFirst: Int? = null
        var previousLast: Int? = null
        snapshotFlow {
            CalendarViewportState(
                firstVisible = axisState.firstVisibleItemIndex,
                lastVisible =
                    axisState.layoutInfo.visibleItemsInfo
                        .lastOrNull()
                        ?.index ?: axisState.firstVisibleItemIndex,
                range = dateRange,
                pendingAnchor = pendingAnchor,
            )
        }.collect { viewport ->
            val range = viewport.range ?: return@collect
            val count = range.dayCount?.takeIf { it <= Int.MAX_VALUE }?.toInt() ?: return@collect
            if (viewport.pendingAnchor != null || range != previousRange) {
                previousRange = range
                previousFirst = viewport.firstVisible
                previousLast = viewport.lastVisible
                return@collect
            }

            val movingBackward = previousFirst?.let { viewport.firstVisible < it } == true
            val movingForward = previousLast?.let { viewport.lastVisible > it } == true
            if (movingBackward && viewport.firstVisible <= RANGE_EXPANSION_THRESHOLD_DAYS) {
                val expanded = range.expandedBefore()
                val addedDays = expanded?.daysAddedBefore(range)
                if (expanded != null && addedDays != null) {
                    val anchorDate = range.startDate.plus(viewport.firstVisible, DateTimeUnit.DAY)
                    pendingAnchor = CalendarViewportAnchor(anchorDate, axisState.firstVisibleItemScrollOffset)
                    dateRange = expanded
                }
            } else if (
                movingForward &&
                count - 1 - viewport.lastVisible <= RANGE_EXPANSION_THRESHOLD_DAYS
            ) {
                dateRange = range.expandedAfter() ?: range
            }
            previousRange = range
            previousFirst = viewport.firstVisible
            previousLast = viewport.lastVisible
        }
    }
    val firstVisibleDay = axisState.firstVisibleItemIndex
    val firstVisibleOffsetPx = axisState.firstVisibleItemScrollOffset

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Quay lại") }
            Text(
                text = "Lịch: $tableName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
        }
        Text(
            text = "Múi giờ Jira: ${timeZoneId ?: "chưa được lưu"}",
            style = MaterialTheme.typography.bodySmall,
        )
        if (!currentProjection.timeZoneValid) {
            CalendarNotice(
                text = "Không xác định được múi giờ Jira của bảng; không thể dựng các ngày lịch.",
                isError = true,
            )
        } else if (windowStart == null || windowEnd == null) {
            CalendarNotice(
                text = "Không xác định được ngày hiện tại để dựng trục lịch.",
                isError = true,
            )
        } else if (!currentProjection.hasValidRanges) {
            val message =
                if (currentProjection.rows.isEmpty()) {
                    "Bảng chưa có ticket nào; cửa sổ ngày vẫn được hiển thị."
                } else {
                    "Không có task nào có Dynamic Start Time và Dynamic Due Date hợp lệ; cửa sổ ngày vẫn được hiển thị."
                }
            CalendarNotice(text = message)
        }
        if (dateRangeTooLarge) {
            CalendarNotice(
                text = "Khoảng ngày vượt quá giới hạn có thể hiển thị của lịch.",
                isError = true,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TicketColumnHeader()
            if (dayCount > 0 && !dateRangeTooLarge) {
                LazyRow(
                    state = axisState,
                    modifier = Modifier.weight(1f).height(HEADER_HEIGHT),
                ) {
                    items(dayCount) { index ->
                        CalendarDayHeader(
                            date = checkNotNull(windowStart).plus(index, DateTimeUnit.DAY),
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f).height(HEADER_HEIGHT))
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().calendarHorizontalInput(axisState),
        ) {
            itemsIndexed(currentProjection.rows, key = { _, row -> row.ticketKey }) { _, row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(ROW_HEIGHT),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TicketCalendarLabel(row = row)
                    if (dayCount > 0) {
                        CalendarTimelineRow(
                            row = row,
                            firstVisibleDay = firstVisibleDay,
                            firstVisibleOffsetPx = firstVisibleOffsetPx,
                            dayCount = dayCount,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f).height(ROW_HEIGHT))
                    }
                }
            }
            if (currentProjection.rows.isEmpty()) {
                item {
                    Text(
                        text = "Bảng đã lưu chưa có ticket nào.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TicketColumnHeader() {
    Box(
        modifier =
            Modifier
                .width(TICKET_COLUMN_WIDTH)
                .height(HEADER_HEIGHT)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = "Mã ticket",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CalendarDayHeader(date: LocalDate) {
    Column(
        modifier =
            Modifier
                .width(DAY_COLUMN_WIDTH)
                .height(HEADER_HEIGHT)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = date.dayOfWeek.vietnameseName(),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
        Text(
            text = date.vietnameseDateLabel(),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun TicketCalendarLabel(row: SavedTableCalendarRow) {
    val statusMessage =
        listOfNotNull(
            row.issue.message(),
            row.outsideWindow?.message(),
            row.overdueOutsideWindow?.let { "Phần quá hạn ${it.message().lowercase()}" },
        ).joinToString(" · ").takeIf(String::isNotEmpty)
    Column(
        modifier =
            Modifier
                .width(TICKET_COLUMN_WIDTH)
                .height(ROW_HEIGHT)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = row.ticketKey,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (statusMessage != null) {
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.labelSmall,
                color =
                    if (row.issue == SavedTableCalendarIssue.INVALID_TIME_ZONE) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CalendarTimelineRow(
    row: SavedTableCalendarRow,
    firstVisibleDay: Int,
    firstVisibleOffsetPx: Int,
    dayCount: Int,
    modifier: Modifier = Modifier,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val barColor = MaterialTheme.colorScheme.primary
    val overdueColor = MaterialTheme.colorScheme.error
    val continuationColor = MaterialTheme.colorScheme.onPrimary
    Canvas(modifier = modifier.height(ROW_HEIGHT)) {
        val visibleTimelineWidth =
            drawCalendarGrid(
                color = gridColor,
                firstVisibleDay = firstVisibleDay,
                firstVisibleOffsetPx = firstVisibleOffsetPx,
                dayCount = dayCount,
            )
        if (
            row.issue == SavedTableCalendarIssue.NONE &&
            row.outsideWindow == null &&
            row.expectedSegments.isNotEmpty()
        ) {
            drawTimelineSegments(
                segments = row.expectedSegments,
                continuesBeforeWindow = row.continuesBeforeWindow,
                continuesAfterWindow = row.continuesAfterWindow,
                firstVisibleDay = firstVisibleDay,
                firstVisibleOffsetPx = firstVisibleOffsetPx,
                visibleTimelineWidth = visibleTimelineWidth,
                color = barColor,
                continuationColor = continuationColor,
            )
        }
        if (row.overdueOutsideWindow == null && row.overdueSegments.isNotEmpty()) {
            drawTimelineSegments(
                segments = row.overdueSegments,
                continuesBeforeWindow = row.overdueContinuesBeforeWindow,
                continuesAfterWindow = row.overdueContinuesAfterWindow,
                firstVisibleDay = firstVisibleDay,
                firstVisibleOffsetPx = firstVisibleOffsetPx,
                visibleTimelineWidth = visibleTimelineWidth,
                color = overdueColor,
                continuationColor = continuationColor,
            )
        }
    }
}

private fun DrawScope.drawTimelineSegments(
    segments: List<CalendarWorkSegment>,
    continuesBeforeWindow: Boolean,
    continuesAfterWindow: Boolean,
    firstVisibleDay: Int,
    firstVisibleOffsetPx: Int,
    visibleTimelineWidth: Float,
    color: Color,
    continuationColor: Color,
) {
    var groupStart = 0
    for (index in 1..segments.size) {
        val isLast = index == segments.size
        val adjacent =
            !isLast &&
                segments[index].dayOffset == segments[index - 1].dayOffset + 1L &&
                segments[index - 1].endFraction == 1.0 &&
                segments[index].startFraction == 0.0
        if (!adjacent) {
            drawTimelineGroup(
                first = segments[groupStart],
                last = segments[index - 1],
                continuesBeforeWindow = continuesBeforeWindow && groupStart == 0,
                continuesAfterWindow = continuesAfterWindow && isLast,
                firstVisibleDay = firstVisibleDay,
                firstVisibleOffsetPx = firstVisibleOffsetPx,
                visibleTimelineWidth = visibleTimelineWidth,
                color = color,
                continuationColor = continuationColor,
            )
            groupStart = index
        }
    }
}

private fun DrawScope.drawTimelineGroup(
    first: CalendarWorkSegment,
    last: CalendarWorkSegment,
    continuesBeforeWindow: Boolean,
    continuesAfterWindow: Boolean,
    firstVisibleDay: Int,
    firstVisibleOffsetPx: Int,
    visibleTimelineWidth: Float,
    color: Color,
    continuationColor: Color,
) {
    val dayWidthPx = DAY_COLUMN_WIDTH.toPx()
    val startX =
        ((first.dayOffset - firstVisibleDay).toFloat() + first.startFraction.toFloat()) * dayWidthPx -
            firstVisibleOffsetPx
    val endX =
        ((last.dayOffset - firstVisibleDay).toFloat() + last.endFraction.toFloat()) * dayWidthPx -
            firstVisibleOffsetPx
    val left = startX.coerceAtLeast(0f)
    val right = endX.coerceAtMost(visibleTimelineWidth)
    if (right <= left) return
    val verticalInset = 10.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(left, verticalInset),
        size = Size(right - left, size.height - verticalInset * 2),
        cornerRadius = CornerRadius(6.dp.toPx()),
    )
    val centerY = size.height / 2f
    if (continuesBeforeWindow && startX in 0f..visibleTimelineWidth) {
        drawContinuationMarker(startX, centerY, pointsRight = false, color = continuationColor)
    }
    if (continuesAfterWindow && endX in 0f..visibleTimelineWidth) {
        drawContinuationMarker(endX, centerY, pointsRight = true, color = continuationColor)
    }
}

private fun DrawScope.drawContinuationMarker(
    x: Float,
    centerY: Float,
    pointsRight: Boolean,
    color: Color,
) {
    val markerLength = 8.dp.toPx()
    val halfHeight = 4.dp.toPx()
    val innerX = if (pointsRight) x - markerLength else x + markerLength
    val strokeWidth = 2.dp.toPx()
    drawLine(
        color = color,
        start = Offset(innerX, centerY - halfHeight),
        end = Offset(x, centerY),
        strokeWidth = strokeWidth,
    )
    drawLine(
        color = color,
        start = Offset(x, centerY),
        end = Offset(innerX, centerY + halfHeight),
        strokeWidth = strokeWidth,
    )
}

private fun DrawScope.drawCalendarGrid(
    color: androidx.compose.ui.graphics.Color,
    firstVisibleDay: Int,
    firstVisibleOffsetPx: Int,
    dayCount: Int,
): Float {
    val dayWidthPx = DAY_COLUMN_WIDTH.toPx()
    val strokeWidth = 1.dp.toPx()
    val remainingDays = (dayCount - firstVisibleDay).coerceAtLeast(0)
    val visibleTimelineWidth =
        (remainingDays * dayWidthPx - firstVisibleOffsetPx)
            .coerceAtLeast(0f)
            .coerceAtMost(size.width)
    var x = -firstVisibleOffsetPx.toFloat()
    while (x <= visibleTimelineWidth) {
        if (x >= 0f) {
            drawLine(
                color = color,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = strokeWidth,
            )
        }
        x += dayWidthPx
    }
    drawLine(
        color = color,
        start = Offset(0f, size.height),
        end = Offset(visibleTimelineWidth, size.height),
        strokeWidth = strokeWidth,
    )
    return visibleTimelineWidth
}

@Composable
private fun CalendarNotice(
    text: String,
    isError: Boolean = false,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

private fun SavedTableCalendarIssue.message(): String? =
    when (this) {
        SavedTableCalendarIssue.NONE -> null
        SavedTableCalendarIssue.MISSING_START -> "Thiếu Start Time"
        SavedTableCalendarIssue.MISSING_DUE -> "Thiếu Due Date"
        SavedTableCalendarIssue.MISSING_BOTH -> "Thiếu Start và Due"
        SavedTableCalendarIssue.INVALID_START -> "Start Time không hợp lệ"
        SavedTableCalendarIssue.INVALID_DUE -> "Due Date không hợp lệ"
        SavedTableCalendarIssue.INVALID_BOTH -> "Hai mốc không hợp lệ"
        SavedTableCalendarIssue.INVALID_TIME_ZONE -> "Múi giờ Jira sai"
        SavedTableCalendarIssue.DUE_BEFORE_START -> "Due Date trước Start"
    }

private fun SavedTableCalendarOutsideWindow.message(): String =
    when (this) {
        SavedTableCalendarOutsideWindow.BEFORE -> "Hoàn toàn trước khung ngày"
        SavedTableCalendarOutsideWindow.AFTER -> "Hoàn toàn sau khung ngày"
    }

private fun kotlinx.datetime.DayOfWeek.vietnameseName(): String =
    when (this) {
        kotlinx.datetime.DayOfWeek.MONDAY -> "Thứ Hai"
        kotlinx.datetime.DayOfWeek.TUESDAY -> "Thứ Ba"
        kotlinx.datetime.DayOfWeek.WEDNESDAY -> "Thứ Tư"
        kotlinx.datetime.DayOfWeek.THURSDAY -> "Thứ Năm"
        kotlinx.datetime.DayOfWeek.FRIDAY -> "Thứ Sáu"
        kotlinx.datetime.DayOfWeek.SATURDAY -> "Thứ Bảy"
        kotlinx.datetime.DayOfWeek.SUNDAY -> "Chủ Nhật"
    }

private fun LocalDate.vietnameseDateLabel(): String =
    "${day.toString().padStart(2, '0')}/${(month.ordinal + 1).toString().padStart(2, '0')}/$year"
