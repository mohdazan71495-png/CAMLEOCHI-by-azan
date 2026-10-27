package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class OpenMeteoForecastResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val elevation: Double? = null,
    val current: CurrentTelemetryDto? = null,
    val hourly: HourlyTelemetryDto? = null,
    val daily: DailyTelemetryDto? = null
)

data class CurrentTelemetryDto(
    @Json(name = "temperature_2m") val temperature2m: Double? = null,
    @Json(name = "apparent_temperature") val apparentTemperature: Double? = null,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Int? = null,
    @Json(name = "surface_pressure") val surfacePressure: Double? = null,
    @Json(name = "pressure_msl") val pressureMsl: Double? = null,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double? = null,
    @Json(name = "wind_gusts_10m") val windGusts10m: Double? = null,
    @Json(name = "wind_direction_10m") val windDirection10m: Int? = null,
    @Json(name = "cloud_cover") val cloudCover: Int? = null,
    @Json(name = "weather_code") val weatherCode: Int? = null
)

data class HourlyTelemetryDto(
    val time: List<String>? = null,
    @Json(name = "temperature_2m") val temperature2m: List<Double?>? = null,
    @Json(name = "uv_index") val uvIndex: List<Double?>? = null,
    @Json(name = "wind_speed_10m") val windSpeed10m: List<Double?>? = null,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int?>? = null,
    @Json(name = "cloud_cover") val cloudCover: List<Int?>? = null,
    val visibility: List<Double?>? = null
)

data class DailyTelemetryDto(
    val sunrise: List<String>? = null,
    val sunset: List<String>? = null,
    @Json(name = "uv_index_max") val uvIndexMax: List<Double?>? = null
)

data class OpenMeteoAirQualityResponse(
    val current: CurrentAirQualityDto? = null
)

data class CurrentAirQualityDto(
    @Json(name = "us_aqi") val usAqi: Int? = null,
    @Json(name = "pm2_5") val pm25: Double? = null,
    @Json(name = "pm10") val pm10: Double? = null
)

data class OpenMeteoGeocodingResponse(
    val results: List<GeocodingResultDto>? = null
)

data class GeocodingResultDto(
    val name: String? = null,
    val admin1: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val elevation: Double? = null
)

interface OpenMeteoForecastService {
    @GET("v1/forecast")
    suspend fun fetchForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") currentFields: String = "temperature_2m,apparent_temperature,relative_humidity_2m,surface_pressure,pressure_msl,wind_speed_10m,wind_gusts_10m,wind_direction_10m,cloud_cover,weather_code",
        @Query("hourly") hourlyFields: String = "temperature_2m,uv_index,wind_speed_10m,precipitation_probability,cloud_cover,visibility",
        @Query("daily") dailyFields: String = "sunrise,sunset,uv_index_max",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 1
    ): OpenMeteoForecastResponse
}

interface OpenMeteoAirQualityService {
    @GET("v1/air-quality")
    suspend fun fetchAirQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") currentFields: String = "us_aqi,pm2_5,pm10"
    ): OpenMeteoAirQualityResponse
}

interface OpenMeteoGeocodingService {
    @GET("v1/search")
    suspend fun searchLocations(
        @Query("name") query: String,
        @Query("count") count: Int = 8,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): OpenMeteoGeocodingResponse
}

object OpenMeteoNetworkModule {
    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    val forecastService: OpenMeteoForecastService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoForecastService::class.java)
    }

    val airQualityService: OpenMeteoAirQualityService by lazy {
        Retrofit.Builder()
            .baseUrl("https://air-quality-api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoAirQualityService::class.java)
    }

    val geocodingService: OpenMeteoGeocodingService by lazy {
        Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoGeocodingService::class.java)
    }
}
