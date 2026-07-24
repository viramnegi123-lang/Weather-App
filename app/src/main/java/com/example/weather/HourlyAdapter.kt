package com.example.weather

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.weather.databinding.ItemHourlyBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HourlyAdapter(private val hourlyList: List<ForecastItem>) :
    RecyclerView.Adapter<HourlyAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemHourlyBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHourlyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = hourlyList[position]
        holder.binding.hourlyTime.text = item.timeString
        holder.binding.hourlyTemp.text = "${item.temp.toInt()}°C"
        
        val condition = getWeatherCondition(item.weatherCode)
        when (condition) {
            "Clear" -> holder.binding.hourlyIcon.setImageResource(R.drawable.sunny)
            "Rain" -> holder.binding.hourlyIcon.setImageResource(R.drawable.rain)
            "Snow" -> holder.binding.hourlyIcon.setImageResource(R.drawable.snow)
            else -> holder.binding.hourlyIcon.setImageResource(R.drawable.white_cloud)
        }
    }

    override fun getItemCount(): Int = hourlyList.size

    private fun getWeatherCondition(code: Int): String {
        return when (code) {
            0 -> "Clear"
            51, 53, 55, 61, 63, 65, 95, 96, 99 -> "Rain"
            71, 73, 75 -> "Snow"
            else -> "Cloudy"
        }
    }
}
