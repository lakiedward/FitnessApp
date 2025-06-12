package com.example.fitnessapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.api.StravaApiService
import com.example.fitnessapp.api.RetrofitClient
import com.example.fitnessapp.model.FTPEstimate
import com.example.fitnessapp.model.TrainingPlan
import com.example.fitnessapp.model.WorkoutStep
import kotlinx.coroutines.launch
import retrofit2.Response
import java.time.Duration

class TrainingDetailViewModel : ViewModel() {
    private val apiService = RetrofitClient.retrofit.create(StravaApiService::class.java)
    
    private val _ftpEstimate = MutableLiveData<FTPEstimate?>()
    val ftpEstimate: LiveData<FTPEstimate?> = _ftpEstimate

    private val _calculatedMetrics = MutableLiveData<TrainingMetrics>()
    val calculatedMetrics: LiveData<TrainingMetrics> = _calculatedMetrics

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun fetchFTPEstimate(token: String, days: Int = 30) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val response = apiService.estimateFtp("Bearer $token", days)
                _ftpEstimate.value = response
                // Calculate metrics if we have a training plan
                _calculatedMetrics.value?.let { metrics ->
                    calculateTrainingMetrics(metrics.training, response.estimatedFTP)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to fetch FTP estimate"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchLastFtpEstimateFromDb(token: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val response = apiService.getLastFtpEstimateFromDb("Bearer $token")
                _ftpEstimate.value = response
                Log.d("TrainingDetailViewModel", "FTP estimate fetched from DB: ${response.estimatedFTP} watts")
            } catch (e: Exception) {
                Log.d("TrainingDetailViewModel", "No FTP estimate found in DB, will use default: ${e.message}")
                _error.value = "No FTP estimate found in database"
                _ftpEstimate.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun calculateTrainingMetrics(training: TrainingPlan, ftp: Float) {
        val steps = training.steps ?: return
        var totalTSS = 0f
        var totalDuration = 0
        var totalCalories = 0f

        Log.d("TrainingDetailViewModel", "Calculating metrics with FTP: $ftp watts")
        Log.d("TrainingDetailViewModel", "Training steps count: ${steps.size}")
        Log.d("TrainingDetailViewModel", "Training ID: ${training.id}, Name: ${training.workout_name}")
        Log.d("TrainingDetailViewModel", "Steps JSON: ${training.stepsJson}")

        steps.forEachIndexed { index, step ->
            Log.d("TrainingDetailViewModel", "Processing step $index: ${step::class.simpleName}")
            when (step) {
                is WorkoutStep.SteadyState -> {
                    val duration = step.duration
                    val intensity = step.power
                    val tss = calculateTSS(duration, intensity, ftp)
                    val calories = calculateCalories(duration, intensity, ftp)
                    
                    Log.d("TrainingDetailViewModel", "SteadyState - Duration: ${duration}s, Intensity: ${intensity}%, TSS: $tss, Calories: $calories")
                    
                    totalTSS += tss
                    totalDuration += duration
                    totalCalories += calories
                }
                is WorkoutStep.IntervalsT -> {
                    Log.d("TrainingDetailViewModel", "IntervalsT - Repeat: ${step.repeat}, On: ${step.on_duration}s@${step.on_power}%, Off: ${step.off_duration}s@${step.off_power}%")
                    repeat(step.repeat) {
                        // On interval
                        val onTSS = calculateTSS(step.on_duration, step.on_power, ftp)
                        val onCalories = calculateCalories(step.on_duration, step.on_power, ftp)
                        
                        // Off interval
                        val offTSS = calculateTSS(step.off_duration, step.off_power, ftp)
                        val offCalories = calculateCalories(step.off_duration, step.off_power, ftp)
                        
                        Log.d("TrainingDetailViewModel", "Interval - On: ${step.on_duration}s@${step.on_power}%, Off: ${step.off_duration}s@${step.off_power}%, TSS: ${onTSS + offTSS}, Calories: ${onCalories + offCalories}")
                        
                        totalTSS += onTSS + offTSS
                        totalDuration += step.on_duration + step.off_duration
                        totalCalories += onCalories + offCalories
                    }
                }
            }
        }

        Log.d("TrainingDetailViewModel", "Final metrics - TSS: $totalTSS, Duration: $totalDuration, Calories: $totalCalories")

        _calculatedMetrics.value = TrainingMetrics(
            training = training,
            tss = totalTSS,
            duration = totalDuration,
            calories = totalCalories
        )
    }

    private fun calculateTSS(duration: Int, intensity: Float, ftp: Float): Float {
        // TSS = (duration in hours) * (intensity factor)^2 * 100
        // intensity is stored as percentage of FTP (0-1), so intensity factor = intensity
        val durationHours = duration / 3600f
        val intensityFactor = intensity  // Already a percentage of FTP
        val tss = durationHours * (intensityFactor * intensityFactor) * 100
        
        Log.d("TrainingDetailViewModel", "TSS Calculation - Duration: ${duration}s, Intensity: ${intensity}%, FTP: ${ftp}w")
        Log.d("TrainingDetailViewModel", "TSS Calculation - DurationHours: ${durationHours}, IntensityFactor: ${intensityFactor}, TSS: ${tss}")
        
        return tss
    }

    private fun calculateCalories(duration: Int, intensity: Float, ftp: Float): Float {
        // Basic calorie calculation based on power output
        // Calories = (Power in watts * duration in hours * 3.6)
        // intensity is stored as percentage of FTP (0-1), need to convert to watts
        val durationHours = duration / 3600f
        val powerWatts = intensity * ftp  // Convert percentage to watts
        return powerWatts * durationHours * 3.6f
    }

    fun clearError() {
        _error.value = null
    }
}

data class TrainingMetrics(
    val training: TrainingPlan,
    val tss: Float,
    val duration: Int,
    val calories: Float
) 