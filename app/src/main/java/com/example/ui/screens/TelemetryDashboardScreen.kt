package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.usecase.AtmosphericCalculator
import com.example.ui.AeroPulseUiState
import com.example.ui.TelemetryChartMetric
import com.example.ui.components.HourlyTelemetryCanvasChart
import com.example.ui.components.ReadinessScoreGauge
import com.example.ui.components.SolarHorizonCardContent
import com.example.ui.components.statusAccentColor
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ObsidianNight
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.TelemetryCaution
import com.example.ui.theme.TelemetryEmerald
import com.example.ui.theme.TelemetryHoldCoral
import java.util.Locale

@Composable
fun TelemetryDashboardScreen(
    uiState: AeroPulseUiState,
    onSyncLiveTelemetry: () -> Unit,
    onSelectStation: (Long) -> Unit,
    onSelectChartMetric: (TelemetryChartMetric) -> Unit,
    onSaveFieldNotes: (Long, String) -> Unit,
    onDismissBanner: () -> Unit,
    onNavigateToWindows: () -> Unit,
    modifier: Modifier = Modifier
) {
    val station = uiState.primaryStation
    val prefs = uiState.preferences

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("telemetry_dashboard_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        if (station == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Box
        }

        var isEditingNotes by remember(station.id) { mutableStateOf(false) }
        var notesDraft by remember(station.id, station.fieldNotes) { mutableStateOf(station.fieldNotes) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Top Bar & Live Sync Controls
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AEROPULSE // FIELD TELEMETRY",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                        Text(
                            text = station.name,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.testTag("active_station_title")
                        )
                        Text(
                            text = "${station.region}, ${station.country} • ${station.elevationMeters} m ASL",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onSyncLiveTelemetry,
                        enabled = !uiState.isSyncing,
                        modifier = Modifier.testTag("sync_telemetry_button")
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync Live Telemetry",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(if (uiState.isSyncing) "Syncing" else "Live Sync")
                    }
                }
            }

            // 2. Status Banner (Success or Offline Warning)
            item {
                AnimatedVisibility(visible = uiState.statusBannerMessage != null) {
                    val bannerMsg = uiState.statusBannerMessage.orEmpty()
                    val bannerColor = if (uiState.isErrorBanner) TelemetryCaution else TelemetryEmerald
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = bannerColor.copy(alpha = 0.16f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("status_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = bannerMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onDismissBanner,
                                modifier = Modifier.testTag("dismiss_banner_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss message"
                                )
                            }
                        }
                    }
                }
            }

            // 3. Quick Station Switcher Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.stations.forEach { itemStation ->
                        FilterChip(
                            selected = itemStation.id == station.id,
                            onClick = { onSelectStation(itemStation.id) },
                            label = {
                                Text("${itemStation.name} (${itemStation.elevationMeters}m)")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("station_chip_${itemStation.id}")
                        )
                    }
                }
            }

            // 4. Hero Atmospheric Telemetry Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianNight)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_atmosphere),
                            contentDescription = "Atmospheric horizon banner",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            ObsidianNight.copy(alpha = 0.55f),
                                            ObsidianNight.copy(alpha = 0.92f)
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column {
                                        Text(
                                            text = AtmosphericCalculator.weatherConditionLabel(station.weatherCode).uppercase(),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = SolarAmber
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = AtmosphericCalculator.formatTemperature(
                                                station.temperatureC,
                                                prefs.tempUnit
                                            ),
                                            style = MaterialTheme.typography.displayLarge,
                                            color = Color.White,
                                            modifier = Modifier.testTag("hero_temperature_text")
                                        )
                                        Text(
                                            text = "Feels like ${
                                                AtmosphericCalculator.formatTemperature(
                                                    station.apparentTempC,
                                                    prefs.tempUnit
                                                )
                                            } • Dew Point ${
                                                AtmosphericCalculator.formatTemperature(
                                                    uiState.dewPointC,
                                                    prefs.tempUnit
                                                )
                                            }",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFCBD5E1)
                                        )
                                    }

                                    val topEval = uiState.evaluations.firstOrNull()
                                    if (topEval != null) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable { onNavigateToWindows() }
                                                .padding(4.dp)
                                                .testTag("hero_readiness_gauge")
                                        ) {
                                            ReadinessScoreGauge(
                                                score = topEval.currentScore,
                                                status = topEval.status,
                                                size = 82.dp
                                            )
                                            Text(
                                                text = topEval.profile.category.badgeCode,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFE2E8F0)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    HeroQuickStat(
                                        label = "WIND (${AtmosphericCalculator.compassDirection(station.windDirectionDeg)})",
                                        value = AtmosphericCalculator.formatWindSpeed(station.windSpeedKmh, prefs.windUnit)
                                    )
                                    HeroQuickStat(
                                        label = "PEAK GUST",
                                        value = AtmosphericCalculator.formatWindSpeed(station.windGustsKmh, prefs.windUnit)
                                    )
                                    HeroQuickStat(
                                        label = "QFE PRESSURE",
                                        value = AtmosphericCalculator.formatPressure(station.surfacePressureHpa, prefs.pressureUnit)
                                    )
                                    HeroQuickStat(
                                        label = "US AQI",
                                        value = "${station.usAqi}"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. 24-Hour Multi-Metric Telemetry Curve Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "24-HOUR TELEMETRY VECTOR",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricCyan
                                )
                                Text(
                                    text = "Diurnal Atmospheric Curve",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TelemetryChartMetric.entries.forEach { metric ->
                                FilterChip(
                                    selected = uiState.selectedChartMetric == metric,
                                    onClick = { onSelectChartMetric(metric) },
                                    label = { Text(metric.label) },
                                    modifier = Modifier.testTag("chart_metric_${metric.name}")
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        HourlyTelemetryCanvasChart(
                            hourlyPoints = station.hourlyPoints,
                            metric = uiState.selectedChartMetric
                        )
                    }
                }
            }

            // 6. Precision Atmospheric Metric Grid (6 Cards)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TelemetryGridCard(
                            title = "UV RADIATION",
                            value = String.format(Locale.US, "%.1f", station.uvIndex),
                            subtitle = uvCategoryLabel(station.uvIndex),
                            icon = Icons.Default.WbSunny,
                            accent = SolarAmber,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryGridCard(
                            title = "AIR QUALITY (US)",
                            value = "AQI ${station.usAqi}",
                            subtitle = "PM2.5: ${station.pm25} µg/m³",
                            icon = Icons.Default.Air,
                            accent = aqiAccentColor(station.usAqi),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TelemetryGridCard(
                            title = "SEA-LEVEL QNH",
                            value = AtmosphericCalculator.formatPressure(
                                station.seaLevelPressureHpa,
                                prefs.pressureUnit
                            ),
                            subtitle = "Surface: ${
                                AtmosphericCalculator.formatPressure(
                                    station.surfacePressureHpa,
                                    prefs.pressureUnit
                                )
                            }",
                            icon = Icons.Default.Speed,
                            accent = ElectricCyan,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryGridCard(
                            title = "HUMIDITY & PRECIP",
                            value = "${station.humidityPercent}% RH",
                            subtitle = "Precip Risk: ${station.precipProbabilityPercent}%",
                            icon = Icons.Default.WaterDrop,
                            accent = ElectricCyan,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TelemetryGridCard(
                            title = "VISIBILITY RANGE",
                            value = String.format(Locale.US, "%.1f km", station.visibilityMeters / 1000.0),
                            subtitle = "Optical Horizon",
                            icon = Icons.Default.Visibility,
                            accent = TelemetryEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryGridCard(
                            title = "CLOUD CANOPY",
                            value = "${station.cloudCoverPercent}%",
                            subtitle = String.format(
                                Locale.US,
                                "Coords: %.2f°, %.2f°",
                                station.latitude,
                                station.longitude
                            ),
                            icon = Icons.Default.CloudQueue,
                            accent = SolarAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 7. Solar Ephemeris & Golden/Blue Hour Card
            uiState.solarWindows?.let { solar ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "SOLAR EPHEMERIS // GOLDEN & BLUE HOURS",
                                style = MaterialTheme.typography.labelMedium,
                                color = SolarAmber
                            )
                            Text(
                                text = "Photometric Horizon Windows",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            SolarHorizonCardContent(
                                solar = solar,
                                uvIndex = station.uvIndex
                            )
                        }
                    }
                }
            }

            // 8. Field Observation Notes Card (Persistent in Room)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "STATION LOGBOOK // LOCAL ROOM SQLITE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TelemetryEmerald
                                )
                                Text(
                                    text = "Field Observation Notes",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            AssistChip(
                                onClick = { isEditingNotes = !isEditingNotes },
                                label = { Text(if (isEditingNotes) "Close" else "Edit Note") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.testTag("edit_field_notes_chip")
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isEditingNotes) {
                            OutlinedTextField(
                                value = notesDraft,
                                onValueChange = { notesDraft = it },
                                label = { Text("Station terrain & microclimate notes") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("field_notes_input"),
                                minLines = 2
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    onSaveFieldNotes(station.id, notesDraft)
                                    isEditingNotes = false
                                },
                                modifier = Modifier.testTag("save_field_notes_button")
                            ) {
                                Text("Save to Local Database")
                            }
                        } else {
                            Text(
                                text = station.fieldNotes.ifBlank {
                                    "No field observations recorded yet for ${station.name}."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("field_notes_display")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroQuickStat(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF94A3B8)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TelemetryGridCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun uvCategoryLabel(uv: Double): String = when {
    uv < 3.0 -> "Low Exposure"
    uv < 6.0 -> "Moderate Solar Flux"
    uv < 8.0 -> "High UV Intensity"
    uv < 11.0 -> "Very High — SPF 50+"
    else -> "Extreme Stratospheric UV"
}

private fun aqiAccentColor(aqi: Int): Color = when {
    aqi <= 50 -> TelemetryEmerald
    aqi <= 100 -> SolarAmber
    else -> TelemetryHoldCoral
}
