package com.johnpaulpatigas.weightcast.feature.dashboard.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.johnpaulpatigas.weightcast.core.model.WeightUnit
import com.johnpaulpatigas.weightcast.core.util.DateTimeUtils
import com.johnpaulpatigas.weightcast.core.util.WeightConverter
import com.johnpaulpatigas.weightcast.domain.model.ChartPoint
import kotlin.math.max
import kotlin.math.min

@Composable
fun WeightTrendChart(
    chartPoints: List<ChartPoint>,
    displayUnit: WeightUnit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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

                // Legend Items
                LegendItem(color = MaterialTheme.colorScheme.primary, label = "Actual Weight")
                Spacer(modifier = Modifier.width(12.dp))
                LegendItem(color = MaterialTheme.colorScheme.tertiary, label = "Smoothed Trend")
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
                ChartCanvas(
                    chartPoints = chartPoints,
                    displayUnit = displayUnit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
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
private fun ChartCanvas(
    chartPoints: List<ChartPoint>,
    displayUnit: WeightUnit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val trendColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    // Convert weights to target display unit
    val pointsInUnit = chartPoints.map { point ->
        val actualConverted = point.actualWeightInKg?.let { WeightConverter.fromCanonicalKg(it, displayUnit) }
        val trendConverted = point.smoothedTrendInKg?.let { WeightConverter.fromCanonicalKg(it, displayUnit) }
        Triple(point.timestamp, actualConverted, trendConverted)
    }

    val validActuals = pointsInUnit.mapNotNull { it.second }
    val validTrends = pointsInUnit.mapNotNull { it.third }
    val allValues = validActuals + validTrends

    val rawMin = allValues.minOrNull() ?: 0.0
    val rawMax = allValues.maxOrNull() ?: 100.0

    // Add padding to Y-axis range
    val yMin = max(0.0, rawMin - 1.0)
    val yMax = rawMax + 1.0
    val yRange = max(0.5, yMax - yMin)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingLeft = 60f
        val paddingBottom = 40f
        val chartWidth = width - paddingLeft
        val chartHeight = height - paddingBottom

        // Draw horizontal grid lines (3 lines)
        val gridLineCount = 3
        for (i in 0 until gridLineCount) {
            val ratio = i / (gridLineCount - 1).toFloat()
            val yPos = chartHeight - (ratio * chartHeight)
            val gridVal = WeightConverter.roundTo(yMin + (ratio * yRange), 1)

            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, yPos),
                end = Offset(width, yPos),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
        }

        if (pointsInUnit.size == 1) {
            // Single point rendering
            val item = pointsInUnit.first()
            val x = paddingLeft + (chartWidth / 2f)
            val valKg = item.second ?: yMin
            val y = chartHeight - (((valKg - yMin) / yRange).toFloat() * chartHeight)

            drawCircle(color = primaryColor, radius = 6.dp.toPx(), center = Offset(x, y))
            return@Canvas
        }

        val minTime = pointsInUnit.first().first
        val maxTime = pointsInUnit.last().first
        val timeRangeMs = max(1L, maxTime - minTime)

        fun getX(timestamp: Long): Float {
            val progress = (timestamp - minTime).toFloat() / timeRangeMs
            return paddingLeft + (progress * chartWidth)
        }

        fun getY(valKg: Double): Float {
            val progress = ((valKg - yMin) / yRange).toFloat()
            return chartHeight - (progress * chartHeight)
        }

        // Draw Actual Weight Line & Points
        val actualPath = Path()
        var firstActual = true

        pointsInUnit.forEach { (ts, actual, _) ->
            if (actual != null) {
                val x = getX(ts)
                val y = getY(actual)
                if (firstActual) {
                    actualPath.moveTo(x, y)
                    firstActual = false
                } else {
                    actualPath.lineTo(x, y)
                }
            }
        }

        drawPath(
            path = actualPath,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw Smoothed Trend Line
        val trendPath = Path()
        var firstTrend = true

        pointsInUnit.forEach { (ts, _, trend) ->
            if (trend != null) {
                val x = getX(ts)
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
            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
        )

        // Draw data points for actual weight
        pointsInUnit.forEach { (ts, actual, _) ->
            if (actual != null) {
                val x = getX(ts)
                val y = getY(actual)
                drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = Offset(x, y))
            }
        }
    }
}
