package com.jiratools.ui.saved

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jiratools.domain.BurndownPoint
import com.jiratools.domain.CategoryCount
import com.jiratools.domain.SavedTable
import com.jiratools.domain.SavedTableChartCalculator
import com.jiratools.domain.SavedTableChartField
import com.jiratools.domain.SavedTableChartType
import com.jiratools.domain.UpdatedTicketsByDayPoint
import com.jiratools.platform.AppClock

@Composable
fun SavedTableChartsArea(
    table: SavedTable,
    chartType: SavedTableChartType,
    chartField: SavedTableChartField,
    clock: AppClock,
    onChartTypeChange: (SavedTableChartType) -> Unit,
    onChartFieldChange: (SavedTableChartField) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val categoryProjection =
        remember(table.tickets, chartField) {
            SavedTableChartCalculator.categoryCounts(table.tickets, chartField)
        }
    val updatedProjection =
        remember(table.tickets, table.jiraTimeZone) {
            SavedTableChartCalculator.updatedTicketsByDay(table.tickets, table.jiraTimeZone)
        }
    val burndownProjection =
        remember(table.burndownSnapshots) {
            SavedTableChartCalculator.burndown(table.burndownSnapshots)
        }
    val colors = chartColors()

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Đồ thị • ${table.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onBack) { Text("Quay lại") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            SavedTableChartType.entries.take(2).forEach { type ->
                ChartTypeChip(type, selected = chartType == type, onClick = { onChartTypeChange(type) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            SavedTableChartType.entries.drop(2).forEach { type ->
                ChartTypeChip(type, selected = chartType == type, onClick = { onChartTypeChange(type) })
            }
        }
        if (chartType == SavedTableChartType.PIE || chartType == SavedTableChartType.COLUMN) {
            Text("Nhóm theo", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SavedTableChartField.entries.forEach { field ->
                    FilterChip(
                        selected = chartField == field,
                        onClick = { onChartFieldChange(field) },
                        label = { Text(field.label()) },
                    )
                }
            }
        }
        Text(
            chartType.description(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        when (chartType) {
            SavedTableChartType.PIE,
            SavedTableChartType.COLUMN,
            -> {
                if (table.tickets.isEmpty()) {
                    EmptyChartMessage("Bảng chưa có ticket để trực quan hoá.")
                } else {
                    when (chartType) {
                        SavedTableChartType.PIE -> PieChart(categoryProjection.counts, colors)
                        SavedTableChartType.COLUMN -> ColumnChart(categoryProjection.counts, colors)
                    }
                    CategoryLegend(categoryProjection.counts, colors)
                    Text(
                        "Tổng: ${categoryProjection.totalCount} ticket",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            SavedTableChartType.LINE -> {
                if (updatedProjection.points.isEmpty()) {
                    EmptyChartMessage(
                        if (table.tickets.isEmpty()) {
                            "Bảng chưa có ticket để trực quan hoá."
                        } else {
                            "Không có ticket nào có thời điểm Updated hợp lệ."
                        },
                    )
                } else {
                    LineChart(updatedProjection.points, colors.first())
                    LegendItem(colors.first(), "Số ticket được cập nhật theo ngày")
                    updatedProjection.points.forEach { point ->
                        Text(
                            "${point.date}: ${point.ticketCount} ticket",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (updatedProjection.missingUpdatedCount > 0) {
                        Text(
                            "Không có Updated hợp lệ: ${updatedProjection.missingUpdatedCount} ticket.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (updatedProjection.usedUtcFallback) {
                        Text(
                            "Không có múi giờ Jira hợp lệ; ngày được nhóm theo UTC.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            SavedTableChartType.BURNDOWN -> {
                val points = burndownProjection.points
                if (points.isEmpty()) {
                    EmptyChartMessage("Chưa có lịch sử burndown. Lịch sử bắt đầu khi bảng được lưu thành công.")
                } else if (table.tickets.isEmpty() && points.singleOrNull()?.ticketCount == 0) {
                    EmptyChartMessage("Bảng chưa có ticket để trực quan hoá.")
                } else {
                    BurndownChart(points, colors.first())
                    LegendItem(colors.first(), "Ticket chưa hoàn tất")
                    points.forEach { point -> BurndownLegendItem(point, clock) }
                    if (points.size == 1) {
                        Text(
                            "Lịch sử mới bắt đầu; hiện chỉ có baseline.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartTypeChip(
    type: SavedTableChartType,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(type.label()) },
    )
}

@Composable
private fun CategoryLegend(
    counts: List<CategoryCount>,
    colors: List<Color>,
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        counts.forEachIndexed { index, count ->
            LegendItem(color = colors[index % colors.size], label = "${count.label}: ${count.ticketCount}")
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Canvas(modifier = Modifier.padding(end = 8.dp).size(12.dp)) {
            drawRect(color = color)
        }
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PieChart(
    counts: List<CategoryCount>,
    colors: List<Color>,
) {
    val total = counts.sumOf(CategoryCount::ticketCount).toFloat()
    Canvas(modifier = Modifier.fillMaxWidth().height(220.dp).padding(8.dp)) {
        var startAngle = -90f
        val diameter = size.minDimension
        val topLeft =
            androidx.compose.ui.geometry
                .Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val chartSize =
            androidx.compose.ui.geometry
                .Size(diameter, diameter)
        counts.forEachIndexed { index, count ->
            val sweep = count.ticketCount / total * 360f
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = topLeft,
                size = chartSize,
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun ColumnChart(
    counts: List<CategoryCount>,
    colors: List<Color>,
) {
    Canvas(modifier = Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 8.dp, vertical = 16.dp)) {
        val max = counts.maxOfOrNull(CategoryCount::ticketCount)?.toFloat()?.takeIf { it > 0f } ?: return@Canvas
        val gap = size.width / (counts.size * 5f)
        val barWidth = (size.width - gap * (counts.size + 1)) / counts.size
        counts.forEachIndexed { index, count ->
            val height = size.height * count.ticketCount / max
            drawRect(
                color = colors[index % colors.size],
                topLeft =
                    androidx.compose.ui.geometry
                        .Offset(gap + index * (barWidth + gap), size.height - height),
                size =
                    androidx.compose.ui.geometry
                        .Size(barWidth, height),
            )
        }
    }
}

@Composable
private fun LineChart(
    points: List<UpdatedTicketsByDayPoint>,
    color: Color,
) {
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp).padding(8.dp)) {
        val maxCount =
            points.maxOfOrNull(UpdatedTicketsByDayPoint::ticketCount)?.toFloat()?.coerceAtLeast(1f) ?: return@Canvas
        val firstDay = points.first().date.toEpochDays()
        val dayRange = (points.last().date.toEpochDays() - firstDay).toFloat()
        val positions =
            points.map { point ->
                androidx.compose.ui.geometry.Offset(
                    x =
                        if (dayRange ==
                            0f
                        ) {
                            size.width / 2f
                        } else {
                            (point.date.toEpochDays() - firstDay) / dayRange * size.width
                        },
                    y = size.height - point.ticketCount / maxCount * size.height,
                )
            }
        positions.zipWithNext().forEach { (start, end) ->
            drawLine(color = color, start = start, end = end, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
        positions.forEach { position -> drawCircle(color = color, radius = 5.dp.toPx(), center = position) }
    }
}

@Composable
private fun BurndownChart(
    points: List<BurndownPoint>,
    color: Color,
) {
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp).padding(8.dp)) {
        val maxCount = points.maxOfOrNull(BurndownPoint::ticketCount)?.toFloat()?.coerceAtLeast(1f) ?: return@Canvas
        val xStep = if (points.size <= 1) 0f else size.width / (points.size - 1)
        val positions =
            points.mapIndexed { index, point ->
                androidx.compose.ui.geometry.Offset(
                    x = index * xStep,
                    y = size.height - point.remainingCount / maxCount * size.height,
                )
            }
        positions.zipWithNext().forEach { (start, end) ->
            drawLine(color = color, start = start, end = end, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
        positions.forEach { position -> drawCircle(color = color, radius = 5.dp.toPx(), center = position) }
    }
}

@Composable
private fun BurndownLegendItem(
    point: BurndownPoint,
    clock: AppClock,
) {
    Text(
        "${clock.format(point.recordedAtMillis)}: ${point.remainingCount}/${point.ticketCount} còn lại" +
            if (point.unknownCategoryCount > 0) " (${point.unknownCategoryCount} chưa rõ trạng thái)" else "",
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun EmptyChartMessage(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun chartColors(): List<Color> {
    val scheme = MaterialTheme.colorScheme
    return listOf(
        scheme.primary,
        scheme.secondary,
        scheme.tertiary,
        scheme.error,
        scheme.primaryContainer,
        scheme.secondaryContainer,
    )
}

private fun SavedTableChartType.label(): String =
    when (this) {
        SavedTableChartType.PIE -> "Pie"
        SavedTableChartType.COLUMN -> "Column"
        SavedTableChartType.LINE -> "Line"
        SavedTableChartType.BURNDOWN -> "Burndown"
    }

private fun SavedTableChartType.description(): String =
    when (this) {
        SavedTableChartType.PIE -> "Phân bố ticket theo nhóm"
        SavedTableChartType.COLUMN -> "Số ticket trong từng nhóm"
        SavedTableChartType.LINE -> "Số ticket được cập nhật theo ngày Updated"
        SavedTableChartType.BURNDOWN -> "Số ticket chưa hoàn tất tại mỗi lần lưu/làm mới"
    }

private fun SavedTableChartField.label(): String =
    when (this) {
        SavedTableChartField.STATUS -> "Status"
        SavedTableChartField.ASSIGNEE -> "Assignee"
        SavedTableChartField.PRIORITY -> "Priority"
    }
