package com.example.fitnessapp.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.api.StravaApiService
import com.example.fitnessapp.api.RetrofitClient
import com.example.fitnessapp.model.FTPEstimate
import com.example.fitnessapp.viewmodel.HealthConnectState
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val apiService = RetrofitClient.retrofit.create(StravaApiService::class.java)

    private val _ftpEstimate = MutableLiveData<FTPEstimate?>()
    val ftpEstimate: LiveData<FTPEstimate?> = _ftpEstimate

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _sleepHours = MutableLiveData<Double>(0.0)
    val sleepHours: LiveData<Double> = _sleepHours

    private val _caloriesBurned = MutableLiveData<Double>(0.0)
    val caloriesBurned: LiveData<Double> = _caloriesBurned

    private val _calorieAllowance = MutableLiveData<Double>(0.0)
    val calorieAllowance: LiveData<Double> = _calorieAllowance

    fun fetchFTPEstimate(token: String, days: Int = 30) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val response = apiService.estimateFtp("Bearer $token", days)
                _ftpEstimate.value = response
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to fetch FTP estimate"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun fetchSleepData(context: Context) {
        viewModelScope.launch {
            try {
                val healthConnectViewModel = HealthConnectViewModel.getInstance(context)
                if (healthConnectViewModel.healthConnectState.value is HealthConnectState.Connected) {
                    val sleepHours = healthConnectViewModel.getTodaysSleepHours()
                    _sleepHours.value = sleepHours
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to fetch sleep data"
                e.printStackTrace()
            }
        }
    }

    fun fetchCalorieData(context: Context) {
        viewModelScope.launch {
            try {
                val healthConnectViewModel = HealthConnectViewModel.getInstance(context)
                if (healthConnectViewModel.healthConnectState.value is HealthConnectState.Connected) {
                    val caloriesBurned = healthConnectViewModel.getTodaysTotalCaloriesBurned()
                    _caloriesBurned.value = caloriesBurned
                    
                    // Calculate calorie allowance
                    val allowance = calculateCalorieAllowance(caloriesBurned)
                    _calorieAllowance.value = allowance
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to fetch calorie data"
                e.printStackTrace()
            }
        }
    }

    private fun calculateCalorieAllowance(caloriesBurned: Double): Double {
        // Base daily calorie needs (BMR + activity)
        // This is a simplified calculation - in a real app, you'd use user's age, weight, height, activity level
        val baseCalories = 2000.0 // Average adult daily calorie needs
        
        // If we have calories burned data, adjust the allowance
        // The idea is: base calories + calories burned = total allowance
        // This encourages users to eat more when they're more active
        return if (caloriesBurned > 0) {
            // Only add a portion of burned calories to avoid overeating
            // Typically, you want to eat back about 50-70% of exercise calories
            val exerciseCalories = caloriesBurned * 0.6 // 60% of burned calories
            baseCalories + exerciseCalories
        } else {
            baseCalories
        }
    }
} 
