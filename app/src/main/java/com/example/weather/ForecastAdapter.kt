package com.example.weather

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.weather.databinding.ItemForecastBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ForecastAdapter(private val forecastList: List<ForecastItem>) :
    RecyclerView.Adapter<ForecastAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemForecastBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemForecastBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = forecastList[position]
        
        holder.binding.forecastDay.text = try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(item.timeString)
            SimpleDateFormat("EEEE", Locale.getDefault()).format(date!!)
        } catch (e: Exception) { item.timeString }

        holder.binding.forecastTemp.text = "${item.maxTemp?.toInt() ?: 0}° / ${item.minTemp?.toInt() ?: 0}°"

        val condition = getWeatherCondition(item.weatherCode)
        when (condition) {
            "Clear" -> holder.binding.forecastIcon.setImageResource(R.drawable.sunny)
            "Rain" -> holder.binding.forecastIcon.setImageResource(R.drawable.rain)
            "Snow" -> holder.binding.forecastIcon.setImageResource(R.drawable.snow)
            else -> holder.binding.forecastIcon.setImageResource(R.drawable.white_cloud)
        }
    }

    override fun getItemCount(): Int = forecastList.size

    private fun getWeatherCondition(code: Int): String {
        return when (code) {
            0 -> "Clear"
            51, 53, 55, 61, 63, 65, 95, 96, 99 -> "Rain"
            71, 73, 75 -> "Snow"
            else -> "Cloudy"
        }
    }
}
