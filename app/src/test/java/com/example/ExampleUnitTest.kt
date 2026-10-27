package com.example

import com.example.data.local.SeedTelemetryData
import com.example.domain.model.ActivityCategory
import com.example.domain.model.ActivityProfile
import com.example.domain.model.PressureUnit
import com.example.domain.model.ReadinessStatus
import com.example.domain.model.TemperatureUnit
import com.example.domain.model.WindSpeedUnit
import com.example.domain.usecase.ActivityWindowEvaluator
import com.example.domain.usecase.AtmosphericCalculator
import com.example.domain.usecase.InputValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `temperature wind and pressure unit conversions are accurate`() {
        assertEquals("20.0°C", AtmosphericCalculator.formatTemperature(20.0, TemperatureUnit.CELSIUS))
        assertEquals("68.0°F", AtmosphericCalculator.formatTemperature(20.0, TemperatureUnit.FAHRENHEIT))
        assertEquals("36.0 km/h", AtmosphericCalculator.formatWindSpeed(36.0, WindSpeedUnit.KMH))
        assertEquals("10.0 m/s", AtmosphericCalculator.formatWindSpeed(36.0, WindSpeedUnit.MS))
        assertEquals("1013.3 hPa", AtmosphericCalculator.formatPressure(1013.25, PressureUnit.HPA))
        assertEquals("29.92 inHg", AtmosphericCalculator.formatPressure(1013.25, PressureUnit.INHG))
    }

    @Test
    fun `barometric altitude and QNH calibration are mutually consistent`() {
        val calibratedQnh = AtmosphericCalculator.calibrateSeaLevelPressureHpa(
            surfacePressureHpa = 898.4,
            knownElevationMeters = 1035
        )
        val estimatedAltitude = AtmosphericCalculator.calculateBarometricAltitudeMeters(
            pressureHpa = 898.4,
            seaLevelReferenceHpa = calibratedQnh
        )
        assertEquals(1035.0, estimatedAltitude, 1.5)
    }

    @Test
    fun `solar windows compute valid golden and blue hour intervals`() {
        val windows = AtmosphericCalculator.computeSolarWindows(
            sunriseIso = "2026-10-27T06:30",
            sunsetIso = "2026-10-27T18:30"
        )
        assertEquals("06:30", windows.sunriseTime)
        assertEquals("18:30", windows.sunsetTime)
        assertEquals("12:30", windows.solarNoonTime)
        assertEquals("12h 0m", windows.daylightDurationLabel)
        assertTrue(windows.morningGoldenHour.startsWith("06:30"))
        assertTrue(windows.eveningGoldenHour.endsWith("18:30"))
    }

    @Test
    fun `station input validator rejects out of bounds coordinates and accepts valid station`() {
        val invalidLat = InputValidator.validateStationInput(
            name = "Summit Base",
            region = "Valais",
            country = "Switzerland",
            latitudeText = "98.5",
            longitudeText = "7.75",
            elevationText = "1600"
        )
        assertFalse(invalidLat.isValid)

        val valid = InputValidator.validateStationInput(
            name = "Summit Base",
            region = "Valais",
            country = "Switzerland",
            latitudeText = "46.0207",
            longitudeText = "7.7491",
            elevationText = "1608"
        )
        assertTrue(valid.isValid)
    }

    @Test
    fun `activity window evaluator distinguishes GO and HOLD conditions`() {
        val calmStation = SeedTelemetryData.initialStations().first().toDomain()
        val trailRunProfile = ActivityProfile(
            id = 1L,
            name = "Alpine Trail Run",
            category = ActivityCategory.TRAIL_RUN,
            maxWindKmh = 35.0,
            maxGustKmh = 50.0,
            maxPrecipProbPercent = 35,
            maxUvIndex = 8.0,
            maxCloudCoverPercent = 85,
            minTempC = -4.0,
            maxTempC = 26.0,
            maxAqi = 75,
            isBuiltIn = true
        )
        val goEval = ActivityWindowEvaluator.evaluate(calmStation, trailRunProfile)
        assertEquals(ReadinessStatus.GO, goEval.status)
        assertTrue(goEval.currentScore >= 76)

        val stormyStation = calmStation.copy(
            windSpeedKmh = 58.0,
            windGustsKmh = 82.0,
            precipProbabilityPercent = 85
        )
        val holdEval = ActivityWindowEvaluator.evaluate(stormyStation, trailRunProfile)
        assertEquals(ReadinessStatus.HOLD, holdEval.status)
        assertTrue(holdEval.currentScore < 48)
    }
}
