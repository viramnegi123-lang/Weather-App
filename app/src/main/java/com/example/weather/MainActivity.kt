package com.example.weather

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weather.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var tts: TextToSpeech? = null
    private var aiSummary = ""
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var apiInterface: ApiInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.open-meteo.com/v1/")
            .build()
        apiInterface = retrofit.create(ApiInterface::class.java)

        tts = TextToSpeech(this, this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        checkLocationPermissionAndFetchWeather()
        setupSearchView()

        binding.voiceAssistantBtn.setOnClickListener {
            if (aiSummary.isNotEmpty()) {
                tts?.speak(aiSummary, TextToSpeech.QUEUE_FLUSH, null, null)
            } else {
                Toast.makeText(this, "Wait for data to load", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkLocationPermissionAndFetchWeather() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && 
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 100)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                processLocation(location)
            } else {
                // Request a fresh location if lastLocation is null
                val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 1000
                ).setMaxUpdates(1).build()
                
                fusedLocationClient.requestLocationUpdates(locationRequest, object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                        val newLocation = result.lastLocation
                        if (newLocation != null) {
                            processLocation(newLocation)
                        } else {
                            fetchWeatherForCity("Dehradun")
                        }
                    }
                }, android.os.Looper.getMainLooper())
            }
        }
    }

    private fun processLocation(location: android.location.Location) {
        val cityName = try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            addresses?.firstOrNull()?.let { 
                it.locality ?: it.subAdminArea ?: it.adminArea 
            } ?: "Current Location"
        } catch (e: Exception) {
            "Current Location"
        }
        fetchWeatherData(location.latitude, location.longitude, cityName)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationPermissionAndFetchWeather()
            } else {
                // If permission denied, show Dehradun
                fetchWeatherForCity("Dehradun")
            }
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    fetchWeatherForCity(query)
                    binding.searchView.clearFocus()
                }
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean = true
        })
    }

    private fun fetchWeatherForCity(cityName: String) {
        if (cityName.isEmpty()) return
        binding.cityName.text = "Searching..."
        
        // Removed the hardcoded "Uttarakhand" suffix to allow searching any location
        apiInterface.searchCity(cityName).enqueue(object : Callback<GeocodingResponse> {
            override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                if (response.isSuccessful) {
                    val results = response.body()?.results
                    
                    // Prefer results in Uttarakhand if they exist, otherwise take the first match
                    val result = results?.find { 
                        it.admin1?.contains("Uttarakhand", ignoreCase = true) == true || 
                        (it.name?.contains("Uttarakhand", ignoreCase = true) == true)
                    } ?: results?.firstOrNull()

                    if (result != null && result.latitude != null && result.longitude != null) {
                        fetchWeatherData(result.latitude, result.longitude, result.name ?: "Unknown")
                    } else {
                        binding.cityName.text = "Place not found"
                    }
                } else {
                    binding.cityName.text = "Search Failed"
                    Toast.makeText(this@MainActivity, "API Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
                binding.cityName.text = "Error"
                Toast.makeText(this@MainActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchWeatherData(lat: Double, lon: Double, cityName: String) {
        binding.cityName.text = "Loading..."
        apiInterface.getWeatherData(lat, lon).enqueue(object : Callback<WeatherApp> {
            override fun onResponse(call: Call<WeatherApp>, response: Response<WeatherApp>) {
                val data = response.body()
                if (response.isSuccessful && data != null) {
                    updateUI(data, cityName)
                } else {
                    binding.cityName.text = "API Error"
                }
            }
            override fun onFailure(call: Call<WeatherApp>, t: Throwable) {
                binding.cityName.text = "Network Error"
            }
        })
    }

    private fun updateUI(data: WeatherApp, cityName: String) {
        val current = data.current
        binding.cityName.text = cityName
        binding.date.text = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault()).format(Date())
        binding.temp.text = "${current.temp.toInt()}°C"
        
        val weatherCondition = getWeatherCondition(current.weatherCode)
        binding.weather.text = weatherCondition
        binding.feelsLike.text = "Feels like: ${current.feels_like.toInt()}°C"
        
        binding.maxTemp.text = "Max: ${data.daily.tempMax.firstOrNull()?.toInt() ?: 0}°C"
        binding.minTemp.text = "Min: ${data.daily.tempMin.firstOrNull()?.toInt() ?: 0}°C"
        
        binding.humidityValue.text = "${current.humidity}%"
        binding.windValue.text = "${current.windSpeed} km/h ${getWindDirection(current.windDeg)}"
        binding.pressureValue.text = "${current.pressure.toInt()} hPa"
        binding.visibilityValue.text = "${(current.visibility / 1000).toInt()} km"
        
        // Time formatting for sunrise/sunset (Open-Meteo returns ISO strings)
        binding.sunriseValue.text = formatIsoTime(data.daily.sunrise.firstOrNull())
        binding.sunsetValue.text = formatIsoTime(data.daily.sunset.firstOrNull())

        changeImagesAccordingToWeather(weatherCondition)
        updateForecasts(data)
        generateAIInsights(data, cityName)
    }

    private fun updateForecasts(data: WeatherApp) {
        // Hourly
        val hourlyItems = data.hourly.time.indices.take(8).map { i ->
            ForecastItem(
                dt = 0, // Not strictly needed for hourly here
                temp = data.hourly.temp[i],
                weatherCode = data.hourly.weatherCode[i],
                timeString = formatIsoTime(data.hourly.time[i])
            )
        }
        binding.hourlyRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.hourlyRecyclerView.adapter = HourlyAdapter(hourlyItems)

        // Daily
        val dailyItems = data.daily.time.indices.map { i ->
            ForecastItem(
                dt = 0,
                temp = (data.daily.tempMax[i] + data.daily.tempMin[i]) / 2,
                maxTemp = data.daily.tempMax[i],
                minTemp = data.daily.tempMin[i],
                weatherCode = data.daily.weatherCode[i],
                timeString = data.daily.time[i] // e.g. "2023-10-27"
            )
        }
        binding.forecastRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.forecastRecyclerView.adapter = ForecastAdapter(dailyItems)
    }

    private fun generateAIInsights(data: WeatherApp, cityName: String) {
        val current = data.current
        val suggestions = mutableListOf<String>()
        val alerts = mutableListOf<String>()
        val voiceParts = mutableListOf<String>()
        
        val condition = getWeatherCondition(current.weatherCode)
        
        // 1. Condition based insights
        when (current.weatherCode) {
            0 -> suggestions.add("✨ Perfect day for outdoor activities!")
            1, 2, 3 -> suggestions.add("🌤️ Great weather for a walk.")
            45, 48 -> {
                alerts.add("⚠️ Low visibility due to fog. Drive safely!")
                voiceParts.add("Be careful while driving as it is foggy outside.")
            }
            51, 53, 55, 61, 63, 65 -> {
                suggestions.add("☔ Don't forget your umbrella!")
                voiceParts.add("It's raining, so don't forget your umbrella.")
            }
            71, 73, 75 -> {
                alerts.add("❄️ Snowfall alert! Keep yourself warm.")
                voiceParts.add("It is snowing, please stay warm.")
            }
            95, 96, 99 -> {
                alerts.add("⛈️ Thunderstorm warning! Stay indoors.")
                voiceParts.add("There is a thunderstorm warning, it's safer to stay indoors.")
            }
        }

        // 2. Temperature based insights
        if (current.temp > 35) {
            alerts.add("🔥 Extreme Heat! Avoid direct sun and stay hydrated.")
            voiceParts.add("The temperature is very high, please stay hydrated and avoid the sun.")
        } else if (current.temp > 28) {
            suggestions.add("🥤 It's quite warm. Keep a water bottle handy.")
        } else if (current.temp < 15 && current.temp >= 5) {
            suggestions.add("🧥 It's chilly. A light jacket is recommended.")
        } else if (current.temp < 5) {
            alerts.add("🥶 Very cold! Wear heavy winter clothes.")
            voiceParts.add("It's freezing outside, make sure to wear heavy winter clothes.")
        }

        // 3. Humidity & Wind
        if (current.humidity > 80) {
            suggestions.add("💧 High humidity. It might feel sticky.")
        }
        if (current.windSpeed > 25) {
            alerts.add("💨 Strong winds detected. Secure loose items.")
            voiceParts.add("Watch out for strong winds today.")
        }

        // Update UI
        binding.aiSuggestionText.text = if (suggestions.isNotEmpty()) suggestions.joinToString("\n") else "Weather looks stable today."
        binding.aiAlertsText.text = alerts.joinToString("\n")
        binding.aiAlertsText.visibility = if (alerts.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE

        // Prepare Voice Summary
        val alertSummary = if (alerts.isNotEmpty()) " Warning: " + alerts.joinToString(". ") else ""
        aiSummary = "Currently in $cityName, it's ${current.temp.toInt()} degrees with $condition. " + 
                    voiceParts.joinToString(" ") + alertSummary
    }

    private fun formatIsoTime(iso: String?): String {
        return try {
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(iso!!)
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date!!)
        } catch (e: Exception) { "00:00" }
    }

    private fun getWeatherCondition(code: Int): String {
        return when (code) {
            0 -> "Clear"
            1, 2, 3 -> "Partly Cloudy"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            71, 73, 75 -> "Snow"
            95, 96, 99 -> "Thunderstorm"
            else -> "Cloudy"
        }
    }

    private fun changeImagesAccordingToWeather(condition: String) {
        when (condition) {
            "Clear" -> {
                binding.root.setBackgroundResource(R.drawable.sunny_background)
                binding.lottieAnimationView.setAnimation(R.raw.sun)
            }
            "Rain", "Drizzle", "Thunderstorm" -> {
                binding.root.setBackgroundResource(R.drawable.rain_background)
                binding.lottieAnimationView.setAnimation(R.raw.rain)
            }
            "Snow" -> {
                binding.root.setBackgroundResource(R.drawable.snow_background)
                binding.lottieAnimationView.setAnimation(R.raw.snow)
            }
            else -> {
                binding.root.setBackgroundResource(R.drawable.cloud_background)
                binding.lottieAnimationView.setAnimation(R.raw.cloud)
            }
        }
        binding.lottieAnimationView.playAnimation()
    }

    private fun getWindDirection(degree: Int): String {
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return directions[((degree % 360) / 45)]
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US
    }

    override fun onDestroy() {
        tts?.stop(); tts?.shutdown()
        super.onDestroy()
    }
}
