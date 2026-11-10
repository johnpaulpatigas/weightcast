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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.johnpaulpatigas.weightcast.core.util.DateTimeUtils
import com.johnpaulpatigas.weightcast.core.util.WeightConverter
import com.johnpaulpatigas.weightcast.core.util.formatKg
import com.johnpaulpatigas.weightcast.domain.model.ChartPoint
import kotlin.math.abs
import kotlin.math.max

@Composable
fun WeightTrendChart(
    chartPoints: List<ChartPoint>,
    modifier: Modifier = Modifier
) {
    var selectedPoint by remember(chartPoints) { mutableStateOf<ChartPoint?>(null) }

    val hasTrends = chartPoints.any { it.smoothedTrendInKg != null }
    val hasForecasts = chartPoints.any { it.forecastWeightInKg != null }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = if (chartPoints.isNotEmpty()) {
                    "Weight trend chart with ${chartPoints.size} measurements and forecasts"
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
                    text = "History, Trend & Forecast",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                LegendItem(color = MaterialTheme.colorScheme.primary, label = "Actual")
                if (hasTrends) {
                    Spacer(modifier = Modifier.width(10.dp))
                    LegendItem(color = MaterialTheme.colorScheme.tertiary, label = "Trend")
                }
                if (hasForecasts) {
                    Spacer(modifier = Modifier.width(10.dp))
                    LegendItem(color = MaterialTheme.colorScheme.secondary, label = "Forecast")
                }
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

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                point.actualWeightInKg?.let { actual ->
                                    Text(
                                        text = "Actual: ${actual.formatKg()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                point.smoothedTrendInKg?.let { trend ->
                                    Text(
                                        text = "Trend: ${trend.formatKg()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                point.forecastWeightInKg?.let { forecast ->
                                    Text(
                                        text = "Forecast: ${forecast.formatKg()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
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
    selectedPoint: ChartPoint?,
    onPointSelected: (ChartPoint?) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val trendColor = MaterialTheme.colorScheme.tertiary
    val forecastColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val intervalBandColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 10.sp
    )

    val validActuals = chartPoints.mapNotNull { it.actualWeightInKg }
    val validTrends = chartPoints.mapNotNull { it.smoothedTrendInKg }
    val validForecasts = chartPoints.mapNotNull { it.forecastWeightInKg }
    val validLowers = chartPoints.mapNotNull { it.lowerBoundInKg }
    val validUppers = chartPoints.mapNotNull { it.upperBoundInKg }

    val allValues = validActuals + validTrends + validForecasts + validLowers + validUppers

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
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
            val gridVal = WeightConverter.roundTo(yMin + (ratio * yRange), 2)

            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, yPos),
                end = Offset(width - paddingRight, yPos),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )

            val textLayoutResult = textMeasurer.measure("$gridVal kg", style = labelStyle)
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(4f, yPos - (textLayoutResult.size.height / 2f))
            )
        }

        if (chartPoints.size == 1) {
            val item = chartPoints.first()
            val x = paddingLeft + (chartWidth / 2f)
            val valKg = item.actualWeightInKg ?: item.forecastWeightInKg ?: yMin
            val y = chartHeight - (((valKg - yMin) / yRange).toFloat() * chartHeight)

            drawCircle(color = primaryColor, radius = 6.dp.toPx(), center = Offset(x, y))
            return@Canvas
        }

        val minTime = chartPoints.first().timestamp
        val maxTime = chartPoints.last().timestamp
        val timeRangeMs = max(1L, maxTime - minTime)

        fun getX(timestamp: Long): Float {
            val progress = (timestamp - minTime).toFloat() / timeRangeMs
            return paddingLeft + (progress * chartWidth)
        }

        fun getY(valKg: Double): Float {
            val progress = ((valKg - yMin) / yRange).toFloat()
            return chartHeight - (progress * chartHeight)
        }

        // 1. Draw Forecast Shaded Prediction Interval Region (Upper to Lower bounds)
        val forecastPts = chartPoints.filter { it.forecastWeightInKg != null }
        if (forecastPts.size >= 2) {
            val intervalPath = Path()
            var firstBound = true

            // Forward along Upper Bound
            forecastPts.forEach { pt ->
                val upperKg = pt.upperBoundInKg ?: pt.forecastWeightInKg!!
                val x = getX(pt.timestamp)
                val y = getY(upperKg)
                if (firstBound) {
                    intervalPath.moveTo(x, y)
                    firstBound = false
                } else {
                    intervalPath.lineTo(x, y)
                }
            }

            // Backward along Lower Bound
            forecastPts.reversed().forEach { pt ->
                val lowerKg = pt.lowerBoundInKg ?: pt.forecastWeightInKg!!
                val x = getX(pt.timestamp)
                val y = getY(lowerKg)
                intervalPath.lineTo(x, y)
            }
            intervalPath.close()

            drawPath(path = intervalPath, color = intervalBandColor)
        }

        // 2. Draw Forecast Trajectory Path (Dotted)
        if (forecastPts.size >= 2) {
            val forecastPath = Path()
            var firstForecast = true

            forecastPts.forEach { pt ->
                val fKg = pt.forecastWeightInKg!!
                val x = getX(pt.timestamp)
                val y = getY(fKg)
                if (firstForecast) {
                    forecastPath.moveTo(x, y)
                    firstForecast = false
                } else {
                    forecastPath.lineTo(x, y)
                }
            }

            drawPath(
                path = forecastPath,
                color = forecastColor,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
            )

            // Draw forecast nodes
            forecastPts.forEach { pt ->
                if (pt.forecastWeightInKg != null && pt.actualWeightInKg == null) {
                    val x = getX(pt.timestamp)
                    val y = getY(pt.forecastWeightInKg)
                    drawCircle(color = forecastColor, radius = 4.dp.toPx(), center = Offset(x, y))
                }
            }
        }

        // 3. Build Historical Actual Weight Path
        val actualPath = Path()
        var prevX = 0f
        var prevY = 0f
        var firstActual = true

        chartPoints.forEach { point ->
            val actual = point.actualWeightInKg
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

        // Draw Fill Gradient under Actual Path
        if (!firstActual) {
            val fillPath = Path().apply {
                addPath(actualPath)
                lineTo(prevX, chartHeight)
                lineTo(paddingLeft, chartHeight)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f),
                        primaryColor.copy(alpha = 0.0f)
                    ),
                    startY = 0f,
                    endY = chartHeight
                )
            )
        }

        // Draw Actual Weight Line (Smooth Bezier Curve)
        drawPath(
            path = actualPath,
            color = primaryColor,
            style = Stroke(width = 3.5.dp.toPx())
        )

        // 4. Draw Smoothed Statistical Trend Line (Dashed)
        val trendPath = Path()
        var firstTrend = true

        chartPoints.forEach { point ->
            val trend = point.smoothedTrendInKg
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

        if (!firstTrend) {
            drawPath(
                path = trendPath,
                color = trendColor,
                style = Stroke(width = 2.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
            )
        }

        // 5. Draw Data Nodes & Selection Highlight
        chartPoints.forEach { point ->
            val nodeVal = point.actualWeightInKg ?: point.forecastWeightInKg
            if (nodeVal != null) {
                val x = getX(point.timestamp)
                val y = getY(nodeVal)
                val nodeColor = if (point.actualWeightInKg != null) primaryColor else forecastColor

                val isSelected = selectedPoint?.timestamp == point.timestamp
                if (isSelected) {
                    drawLine(
                        color = nodeColor.copy(alpha = 0.5f),
                        start = Offset(x, 0f),
                        end = Offset(x, chartHeight),
                        strokeWidth = 2.dp.toPx()
                    )
                    drawCircle(color = nodeColor.copy(alpha = 0.3f), radius = 10.dp.toPx(), center = Offset(x, y))
                    drawCircle(color = nodeColor, radius = 6.dp.toPx(), center = Offset(x, y))
                } else if (point.actualWeightInKg != null) {
                    drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = Offset(x, y))
                }
            }
        }

        // Draw X-axis Date Labels
        val startDateStr = DateTimeUtils.formatDate(minTime)
        val endDateStr = DateTimeUtils.formatDate(maxTime)

        val startTextResult = textMeasurer.measure(startDateStr, style = labelStyle)
        drawText(startTextResult, topLeft = Offset(paddingLeft, height - startTextResult.size.height))

        val endTextResult = textMeasurer.measure(endDateStr, style = labelStyle)
        drawText(endTextResult, topLeft = Offset(width - paddingRight - endTextResult.size.width, height - endTextResult.size.height))
    }
}
