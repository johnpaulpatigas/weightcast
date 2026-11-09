package com.johnpaulpatigas.weightcast.feature.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.core.util.DateTimeUtils
import com.johnpaulpatigas.weightcast.core.util.WeightConverter
import com.johnpaulpatigas.weightcast.domain.model.ChartPoint
import kotlin.math.abs
import kotlin.math.max

@Composable
fun WeightTrendChart(
    chartPoints: List<ChartPoint>,
    displayUnit: WeightUnit,
    modifier: Modifier = Modifier
) {
    var selectedPoint by remember(chartPoints) { mutableStateOf<ChartPoint?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = if (chartPoints.isNotEmpty()) {
                    "Weight trend chart with ${chartPoints.size} measurements"
                } else {
                    "Empty weight trend chart"
                }
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Chart Header Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weight History & Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                LegendItem(color = MaterialTheme.colorScheme.primary, label = "Actual")
                Spacer(modifier = Modifier.width(12.dp))
                LegendItem(color = MaterialTheme.colorScheme.tertiary, label = "Trend")
            }

            // Interactive Tooltip Overlay when a point is selected
            AnimatedVisibility(
                visible = selectedPoint != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                selectedPoint?.let { point ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = DateTimeUtils.formatDate(point.timestamp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                point.actualWeightInKg?.let { actual ->
                                    Text(
                                        text = "Actual: ${WeightConverter.formatDisplay(actual, displayUnit)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                point.smoothedTrendInKg?.let { trend ->
                                    Text(
                                        text = "Trend: ${WeightConverter.formatDisplay(trend, displayUnit)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (chartPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No weight data for selected range",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                InteractiveChartCanvas(
                    chartPoints = chartPoints,
                    displayUnit = displayUnit,
                    selectedPoint = selectedPoint,
                    onPointSelected = { selectedPoint = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.padding(end = 4.dp).height(8.dp).width(8.dp)) {
            drawCircle(color = color)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InteractiveChartCanvas(
    chartPoints: List<ChartPoint>,
    displayUnit: WeightUnit,
    selectedPoint: ChartPoint?,
    onPointSelected: (ChartPoint?) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val trendColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 10.sp
    )

    val pointsInUnit = chartPoints.map { point ->
        val actualConverted = point.actualWeightInKg?.let { WeightConverter.fromCanonicalKg(it, displayUnit) }
        val trendConverted = point.smoothedTrendInKg?.let { WeightConverter.fromCanonicalKg(it, displayUnit) }
        Triple(point, actualConverted, trendConverted)
    }

    val validActuals = pointsInUnit.mapNotNull { it.second }
    val validTrends = pointsInUnit.mapNotNull { it.third }
    val allValues = validActuals + validTrends

    val rawMin = allValues.minOrNull() ?: 0.0
    val rawMax = allValues.maxOrNull() ?: 100.0

    val yMin = max(0.0, rawMin - 1.0)
    val yMax = rawMax + 1.0
    val yRange = max(0.5, yMax - yMin)

    Canvas(
        modifier = modifier.pointerInput(chartPoints) {
            detectTapGestures(
                onTap = { touchOffset ->
                    val width = size.width
                    val paddingLeft = 70f
                    val chartWidth = width - paddingLeft - 20f

                    if (chartPoints.isEmpty()) return@detectTapGestures

                    val minTime = chartPoints.first().timestamp
                    val maxTime = chartPoints.last().timestamp
                    val timeRangeMs = max(1L, maxTime - minTime)

                    val touchX = touchOffset.x
                    val nearest = chartPoints.minByOrNull { point ->
                        val progress = (point.timestamp - minTime).toFloat() / timeRangeMs
                        val px = paddingLeft + (progress * chartWidth)
                        abs(px - touchX)
                    }

                    if (selectedPoint == nearest) {
                        onPointSelected(null)
                    } else {
                        onPointSelected(nearest)
                    }
                }
            )
        }
    ) {
        val width = size.width
        val height = size.height
        val paddingLeft = 70f
        val paddingRight = 20f
        val paddingBottom = 40f
        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingBottom

        // Draw horizontal grid lines & Y-axis labels
        val gridLineCount = 3
        for (i in 0 until gridLineCount) {
            val ratio = i / (gridLineCount - 1).toFloat()
            val yPos = chartHeight - (ratio * chartHeight)
            val gridVal = WeightConverter.roundTo(yMin + (ratio * yRange), 1)

            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, yPos),
                end = Offset(width - paddingRight, yPos),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )

            val textLayoutResult = textMeasurer.measure("$gridVal ${displayUnit.symbol}", style = labelStyle)
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(4f, yPos - (textLayoutResult.size.height / 2f))
            )
        }

        if (pointsInUnit.size == 1) {
            val item = pointsInUnit.first()
            val x = paddingLeft + (chartWidth / 2f)
            val valKg = item.second ?: yMin
            val y = chartHeight - (((valKg - yMin) / yRange).toFloat() * chartHeight)

            drawCircle(color = primaryColor, radius = 6.dp.toPx(), center = Offset(x, y))
            return@Canvas
        }

        val minTime = pointsInUnit.first().first.timestamp
        val maxTime = pointsInUnit.last().first.timestamp
        val timeRangeMs = max(1L, maxTime - minTime)

        fun getX(timestamp: Long): Float {
            val progress = (timestamp - minTime).toFloat() / timeRangeMs
            return paddingLeft + (progress * chartWidth)
        }

        fun getY(valKg: Double): Float {
            val progress = ((valKg - yMin) / yRange).toFloat()
            return chartHeight - (progress * chartHeight)
        }

        // Draw Actual Weight Line (Smooth Bezier Curve)
        val actualPath = Path()
        var prevX = 0f
        var prevY = 0f
        var firstActual = true

        pointsInUnit.forEach { (point, actual, _) ->
            if (actual != null) {
                val x = getX(point.timestamp)
                val y = getY(actual)

                if (firstActual) {
                    actualPath.moveTo(x, y)
                    firstActual = false
                } else {
                    val controlX1 = prevX + (x - prevX) / 2f
                    val controlY1 = prevY
                    val controlX2 = prevX + (x - prevX) / 2f
                    val controlY2 = y
                    actualPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                }
                prevX = x
                prevY = y
            }
        }

        drawPath(
            path = actualPath,
            color = primaryColor,
            style = Stroke(width = 3.5.dp.toPx())
        )

        // Draw Smoothed Trend Line (Dashed)
        val trendPath = Path()
        var firstTrend = true

        pointsInUnit.forEach { (point, _, trend) ->
            if (trend != null) {
                val x = getX(point.timestamp)
                val y = getY(trend)
                if (firstTrend) {
                    trendPath.moveTo(x, y)
                    firstTrend = false
                } else {
                    trendPath.lineTo(x, y)
                }
            }
        }

        drawPath(
            path = trendPath,
            color = trendColor,
            style = Stroke(width = 2.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
        )

        // Draw Actual Data Nodes
        pointsInUnit.forEach { (point, actual, _) ->
            if (actual != null) {
                val x = getX(point.timestamp)
                val y = getY(actual)

                val isSelected = selectedPoint?.timestamp == point.timestamp
                if (isSelected) {
                    // Highlight line & ring
                    drawLine(
                        color = primaryColor.copy(alpha = 0.5f),
                        start = Offset(x, 0f),
                        end = Offset(x, chartHeight),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawCircle(color = primaryColor.copy(alpha = 0.3f), radius = 10.dp.toPx(), center = Offset(x, y))
                    drawCircle(color = primaryColor, radius = 6.dp.toPx(), center = Offset(x, y))
                } else {
                    drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = Offset(x, y))
                }
            }
        }

        // Draw X-axis date labels
        val startDateStr = DateTimeUtils.formatDate(minTime)
        val endDateStr = DateTimeUtils.formatDate(maxTime)

        val startTextResult = textMeasurer.measure(startDateStr, style = labelStyle)
        drawText(startTextResult, topLeft = Offset(paddingLeft, height - startTextResult.size.height))

        val endTextResult = textMeasurer.measure(endDateStr, style = labelStyle)
        drawText(endTextResult, topLeft = Offset(width - paddingRight - endTextResult.size.width, height - endTextResult.size.height))
    }
}
