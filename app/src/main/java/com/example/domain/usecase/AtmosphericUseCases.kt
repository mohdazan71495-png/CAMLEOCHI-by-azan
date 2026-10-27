package com.example.domain.usecase

import com.example.domain.model.ActivityCategory
import com.example.domain.model.ActivityProfile
import com.example.domain.model.FieldStation
import com.example.domain.model.HourlyPoint
import com.example.domain.model.HourlyReadinessPoint
import com.example.domain.model.PressureUnit
import com.example.domain.model.ReadinessEvaluation
import com.example.domain.model.ReadinessStatus
import com.example.domain.model.SolarPhaseWindows
import com.example.domain.model.TemperatureUnit
import com.example.domain.model.WindSpeedUnit
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

object AtmosphericCalculator {

    /**
     * Converts Celsius to user-selected temperature unit.
     */
    fun formatTemperature(tempC: Double, unit: TemperatureUnit): String {
        val value = when (unit) {
            TemperatureUnit.CELSIUS -> tempC
            TemperatureUnit.FAHRENHEIT -> (tempC * 9.0 / 5.0) + 32.0
        }
        return String.format(Locale.US, "%.1f%s", value, unit.symbol)
    }

    /**
     * Converts wind speed in km/h to user-selected unit.
     */
    fun formatWindSpeed(windKmh: Double, unit: WindSpeedUnit): String {
        val value = when (unit) {
            WindSpeedUnit.KMH -> windKmh
            WindSpeedUnit.MPH -> windKmh * 0.621371
            WindSpeedUnit.MS -> windKmh / 3.6
        }
        return String.format(Locale.US, "%.1f %s", value, unit.symbol)
    }

    /**
     * Converts pressure in hPa to user-selected unit.
     */
    fun formatPressure(pressureHpa: Double, unit: PressureUnit): String {
        return when (unit) {
            PressureUnit.HPA -> String.format(Locale.US, "%.1f hPa", pressureHpa)
            PressureUnit.INHG -> String.format(Locale.US, "%.2f inHg", pressureHpa * 0.02953)
        }
    }

    /**
     * Computes Dew Point in Celsius using the Magnus-Tetens approximation.
     */
    fun calculateDewPointC(tempC: Double, relativeHumidityPercent: Int): Double {
        val rh = relativeHumidityPercent.coerceIn(1, 100) / 100.0
        val a = 17.27
        val b = 237.7
        val alpha = ((a * tempC) / (b + tempC)) + ln(rh)
        return (b * alpha) / (a - alpha)
    }

    /**
     * Computes barometric altitude in meters from static pressure (hPa) and sea-level reference pressure (hPa)
     * using the international barometric formula.
     */
    fun calculateBarometricAltitudeMeters(
        pressureHpa: Double,
        seaLevelReferenceHpa: Double = 1013.25
    ): Double {
        if (pressureHpa <= 0.0 || seaLevelReferenceHpa <= 0.0) return 0.0
        val ratio = pressureHpa / seaLevelReferenceHpa
        return 44330.0 * (1.0 - ratio.pow(1.0 / 5.255))
    }

    /**
     * Back-calculates sea-level reference pressure (QNH) given known station elevation in meters
     * and measured surface pressure in hPa.
     */
    fun calibrateSeaLevelPressureHpa(
        surfacePressureHpa: Double,
        knownElevationMeters: Int
    ): Double {
        val factor = (1.0 - (knownElevationMeters / 44330.0)).coerceAtLeast(0.1)
        return surfacePressureHpa / factor.pow(5.255)
    }

    /**
     * Converts wind direction degrees (0..360) to 8-point cardinal compass code.
     */
    fun compassDirection(degrees: Int): String {
        val normalized = ((degrees % 360) + 360) % 360
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = ((normalized + 22.5) / 45.0).toInt() % 8
        return directions[index]
    }

    /**
     * Interprets WMO Weather Code into human-readable atmospheric condition.
     */
    fun weatherConditionLabel(wmoCode: Int): String = when (wmoCode) {
        0 -> "Clear Stratosphere"
        1, 2 -> "Scattered Cumulus"
        3 -> "Overcast Canopy"
        45, 48 -> "Advection Fog / Rime"
        51, 53, 55 -> "Light Mist Drizzle"
        61, 63, 65 -> "Active Frontal Rain"
        71, 73, 75, 77 -> "Alpine Snowfall"
        80, 81, 82 -> "Convective Rain Showers"
        85, 86 -> "Heavy Snow Squalls"
        95, 96, 99 -> "Electrical Storm Cell"
        else -> "Variable Atmospheric Flow"
    }

    /**
     * Computes Golden Hour, Blue Hour, and Solar Noon windows from ISO sunrise/sunset strings.
     */
    fun computeSolarWindows(sunriseIso: String, sunsetIso: String): SolarPhaseWindows {
        val riseMinutes = parseMinutesFromIso(sunriseIso, defaultMinutes = 6 * 60 + 18)
        val setMinutes = parseMinutesFromIso(sunsetIso, defaultMinutes = 18 * 60 + 42)
        val daylightMinutes = (setMinutes - riseMinutes).coerceAtLeast(60)
        val noonMinutes = riseMinutes + daylightMinutes / 2

        val morningBlueStart = (riseMinutes - 28).coerceAtLeast(0)
        val morningGoldenEnd = (riseMinutes + 48).coerceAtMost(23 * 60 + 59)
        val eveningGoldenStart = (setMinutes - 48).coerceAtLeast(0)
        val eveningBlueEnd = (setMinutes + 28).coerceAtMost(23 * 60 + 59)

        val hours = daylightMinutes / 60
        val mins = daylightMinutes % 60

        return SolarPhaseWindows(
            sunriseTime = formatMinutesAsClock(riseMinutes),
            sunsetTime = formatMinutesAsClock(setMinutes),
            morningBlueHour = "${formatMinutesAsClock(morningBlueStart)} – ${formatMinutesAsClock(riseMinutes)}",
            morningGoldenHour = "${formatMinutesAsClock(riseMinutes)} – ${formatMinutesAsClock(morningGoldenEnd)}",
            solarNoonTime = formatMinutesAsClock(noonMinutes),
            eveningGoldenHour = "${formatMinutesAsClock(eveningGoldenStart)} – ${formatMinutesAsClock(setMinutes)}",
            eveningBlueHour = "${formatMinutesAsClock(setMinutes)} – ${formatMinutesAsClock(eveningBlueEnd)}",
            daylightDurationLabel = "${hours}h ${mins}m"
        )
    }

    private fun parseMinutesFromIso(iso: String, defaultMinutes: Int): Int {
        val timePart = iso.substringAfter('T', missingDelimiterValue = iso).trim()
        val parts = timePart.split(":")
        if (parts.size < 2) return defaultMinutes
        val h = parts[0].toIntOrNull() ?: return defaultMinutes
        val m = parts[1].take(2).toIntOrNull() ?: return defaultMinutes
        return (h.coerceIn(0, 23) * 60) + m.coerceIn(0, 59)
    }

    private fun formatMinutesAsClock(totalMinutes: Int): String {
        val normalized = ((totalMinutes % 1440) + 1440) % 1440
        val h = normalized / 60
        val m = normalized % 60
        return String.format(Locale.US, "%02d:%02d", h, m)
    }
}

object InputValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    fun validateStationInput(
        name: String,
        region: String,
        country: String,
        latitudeText: String,
        longitudeText: String,
        elevationText: String
    ): ValidationResult {
        if (name.trim().length < 2) {
            return ValidationResult(false, "Station name must be at least 2 characters.")
        }
        if (country.trim().length < 2) {
            return ValidationResult(false, "Please enter a valid country or territory name.")
        }
        val lat = latitudeText.trim().toDoubleOrNull()
            ?: return ValidationResult(false, "Latitude must be a valid decimal number (e.g., 45.9237).")
        if (lat < -90.0 || lat > 90.0) {
            return ValidationResult(false, "Latitude must be between -90.0° and +90.0°.")
        }
        val lon = longitudeText.trim().toDoubleOrNull()
            ?: return ValidationResult(false, "Longitude must be a valid decimal number (e.g., 6.8694).")
        if (lon < -180.0 || lon > 180.0) {
            return ValidationResult(false, "Longitude must be between -180.0° and +180.0°.")
        }
        val elev = elevationText.trim().toIntOrNull()
            ?: return ValidationResult(false, "Elevation must be a whole number in meters (e.g., 1035).")
        if (elev < -430 || elev > 9000) {
            return ValidationResult(false, "Elevation must be between -430 m and 9,000 m.")
        }
        return ValidationResult(true)
    }

    fun validateActivityProfileInput(
        name: String,
        maxWindText: String,
        maxGustText: String,
        maxPrecipText: String,
        maxUvText: String,
        minTempText: String,
        maxTempText: String
    ): ValidationResult {
        if (name.trim().length < 3) {
            return ValidationResult(false, "Profile name must be at least 3 characters.")
        }
        val maxWind = maxWindText.trim().toDoubleOrNull()
            ?: return ValidationResult(false, "Max sustained wind must be a valid number.")
        if (maxWind < 5.0 || maxWind > 150.0) {
            return ValidationResult(false, "Max wind threshold must be between 5 and 150 km/h.")
        }
        val maxGust = maxGustText.trim().toDoubleOrNull()
            ?: return ValidationResult(false, "Max wind gust must be a valid number.")
        if (maxGust < maxWind || maxGust > 200.0) {
            return ValidationResult(false, "Max gust must be greater than or equal to sustained wind (up to 200 km/h).")
        }
        val maxPrecip = maxPrecipText.trim().toIntOrNull()
            ?: return ValidationResult(false, "Max precipitation probability must be an integer (0–100%).")
        if (maxPrecip !in 5..100) {
            return ValidationResult(false, "Max precipitation probability must be between 5% and 100%.")
        }
        val maxUv = maxUvText.trim().toDoubleOrNull()
            ?: return ValidationResult(false, "Max UV index must be a valid number (1–15).")
        if (maxUv < 1.0 || maxUv > 16.0) {
            return ValidationResult(false, "Max UV threshold must be between 1.0 and 16.0.")
        }
        val minTemp = minTempText.trim().toDoubleOrNull()
            ?: return ValidationResult(false, "Min temperature must be a valid number in °C.")
        val maxTemp = maxTempText.trim().toDoubleOrNull()
            ?: return ValidationResult(false, "Max temperature must be a valid number in °C.")
        if (minTemp >= maxTemp) {
            return ValidationResult(false, "Minimum temperature must be strictly lower than maximum temperature.")
        }
        if (minTemp < -50.0 || maxTemp > 60.0) {
            return ValidationResult(false, "Temperature range must stay within -50°C to +60°C.")
        }
        return ValidationResult(true)
    }
}

object ActivityWindowEvaluator {

    /**
     * Evaluates a FieldStation against an ActivityProfile to produce a 0..100 readiness score,
     * status classification, limiting factors, and 24-hour window curve.
     */
    fun evaluate(station: FieldStation, profile: ActivityProfile): ReadinessEvaluation {
        val limitingFactors = mutableListOf<String>()
        var penalty = 0.0

        // 1. Sustained Wind Check
        if (station.windSpeedKmh > profile.maxWindKmh) {
            val excess = station.windSpeedKmh - profile.maxWindKmh
            penalty += (22.0 + excess * 1.4).coerceAtMost(45.0)
            limitingFactors.add(
                String.format(
                    Locale.US,
                    "Sustained wind %.1f km/h exceeds %.0f km/h threshold",
                    station.windSpeedKmh,
                    profile.maxWindKmh
                )
            )
        } else if (station.windSpeedKmh > profile.maxWindKmh * 0.8) {
            penalty += 9.0
        }

        // 2. Wind Gust Check
        if (station.windGustsKmh > profile.maxGustKmh) {
            val excess = station.windGustsKmh - profile.maxGustKmh
            penalty += (18.0 + excess * 1.1).coerceAtMost(35.0)
            limitingFactors.add(
                String.format(
                    Locale.US,
                    "Peak gusts %.1f km/h exceed %.0f km/h safety limit",
                    station.windGustsKmh,
                    profile.maxGustKmh
                )
            )
        }

        // 3. Precipitation Probability Check
        if (station.precipProbabilityPercent > profile.maxPrecipProbPercent) {
            val excess = station.precipProbabilityPercent - profile.maxPrecipProbPercent
            penalty += (20.0 + excess * 0.6).coerceAtMost(40.0)
            limitingFactors.add(
                "Precipitation risk ${station.precipProbabilityPercent}% > ${profile.maxPrecipProbPercent}% max"
            )
        }

        // 4. Temperature Envelope Check
        if (station.temperatureC < profile.minTempC) {
            val deficit = profile.minTempC - station.temperatureC
            penalty += (18.0 + deficit * 2.2).coerceAtMost(40.0)
            limitingFactors.add(
                String.format(
                    Locale.US,
                    "Ambient %.1f°C below %.0f°C minimum envelope",
                    station.temperatureC,
                    profile.minTempC
                )
            )
        } else if (station.temperatureC > profile.maxTempC) {
            val excess = station.temperatureC - profile.maxTempC
            penalty += (18.0 + excess * 2.2).coerceAtMost(40.0)
            limitingFactors.add(
                String.format(
                    Locale.US,
                    "Ambient %.1f°C above %.0f°C thermal limit",
                    station.temperatureC,
                    profile.maxTempC
                )
            )
        }

        // 5. Category-specific Cloud Cover or UV checks
        if (profile.category == ActivityCategory.ASTROPHOTOGRAPHY &&
            station.cloudCoverPercent > profile.maxCloudCoverPercent
        ) {
            val excess = station.cloudCoverPercent - profile.maxCloudCoverPercent
            penalty += (25.0 + excess * 0.5).coerceAtMost(48.0)
            limitingFactors.add(
                "Cloud cover ${station.cloudCoverPercent}% obscures deep-sky aperture (> ${profile.maxCloudCoverPercent}%)"
            )
        }

        if (station.uvIndex > profile.maxUvIndex) {
            val excess = station.uvIndex - profile.maxUvIndex
            penalty += (14.0 + excess * 3.0).coerceAtMost(30.0)
            limitingFactors.add(
                String.format(
                    Locale.US,
                    "UV Index %.1f exceeds %.1f exposure cap",
                    station.uvIndex,
                    profile.maxUvIndex
                )
            )
        }

        // 6. Air Quality Check
        if (station.usAqi > profile.maxAqi) {
            val excess = station.usAqi - profile.maxAqi
            penalty += (15.0 + excess * 0.35).coerceAtMost(35.0)
            limitingFactors.add("US AQI ${station.usAqi} exceeds clean-air limit (${profile.maxAqi})")
        }

        val currentScore = (100.0 - penalty).roundToInt().coerceIn(5, 100)
        val status = scoreToStatus(currentScore)

        val hourlyReadiness = station.hourlyPoints.map { point ->
            val hScore = evaluateSingleHour(point, profile)
            HourlyReadinessPoint(
                hourLabel = point.hourLabel,
                score = hScore,
                status = scoreToStatus(hScore),
                tempC = point.tempC,
                windKmh = point.windKmh,
                precipProbPercent = point.precipProbPercent
            )
        }

        val bestHour = hourlyReadiness.maxByOrNull { it.score }
        val bestWindowLabel = if (bestHour != null) {
            "${bestHour.hourLabel} (${bestHour.score}/100)"
        } else {
            "Current Window (${currentScore}/100)"
        }

        val headline = when (status) {
            ReadinessStatus.GO -> "All atmospheric parameters nominal for ${profile.name}."
            ReadinessStatus.CAUTION -> "Marginal conditions; monitor ${limitingFactors.firstOrNull() ?: "wind & UV shifts"}."
            ReadinessStatus.HOLD -> "Hold recommended due to ${limitingFactors.firstOrNull() ?: "adverse telemetry"}."
        }

        return ReadinessEvaluation(
            profile = profile,
            stationName = station.name,
            currentScore = currentScore,
            status = status,
            headline = headline,
            limitingFactors = if (limitingFactors.isEmpty()) {
                listOf("Wind, thermal envelope, UV, and AQI within nominal thresholds")
            } else {
                limitingFactors
            },
            bestWindowLabel = bestWindowLabel,
            bestWindowScore = bestHour?.score ?: currentScore,
            hourlyReadiness = hourlyReadiness
        )
    }

    private fun evaluateSingleHour(point: HourlyPoint, profile: ActivityProfile): Int {
        var penalty = 0.0
        if (point.windKmh > profile.maxWindKmh) {
            penalty += (20.0 + (point.windKmh - profile.maxWindKmh) * 1.3).coerceAtMost(45.0)
        }
        if (point.precipProbPercent > profile.maxPrecipProbPercent) {
            penalty += (18.0 + (point.precipProbPercent - profile.maxPrecipProbPercent) * 0.5).coerceAtMost(38.0)
        }
        if (point.tempC < profile.minTempC) {
            penalty += (15.0 + (profile.minTempC - point.tempC) * 2.0).coerceAtMost(35.0)
        } else if (point.tempC > profile.maxTempC) {
            penalty += (15.0 + (point.tempC - profile.maxTempC) * 2.0).coerceAtMost(35.0)
        }
        if (point.uvIndex > profile.maxUvIndex) {
            penalty += (12.0 + (point.uvIndex - profile.maxUvIndex) * 2.5).coerceAtMost(28.0)
        }
        if (profile.category == ActivityCategory.ASTROPHOTOGRAPHY &&
            point.cloudCoverPercent > profile.maxCloudCoverPercent
        ) {
            penalty += (22.0 + (point.cloudCoverPercent - profile.maxCloudCoverPercent) * 0.5).coerceAtMost(45.0)
        }
        return (100.0 - penalty).roundToInt().coerceIn(5, 100)
    }

    fun scoreToStatus(score: Int): ReadinessStatus = when {
        score >= 76 -> ReadinessStatus.GO
        score >= 48 -> ReadinessStatus.CAUTION
        else -> ReadinessStatus.HOLD
    }
}
