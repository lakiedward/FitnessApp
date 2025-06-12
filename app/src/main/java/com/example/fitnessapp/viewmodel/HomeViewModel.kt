package com.example.fitnessapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.api.StravaApiService
import com.example.fitnessapp.api.RetrofitClient
import com.example.fitnessapp.model.FTPEstimate
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val apiService = RetrofitClient.retrofit.create(StravaApiService::class.java)
    
    private val _ftpEstimate = MutableLiveData<FTPEstimate?>()
    val ftpEstimate: LiveData<FTPEstimate?> = _ftpEstimate
    
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
} 