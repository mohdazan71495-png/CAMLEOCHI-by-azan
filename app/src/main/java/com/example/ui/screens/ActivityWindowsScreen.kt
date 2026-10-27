package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.ActivityCategory
import com.example.domain.model.ActivityProfile
import com.example.domain.model.HourlyReadinessPoint
import com.example.domain.model.ReadinessEvaluation
import com.example.ui.AeroPulseUiState
import com.example.ui.components.ReadinessScoreGauge
import com.example.ui.components.statusAccentColor
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SolarAmber
import java.util.Locale

@Composable
fun ActivityWindowsScreen(
    uiState: AeroPulseUiState,
    onSelectProfile: (Long) -> Unit,
    onCreateCustomProfile: (
        name: String,
        category: ActivityCategory,
        maxWind: String,
        maxGust: String,
        maxPrecip: String,
        maxUv: String,
        minTemp: String,
        maxTemp: String,
        onSuccess: () -> Unit
    ) -> Unit,
    onDeleteProfile: (Long) -> Unit,
    onClearValidationError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var profilePendingDelete by remember { mutableStateOf<ActivityProfile?>(null) }

    val selectedEval = uiState.evaluations.firstOrNull {
        it.profile.id == uiState.selectedProfileId
    } ?: uiState.evaluations.firstOrNull()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("activity_windows_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    onClearValidationError()
                    showCreateDialog = true
                },
                containerColor = ElectricCyan,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_activity_profile_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Custom Activity Profile"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
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
                            text = "DECISION ENGINE // GO / CAUTION / HOLD",
                            style = MaterialTheme.typography.labelMedium,
                            color = SolarAmber
                        )
                        Text(
                            text = "Activity Safety Windows",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Evaluated against ${uiState.primaryStation?.name ?: "active station"} telemetry",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Selected Evaluation Spotlight Card
                if (selectedEval != null) {
                    item {
                        SelectedEvaluationSpotlight(
                            evaluation = selectedEval,
                            onRequestDelete = {
                                if (!selectedEval.profile.isBuiltIn) {
                                    profilePendingDelete = selectedEval.profile
                                }
                            }
                        )
                    }
                }

                item {
                    Text(
                        text = "ALL CONFIGURED ACTIVITY PROFILES (${uiState.evaluations.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                }

                items(
                    items = uiState.evaluations,
                    key = { it.profile.id }
                ) { eval ->
                    val isSelected = eval.profile.id == selectedEval?.profile?.id
                    ActivityEvaluationRowCard(
                        evaluation = eval,
                        isSelected = isSelected,
                        onClick = { onSelectProfile(eval.profile.id) },
                        onDeleteClick = if (!eval.profile.isBuiltIn) {
                            { profilePendingDelete = eval.profile }
                        } else null
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateActivityProfileDialog(
            validationError = uiState.formValidationError,
            onDismiss = {
                onClearValidationError()
                showCreateDialog = false
            },
            onSubmit = { name, category, maxWind, maxGust, maxPrecip, maxUv, minTemp, maxTemp ->
                onCreateCustomProfile(
                    name,
                    category,
                    maxWind,
                    maxGust,
                    maxPrecip,
                    maxUv,
                    minTemp,
                    maxTemp
                ) {
                    showCreateDialog = false
                }
            }
        )
    }

    profilePendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { profilePendingDelete = null },
            title = { Text("Delete Custom Activity Rule?") },
            text = {
                Text("Are you sure you want to permanently remove '${target.name}' from your local Room database?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProfile(target.id)
                        profilePendingDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_profile_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { profilePendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SelectedEvaluationSpotlight(
    evaluation: ReadinessEvaluation,
    onRequestDelete: () -> Unit
) {
    val accent = statusAccentColor(evaluation.status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selected_evaluation_spotlight"),
        shape = RoundedCornerShape(22.dp),
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
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = accent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = evaluation.status.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = evaluation.profile.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = evaluation.headline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                ReadinessScoreGauge(
                    score = evaluation.currentScore,
                    status = evaluation.status,
                    size = 94.dp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Best Window Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = ElectricCyan
                    )
                    Column {
                        Text(
                            text = "OPTIMAL 24H WINDOW",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = evaluation.bestWindowLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (!evaluation.profile.isBuiltIn) {
                    IconButton(
                        onClick = onRequestDelete,
                        modifier = Modifier.testTag("delete_selected_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete custom profile",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Limiting Factors Diagnostics
            Text(
                text = "ENVELOPE DIAGNOSTICS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            evaluation.limitingFactors.forEach { factor ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (evaluation.currentScore >= 76) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = factor,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 24-Hour Horizontal Readiness Strip
            Text(
                text = "HOURLY READINESS TIMELINE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                evaluation.hourlyReadiness.forEach { pt ->
                    HourlyReadinessChip(point = pt)
                }
            }
        }
    }
}

@Composable
private fun HourlyReadinessChip(point: HourlyReadinessPoint) {
    val color = statusAccentColor(point.status)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = point.hourLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${point.score}",
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = String.format(Locale.US, "%.0f°C", point.tempC),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ActivityEvaluationRowCard(
    evaluation: ReadinessEvaluation,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)?
) {
    val accent = statusAccentColor(evaluation.status)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("activity_profile_card_${evaluation.profile.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = evaluation.profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (!evaluation.profile.isBuiltIn) {
                        Text(
                            text = "CUSTOM",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = String.format(
                        Locale.US,
                        "Wind ≤ %.0f km/h • Gust ≤ %.0f km/h • Precip ≤ %d%% • %.0f°..%.0f°C",
                        evaluation.profile.maxWindKmh,
                        evaluation.profile.maxGustKmh,
                        evaluation.profile.maxPrecipProbPercent,
                        evaluation.profile.minTempC,
                        evaluation.profile.maxTempC
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Best Window: ${evaluation.bestWindowLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${evaluation.currentScore}",
                        style = MaterialTheme.typography.titleMedium,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (onDeleteClick != null) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete profile",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateActivityProfileDialog(
    validationError: String?,
    onDismiss: () -> Unit,
    onSubmit: (
        name: String,
        category: ActivityCategory,
        maxWind: String,
        maxGust: String,
        maxPrecip: String,
        maxUv: String,
        minTemp: String,
        maxTemp: String
    ) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(ActivityCategory.TRAIL_RUN) }
    var maxWind by rememberSaveable { mutableStateOf("28") }
    var maxGust by rememberSaveable { mutableStateOf("42") }
    var maxPrecip by rememberSaveable { mutableStateOf("25") }
    var maxUv by rememberSaveable { mutableStateOf("8.0") }
    var minTemp by rememberSaveable { mutableStateOf("2") }
    var maxTemp by rememberSaveable { mutableStateOf("28") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Activity Safety Profile") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (validationError != null) {
                    Text(
                        text = validationError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("profile_validation_error")
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name (e.g., Ridge Paragliding)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_profile_name")
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ActivityCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.badgeCode) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = maxWind,
                        onValueChange = { maxWind = it },
                        label = { Text("Max Wind (km/h)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_profile_max_wind")
                    )
                    OutlinedTextField(
                        value = maxGust,
                        onValueChange = { maxGust = it },
                        label = { Text("Max Gust (km/h)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_profile_max_gust")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = maxPrecip,
                        onValueChange = { maxPrecip = it },
                        label = { Text("Max Precip (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_profile_max_precip")
                    )
                    OutlinedTextField(
                        value = maxUv,
                        onValueChange = { maxUv = it },
                        label = { Text("Max UV Index") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_profile_max_uv")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minTemp,
                        onValueChange = { minTemp = it },
                        label = { Text("Min Temp (°C)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_profile_min_temp")
                    )
                    OutlinedTextField(
                        value = maxTemp,
                        onValueChange = { maxTemp = it },
                        label = { Text("Max Temp (°C)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_profile_max_temp")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        name,
                        selectedCategory,
                        maxWind,
                        maxGust,
                        maxPrecip,
                        maxUv,
                        minTemp,
                        maxTemp
                    )
                },
                modifier = Modifier.testTag("save_profile_confirm_button")
            ) {
                Text("Save Rule")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
