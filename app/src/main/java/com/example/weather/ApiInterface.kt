package com.example.weather

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiInterface {
    @GET("forecast")
    fun getWeatherData(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m,visibility",
        @Query("hourly") hourly: String = "temperature_2m,weather_code",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset",
        @Query("timezone") tz: String = "auto"
    ): Call<WeatherApp>

    // For searching city coordinates
    @GET("https://geocoding-api.open-meteo.com/v1/search")
    fun searchCity(
        @Query("name") cityName: String,
        @Query("count") count: Int = 10,
        @Query("language") lang: String = "en",
        @Query("format") format: String = "json"
    ): Call<GeocodingResponse>
}

data class GeocodingResponse(
    val results: List<GeocodingResult>?
)

data class GeocodingResult(
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val country: String? = null,
    val admin1: String? = null
)
