package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.FieldStation
import com.example.domain.model.GeocodingCandidate
import com.example.domain.model.StationSortOption
import com.example.domain.usecase.AtmosphericCalculator
import com.example.ui.AeroPulseUiState
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.TelemetryEmerald
import java.util.Locale

@Composable
fun StationsDirectoryScreen(
    uiState: AeroPulseUiState,
    onSearchQueryChange: (String) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onSortOptionChange: (StationSortOption) -> Unit,
    onSelectPrimaryStation: (Long) -> Unit,
    onToggleStationFavorite: (FieldStation) -> Unit,
    onDeleteStation: (Long) -> Unit,
    onSearchRemoteLocations: (String) -> Unit,
    onAddStationFromCandidate: (GeocodingCandidate) -> Unit,
    onValidateAndAddManualStation: (
        name: String,
        region: String,
        country: String,
        lat: String,
        lon: String,
        elev: String,
        onSuccess: () -> Unit
    ) -> Unit,
    onPopulateFromGps: ((lat: String, lon: String, elev: String) -> Unit) -> Unit,
    onClearValidationError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var stationPendingDelete by remember { mutableStateOf<FieldStation?>(null) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("stations_directory_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    onClearValidationError()
                    showAddDialog = true
                },
                containerColor = ElectricCyan,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_station_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Field Station"
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column {
                        Text(
                            text = "GLOBAL NETWORK // ROOM SQLITE CACHE",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                        Text(
                            text = "Monitored Field Stations",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Search, filter, sort, or add custom coordinates & Open-Meteo stations",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Search Bar
                item {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search station name, region, or country…") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.testTag("clear_station_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("station_search_input")
                    )
                }

                // Filter & Sort Chips
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = uiState.favoritesOnly,
                            onClick = onToggleFavoritesOnly,
                            label = { Text("Favorites Only") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (uiState.favoritesOnly) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("filter_favorites_chip")
                        )

                        StationSortOption.entries.forEach { option ->
                            FilterChip(
                                selected = uiState.sortOption == option,
                                onClick = { onSortOptionChange(option) },
                                label = { Text(option.label) },
                                modifier = Modifier.testTag("sort_chip_${option.name}")
                            )
                        }
                    }
                }

                if (uiState.filteredStations.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("empty_stations_state"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No matching field stations",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Try clearing your search filter or tap + to add a new station.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = uiState.filteredStations,
                        key = { it.id }
                    ) { station ->
                        StationDirectoryCard(
                            station = station,
                            uiState = uiState,
                            onSelectPrimary = { onSelectPrimaryStation(station.id) },
                            onToggleFavorite = { onToggleStationFavorite(station) },
                            onRequestDelete = {
                                if (uiState.stations.size > 1) {
                                    stationPendingDelete = station
                                }
                            },
                            canDelete = uiState.stations.size > 1
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddStationDialog(
            uiState = uiState,
            onDismiss = {
                onClearValidationError()
                showAddDialog = false
            },
            onSearchRemote = onSearchRemoteLocations,
            onSelectCandidate = { candidate ->
                onAddStationFromCandidate(candidate)
                showAddDialog = false
            },
            onPopulateFromGps = onPopulateFromGps,
            onSubmitManual = { name, region, country, lat, lon, elev ->
                onValidateAndAddManualStation(name, region, country, lat, lon, elev) {
                    showAddDialog = false
                }
            }
        )
    }

    stationPendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { stationPendingDelete = null },
            title = { Text("Remove Field Station?") },
            text = {
                Text("Remove '${target.name}' (${target.country}) and its cached telemetry from your local database?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteStation(target.id)
                        stationPendingDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_station_button")
                ) {
                    Text("Delete Station")
                }
            },
            dismissButton = {
                TextButton(onClick = { stationPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StationDirectoryCard(
    station: FieldStation,
    uiState: AeroPulseUiState,
    onSelectPrimary: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRequestDelete: () -> Unit,
    canDelete: Boolean
) {
    val prefs = uiState.preferences
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelectPrimary)
            .testTag("station_directory_card_${station.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (station.isPrimary) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = station.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        if (station.isPrimary) {
                            Surface(
                                color = TelemetryEmerald.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = TelemetryEmerald,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "PRIMARY",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TelemetryEmerald
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "${station.region}, ${station.country} • ${station.elevationMeters} m ASL",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(
                            Locale.US,
                            "Lat %.4f°, Lon %.4f°",
                            station.latitude,
                            station.longitude
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.testTag("favorite_station_button_${station.id}")
                    ) {
                        Icon(
                            imageVector = if (station.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Toggle Favorite",
                            tint = if (station.isFavorite) SolarAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (canDelete) {
                        IconButton(
                            onClick = onRequestDelete,
                            modifier = Modifier.testTag("delete_station_button_${station.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Station",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DirectoryMetricColumn(
                    label = "TEMP",
                    value = AtmosphericCalculator.formatTemperature(station.temperatureC, prefs.tempUnit)
                )
                DirectoryMetricColumn(
                    label = "WIND",
                    value = AtmosphericCalculator.formatWindSpeed(station.windSpeedKmh, prefs.windUnit)
                )
                DirectoryMetricColumn(
                    label = "PRESSURE",
                    value = AtmosphericCalculator.formatPressure(station.surfacePressureHpa, prefs.pressureUnit)
                )
                DirectoryMetricColumn(
                    label = "UV / AQI",
                    value = String.format(Locale.US, "UV %.1f • AQI %d", station.uvIndex, station.usAqi)
                )
            }
        }
    }
}

@Composable
private fun DirectoryMetricColumn(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AddStationDialog(
    uiState: AeroPulseUiState,
    onDismiss: () -> Unit,
    onSearchRemote: (String) -> Unit,
    onSelectCandidate: (GeocodingCandidate) -> Unit,
    onPopulateFromGps: ((lat: String, lon: String, elev: String) -> Unit) -> Unit,
    onSubmitManual: (
        name: String,
        region: String,
        country: String,
        lat: String,
        lon: String,
        elev: String
    ) -> Unit
) {
    var remoteQuery by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var region by rememberSaveable { mutableStateOf("") }
    var country by rememberSaveable { mutableStateOf("") }
    var latitude by rememberSaveable { mutableStateOf("") }
    var longitude by rememberSaveable { mutableStateOf("") }
    var elevation by rememberSaveable { mutableStateOf("250") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            onPopulateFromGps { lat, lon, elev ->
                latitude = lat
                longitude = lon
                elevation = elev
                if (name.isBlank()) name = "GPS Field Point"
                if (country.isBlank()) country = "Local GPS Sector"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Field Station") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.formValidationError != null) {
                    Text(
                        text = uiState.formValidationError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("station_validation_error")
                    )
                }

                // Online Open-Meteo Geocoding Search
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = remoteQuery,
                        onValueChange = { remoteQuery = it },
                        label = { Text("Lookup City (Open-Meteo)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("geocoding_query_input")
                    )
                    Button(
                        onClick = { onSearchRemote(remoteQuery) },
                        enabled = !uiState.isSearchingRemote,
                        modifier = Modifier.testTag("geocoding_search_button")
                    ) {
                        if (uiState.isSearchingRemote) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Find")
                        }
                    }
                }

                if (uiState.geocodingResults.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        uiState.geocodingResults.take(3).forEach { candidate ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectCandidate(candidate) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${candidate.name}, ${candidate.country}",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "${candidate.region} • ${candidate.elevationMeters}m",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "Add",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ElectricCyan
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MANUAL COORDINATES",
                        style = MaterialTheme.typography.labelSmall,
                        color = SolarAmber
                    )
                    AssistChip(
                        onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        label = { Text("Use GPS") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("use_gps_button")
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Station Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_station_name")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = region,
                        onValueChange = { region = it },
                        label = { Text("Region/State") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_station_region")
                    )
                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Country") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_station_country")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it },
                        label = { Text("Lat (-90..90)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_station_lat")
                    )
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        label = { Text("Lon (-180..180)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_station_lon")
                    )
                }
                OutlinedTextField(
                    value = elevation,
                    onValueChange = { elevation = it },
                    label = { Text("Elevation (meters ASL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_station_elevation")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmitManual(name, region, country, latitude, longitude, elevation)
                },
                modifier = Modifier.testTag("save_station_confirm_button")
            ) {
                Text("Save Station")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
