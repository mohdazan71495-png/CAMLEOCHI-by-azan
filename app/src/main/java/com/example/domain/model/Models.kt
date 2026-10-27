package com.example.domain.model

enum class TemperatureUnit(val label: String, val symbol: String) {
    CELSIUS("Celsius", "°C"),
    FAHRENHEIT("Fahrenheit", "°F")
}

enum class WindSpeedUnit(val label: String, val symbol: String) {
    KMH("Kilometers/h", "km/h"),
    MPH("Miles/h", "mph"),
    MS("Meters/s", "m/s")
}

enum class PressureUnit(val label: String, val symbol: String) {
    HPA("Hectopascals", "hPa"),
    INHG("Inches of Mercury", "inHg")
}

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    DARK("Observatory Dark"),
    LIGHT("Glacier Light")
}

enum class StationSortOption(val label: String) {
    PRIMARY_FIRST("Primary & Favorites"),
    NAME_ASC("Name (A–Z)"),
    ELEVATION_DESC("Highest Elevation"),
    TEMP_DESC("Warmest First"),
    WIND_ASC("Calmest Wind")
}

enum class ActivityCategory(val displayName: String, val badgeCode: String) {
    TRAIL_RUN("Alpine Trail Run", "RUN"),
    ASTROPHOTOGRAPHY("Deep-Sky Astrophotography", "ASTRO"),
    DRONE_FLIGHT("Aerial Drone Telemetry", "UAV"),
    CYCLING("Endurance Road Cycling", "BIKE"),
    SOLAR_SURVEY("Solar & UV Field Survey", "SOLAR")
}

enum class ReadinessStatus(val label: String) {
    GO("OPTIMAL GO"),
    CAUTION("MARGINAL CAUTION"),
    HOLD("SAFETY HOLD")
}

data class HourlyPoint(
    val hourLabel: String,
    val tempC: Double,
    val uvIndex: Double,
    val windKmh: Double,
    val precipProbPercent: Int,
    val cloudCoverPercent: Int
)

data class FieldStation(
    val id: Long = 0L,
    val name: String,
    val region: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Int,
    val isFavorite: Boolean = false,
    val isPrimary: Boolean = false,
    val temperatureC: Double,
    val apparentTempC: Double,
    val humidityPercent: Int,
    val surfacePressureHpa: Double,
    val seaLevelPressureHpa: Double,
    val windSpeedKmh: Double,
    val windGustsKmh: Double,
    val windDirectionDeg: Int,
    val uvIndex: Double,
    val visibilityMeters: Int,
    val cloudCoverPercent: Int,
    val precipProbabilityPercent: Int,
    val weatherCode: Int,
    val sunriseIso: String,
    val sunsetIso: String,
    val usAqi: Int,
    val pm25: Double,
    val pm10: Double,
    val hourlyPoints: List<HourlyPoint>,
    val fieldNotes: String = "",
    val lastSyncedEpochMillis: Long
)

data class ActivityProfile(
    val id: Long = 0L,
    val name: String,
    val category: ActivityCategory,
    val maxWindKmh: Double,
    val maxGustKmh: Double,
    val maxPrecipProbPercent: Int,
    val maxUvIndex: Double,
    val maxCloudCoverPercent: Int,
    val minTempC: Double,
    val maxTempC: Double,
    val maxAqi: Int,
    val isBuiltIn: Boolean = false
)

data class HourlyReadinessPoint(
    val hourLabel: String,
    val score: Int,
    val status: ReadinessStatus,
    val tempC: Double,
    val windKmh: Double,
    val precipProbPercent: Int
)

data class ReadinessEvaluation(
    val profile: ActivityProfile,
    val stationName: String,
    val currentScore: Int,
    val status: ReadinessStatus,
    val headline: String,
    val limitingFactors: List<String>,
    val bestWindowLabel: String,
    val bestWindowScore: Int,
    val hourlyReadiness: List<HourlyReadinessPoint>
)

data class SolarPhaseWindows(
    val sunriseTime: String,
    val sunsetTime: String,
    val morningBlueHour: String,
    val morningGoldenHour: String,
    val solarNoonTime: String,
    val eveningGoldenHour: String,
    val eveningBlueHour: String,
    val daylightDurationLabel: String
)

data class DeviceSensorSnapshot(
    val pressureHpa: Float? = null,
    val ambientTempC: Float? = null,
    val lightLux: Float? = null,
    val relativeHumidityPercent: Float? = null,
    val hasBarometer: Boolean = false,
    val hasThermometer: Boolean = false,
    val hasLightSensor: Boolean = false,
    val hasHumiditySensor: Boolean = false,
    val barometricAltitudeMeters: Double? = null,
    val pressureTrendLabel: String = "Stable",
    val fieldLogEntries: List<BarometerLogEntry> = emptyList()
)

data class BarometerLogEntry(
    val timestampMillis: Long,
    val pressureHpa: Double,
    val altitudeMeters: Double,
    val note: String
)

data class UserPreferences(
    val tempUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val windUnit: WindSpeedUnit = WindSpeedUnit.KMH,
    val pressureUnit: PressureUnit = PressureUnit.HPA,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val hasCompletedOnboarding: Boolean = false,
    val seaLevelReferenceHpa: Double = 1013.25,
    val autoSyncOnLaunch: Boolean = true
)

data class GeocodingCandidate(
    val name: String,
    val region: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Int
)
