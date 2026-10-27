package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.HourlyPoint
import com.example.domain.model.ReadinessStatus
import com.example.domain.model.SolarPhaseWindows
import com.example.ui.TelemetryChartMetric
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.TelemetryCaution
import com.example.ui.theme.TelemetryEmerald
import com.example.ui.theme.TelemetryHoldCoral
import java.util.Locale

@Composable
fun statusAccentColor(status: ReadinessStatus): Color = when (status) {
    ReadinessStatus.GO -> TelemetryEmerald
    ReadinessStatus.CAUTION -> TelemetryCaution
    ReadinessStatus.HOLD -> TelemetryHoldCoral
}

@Composable
fun ReadinessScoreGauge(
    score: Int,
    status: ReadinessStatus,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (score.coerceIn(0, 100)) / 100f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "readiness_score_arc"
    )
    val ringColor = statusAccentColor(status)
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 10.dp.toPx()
            val arcDiameter = minOf(this.size.width, this.size.height) - strokePx
            val topLeft = Offset(
                (this.size.width - arcDiameter) / 2f,
                (this.size.height - arcDiameter) / 2f
            )
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcDiameter, arcDiameter),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcDiameter, arcDiameter),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = status.name,
                style = MaterialTheme.typography.labelSmall,
                color = ringColor
            )
        }
    }
}

@Composable
fun HourlyTelemetryCanvasChart(
    hourlyPoints: List<HourlyPoint>,
    metric: TelemetryChartMetric,
    modifier: Modifier = Modifier
) {
    if (hourlyPoints.isEmpty()) return

    val values = hourlyPoints.map { pt ->
        when (metric) {
            TelemetryChartMetric.TEMPERATURE -> pt.tempC.toFloat()
            TelemetryChartMetric.UV_INDEX -> pt.uvIndex.toFloat()
            TelemetryChartMetric.WIND_SPEED -> pt.windKmh.toFloat()
            TelemetryChartMetric.PRECIP_PROB -> pt.precipProbPercent.toFloat()
        }
    }

    val lineColor = when (metric) {
        TelemetryChartMetric.TEMPERATURE -> ElectricCyan
        TelemetryChartMetric.UV_INDEX -> SolarAmber
        TelemetryChartMetric.WIND_SPEED -> TelemetryEmerald
        TelemetryChartMetric.PRECIP_PROB -> Color(0xFF818CF8)
    }

    val minVal = (values.minOrNull() ?: 0f)
    val maxVal = (values.maxOrNull() ?: 1f).let { if (it <= minVal) minVal + 1f else it }
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = String.format(Locale.US, "Peak: %.1f", maxVal),
                style = MaterialTheme.typography.labelSmall,
                color = lineColor
            )
            Text(
                text = String.format(Locale.US, "Low: %.1f", minVal),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
        ) {
            val w = size.width
            val h = size.height
            val padVertical = 14.dp.toPx()
            val usableH = (h - padVertical * 2f).coerceAtLeast(1f)

            // Draw 3 horizontal reference lines
            for (i in 0..2) {
                val y = padVertical + (usableH * i / 2f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val stepX = if (values.size > 1) w / (values.size - 1) else w
            val points = values.mapIndexed { index, v ->
                val normalized = (v - minVal) / (maxVal - minVal)
                val x = index * stepX
                val y = padVertical + usableH * (1f - normalized)
                Offset(x, y)
            }

            val linePath = Path().apply {
                points.forEachIndexed { idx, pt ->
                    if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                }
            }

            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(points.last().x, h)
                lineTo(points.first().x, h)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent),
                    startY = 0f,
                    endY = h
                )
            )

            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            points.forEachIndexed { idx, pt ->
                if (idx % 4 == 0 || idx == points.lastIndex) {
                    drawCircle(
                        color = lineColor,
                        radius = 4.dp.toPx(),
                        center = pt
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val labels = listOf(0, 6, 12, 18, 23).mapNotNull { idx ->
                hourlyPoints.getOrNull(idx)?.hourLabel
            }
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SolarHorizonCardContent(
    solar: SolarPhaseWindows,
    uvIndex: Double,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
        ) {
            val w = size.width
            val h = size.height
            val horizonY = h - 14.dp.toPx()

            drawLine(
                color = ElectricCyan.copy(alpha = 0.4f),
                start = Offset(0f, horizonY),
                end = Offset(w, horizonY),
                strokeWidth = 1.5.dp.toPx()
            )

            val arcWidth = w * 0.84f
            val arcLeft = (w - arcWidth) / 2f
            drawArc(
                brush = Brush.horizontalGradient(
                    colors = listOf(ElectricCyan, SolarAmber, ElectricCyan)
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(arcLeft, 12.dp.toPx()),
                size = Size(arcWidth, (horizonY - 12.dp.toPx()) * 2f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Solar orb at apex
            val sunCenter = Offset(w / 2f, 14.dp.toPx())
            drawCircle(
                color = SolarAmber.copy(alpha = 0.28f),
                radius = 16.dp.toPx(),
                center = sunCenter
            )
            drawCircle(
                color = SolarAmber,
                radius = 7.dp.toPx(),
                center = sunCenter
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SolarMetricPill("SUNRISE", solar.sunriseTime, ElectricCyan)
            SolarMetricPill("SOLAR NOON", solar.solarNoonTime, SolarAmber)
            SolarMetricPill("DAYLIGHT", solar.daylightDurationLabel, TelemetryEmerald)
            SolarMetricPill("SUNSET", solar.sunsetTime, ElectricCyan)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = "AM GOLDEN HOUR",
                        style = MaterialTheme.typography.labelSmall,
                        color = SolarAmber
                    )
                    Text(
                        text = solar.morningGoldenHour,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Blue: ${solar.morningBlueHour}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = "PM GOLDEN HOUR",
                        style = MaterialTheme.typography.labelSmall,
                        color = SolarAmber
                    )
                    Text(
                        text = solar.eveningGoldenHour,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Blue: ${solar.eveningBlueHour}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SolarMetricPill(
    label: String,
    value: String,
    accent: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = accent,
            fontWeight = FontWeight.Bold
        )
    }
}
