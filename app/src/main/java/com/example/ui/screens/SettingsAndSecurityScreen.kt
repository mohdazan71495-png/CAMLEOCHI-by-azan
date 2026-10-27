package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.PressureUnit
import com.example.domain.model.TemperatureUnit
import com.example.domain.model.ThemeMode
import com.example.domain.model.WindSpeedUnit
import com.example.ui.AeroPulseUiState
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.TelemetryEmerald

@Composable
fun SettingsAndSecurityScreen(
    uiState: AeroPulseUiState,
    onSelectTempUnit: (TemperatureUnit) -> Unit,
    onSelectWindUnit: (WindSpeedUnit) -> Unit,
    onSelectPressureUnit: (PressureUnit) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onReopenOnboarding: () -> Unit,
    onResetAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = uiState.preferences
    var showResetConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_security_screen"),
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
                        text = "CONFIGURATION // DATASTORE PREFERENCES",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                    Text(
                        text = "Units, Theme & Privacy",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Customize telemetry instrumentation units, display mode, and local database controls",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. Units Configuration Card
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
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "TELEMETRY UNITS",
                            style = MaterialTheme.typography.labelMedium,
                            color = SolarAmber
                        )

                        // Temperature Unit
                        Column {
                            Text(
                                text = "Temperature Scale",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TemperatureUnit.entries.forEach { unit ->
                                    FilterChip(
                                        selected = prefs.tempUnit == unit,
                                        onClick = { onSelectTempUnit(unit) },
                                        label = { Text("${unit.label} (${unit.symbol})") },
                                        modifier = Modifier.testTag("temp_unit_${unit.name}")
                                    )
                                }
                            }
                        }

                        // Wind Speed Unit
                        Column {
                            Text(
                                text = "Anemometer Wind Speed",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WindSpeedUnit.entries.forEach { unit ->
                                    FilterChip(
                                        selected = prefs.windUnit == unit,
                                        onClick = { onSelectWindUnit(unit) },
                                        label = { Text(unit.symbol) },
                                        modifier = Modifier.testTag("wind_unit_${unit.name}")
                                    )
                                }
                            }
                        }

                        // Barometric Pressure Unit
                        Column {
                            Text(
                                text = "Barometric Pressure",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PressureUnit.entries.forEach { unit ->
                                    FilterChip(
                                        selected = prefs.pressureUnit == unit,
                                        onClick = { onSelectPressureUnit(unit) },
                                        label = { Text("${unit.label} (${unit.symbol})") },
                                        modifier = Modifier.testTag("pressure_unit_${unit.name}")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Display Theme & Sync Behavior Card
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
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "APPEARANCE & NETWORK SYNC",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )

                        Column {
                            Text(
                                text = "Observatory Color Theme",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ThemeMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = prefs.themeMode == mode,
                                        onClick = { onSelectThemeMode(mode) },
                                        label = { Text(mode.label) },
                                        modifier = Modifier.testTag("theme_mode_${mode.name}")
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Sync Open-Meteo on Launch",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Automatically refresh HTTPS station forecasts while preserving offline Room fallback",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = prefs.autoSyncOnLaunch,
                                onCheckedChange = onToggleAutoSync,
                                modifier = Modifier.testTag("auto_sync_switch")
                            )
                        }
                    }
                }
            }

            // 3. Security & Privacy Architecture Card
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
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = TelemetryEmerald
                            )
                            Text(
                                text = stringResource(id = R.string.privacy_notice_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.privacy_notice_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 4. Onboarding & Database Reset Controls
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
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "SYSTEM MAINTENANCE",
                            style = MaterialTheme.typography.labelMedium,
                            color = SolarAmber
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onReopenOnboarding,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reopen_onboarding_button")
                            ) {
                                Icon(imageVector = Icons.Default.School, contentDescription = null)
                                Spacer(modifier = Modifier.padding(4.dp))
                                Text("Replay Intro")
                            }
                            Button(
                                onClick = { showResetConfirm = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("factory_reset_button")
                            ) {
                                Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null)
                                Spacer(modifier = Modifier.padding(4.dp))
                                Text("Reset Database")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Restore Factory Reference Data?") },
            text = {
                Text("This will reset all custom field stations, activity safety profiles, and barometer logs back to the 6 reference world stations and 5 default activity rules.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAllData()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_factory_reset_button")
                ) {
                    Text("Confirm Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
