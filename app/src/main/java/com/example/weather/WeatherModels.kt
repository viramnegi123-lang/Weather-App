package com.example.weather

import com.google.gson.annotations.SerializedName

data class WeatherApp(
    val latitude: Double,
    val longitude: Double,
    val current: CurrentMeteo,
    val hourly: HourlyMeteo,
    val daily: DailyMeteo,
    val name: String = "Current Location", // Manually set
    val dt: Long = System.currentTimeMillis() / 1000
)

data class CurrentMeteo(
    val time: String,
    @SerializedName("temperature_2m") val temp: Double,
    @SerializedName("relative_humidity_2m") val humidity: Int,
    @SerializedName("apparent_temperature") val feels_like: Double,
    @SerializedName("weather_code") val weatherCode: Int,
    @SerializedName("surface_pressure") val pressure: Double,
    @SerializedName("wind_speed_10m") val windSpeed: Double,
    @SerializedName("wind_direction_10m") val windDeg: Int,
    @SerializedName("visibility") val visibility: Double
)

data class HourlyMeteo(
    val time: List<String>,
    @SerializedName("temperature_2m") val temp: List<Double>,
    @SerializedName("weather_code") val weatherCode: List<Int>
)

data class DailyMeteo(
    val time: List<String>,
    @SerializedName("weather_code") val weatherCode: List<Int>,
    @SerializedName("temperature_2m_max") val tempMax: List<Double>,
    @SerializedName("temperature_2m_min") val tempMin: List<Double>,
    val sunrise: List<String>,
    val sunset: List<String>
)

// UI Compatibility / Helper Classes for Adapters
data class ForecastItem(
    val dt: Long,
    val temp: Double,
    val maxTemp: Double? = null,
    val minTemp: Double? = null,
    val weatherCode: Int,
    val timeString: String
)
