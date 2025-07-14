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
} 
