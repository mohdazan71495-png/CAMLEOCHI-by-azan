package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.ActivityCategory
import com.example.domain.model.ActivityProfile
import com.example.domain.model.BarometerLogEntry
import com.example.domain.model.FieldStation
import com.example.domain.model.HourlyPoint
import java.util.Locale

@Entity(tableName = "field_stations")
data class FieldStationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
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
    val hourlyPayload: String, // pipe-separated "00:00,12.4,0.0,11.2,10,20|..."
    val fieldNotes: String = "",
    val lastSyncedEpochMillis: Long
) {
    fun toDomain(): FieldStation {
        val parsedHours = hourlyPayload.split("|")
            .filter { it.isNotBlank() }
            .mapNotNull { token ->
                val parts = token.split(",")
                if (parts.size >= 6) {
                    HourlyPoint(
                        hourLabel = parts[0],
                        tempC = parts[1].toDoubleOrNull() ?: temperatureC,
                        uvIndex = parts[2].toDoubleOrNull() ?: 0.0,
                        windKmh = parts[3].toDoubleOrNull() ?: windSpeedKmh,
                        precipProbPercent = parts[4].toIntOrNull() ?: 0,
                        cloudCoverPercent = parts[5].toIntOrNull() ?: cloudCoverPercent
                    )
                } else null
            }

        return FieldStation(
            id = id,
            name = name,
            region = region,
            country = country,
            latitude = latitude,
            longitude = longitude,
            elevationMeters = elevationMeters,
            isFavorite = isFavorite,
            isPrimary = isPrimary,
            temperatureC = temperatureC,
            apparentTempC = apparentTempC,
            humidityPercent = humidityPercent,
            surfacePressureHpa = surfacePressureHpa,
            seaLevelPressureHpa = seaLevelPressureHpa,
            windSpeedKmh = windSpeedKmh,
            windGustsKmh = windGustsKmh,
            windDirectionDeg = windDirectionDeg,
            uvIndex = uvIndex,
            visibilityMeters = visibilityMeters,
            cloudCoverPercent = cloudCoverPercent,
            precipProbabilityPercent = precipProbabilityPercent,
            weatherCode = weatherCode,
            sunriseIso = sunriseIso,
            sunsetIso = sunsetIso,
            usAqi = usAqi,
            pm25 = pm25,
            pm10 = pm10,
            hourlyPoints = if (parsedHours.isNotEmpty()) parsedHours else defaultHourlyCurve(temperatureC, uvIndex, windSpeedKmh, precipProbabilityPercent, cloudCoverPercent),
            fieldNotes = fieldNotes,
            lastSyncedEpochMillis = lastSyncedEpochMillis
        )
    }

    companion object {
        fun encodeHourlyPoints(points: List<HourlyPoint>): String {
            return points.joinToString("|") { pt ->
                String.format(
                    Locale.US,
                    "%s,%.1f,%.1f,%.1f,%d,%d",
                    pt.hourLabel,
                    pt.tempC,
                    pt.uvIndex,
                    pt.windKmh,
                    pt.precipProbPercent,
                    pt.cloudCoverPercent
                )
            }
        }

        fun defaultHourlyCurve(
            baseTempC: Double,
            peakUv: Double,
            baseWindKmh: Double,
            basePrecipProb: Int,
            baseCloud: Int
        ): List<HourlyPoint> {
            return (0 until 24).map { hour ->
                val diurnalOffset = when (hour) {
                    in 0..5 -> -3.5 + (hour * 0.2)
                    in 6..14 -> -2.0 + ((hour - 6) * 0.75)
                    else -> 4.0 - ((hour - 14) * 0.7)
                }
                val uv = when (hour) {
                    in 7..17 -> {
                        val distFromNoon = kotlin.math.abs(12 - hour)
                        (peakUv * (1.0 - distFromNoon / 6.0)).coerceAtLeast(0.2)
                    }
                    else -> 0.0
                }
                val wind = (baseWindKmh + (if (hour in 11..17) 3.5 else -1.5)).coerceAtLeast(2.0)
                val precip = (basePrecipProb + (if (hour in 14..19) 6 else 0)).coerceIn(0, 100)
                HourlyPoint(
                    hourLabel = String.format(Locale.US, "%02d:00", hour),
                    tempC = baseTempC + diurnalOffset,
                    uvIndex = uv,
                    windKmh = wind,
                    precipProbPercent = precip,
                    cloudCoverPercent = baseCloud
                )
            }
        }
    }
}

@Entity(tableName = "activity_profiles")
data class ActivityProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val categoryName: String,
    val maxWindKmh: Double,
    val maxGustKmh: Double,
    val maxPrecipProbPercent: Int,
    val maxUvIndex: Double,
    val maxCloudCoverPercent: Int,
    val minTempC: Double,
    val maxTempC: Double,
    val maxAqi: Int,
    val isBuiltIn: Boolean = false
) {
    fun toDomain(): ActivityProfile {
        val cat = ActivityCategory.entries.firstOrNull { it.name == categoryName }
            ?: ActivityCategory.TRAIL_RUN
        return ActivityProfile(
            id = id,
            name = name,
            category = cat,
            maxWindKmh = maxWindKmh,
            maxGustKmh = maxGustKmh,
            maxPrecipProbPercent = maxPrecipProbPercent,
            maxUvIndex = maxUvIndex,
            maxCloudCoverPercent = maxCloudCoverPercent,
            minTempC = minTempC,
            maxTempC = maxTempC,
            maxAqi = maxAqi,
            isBuiltIn = isBuiltIn
        )
    }
}

@Entity(tableName = "barometer_logs")
data class BarometerLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestampMillis: Long,
    val pressureHpa: Double,
    val altitudeMeters: Double,
    val note: String
) {
    fun toDomain(): BarometerLogEntry = BarometerLogEntry(
        timestampMillis = timestampMillis,
        pressureHpa = pressureHpa,
        altitudeMeters = altitudeMeters,
        note = note
    )
}

object SeedTelemetryData {
    fun initialStations(nowMillis: Long = System.currentTimeMillis()): List<FieldStationEntity> {
        return listOf(
            createSeedStation(
                name = "Chamonix Aiguille",
                region = "Auvergne-Rhône-Alpes",
                country = "France",
                latitude = 45.9237,
                longitude = 6.8694,
                elevationMeters = 1035,
                isFavorite = true,
                isPrimary = true,
                temperatureC = 11.4,
                apparentTempC = 9.8,
                humidityPercent = 58,
                surfacePressureHpa = 898.4,
                seaLevelPressureHpa = 1016.2,
                windSpeedKmh = 14.5,
                windGustsKmh = 24.0,
                windDirectionDeg = 285,
                uvIndex = 5.4,
                visibilityMeters = 24000,
                cloudCoverPercent = 18,
                precipProbabilityPercent = 12,
                weatherCode = 1,
                sunriseIso = "2026-10-27T06:48",
                sunsetIso = "2026-10-27T18:12",
                usAqi = 19,
                pm25 = 4.2,
                pm10 = 8.1,
                fieldNotes = "High-visibility alpine valley corridor. Watch katabatic gusts after 16:00.",
                nowMillis = nowMillis
            ),
            createSeedStation(
                name = "Mauna Kea Ridge",
                region = "Hawaii Island",
                country = "United States",
                latitude = 19.8207,
                longitude = -155.4681,
                elevationMeters = 4205,
                isFavorite = true,
                isPrimary = false,
                temperatureC = 3.2,
                apparentTempC = -1.4,
                humidityPercent = 22,
                surfacePressureHpa = 616.5,
                seaLevelPressureHpa = 1014.8,
                windSpeedKmh = 21.0,
                windGustsKmh = 33.5,
                windDirectionDeg = 75,
                uvIndex = 9.2,
                visibilityMeters = 45000,
                cloudCoverPercent = 6,
                precipProbabilityPercent = 4,
                weatherCode = 0,
                sunriseIso = "2026-10-27T06:18",
                sunsetIso = "2026-10-27T17:54",
                usAqi = 11,
                pm25 = 1.8,
                pm10 = 3.4,
                fieldNotes = "Sub-arcsecond astronomical seeing above inversion layer.",
                nowMillis = nowMillis
            ),
            createSeedStation(
                name = "Zermatt Gornergrat",
                region = "Valais",
                country = "Switzerland",
                latitude = 46.0207,
                longitude = 7.7491,
                elevationMeters = 1608,
                isFavorite = true,
                isPrimary = false,
                temperatureC = 7.8,
                apparentTempC = 5.9,
                humidityPercent = 64,
                surfacePressureHpa = 838.0,
                seaLevelPressureHpa = 1017.5,
                windSpeedKmh = 18.2,
                windGustsKmh = 29.0,
                windDirectionDeg = 210,
                uvIndex = 4.8,
                visibilityMeters = 19500,
                cloudCoverPercent = 28,
                precipProbabilityPercent = 18,
                weatherCode = 2,
                sunriseIso = "2026-10-27T06:52",
                sunsetIso = "2026-10-27T18:05",
                usAqi = 22,
                pm25 = 5.1,
                pm10 = 9.6,
                fieldNotes = "Stable barometer; crisp morning golden hour over Matterhorn east face.",
                nowMillis = nowMillis
            ),
            createSeedStation(
                name = "Banff Sulphur Peak",
                region = "Alberta",
                country = "Canada",
                latitude = 51.1784,
                longitude = -115.5708,
                elevationMeters = 1383,
                isFavorite = false,
                isPrimary = false,
                temperatureC = 4.6,
                apparentTempC = 1.2,
                humidityPercent = 52,
                surfacePressureHpa = 861.2,
                seaLevelPressureHpa = 1012.9,
                windSpeedKmh = 26.4,
                windGustsKmh = 41.0,
                windDirectionDeg = 310,
                uvIndex = 3.9,
                visibilityMeters = 22000,
                cloudCoverPercent = 34,
                precipProbabilityPercent = 24,
                weatherCode = 2,
                sunriseIso = "2026-10-27T07:14",
                sunsetIso = "2026-10-27T18:02",
                usAqi = 24,
                pm25 = 5.8,
                pm10 = 11.2,
                fieldNotes = "Chinook arch forming along eastern Rockies crest.",
                nowMillis = nowMillis
            ),
            createSeedStation(
                name = "Kyoto Arashiyama",
                region = "Kansai",
                country = "Japan",
                latitude = 35.0116,
                longitude = 135.7681,
                elevationMeters = 68,
                isFavorite = false,
                isPrimary = false,
                temperatureC = 18.6,
                apparentTempC = 18.2,
                humidityPercent = 66,
                surfacePressureHpa = 1008.4,
                seaLevelPressureHpa = 1016.4,
                windSpeedKmh = 9.2,
                windGustsKmh = 15.0,
                windDirectionDeg = 140,
                uvIndex = 6.1,
                visibilityMeters = 16000,
                cloudCoverPercent = 22,
                precipProbabilityPercent = 15,
                weatherCode = 1,
                sunriseIso = "2026-10-27T06:11",
                sunsetIso = "2026-10-27T17:14",
                usAqi = 34,
                pm25 = 8.4,
                pm10 = 14.9,
                fieldNotes = "Calm basin airflow ideal for drone photogrammetry and cycling.",
                nowMillis = nowMillis
            ),
            createSeedStation(
                name = "Reykjavik Aurora Bay",
                region = "Capital Region",
                country = "Iceland",
                latitude = 64.1466,
                longitude = -21.9426,
                elevationMeters = 37,
                isFavorite = false,
                isPrimary = false,
                temperatureC = 2.4,
                apparentTempC = -2.8,
                humidityPercent = 74,
                surfacePressureHpa = 996.2,
                seaLevelPressureHpa = 1000.8,
                windSpeedKmh = 34.0,
                windGustsKmh = 52.0,
                windDirectionDeg = 45,
                uvIndex = 1.4,
                visibilityMeters = 14000,
                cloudCoverPercent = 62,
                precipProbabilityPercent = 48,
                weatherCode = 3,
                sunriseIso = "2026-10-27T08:45",
                sunsetIso = "2026-10-27T17:38",
                usAqi = 14,
                pm25 = 2.9,
                pm10 = 6.2,
                fieldNotes = "North Atlantic low-pressure trough; high gust caution for UAVs.",
                nowMillis = nowMillis
            )
        )
    }

    private fun createSeedStation(
        name: String,
        region: String,
        country: String,
        latitude: Double,
        longitude: Double,
        elevationMeters: Int,
        isFavorite: Boolean,
        isPrimary: Boolean,
        temperatureC: Double,
        apparentTempC: Double,
        humidityPercent: Int,
        surfacePressureHpa: Double,
        seaLevelPressureHpa: Double,
        windSpeedKmh: Double,
        windGustsKmh: Double,
        windDirectionDeg: Int,
        uvIndex: Double,
        visibilityMeters: Int,
        cloudCoverPercent: Int,
        precipProbabilityPercent: Int,
        weatherCode: Int,
        sunriseIso: String,
        sunsetIso: String,
        usAqi: Int,
        pm25: Double,
        pm10: Double,
        fieldNotes: String,
        nowMillis: Long
    ): FieldStationEntity {
        val curve = FieldStationEntity.defaultHourlyCurve(
            baseTempC = temperatureC,
            peakUv = uvIndex,
            baseWindKmh = windSpeedKmh,
            basePrecipProb = precipProbabilityPercent,
            baseCloud = cloudCoverPercent
        )
        return FieldStationEntity(
            name = name,
            region = region,
            country = country,
            latitude = latitude,
            longitude = longitude,
            elevationMeters = elevationMeters,
            isFavorite = isFavorite,
            isPrimary = isPrimary,
            temperatureC = temperatureC,
            apparentTempC = apparentTempC,
            humidityPercent = humidityPercent,
            surfacePressureHpa = surfacePressureHpa,
            seaLevelPressureHpa = seaLevelPressureHpa,
            windSpeedKmh = windSpeedKmh,
            windGustsKmh = windGustsKmh,
            windDirectionDeg = windDirectionDeg,
            uvIndex = uvIndex,
            visibilityMeters = visibilityMeters,
            cloudCoverPercent = cloudCoverPercent,
            precipProbabilityPercent = precipProbabilityPercent,
            weatherCode = weatherCode,
            sunriseIso = sunriseIso,
            sunsetIso = sunsetIso,
            usAqi = usAqi,
            pm25 = pm25,
            pm10 = pm10,
            hourlyPayload = FieldStationEntity.encodeHourlyPoints(curve),
            fieldNotes = fieldNotes,
            lastSyncedEpochMillis = nowMillis
        )
    }

    fun initialActivityProfiles(): List<ActivityProfileEntity> = listOf(
        ActivityProfileEntity(
            name = "Alpine Trail Run",
            categoryName = ActivityCategory.TRAIL_RUN.name,
            maxWindKmh = 35.0,
            maxGustKmh = 50.0,
            maxPrecipProbPercent = 35,
            maxUvIndex = 8.0,
            maxCloudCoverPercent = 85,
            minTempC = -4.0,
            maxTempC = 26.0,
            maxAqi = 75,
            isBuiltIn = true
        ),
        ActivityProfileEntity(
            name = "Deep-Sky Astrophotography",
            categoryName = ActivityCategory.ASTROPHOTOGRAPHY.name,
            maxWindKmh = 22.0,
            maxGustKmh = 32.0,
            maxPrecipProbPercent = 15,
            maxUvIndex = 12.0,
            maxCloudCoverPercent = 20,
            minTempC = -15.0,
            maxTempC = 30.0,
            maxAqi = 50,
            isBuiltIn = true
        ),
        ActivityProfileEntity(
            name = "Aerial Drone Telemetry",
            categoryName = ActivityCategory.DRONE_FLIGHT.name,
            maxWindKmh = 28.0,
            maxGustKmh = 38.0,
            maxPrecipProbPercent = 15,
            maxUvIndex = 11.0,
            maxCloudCoverPercent = 90,
            minTempC = 0.0,
            maxTempC = 35.0,
            maxAqi = 100,
            isBuiltIn = true
        ),
        ActivityProfileEntity(
            name = "Endurance Road Cycling",
            categoryName = ActivityCategory.CYCLING.name,
            maxWindKmh = 30.0,
            maxGustKmh = 44.0,
            maxPrecipProbPercent = 25,
            maxUvIndex = 7.5,
            maxCloudCoverPercent = 85,
            minTempC = 4.0,
            maxTempC = 32.0,
            maxAqi = 65,
            isBuiltIn = true
        ),
        ActivityProfileEntity(
            name = "Solar & UV Field Survey",
            categoryName = ActivityCategory.SOLAR_SURVEY.name,
            maxWindKmh = 40.0,
            maxGustKmh = 55.0,
            maxPrecipProbPercent = 20,
            maxUvIndex = 10.5,
            maxCloudCoverPercent = 35,
            minTempC = -5.0,
            maxTempC = 38.0,
            maxAqi = 80,
            isBuiltIn = true
        )
    )
}
