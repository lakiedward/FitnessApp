package com.example.fitnessapp.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.api.ApiConfig
import com.example.fitnessapp.api.StravaApiService
import com.example.fitnessapp.model.*
import com.example.fitnessapp.utils.AuthManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL
import java.util.Date
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.asLiveData

sealed class StravaState {
    object Initial : StravaState()
    object Connecting : StravaState()
    data class Connected(val userData: StravaUserData) : StravaState()
    data class Error(val message: String) : StravaState()
}

class StravaViewModel(private val context: Context) : ViewModel() {
    private val _stravaState = MutableStateFlow<StravaState>(StravaState.Initial)
    val stravaState: StateFlow<StravaState> = _stravaState.asStateFlow()

    private val _stravaUserData = MutableStateFlow<StravaUserData?>(null)
    val stravaUserData: StateFlow<StravaUserData?> = _stravaUserData.asStateFlow()

    private val _stravaActivities = MutableStateFlow<List<StravaActivity>>(emptyList())
    val stravaActivities: StateFlow<List<StravaActivity>> = _stravaActivities.asStateFlow()

    private val _stravaAthlete = MutableStateFlow<StravaAthlete?>(null)
    val stravaAthlete: StateFlow<StravaAthlete?> = _stravaAthlete.asStateFlow()

    private val _activitiesBySport = MutableStateFlow<Map<String, List<StravaActivity>>>(emptyMap())
    val activitiesBySport: StateFlow<Map<String, List<StravaActivity>>> = _activitiesBySport.asStateFlow()

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("strava_prefs", Context.MODE_PRIVATE)
    }

    private val authManager = AuthManager(context)
    private val apiService: StravaApiService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL) 
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(StravaApiService::class.java)
        checkConnectionStatus()
    }

    private fun setStravaState(state: StravaState) {
        Log.d("STRAVA_DEBUG", "[StravaViewModel] setStravaState: $state")
        _stravaState.value = state
    }

    private fun checkConnectionStatus() {
        viewModelScope.launch {
            try {
                val token = authManager.getStravaToken()
                Log.d("STRAVA_DEBUG", "[checkConnectionStatus] token=$token")
                if (token != null) {
                    try {
                        val response = withContext(Dispatchers.IO) {
                            apiService.getAthlete("Bearer ${authManager.getJwtToken()}").execute()
                        }
                        Log.d("STRAVA_DEBUG", "[checkConnectionStatus] getAthlete response: ${response.isSuccessful}")
                        if (response.isSuccessful) {
                            val athlete = response.body()
                            Log.d("STRAVA_DEBUG", "[checkConnectionStatus] athlete=$athlete")
                            if (athlete != null) {
                                _stravaAthlete.value = athlete
                                _stravaUserData.value = StravaUserData(
                                    userId = authManager.getUserId() ?: 0,
                                    stravaId = athlete.id,
                                    accessToken = token.accessToken,
                                    refreshToken = token.refreshToken,
                                    tokenExpiresAt = token.expiresAt
                                )
                                setStravaState(StravaState.Connected(_stravaUserData.value!!))
                                syncStravaActivities()
                            } else {
                                setStravaState(StravaState.Error("Failed to get athlete data: null body"))
                            }
                        } else {
                            setStravaState(StravaState.Error("Failed to get athlete data: ${response.message()}"))
                        }
                    } catch (e: Exception) {
                        Log.e("STRAVA_DEBUG", "[checkConnectionStatus] Error getting athlete data", e)
                        setStravaState(StravaState.Error("Failed to get athlete data: ${e.message}"))
                    }
                } else {
                    setStravaState(StravaState.Initial)
                }
            } catch (e: Exception) {
                Log.e("STRAVA_DEBUG", "[checkConnectionStatus] Error", e)
                setStravaState(StravaState.Error("Failed to check connection status: ${e.message}"))
            }
        }
    }

    suspend fun getAuthUrl(): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getAuthUrl()
                response["auth_url"] ?: throw Exception("No auth URL in response")
            } catch (e: Exception) {
                Log.e("StravaViewModel", "Error getting auth URL", e)
                throw e
            }
        }
    }

    fun connect() {
        viewModelScope.launch {
            try {
                _stravaState.value = StravaState.Connecting
                val authUrl = getAuthUrl()
                // The URL will be opened by the UI layer
            } catch (e: Exception) {
                Log.e("StravaViewModel", "Error connecting to Strava", e)
                _stravaState.value = StravaState.Error("Failed to connect: ${e.message}")
            }
        }
    }

    fun handleAuthCode(code: String) {
        viewModelScope.launch {
            try {
                setStravaState(StravaState.Connecting)
                val jwtToken = authManager.getJwtToken() ?: throw Exception("Not logged in")
                Log.d("STRAVA_DEBUG", "[handleAuthCode] jwtToken=$jwtToken, code=$code")

                val response = withContext(Dispatchers.IO) {
                    apiService.exchangeCodeForToken(
                        jwtToken = "Bearer $jwtToken",
                        code = code
                    ).execute()
                }
                Log.d("STRAVA_DEBUG", "[handleAuthCode] exchangeCodeForToken response: ${response.isSuccessful}")
                if (!response.isSuccessful) {
                    throw Exception("Failed to exchange code: ${response.errorBody()?.string()}")
                }
                val token = response.body() ?: throw Exception("No token in response")
                Log.d("STRAVA_DEBUG", "[handleAuthCode] token=$token")
                authManager.saveStravaToken(token)

                val athleteResponse = withContext(Dispatchers.IO) {
                    apiService.getAthlete("Bearer $jwtToken").execute()
                }
                Log.d("STRAVA_DEBUG", "[handleAuthCode] getAthlete response: ${athleteResponse.isSuccessful}")
                val athlete = athleteResponse.body()
                Log.d("STRAVA_DEBUG", "[handleAuthCode] athlete=$athlete")
                if (athlete != null) {
                    _stravaAthlete.value = athlete
                    _stravaUserData.value = StravaUserData(
                        userId = authManager.getUserId() ?: 0,
                        stravaId = athlete.id,
                        accessToken = token.accessToken,
                        refreshToken = token.refreshToken,
                        tokenExpiresAt = token.expiresAt
                    )
                    setStravaState(StravaState.Connected(_stravaUserData.value!!))
                    syncStravaActivities()
                } else {
                    setStravaState(StravaState.Error("Failed to get athlete data after auth code exchange"))
                }
            } catch (e: Exception) {
                Log.e("STRAVA_DEBUG", "[handleAuthCode] Error handling auth code", e)
                setStravaState(StravaState.Error("Failed to connect: ${e.message}"))
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            try {
                authManager.clearStravaToken()
                _stravaState.value = StravaState.Initial
                _stravaUserData.value = null
                _stravaAthlete.value = null
                _stravaActivities.value = emptyList()
            } catch (e: Exception) {
                Log.e("StravaViewModel", "Error disconnecting from Strava", e)
                _stravaState.value = StravaState.Error("Failed to disconnect: ${e.message}")
            }
        }
    }

    fun refreshActivities() {
        viewModelScope.launch {
            try {
                val jwtToken = authManager.getJwtToken() ?: throw Exception("Not logged in")
                val response = apiService.getActivities("Bearer $jwtToken").execute()
                
                if (!response.isSuccessful) {
                    throw Exception("Failed to get activities: ${response.errorBody()?.string()}")
                }
                
                _stravaActivities.value = response.body() ?: emptyList()
            } catch (e: Exception) {
                Log.e("StravaViewModel", "Error refreshing activities", e)
                _stravaState.value = StravaState.Error("Failed to refresh activities: ${e.message}")
            }
        }
    }

    fun fetchActivitiesBySport() {
        viewModelScope.launch {
            try {
                val jwtToken = authManager.getJwtToken() ?: throw Exception("Not logged in")
                val result = apiService.getActivitiesBySport("Bearer $jwtToken")
                _activitiesBySport.value = result
            } catch (e: Exception) {
                Log.e("StravaViewModel", "Error fetching activities by sport", e)
                _activitiesBySport.value = emptyMap()
            }
        }
    }

    fun fetchStravaActivities(jwtToken: String) {
        viewModelScope.launch {
            Log.d("STRAVA_DEBUG", "fetchStravaActivities() called with JWT: $jwtToken")
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getActivities("Bearer $jwtToken").execute()
                }
                if (response.isSuccessful) {
                    val activities = response.body() ?: emptyList()
                    Log.d("STRAVA_DEBUG", "Fetched "+activities.size+" activities")
                    _stravaActivities.value = activities
                } else {
                    Log.e("STRAVA_DEBUG", "Failed to fetch activities: "+response.errorBody()?.string())
                }
            } catch (e: Exception) {
                Log.e("STRAVA_DEBUG", "Exception fetching activities", e)
            }
        }
    }

    fun syncStravaActivities() {
        val jwtToken = authManager.getJwtToken() ?: return
        viewModelScope.launch {
            try {
                val response = apiService.syncStravaActivities("Bearer $jwtToken")
                if (response.isSuccessful) {
                    // Poți afișa un mesaj de succes
                    fetchStravaActivitiesFromDb()
                } else {
                    // Tratează eroarea
                }
            } catch (e: Exception) {
                // Tratează excepția
            }
        }
    }

    fun fetchStravaActivitiesFromDb() {
        val jwtToken = authManager.getJwtToken() ?: return
        viewModelScope.launch {
            try {
                val response = apiService.getStravaActivitiesFromDb("Bearer $jwtToken")
                if (response.isSuccessful) {
                    _stravaActivities.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // Tratează excepția
            }
        }
    }
}

class StravaViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StravaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StravaViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 