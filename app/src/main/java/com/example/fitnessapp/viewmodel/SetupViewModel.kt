package com.example.fitnessapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.api.ApiService
import com.example.fitnessapp.api.RetrofitClient
import com.example.fitnessapp.model.DetaliiUserCycling
import com.example.fitnessapp.model.PacePredictions
import com.example.fitnessapp.model.RunningPrediction
import com.example.fitnessapp.model.SetupStatusResponse
import com.example.fitnessapp.model.SwimPacePredictions
import com.example.fitnessapp.model.SwimPrediction
import kotlinx.coroutines.launch

class SetupViewModel : ViewModel() {
    
    private val apiService = RetrofitClient.retrofit.create(ApiService::class.java)
    
    private val _setupStatus = MutableLiveData<SetupStatusResponse>()
    val setupStatus: LiveData<SetupStatusResponse> = _setupStatus
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _submitSuccess = MutableLiveData<Boolean>()
    val submitSuccess: LiveData<Boolean> = _submitSuccess
    
    fun checkSetupStatus(token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d("SetupViewModel", "Checking setup status...")
            try {
                val response = apiService.getSetupStatus("Bearer $token")
                if (response.isSuccessful) {
                    response.body()?.let { setupStatus ->
                        _setupStatus.value = setupStatus
                        Log.d("SetupViewModel", "Setup status received: ${setupStatus.missing}")
                        
                        // Log specific information about what's missing
                        when {
                            setupStatus.missing.isEmpty() -> {
                                Log.d("SetupViewModel", "✅ All sports data is complete - proceeding to plan length screen")
                            }
                            setupStatus.missing.contains("cycling_ftp") -> {
                                Log.d("SetupViewModel", "⚠️ Missing cycling FTP - navigating to FTP entry screen")
                            }
                            setupStatus.missing.contains("running_prediction") -> {
                                Log.d("SetupViewModel", "⚠️ Missing running predictions - navigating to running pace screen")
                            }
                            setupStatus.missing.contains("swim_prediction") -> {
                                Log.d("SetupViewModel", "⚠️ Missing swimming predictions - navigating to swim pace screen")
                            }
                            else -> {
                                Log.d("SetupViewModel", "⚠️ Missing other data: ${setupStatus.missing}")
                            }
                        }
                    }
                } else {
                    val errorMsg = "Failed to check setup status: ${response.message()}"
                    _error.value = errorMsg
                    Log.e("SetupViewModel", errorMsg)
                    Log.e("SetupViewModel", "Response code: ${response.code()}")
                }
            } catch (e: Exception) {
                val errorMsg = "Error checking setup status: ${e.message}"
                _error.value = errorMsg
                Log.e("SetupViewModel", errorMsg, e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun submitCyclingData(token: String, ftp: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Validate token
                if (token.isBlank()) {
                    _error.value = "Token is missing or empty"
                    Log.e("SetupViewModel", "Token is missing or empty")
                    return@launch
                }
                
                // Validate FTP value
                if (ftp <= 0) {
                    _error.value = "FTP value must be greater than 0"
                    Log.e("SetupViewModel", "Invalid FTP value: $ftp")
                    return@launch
                }
                
                val cyclingData = DetaliiUserCycling(ftp)
                Log.d("SetupViewModel", "Submitting cycling data: $cyclingData")
                Log.d("SetupViewModel", "FTP value: $ftp")
                Log.d("SetupViewModel", "Token: ${token.take(20)}...") // Log only first 20 chars for security
                
                val response = apiService.addOrUpdateCyclingData("Bearer $token", cyclingData)
                Log.d("SetupViewModel", "Response code: ${response.code()}")
                Log.d("SetupViewModel", "Response body: ${response.body()}")
                Log.d("SetupViewModel", "Response error: ${response.errorBody()?.string()}")
                if (response.isSuccessful) {
                    _submitSuccess.value = true
                    Log.d("SetupViewModel", "Cycling data submitted successfully")
                } else {
                    val errorMsg = "Failed to submit cycling data: ${response.message()}"
                    _error.value = errorMsg
                    Log.e("SetupViewModel", errorMsg)
                }
            } catch (e: Exception) {
                _error.value = "Error submitting cycling data: ${e.message}"
                Log.e("SetupViewModel", "Error submitting cycling data", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun submitRunningPacePredictions(token: String, predictions: List<RunningPrediction>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val pacePredictions = PacePredictions(predictions)
                val response = apiService.addManualRunningPacePredictions("Bearer $token", pacePredictions)
                if (response.isSuccessful) {
                    _submitSuccess.value = true
                    Log.d("SetupViewModel", "Running pace predictions submitted successfully")
                } else {
                    _error.value = "Failed to submit running pace predictions: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Error submitting running pace predictions: ${e.message}"
                Log.e("SetupViewModel", "Error submitting running pace predictions", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun submitSwimPacePredictions(token: String, predictions: List<SwimPrediction>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val swimPacePredictions = SwimPacePredictions(predictions)
                val response = apiService.addManualSwimPacePredictions("Bearer $token", swimPacePredictions)
                if (response.isSuccessful) {
                    _submitSuccess.value = true
                    Log.d("SetupViewModel", "Swim pace predictions submitted successfully")
                } else {
                    _error.value = "Failed to submit swim pace predictions: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Error submitting swim pace predictions: ${e.message}"
                Log.e("SetupViewModel", "Error submitting swim pace predictions", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearSubmitSuccess() {
        _submitSuccess.value = false
    }
}
