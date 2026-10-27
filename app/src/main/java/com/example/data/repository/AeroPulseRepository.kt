package com.example.data.repository

import com.example.data.local.ActivityProfileEntity
import com.example.data.local.AeroPulseDao
import com.example.data.local.BarometerLogEntity
import com.example.data.local.FieldStationEntity
import com.example.data.local.SeedTelemetryData
import com.example.data.remote.OpenMeteoAirQualityService
import com.example.data.remote.OpenMeteoForecastService
import com.example.data.remote.OpenMeteoGeocodingService
import com.example.data.remote.OpenMeteoNetworkModule
import com.example.domain.model.ActivityProfile
import com.example.domain.model.BarometerLogEntry
import com.example.domain.model.FieldStation
import com.example.domain.model.GeocodingCandidate
import com.example.domain.model.HourlyPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AeroPulseRepository(
    private val dao: AeroPulseDao,
    private val forecastService: OpenMeteoForecastService = OpenMeteoNetworkModule.forecastService,
    private val airQualityService: OpenMeteoAirQualityService = OpenMeteoNetworkModule.airQualityService,
    private val geocodingService: OpenMeteoGeocodingService = OpenMeteoNetworkModule.geocodingService
) {

    val stationsFlow: Flow<List<FieldStation>> = dao.observeAllStations().map { list ->
        list.map { it.toDomain() }
    }

    val activityProfilesFlow: Flow<List<ActivityProfile>> = dao.observeActivityProfiles().map { list ->
        list.map { it.toDomain() }
    }

    val barometerLogsFlow: Flow<List<BarometerLogEntry>> = dao.observeBarometerLogs().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        if (dao.getStationCount() == 0) {
            dao.insertStations(SeedTelemetryData.initialStations())
        }
        if (dao.getActivityProfileCount() == 0) {
            dao.insertActivityProfiles(SeedTelemetryData.initialActivityProfiles())
        }
    }

    suspend fun syncAllStationsFromNetwork(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val existing = dao.getAllStationsOnce()
            var updatedCount = 0
            for (station in existing) {
                val updated = fetchRemoteTelemetryForEntity(station)
                if (updated != null) {
                    dao.updateStation(updated)
                    updatedCount++
                }
            }
            if (updatedCount == 0 && existing.isNotEmpty()) {
                throw IllegalStateException("Unable to reach Open-Meteo servers; displaying local Room SQLite telemetry.")
            }
            updatedCount
        }
    }

    suspend fun syncSingleStation(stationId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val target = dao.getAllStationsOnce().firstOrNull { it.id == stationId }
                ?: throw IllegalArgumentException("Station not found")
            val updated = fetchRemoteTelemetryForEntity(target)
                ?: throw IllegalStateException("Network request failed; keeping local Room cache.")
            dao.updateStation(updated)
        }
    }

    private suspend fun fetchRemoteTelemetryForEntity(station: FieldStationEntity): FieldStationEntity? {
        return try {
            val forecast = forecastService.fetchForecast(
                latitude = station.latitude,
                longitude = station.longitude
            )
            val airQuality = runCatching {
                airQualityService.fetchAirQuality(
                    latitude = station.latitude,
                    longitude = station.longitude
                )
            }.getOrNull()

            val current = forecast.current ?: return null
            val hourly = forecast.hourly
            val daily = forecast.daily

            val hourlyPoints = mutableListOf<HourlyPoint>()
            val times = hourly?.time.orEmpty()
            val temps = hourly?.temperature2m.orEmpty()
            val uvs = hourly?.uvIndex.orEmpty()
            val winds = hourly?.windSpeed10m.orEmpty()
            val precips = hourly?.precipitationProbability.orEmpty()
            val clouds = hourly?.cloudCover.orEmpty()

            val count = minOf(24, times.size)
            for (i in 0 until count) {
                val rawTime = times.getOrNull(i).orEmpty()
                val hourLabel = rawTime.substringAfter('T', missingDelimiterValue = rawTime).ifBlank {
                    String.format("%02d:00", i)
                }
                hourlyPoints.add(
                    HourlyPoint(
                        hourLabel = hourLabel,
                        tempC = temps.getOrNull(i) ?: current.temperature2m ?: station.temperatureC,
                        uvIndex = uvs.getOrNull(i) ?: 0.0,
                        windKmh = winds.getOrNull(i) ?: current.windSpeed10m ?: station.windSpeedKmh,
                        precipProbPercent = precips.getOrNull(i) ?: 0,
                        cloudCoverPercent = clouds.getOrNull(i) ?: current.cloudCover ?: station.cloudCoverPercent
                    )
                )
            }

            val maxUv = daily?.uvIndexMax?.firstOrNull()
                ?: hourlyPoints.maxOfOrNull { it.uvIndex }
                ?: station.uvIndex
            val maxPrecip = hourlyPoints.maxOfOrNull { it.precipProbPercent } ?: station.precipProbabilityPercent
            val currentVis = hourly?.visibility?.firstOrNull()?.toInt() ?: station.visibilityMeters

            station.copy(
                elevationMeters = forecast.elevation?.toInt() ?: station.elevationMeters,
                temperatureC = current.temperature2m ?: station.temperatureC,
                apparentTempC = current.apparentTemperature ?: station.apparentTempC,
                humidityPercent = current.relativeHumidity2m ?: station.humidityPercent,
                surfacePressureHpa = current.surfacePressure ?: station.surfacePressureHpa,
                seaLevelPressureHpa = current.pressureMsl ?: station.seaLevelPressureHpa,
                windSpeedKmh = current.windSpeed10m ?: station.windSpeedKmh,
                windGustsKmh = current.windGusts10m ?: station.windGustsKmh,
                windDirectionDeg = current.windDirection10m ?: station.windDirectionDeg,
                uvIndex = maxUv,
                visibilityMeters = currentVis,
                cloudCoverPercent = current.cloudCover ?: station.cloudCoverPercent,
                precipProbabilityPercent = maxPrecip,
                weatherCode = current.weatherCode ?: station.weatherCode,
                sunriseIso = daily?.sunrise?.firstOrNull() ?: station.sunriseIso,
                sunsetIso = daily?.sunset?.firstOrNull() ?: station.sunsetIso,
                usAqi = airQuality?.current?.usAqi ?: station.usAqi,
                pm25 = airQuality?.current?.pm25 ?: station.pm25,
                pm10 = airQuality?.current?.pm10 ?: station.pm10,
                hourlyPayload = if (hourlyPoints.isNotEmpty()) {
                    FieldStationEntity.encodeHourlyPoints(hourlyPoints)
                } else {
                    station.hourlyPayload
                },
                lastSyncedEpochMillis = System.currentTimeMillis()
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun searchLocationsRemote(query: String): Result<List<GeocodingCandidate>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = geocodingService.searchLocations(query.trim())
                response.results.orEmpty().mapNotNull { dto ->
                    val name = dto.name ?: return@mapNotNull null
                    val lat = dto.latitude ?: return@mapNotNull null
                    val lon = dto.longitude ?: return@mapNotNull null
                    GeocodingCandidate(
                        name = name,
                        region = dto.admin1 ?: "Region",
                        country = dto.country ?: "Global",
                        latitude = lat,
                        longitude = lon,
                        elevationMeters = dto.elevation?.toInt() ?: 150
                    )
                }
            }
        }

    suspend fun addStation(
        name: String,
        region: String,
        country: String,
        latitude: Double,
        longitude: Double,
        elevationMeters: Int,
        makePrimary: Boolean = true
    ): Long = withContext(Dispatchers.IO) {
        val defaultCurve = FieldStationEntity.defaultHourlyCurve(
            baseTempC = 14.0,
            peakUv = 5.5,
            baseWindKmh = 14.0,
            basePrecipProb = 15,
            baseCloud = 25
        )
        val initialEntity = FieldStationEntity(
            name = name.trim(),
            region = region.trim().ifBlank { "Field Sector" },
            country = country.trim(),
            latitude = latitude,
            longitude = longitude,
            elevationMeters = elevationMeters,
            isFavorite = true,
            isPrimary = makePrimary,
            temperatureC = 14.0,
            apparentTempC = 13.2,
            humidityPercent = 56,
            surfacePressureHpa = 1002.0,
            seaLevelPressureHpa = 1014.5,
            windSpeedKmh = 14.0,
            windGustsKmh = 22.0,
            windDirectionDeg = 240,
            uvIndex = 5.5,
            visibilityMeters = 20000,
            cloudCoverPercent = 25,
            precipProbabilityPercent = 15,
            weatherCode = 1,
            sunriseIso = "2026-10-27T06:35",
            sunsetIso = "2026-10-27T17:55",
            usAqi = 24,
            pm25 = 5.0,
            pm10 = 9.5,
            hourlyPayload = FieldStationEntity.encodeHourlyPoints(defaultCurve),
            fieldNotes = "Custom field station added locally.",
            lastSyncedEpochMillis = System.currentTimeMillis()
        )
        val enriched = fetchRemoteTelemetryForEntity(initialEntity) ?: initialEntity
        if (makePrimary) {
            dao.clearAllPrimaryFlags()
        }
        dao.insertStation(enriched)
    }

    suspend fun selectPrimaryStation(stationId: Long) = withContext(Dispatchers.IO) {
        dao.selectPrimaryStation(stationId)
    }

    suspend fun toggleFavorite(stationId: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        dao.setStationFavorite(stationId, isFavorite)
    }

    suspend fun updateStationNotes(stationId: Long, notes: String) = withContext(Dispatchers.IO) {
        dao.updateStationNotes(stationId, notes.trim())
    }

    suspend fun deleteStation(stationId: Long) = withContext(Dispatchers.IO) {
        dao.deleteStationById(stationId)
        val remaining = dao.getAllStationsOnce()
        if (remaining.isNotEmpty() && remaining.none { it.isPrimary }) {
            dao.markStationAsPrimary(remaining.first().id)
        }
    }

    suspend fun saveActivityProfile(
        id: Long = 0L,
        name: String,
        categoryName: String,
        maxWindKmh: Double,
        maxGustKmh: Double,
        maxPrecipProbPercent: Int,
        maxUvIndex: Double,
        maxCloudCoverPercent: Int,
        minTempC: Double,
        maxTempC: Double,
        maxAqi: Int
    ) = withContext(Dispatchers.IO) {
        val entity = ActivityProfileEntity(
            id = id,
            name = name.trim(),
            categoryName = categoryName,
            maxWindKmh = maxWindKmh,
            maxGustKmh = maxGustKmh,
            maxPrecipProbPercent = maxPrecipProbPercent,
            maxUvIndex = maxUvIndex,
            maxCloudCoverPercent = maxCloudCoverPercent,
            minTempC = minTempC,
            maxTempC = maxTempC,
            maxAqi = maxAqi,
            isBuiltIn = false
        )
        dao.insertActivityProfile(entity)
    }

    suspend fun deleteActivityProfile(profileId: Long) = withContext(Dispatchers.IO) {
        dao.deleteActivityProfileById(profileId)
    }

    suspend fun logBarometerSnapshot(
        pressureHpa: Double,
        altitudeMeters: Double,
        note: String
    ) = withContext(Dispatchers.IO) {
        dao.insertBarometerLog(
            BarometerLogEntity(
                timestampMillis = System.currentTimeMillis(),
                pressureHpa = pressureHpa,
                altitudeMeters = altitudeMeters,
                note = note.trim().ifBlank { "Manual Barometer Snapshot" }
            )
        )
    }

    suspend fun clearBarometerHistory() = withContext(Dispatchers.IO) {
        dao.clearBarometerLogs()
    }

    suspend fun resetAllDataToDefaults() = withContext(Dispatchers.IO) {
        dao.deleteAllStations()
        dao.deleteAllProfiles()
        dao.clearBarometerLogs()
        dao.insertStations(SeedTelemetryData.initialStations())
        dao.insertActivityProfiles(SeedTelemetryData.initialActivityProfiles())
    }
}
