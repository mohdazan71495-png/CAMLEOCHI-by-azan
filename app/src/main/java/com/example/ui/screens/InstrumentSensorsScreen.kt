package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.usecase.AtmosphericCalculator
import com.example.ui.AeroPulseUiState
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.TelemetryEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InstrumentSensorsScreen(
    uiState: AeroPulseUiState,
    onCalibrateFromActiveStation: () -> Unit,
    onAdjustSeaLevelReference: (Double) -> Unit,
    onRecordBarometerSnapshot: (String) -> Unit,
    onClearBarometerLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sensor = uiState.sensorSnapshot
    val prefs = uiState.preferences
    val station = uiState.primaryStation

    var logNote by rememberSaveable { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }

    val effectivePressureHpa = sensor.pressureHpa?.toDouble()
        ?: station?.surfacePressureHpa
        ?: 1002.0

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("instrument_sensors_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "HARDWARE SENSOR BUS // BAROMETRIC ALTIMETER",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                    Text(
                        text = "Field Instrument Deck",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (sensor.hasBarometer) {
                            "Streaming live Android TYPE_PRESSURE hardware sensor"
                        } else {
                            "Hardware barometer sensor not detected on device — coupled to ${station?.name ?: "station"} QFE reference"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Primary Altimeter & Barometer Instrument Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "BAROMETRIC ALTITUDE (QNH CALIBRATED)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SolarAmber
                                )
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "%.0f m ASL",
                                        sensor.barometricAltitudeMeters ?: 0.0
                                    ),
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("barometric_altitude_value")
                                )
                                Text(
                                    text = "Static Pressure: ${
                                        AtmosphericCalculator.formatPressure(
                                            effectivePressureHpa,
                                            prefs.pressureUnit
                                        )
                                    } • ${sensor.pressureTrendLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(ElectricCyan.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CompassCalibration,
                                    contentDescription = "Altimeter Calibration",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Sea-Level Reference Pressure (QNH) Slider & Auto-Calibrate
                        Text(
                            text = String.format(
                                Locale.US,
                                "SEA-LEVEL QNH REFERENCE: %.1f hPa",
                                prefs.seaLevelReferenceHpa
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                        Slider(
                            value = prefs.seaLevelReferenceHpa.toFloat(),
                            onValueChange = { onAdjustSeaLevelReference(it.toDouble()) },
                            valueRange = 960f..1050f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("qnh_reference_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Calibrate against ${station?.name ?: "station"} (${station?.elevationMeters ?: 0} m)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = onCalibrateFromActiveStation,
                                modifier = Modifier.testTag("calibrate_qnh_button")
                            ) {
                                Text("Auto-Calibrate QNH")
                            }
                        }
                    }
                }
            }

            // Hardware Sensor Bus Diagnostics Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SensorStatusTile(
                            name = "MEMS BAROMETER",
                            reading = sensor.pressureHpa?.let { String.format(Locale.US, "%.1f hPa", it) }
                                ?: String.format(Locale.US, "%.1f hPa (Station)", effectivePressureHpa),
                            status = if (sensor.hasBarometer) "Hardware Active" else "Station Coupled",
                            icon = Icons.Default.Speed,
                            accent = ElectricCyan,
                            modifier = Modifier.weight(1f)
                        )
                        SensorStatusTile(
                            name = "PHOTOMETER (LUX)",
                            reading = sensor.lightLux?.let { String.format(Locale.US, "%.0f lx", it) }
                                ?: "N/A",
                            status = if (sensor.hasLightSensor) "Hardware Active" else "Sensor Absent",
                            icon = Icons.Default.LightMode,
                            accent = SolarAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SensorStatusTile(
                            name = "AMBIENT THERMISTOR",
                            reading = sensor.ambientTempC?.let { String.format(Locale.US, "%.1f °C", it) }
                                ?: "${station?.temperatureC ?: 0.0} °C (Station)",
                            status = if (sensor.hasThermometer) "Hardware Active" else "Station Coupled",
                            icon = Icons.Default.Thermostat,
                            accent = TelemetryEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        SensorStatusTile(
                            name = "HYGROMETER (RH)",
                            reading = sensor.relativeHumidityPercent?.let { String.format(Locale.US, "%.0f %%", it) }
                                ?: "${station?.humidityPercent ?: 0} % (Station)",
                            status = if (sensor.hasHumiditySensor) "Hardware Active" else "Station Coupled",
                            icon = Icons.Default.WaterDrop,
                            accent = ElectricCyan,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Record Barometer Snapshot Card
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
                            text = "ALTIMETER SNAPSHOT LOGGER // ROOM PERSISTENCE",
                            style = MaterialTheme.typography.labelMedium,
                            color = TelemetryEmerald
                        )
                        Text(
                            text = "Log Waypoint Pressure & Altitude",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = logNote,
                                onValueChange = { logNote = it },
                                placeholder = { Text("Waypoint label (e.g., Col du Midi Camp)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("barometer_log_note_input")
                            )
                            Button(
                                onClick = {
                                    onRecordBarometerSnapshot(logNote)
                                    logNote = ""
                                },
                                modifier = Modifier.testTag("record_barometer_snapshot_button")
                            ) {
                                Text("Log")
                            }
                        }
                    }
                }
            }

            // Logged Snapshots Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECORDED BAROMETRIC SNAPSHOTS (${sensor.fieldLogEntries.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                    if (sensor.fieldLogEntries.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier.testTag("clear_barometer_logs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear barometer logs",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (sensor.fieldLogEntries.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "No barometer snapshots recorded yet. Tap 'Log' above to track pressure trends and elevation gain.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(sensor.fieldLogEntries) { entry ->
                    val formatter = remember { SimpleDateFormat("HH:mm:ss • MMM dd", Locale.US) }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.note,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatter.format(Date(entry.timestampMillis)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "%.0f m ASL", entry.altitudeMeters),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SolarAmber,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f hPa", entry.pressureHpa),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Barometer Logbook?") },
            text = { Text("Delete all recorded barometric pressure and altitude snapshots from your device?") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearBarometerLogs()
                        showClearConfirm = false
                    },
                    modifier = Modifier.testTag("confirm_clear_barometer_button")
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SensorStatusTile(
    name: String,
    reading: String,
    status: String,
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
                Icon(
                    imageVector = icon,
                    contentDescription = name,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = reading,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = accent
            )
        }
    }
}
