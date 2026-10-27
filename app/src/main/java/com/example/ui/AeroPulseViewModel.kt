package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AeroPulseDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.AeroPulseRepository
import com.example.data.sensor.DeviceSensorMonitor
import com.example.domain.model.ActivityCategory
import com.example.domain.model.ActivityProfile
import com.example.domain.model.DeviceSensorSnapshot
import com.example.domain.model.FieldStation
import com.example.domain.model.GeocodingCandidate
import com.example.domain.model.PressureUnit
import com.example.domain.model.ReadinessEvaluation
import com.example.domain.model.SolarPhaseWindows
import com.example.domain.model.StationSortOption
import com.example.domain.model.TemperatureUnit
import com.example.domain.model.ThemeMode
import com.example.domain.model.UserPreferences
import com.example.domain.model.WindSpeedUnit
import com.example.domain.usecase.ActivityWindowEvaluator
import com.example.domain.usecase.AtmosphericCalculator
import com.example.domain.usecase.InputValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NavigationTab(val route: String, val label: String) {
    TELEMETRY("telemetry_dashboard", "Telemetry"),
    WINDOWS("activity_windows", "Windows"),
    STATIONS("stations_directory", "Stations"),
    INSTRUMENT("instrument_sensors", "Instrument"),
    SETTINGS("settings_security", "Settings")
}

enum class TelemetryChartMetric(val label: String) {
    TEMPERATURE("Temp"),
    UV_INDEX("UV Index"),
    WIND_SPEED("Wind"),
    PRECIP_PROB("Precip %")
}

data class AeroPulseUiState(
    val isSplashVisible: Boolean = true,
    val currentTab: NavigationTab = NavigationTab.TELEMETRY,
    val preferences: UserPreferences = UserPreferences(),
    val stations: List<FieldStation> = emptyList(),
    val filteredStations: List<FieldStation> = emptyList(),
    val primaryStation: FieldStation? = null,
    val solarWindows: SolarPhaseWindows? = null,
    val dewPointC: Double = 0.0,
    val activityProfiles: List<ActivityProfile> = emptyList(),
    val evaluations: List<ReadinessEvaluation> = emptyList(),
    val selectedProfileId: Long? = null,
    val selectedChartMetric: TelemetryChartMetric = TelemetryChartMetric.TEMPERATURE,
    val searchQuery: String = "",
    val favoritesOnly: Boolean = false,
    val sortOption: StationSortOption = StationSortOption.PRIMARY_FIRST,
    val isSyncing: Boolean = false,
    val statusBannerMessage: String? = null,
    val isErrorBanner: Boolean = false,
    val geocodingResults: List<GeocodingCandidate> = emptyList(),
    val isSearchingRemote: Boolean = false,
    val formValidationError: String? = null,
    val sensorSnapshot: DeviceSensorSnapshot = DeviceSensorSnapshot()
)

private data class LocalFilterState(
    val currentTab: NavigationTab = NavigationTab.TELEMETRY,
    val isSplashVisible: Boolean = true,
    val selectedProfileId: Long? = null,
    val selectedChartMetric: TelemetryChartMetric = TelemetryChartMetric.TEMPERATURE,
    val searchQuery: String = "",
    val favoritesOnly: Boolean = false,
    val sortOption: StationSortOption = StationSortOption.PRIMARY_FIRST,
    val isSyncing: Boolean = false,
    val statusBannerMessage: String? = null,
    val isErrorBanner: Boolean = false,
    val geocodingResults: List<GeocodingCandidate> = emptyList(),
    val isSearchingRemote: Boolean = false,
    val formValidationError: String? = null
)

class AeroPulseViewModel(
    private val repository: AeroPulseRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val sensorMonitor: DeviceSensorMonitor
) : ViewModel() {

    private val localState = MutableStateFlow(LocalFilterState())

    val uiState: StateFlow<AeroPulseUiState> = combine(
        localState,
        preferencesRepository.preferencesFlow,
        repository.stationsFlow,
        repository.activityProfilesFlow,
        combine(
            sensorMonitor.observeHardwareSensors(),
            repository.barometerLogsFlow
        ) { rawSensor, logs -> Pair(rawSensor, logs) }
    ) { local, prefs, stations, profiles, (rawSensor, logs) ->
        val primary = stations.firstOrNull { it.isPrimary } ?: stations.firstOrNull()

        val filtered = stations
            .filter { station ->
                val matchesQuery = local.searchQuery.isBlank() ||
                    station.name.contains(local.searchQuery, ignoreCase = true) ||
                    station.region.contains(local.searchQuery, ignoreCase = true) ||
                    station.country.contains(local.searchQuery, ignoreCase = true)
                val matchesFav = !local.favoritesOnly || station.isFavorite
                matchesQuery && matchesFav
            }
            .let { list ->
                when (local.sortOption) {
                    StationSortOption.PRIMARY_FIRST ->
                        list.sortedWith(compareByDescending<FieldStation> { it.isPrimary }.thenByDescending { it.isFavorite }.thenBy { it.name })
                    StationSortOption.NAME_ASC ->
                        list.sortedBy { it.name.lowercase() }
                    StationSortOption.ELEVATION_DESC ->
                        list.sortedByDescending { it.elevationMeters }
                    StationSortOption.TEMP_DESC ->
                        list.sortedByDescending { it.temperatureC }
                    StationSortOption.WIND_ASC ->
                        list.sortedBy { it.windSpeedKmh }
                }
            }

        val evaluations = if (primary != null) {
            profiles.map { profile -> ActivityWindowEvaluator.evaluate(primary, profile) }
        } else {
            emptyList()
        }

        val solar = primary?.let {
            AtmosphericCalculator.computeSolarWindows(it.sunriseIso, it.sunsetIso)
        }
        val dewPoint = primary?.let {
            AtmosphericCalculator.calculateDewPointC(it.temperatureC, it.humidityPercent)
        } ?: 0.0

        val effectivePressureHpa = rawSensor.pressureHpa?.toDouble()
            ?: primary?.surfacePressureHpa
            ?: 1002.0

        val computedAltitude = AtmosphericCalculator.calculateBarometricAltitudeMeters(
            pressureHpa = effectivePressureHpa,
            seaLevelReferenceHpa = prefs.seaLevelReferenceHpa
        )

        val pressureTrend = when {
            logs.size >= 2 -> {
                val delta = effectivePressureHpa - logs.first().pressureHpa
                when {
                    delta > 1.2 -> "Rising (+${String.format("%.1f", delta)} hPa)"
                    delta < -1.2 -> "Falling (${String.format("%.1f", delta)} hPa)"
                    else -> "Steady (±0.5 hPa)"
                }
            }
            else -> "Nominal Barometric Equilibrium"
        }

        val sensorSnapshot = DeviceSensorSnapshot(
            pressureHpa = rawSensor.pressureHpa,
            ambientTempC = rawSensor.ambientTempC,
            lightLux = rawSensor.lightLux,
            relativeHumidityPercent = rawSensor.relativeHumidityPercent,
            hasBarometer = rawSensor.hasBarometer,
            hasThermometer = rawSensor.hasThermometer,
            hasLightSensor = rawSensor.hasLightSensor,
            hasHumiditySensor = rawSensor.hasHumiditySensor,
            barometricAltitudeMeters = computedAltitude,
            pressureTrendLabel = pressureTrend,
            fieldLogEntries = logs
        )

        AeroPulseUiState(
            isSplashVisible = local.isSplashVisible,
            currentTab = local.currentTab,
            preferences = prefs,
            stations = stations,
            filteredStations = filtered,
            primaryStation = primary,
            solarWindows = solar,
            dewPointC = dewPoint,
            activityProfiles = profiles,
            evaluations = evaluations,
            selectedProfileId = local.selectedProfileId ?: profiles.firstOrNull()?.id,
            selectedChartMetric = local.selectedChartMetric,
            searchQuery = local.searchQuery,
            favoritesOnly = local.favoritesOnly,
            sortOption = local.sortOption,
            isSyncing = local.isSyncing,
            statusBannerMessage = local.statusBannerMessage,
            isErrorBanner = local.isErrorBanner,
            geocodingResults = local.geocodingResults,
            isSearchingRemote = local.isSearchingRemote,
            formValidationError = local.formValidationError,
            sensorSnapshot = sensorSnapshot
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AeroPulseUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            localState.update { it.copy(isSplashVisible = false) }
        }
    }

    fun dismissSplash() {
        localState.update { it.copy(isSplashVisible = false) }
    }

    fun selectTab(tab: NavigationTab) {
        localState.update { it.copy(currentTab = tab, formValidationError = null) }
    }

    fun selectChartMetric(metric: TelemetryChartMetric) {
        localState.update { it.copy(selectedChartMetric = metric) }
    }

    fun selectActivityProfile(profileId: Long) {
        localState.update { it.copy(selectedProfileId = profileId) }
    }

    fun updateSearchQuery(query: String) {
        localState.update { it.copy(searchQuery = query) }
    }

    fun toggleFavoritesOnly() {
        localState.update { it.copy(favoritesOnly = !it.favoritesOnly) }
    }

    fun updateSortOption(option: StationSortOption) {
        localState.update { it.copy(sortOption = option) }
    }

    fun clearBanner() {
        localState.update { it.copy(statusBannerMessage = null, isErrorBanner = false) }
    }

    fun clearValidationError() {
        localState.update { it.copy(formValidationError = null) }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
        }
    }

    fun reopenOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(false)
        }
    }

    fun syncLiveTelemetry() {
        if (localState.value.isSyncing) return
        viewModelScope.launch {
            localState.update {
                it.copy(isSyncing = true, statusBannerMessage = null, isErrorBanner = false)
            }
            val result = repository.syncAllStationsFromNetwork()
            result.fold(
                onSuccess = { count ->
                    localState.update {
                        it.copy(
                            isSyncing = false,
                            statusBannerMessage = "Synced live Open-Meteo telemetry for $count stations.",
                            isErrorBanner = false
                        )
                    }
                },
                onFailure = { err ->
                    localState.update {
                        it.copy(
                            isSyncing = false,
                            statusBannerMessage = err.message ?: "Offline mode active: using local Room SQLite cache.",
                            isErrorBanner = true
                        )
                    }
                }
            )
        }
    }

    fun selectPrimaryStation(stationId: Long) {
        viewModelScope.launch {
            repository.selectPrimaryStation(stationId)
            localState.update {
                it.copy(statusBannerMessage = "Primary field station updated.", isErrorBanner = false)
            }
        }
    }

    fun toggleStationFavorite(station: FieldStation) {
        viewModelScope.launch {
            repository.toggleFavorite(station.id, !station.isFavorite)
        }
    }

    fun saveStationNotes(stationId: Long, notes: String) {
        viewModelScope.launch {
            repository.updateStationNotes(stationId, notes)
            localState.update {
                it.copy(statusBannerMessage = "Field notes saved to Room database.", isErrorBanner = false)
            }
        }
    }

    fun deleteStation(stationId: Long) {
        viewModelScope.launch {
            repository.deleteStation(stationId)
            localState.update {
                it.copy(statusBannerMessage = "Station removed from local database.", isErrorBanner = false)
            }
        }
    }

    fun searchRemoteLocations(query: String) {
        if (query.trim().length < 2) {
            localState.update {
                it.copy(formValidationError = "Enter at least 2 characters to search Open-Meteo Geocoding.")
            }
            return
        }
        viewModelScope.launch {
            localState.update { it.copy(isSearchingRemote = true, formValidationError = null) }
            val result = repository.searchLocationsRemote(query)
            result.fold(
                onSuccess = { list ->
                    localState.update {
                        it.copy(
                            isSearchingRemote = false,
                            geocodingResults = list,
                            formValidationError = if (list.isEmpty()) "No matching locations found. You can also enter coordinates manually below." else null
                        )
                    }
                },
                onFailure = {
                    localState.update {
                        it.copy(
                            isSearchingRemote = false,
                            formValidationError = "Geocoding lookup unavailable offline. Enter coordinates manually below."
                        )
                    }
                }
            )
        }
    }

    fun addStationFromCandidate(candidate: GeocodingCandidate) {
        viewModelScope.launch {
            repository.addStation(
                name = candidate.name,
                region = candidate.region,
                country = candidate.country,
                latitude = candidate.latitude,
                longitude = candidate.longitude,
                elevationMeters = candidate.elevationMeters,
                makePrimary = true
            )
            localState.update {
                it.copy(
                    geocodingResults = emptyList(),
                    formValidationError = null,
                    statusBannerMessage = "Added ${candidate.name} as active field station.",
                    isErrorBanner = false
                )
            }
        }
    }

    fun validateAndAddManualStation(
        name: String,
        region: String,
        country: String,
        latitudeText: String,
        longitudeText: String,
        elevationText: String,
        onSuccess: () -> Unit
    ) {
        val validation = InputValidator.validateStationInput(
            name = name,
            region = region,
            country = country,
            latitudeText = latitudeText,
            longitudeText = longitudeText,
            elevationText = elevationText
        )
        if (!validation.isValid) {
            localState.update { it.copy(formValidationError = validation.errorMessage) }
            return
        }
        viewModelScope.launch {
            repository.addStation(
                name = name,
                region = region,
                country = country,
                latitude = latitudeText.trim().toDouble(),
                longitude = longitudeText.trim().toDouble(),
                elevationMeters = elevationText.trim().toInt(),
                makePrimary = true
            )
            localState.update {
                it.copy(
                    formValidationError = null,
                    statusBannerMessage = "Saved $name to local Room database.",
                    isErrorBanner = false
                )
            }
            onSuccess()
        }
    }

    fun populateCoordinatesFromGps(
        onCoordinatesFound: (lat: String, lon: String, elev: String) -> Unit
    ) {
        viewModelScope.launch {
            val coords = sensorMonitor.fetchCurrentGpsCoordinates()
            if (coords != null) {
                onCoordinatesFound(
                    String.format(java.util.Locale.US, "%.4f", coords.latitude),
                    String.format(java.util.Locale.US, "%.4f", coords.longitude),
                    coords.altitudeMeters.toString()
                )
                localState.update { it.copy(formValidationError = null) }
            } else {
                localState.update {
                    it.copy(formValidationError = "GPS fix unavailable in current environment; enter coordinates manually.")
                }
            }
        }
    }

    fun validateAndSaveActivityProfile(
        name: String,
        category: ActivityCategory,
        maxWindText: String,
        maxGustText: String,
        maxPrecipText: String,
        maxUvText: String,
        minTempText: String,
        maxTempText: String,
        onSuccess: () -> Unit
    ) {
        val validation = InputValidator.validateActivityProfileInput(
            name = name,
            maxWindText = maxWindText,
            maxGustText = maxGustText,
            maxPrecipText = maxPrecipText,
            maxUvText = maxUvText,
            minTempText = minTempText,
            maxTempText = maxTempText
        )
        if (!validation.isValid) {
            localState.update { it.copy(formValidationError = validation.errorMessage) }
            return
        }
        viewModelScope.launch {
            repository.saveActivityProfile(
                name = name,
                categoryName = category.name,
                maxWindKmh = maxWindText.trim().toDouble(),
                maxGustKmh = maxGustText.trim().toDouble(),
                maxPrecipProbPercent = maxPrecipText.trim().toInt(),
                maxUvIndex = maxUvText.trim().toDouble(),
                maxCloudCoverPercent = if (category == ActivityCategory.ASTROPHOTOGRAPHY) 25 else 85,
                minTempC = minTempText.trim().toDouble(),
                maxTempC = maxTempText.trim().toDouble(),
                maxAqi = 75
            )
            localState.update {
                it.copy(
                    formValidationError = null,
                    statusBannerMessage = "Custom activity profile '${name.trim()}' saved.",
                    isErrorBanner = false
                )
            }
            onSuccess()
        }
    }

    fun deleteActivityProfile(profileId: Long) {
        viewModelScope.launch {
            repository.deleteActivityProfile(profileId)
            localState.update {
                it.copy(statusBannerMessage = "Activity profile deleted.", isErrorBanner = false)
            }
        }
    }

    fun calibrateSeaLevelPressureFromStation() {
        val primary = uiState.value.primaryStation ?: return
        val rawPressure = uiState.value.sensorSnapshot.pressureHpa?.toDouble() ?: primary.surfacePressureHpa
        val calibratedQnh = AtmosphericCalculator.calibrateSeaLevelPressureHpa(
            surfacePressureHpa = rawPressure,
            knownElevationMeters = primary.elevationMeters
        )
        viewModelScope.launch {
            preferencesRepository.setSeaLevelReferenceHpa(calibratedQnh)
            localState.update {
                it.copy(
                    statusBannerMessage = String.format(
                        java.util.Locale.US,
                        "Calibrated QNH to %.1f hPa using %s (%d m).",
                        calibratedQnh,
                        primary.name,
                        primary.elevationMeters
                    ),
                    isErrorBanner = false
                )
            }
        }
    }

    fun adjustSeaLevelReferenceHpa(qnhHpa: Double) {
        viewModelScope.launch {
            preferencesRepository.setSeaLevelReferenceHpa(qnhHpa)
        }
    }

    fun recordBarometerLog(note: String) {
        val state = uiState.value
        val pressure = state.sensorSnapshot.pressureHpa?.toDouble()
            ?: state.primaryStation?.surfacePressureHpa
            ?: 1013.25
        val altitude = state.sensorSnapshot.barometricAltitudeMeters ?: 0.0
        viewModelScope.launch {
            repository.logBarometerSnapshot(pressure, altitude, note)
            localState.update {
                it.copy(statusBannerMessage = "Logged barometric altitude snapshot.", isErrorBanner = false)
            }
        }
    }

    fun clearBarometerLogs() {
        viewModelScope.launch {
            repository.clearBarometerHistory()
            localState.update {
                it.copy(statusBannerMessage = "Cleared barometer snapshot log.", isErrorBanner = false)
            }
        }
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        viewModelScope.launch { preferencesRepository.setTemperatureUnit(unit) }
    }

    fun setWindSpeedUnit(unit: WindSpeedUnit) {
        viewModelScope.launch { preferencesRepository.setWindSpeedUnit(unit) }
    }

    fun setPressureUnit(unit: PressureUnit) {
        viewModelScope.launch { preferencesRepository.setPressureUnit(unit) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setAutoSyncOnLaunch(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoSyncOnLaunch(enabled) }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.resetAllDataToDefaults()
            preferencesRepository.setSeaLevelReferenceHpa(1013.25)
            localState.update {
                it.copy(
                    statusBannerMessage = "Database restored to factory reference stations and profiles.",
                    isErrorBanner = false
                )
            }
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val db = AeroPulseDatabase.getInstance(appContext)
                    val repo = AeroPulseRepository(db.aeroPulseDao())
                    val prefs = UserPreferencesRepository(appContext)
                    val sensors = DeviceSensorMonitor(appContext)
                    return AeroPulseViewModel(repo, prefs, sensors) as T
                }
            }
    }
}
