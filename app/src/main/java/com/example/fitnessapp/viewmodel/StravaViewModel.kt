package com.example.fitnessapp.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.api.ApiService
import com.example.fitnessapp.api.RetrofitClient
import com.example.fitnessapp.api.StravaApiService
import com.example.fitnessapp.model.ActivityStreamsResponse
import com.example.fitnessapp.model.FTPEstimate
import com.example.fitnessapp.model.StravaActivity
import com.example.fitnessapp.model.StravaAthlete
import com.example.fitnessapp.model.StravaUserData
import com.example.fitnessapp.utils.AuthManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

sealed class StravaState {
    object Initial : StravaState()
    object Connecting : StravaState()
    data class Connected(val userData: StravaUserData) : StravaState()
    data class Error(val message: String) : StravaState()
    object NotConnected : StravaState()
}

class StravaViewModel(private val context: Context) : ViewModel() {
    private val _stravaState = MutableStateFlow<StravaState>(StravaState.Initial)
    val stravaState: StateFlow<StravaState> = _stravaState.asStateFlow()

    private val _stravaUserData = MutableStateFlow<StravaUserData?>(null)
    val stravaUserData: StateFlow<StravaUserData?> = _stravaUserData.asStateFlow()

    private val _stravaActivities = MutableStateFlow<List<StravaActivity>>(emptyList())
    val stravaActivities: StateFlow<List<StravaActivity>> = _stravaActivities.asStateFlow()

    private val _databaseActivities = MutableStateFlow<List<StravaActivity>>(emptyList())
    val databaseActivities: StateFlow<List<StravaActivity>> = _databaseActivities.asStateFlow()

    private val _stravaAthlete = MutableStateFlow<StravaAthlete?>(null)
    val stravaAthlete: StateFlow<StravaAthlete?> = _stravaAthlete.asStateFlow()

    private val _activitiesBySport = MutableStateFlow<Map<String, List<StravaActivity>>>(emptyMap())
    val activitiesBySport: StateFlow<Map<String, List<StravaActivity>>> = _activitiesBySport.asStateFlow()

    private val _ftpEstimate = MutableStateFlow<FTPEstimate?>(null)
    val ftpEstimate: StateFlow<FTPEstimate?> = _ftpEstimate.asStateFlow()

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("strava_prefs", Context.MODE_PRIVATE)
    }

    private val authManager = AuthManager(context)
    private val apiService: StravaApiService
    private val mainApiService: ApiService

    // Add request throttling
    private var isCheckingConnection = false
    private var lastConnectionCheckTime = 0L
    private val minCheckInterval = 10000L // Minimum 10 seconds between checks (increased to avoid rate limiting)

    // Track OAuth completion to be more patient with rate limits
    private var expectingConnectionAfterOAuth = false

    // Flag to prevent unnecessary connection checks after OAuth
    private var justCompletedOAuth = false

    // Job reference for checkConnectionStatus to allow cancellation
    private var checkConnectionJob: kotlinx.coroutines.Job? = null

    // Flag to completely disable connection checks during OAuth process
    private var isOAuthInProgress = false

    // Timestamp of last OAuth completion to prevent immediate checks
    private var lastOAuthCompletionTime = 0L
    private val oAuthCooldownPeriod = 30000L // 30 seconds cooldown after OAuth

    companion object {
        @Volatile
        private var INSTANCE: StravaViewModel? = null

        fun getInstance(context: Context): StravaViewModel {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StravaViewModel(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun clearInstance() {
            INSTANCE = null
        }
    }

    init {
        Log.d("STRAVA_DEBUG", "=== StravaViewModel INIT ===")
        Log.d("STRAVA_DEBUG", "[init] Initial state: ${_stravaState.value}")
        Log.d("STRAVA_DEBUG", "[init] isOAuthInProgress: $isOAuthInProgress")
        Log.d("STRAVA_DEBUG", "[init] justCompletedOAuth: $justCompletedOAuth")

        // Use the proper RetrofitClient with configured timeouts
        apiService = RetrofitClient.retrofit.create(StravaApiService::class.java)
        mainApiService = RetrofitClient.retrofit.create(ApiService::class.java)

        // Initial check only - don't start periodic checks
        Log.d("STRAVA_DEBUG", "[init] Performing initial connection check")
        checkConnectionStatus()

        // Don't start periodic checks - they're not needed after successful connection
        Log.d("STRAVA_DEBUG", "[init] Skipping periodic connection checks to avoid unnecessary API calls")
    }

    private fun setStravaState(newState: StravaState) {
        Log.d("STRAVA_DEBUG", "=== setStravaState() CALLED ===")
        Log.d("STRAVA_DEBUG", "[StravaViewModel] setStravaState: $newState")
        Log.d("STRAVA_DEBUG", "[StravaViewModel] Previous state: ${_stravaState.value}")
        Log.d("STRAVA_DEBUG", "[StravaViewModel] ViewModel instance: ${this.hashCode()}")
        Log.d("STRAVA_DEBUG", "[StravaViewModel] Thread: ${Thread.currentThread().name}")
        Log.d("STRAVA_DEBUG", "[StravaViewModel] isOAuthInProgress: $isOAuthInProgress")
        Log.d("STRAVA_DEBUG", "[StravaViewModel] justCompletedOAuth: $justCompletedOAuth")

        _stravaState.value = newState
        Log.d("STRAVA_DEBUG", "[StravaViewModel] State updated successfully")
        Log.d("STRAVA_DEBUG", "[StravaViewModel] New state value: ${_stravaState.value}")
    }

    private fun resetConnectionCheckState() {
        isCheckingConnection = false
        checkConnectionJob = null
        Log.d("STRAVA_DEBUG", "[StravaViewModel] Connection check state reset")
    }

    fun checkConnectionStatus() {
        Log.d("STRAVA_DEBUG", "=== checkConnectionStatus START ===")
        Log.d("STRAVA_DEBUG", "[checkConnectionStatus] isOAuthInProgress: $isOAuthInProgress")
        Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Current state: ${_stravaState.value}")
        Log.d("STRAVA_DEBUG", "[checkConnectionStatus] isCheckingConnection: $isCheckingConnection")
        Log.d("STRAVA_DEBUG", "[checkConnectionStatus] justCompletedOAuth: $justCompletedOAuth")
        Log.d("STRAVA_DEBUG", "[checkConnectionStatus] checkConnectionJob: ${checkConnectionJob?.isActive}")

        // Don't check if OAuth is in progress
        if (isOAuthInProgress) {
            Log.d("STRAVA_DEBUG", "[checkConnectionStatus] OAuth in progress, skipping check")
            return
        }

        // Don't check if already connected - this prevents overriding Connected state
        if (_stravaState.value is StravaState.Connected) {
            Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Already connected, skipping check")
            return
        }

        // Don't check if currently connecting - this prevents multiple simultaneous checks
        if (_stravaState.value is StravaState.Connecting) {
            Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Currently connecting, skipping check")
            return
        }

        if (isCheckingConnection) {
            Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Already checking connection, skipping")
            return
        }

        val currentTime = System.currentTimeMillis() / 1000
        Log.d("STRAVA_DEBUG", "[checkConnectionStatus] currentTime: $currentTime, lastConnectionCheckTime: $lastConnectionCheckTime, minCheckInterval: $minCheckInterval")
        if (currentTime - lastConnectionCheckTime < minCheckInterval) {
            Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Too soon to check again, skipping")
            return
        }

        // Don't check immediately after OAuth completion
        if (justCompletedOAuth) {
            Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Just completed OAuth, skipping check")
            return
        }

        // Check OAuth cooldown period
        val timeSinceOAuth = currentTime - lastOAuthCompletionTime
        if (timeSinceOAuth < oAuthCooldownPeriod / 1000) {
            Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Within OAuth cooldown period (${timeSinceOAuth}s < ${oAuthCooldownPeriod / 1000}s), skipping check")
            return
        }

        Log.d("STRAVA_DEBUG", "[checkConnectionStatus] All checks passed, proceeding with connection check")
        isCheckingConnection = true
        lastConnectionCheckTime = currentTime

        Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Starting connection check")
        checkConnectionJob = viewModelScope.launch {
            Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Job started")
            Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Job isActive: ${isActive}")
            Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Job hashCode: ${hashCode()}")
            try {
                Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Starting connection verification")
                val jwtToken = authManager.getJwtToken()
                Log.d("STRAVA_DEBUG", "[checkConnectionStatus] JWT token exists: ${!jwtToken.isNullOrEmpty()}")

                if (jwtToken == null) {
                    Log.d("STRAVA_DEBUG", "[checkConnectionStatus] No JWT token, setting NotConnected")
                    setStravaState(StravaState.NotConnected)
                    isCheckingConnection = false
                    Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Job completed - no JWT")
                    return@launch
                }

                // Check if we have a stored Strava token and if it's still valid
                val storedToken = authManager.getStravaToken()
                Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Stored token exists: ${storedToken != null}")

                if (storedToken != null) {
                    val tokenExpiry = storedToken.expiresAt
                    val currentTime = System.currentTimeMillis() / 1000
                    Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Token expiry: $tokenExpiry, current time: $currentTime")

                    if (tokenExpiry > currentTime) {
                        // Token is still valid, set connected state
                        Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Token is still valid, setting Connected")
                        _stravaUserData.value = StravaUserData(
                            userId = authManager.getUserId() ?: 0,
                            stravaId = 0, // Will be updated when we get athlete data
                            accessToken = storedToken.accessToken,
                            refreshToken = storedToken.refreshToken,
                            tokenExpiresAt = storedToken.expiresAt
                        )
                        setStravaState(StravaState.Connected(_stravaUserData.value!!))

                        // Fetch athlete data in background
                        Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Fetching athlete data in background")
                        fetchAthleteData()
                    } else {
                        Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Token is expired, trying to refresh")
                        // Token is expired, try to refresh it
                        val response = withContext(Dispatchers.IO) {
                            Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Making API call to refresh token")
                            Log.d("STRAVA_DEBUG", "[checkConnectionStatus] JWT token: ${jwtToken.take(10)}...${jwtToken.takeLast(10)}")
                            Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Calling apiService.refreshToken()")
                            val result = apiService.refreshToken("Bearer $jwtToken").execute()
                            Log.d("STRAVA_DEBUG", "[checkConnectionStatus] refreshToken API call completed")
                            Log.d("STRAVA_DEBUG", "[checkConnectionStatus] refreshToken response code: ${result.code()}")
                            Log.d("STRAVA_DEBUG", "[checkConnectionStatus] refreshToken response successful: ${result.isSuccessful}")
                            Log.d("STRAVA_DEBUG", "[checkConnectionStatus] refreshToken response body: ${result.body()}")
                            Log.d("STRAVA_DEBUG", "[checkConnectionStatus] refreshToken error body: ${result.errorBody()?.string()}")
                            result
                        }

                Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Response code: ${response.code()}")
                Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Response successful: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    val tokenData = response.body()
                    Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Token data received: ${tokenData != null}")

                    if (tokenData != null && !tokenData.accessToken.isNullOrEmpty()) {
                        Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Valid token found, setting Connected")
                        Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Token: ${tokenData.accessToken.take(10)}...${tokenData.accessToken.takeLast(10)}")

                        // Save the token
                        authManager.saveStravaToken(tokenData)
                        Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Token saved to SharedPreferences")

                        // Set connected state
                        _stravaUserData.value = StravaUserData(
                            userId = authManager.getUserId() ?: 0,
                            stravaId = 0, // Will be updated when we get athlete data
                            accessToken = tokenData.accessToken,
                            refreshToken = tokenData.refreshToken,
                            tokenExpiresAt = tokenData.expiresAt
                        )
                        Log.d("STRAVA_DEBUG", "[checkConnectionStatus] StravaUserData created")

                        Log.d("STRAVA_DEBUG", "[checkConnectionStatus] About to set Connected state")
                        setStravaState(StravaState.Connected(_stravaUserData.value!!))
                        Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Connected state set successfully")

                        // Fetch athlete data in background
                        Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Fetching athlete data in background")
                        fetchAthleteData()
                    } else {
                        Log.d("STRAVA_DEBUG", "[checkConnectionStatus] No stored token, setting NotConnected")
                        setStravaState(StravaState.NotConnected)
                    }
                } else {
                    Log.d("STRAVA_DEBUG", "[checkConnectionStatus] API call failed, setting NotConnected")
                    Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Error body: ${response.errorBody()?.string()}")
                    setStravaState(StravaState.NotConnected)
                }
                    }
                } else {
                    Log.d("STRAVA_DEBUG", "[checkConnectionStatus] No stored token, setting NotConnected")
                    setStravaState(StravaState.NotConnected)
                }
            } catch (e: Exception) {
                Log.e("STRAVA_DEBUG", "[checkConnectionStatus] Exception during connection check", e)
                if (e is CancellationException) {
                    Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Job was cancelled")
                } else {
                    Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Setting NotConnected due to exception")
                    setStravaState(StravaState.NotConnected)
                }
            } finally {
                Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Finally block - resetting isCheckingConnection")
                isCheckingConnection = false
                Log.d("STRAVA_DEBUG", "[checkConnectionStatus] isCheckingConnection set to: $isCheckingConnection")
                Log.d("STRAVA_DEBUG", "[checkConnectionStatus] Job completed")
            }
        }
    }

    suspend fun getAuthUrl(): String {
        return withContext(Dispatchers.IO) {
            try {
                val jwtToken = authManager.getJwtToken() ?: throw Exception("Not logged in")
                val response = apiService.getAuthUrl("Bearer $jwtToken")
                val authUrl = response["auth_url"] ?: throw Exception("No auth URL in response")
                Log.d("STRAVA_DEBUG", "[getAuthUrl] Received auth URL: $authUrl")
                authUrl
            } catch (e: Exception) {
                Log.e("STRAVA_DEBUG", "[getAuthUrl] Error getting auth URL", e)
                throw e
            }
        }
    }

    suspend fun connect(): String {
        Log.d("STRAVA_DEBUG", "=== connect() START ===")
        Log.d("STRAVA_DEBUG", "[connect] Current state before: ${_stravaState.value}")
        Log.d("STRAVA_DEBUG", "[connect] isOAuthInProgress before: $isOAuthInProgress")
        try {
            isOAuthInProgress = true
            Log.d("STRAVA_DEBUG", "[connect] OAuth started, disabling connection checks")
            Log.d("STRAVA_DEBUG", "[connect] isOAuthInProgress set to: $isOAuthInProgress")
            _stravaState.value = StravaState.Connecting
            Log.d("STRAVA_DEBUG", "[connect] State set to Connecting")
            val authUrl = getAuthUrl()
            Log.d("STRAVA_DEBUG", "[connect] Opening auth URL: $authUrl")
            Log.d("STRAVA_DEBUG", "[connect] Returning auth URL, OAuth flow initiated")
            return authUrl
        } catch (e: Exception) {
            Log.e("STRAVA_DEBUG", "[connect] Error connecting to Strava", e)
            _stravaState.value = StravaState.Error("Failed to connect: ${e.message}")
            isOAuthInProgress = false
            Log.d("STRAVA_DEBUG", "[connect] Error occurred, isOAuthInProgress reset to: $isOAuthInProgress")
            throw e
        }
    }

    suspend fun handleAuthCode(code: String) {
        Log.d("STRAVA_DEBUG", "[handleAuthCode] Processing auth code: ${code.take(10)}...")
        try {
            val jwtToken = authManager.getJwtToken() ?: throw Exception("Not logged in")
            val response = withContext(Dispatchers.IO) {
                apiService.exchangeCodeForToken("Bearer $jwtToken", code).execute()
            }

            if (response.isSuccessful) {
                val token = response.body()
                if (token != null) {
                    Log.d("STRAVA_DEBUG", "[handleAuthCode] Token exchange successful, saving token")
                    authManager.saveStravaToken(token)

                    // Force refresh token to ensure we have the latest one from backend
                    Log.d("STRAVA_DEBUG", "[handleAuthCode] Force refreshing token to get latest from backend")
                    try {
                        val refreshResponse = withContext(Dispatchers.IO) {
                            apiService.refreshToken("Bearer $jwtToken").execute()
                        }

                        if (refreshResponse.isSuccessful) {
                            val refreshedToken = refreshResponse.body()
                            if (refreshedToken != null) {
                                Log.d("STRAVA_DEBUG", "[handleAuthCode] Token refreshed successfully, saving updated token")
                                authManager.saveStravaToken(refreshedToken)
                            } else {
                                Log.w("STRAVA_DEBUG", "[handleAuthCode] No token in refresh response, using original token")
                            }
                        } else {
                            Log.w("STRAVA_DEBUG", "[handleAuthCode] Failed to refresh token: ${refreshResponse.errorBody()?.string()}, using original token")
                        }
                    } catch (e: Exception) {
                        Log.w("STRAVA_DEBUG", "[handleAuthCode] Error refreshing token: ${e.message}, using original token")
                    }

                    // Now try to get athlete data with the updated token
                    val updatedToken = authManager.getStravaToken()
                    Log.d("STRAVA_DEBUG", "[handleAuthCode] Using updated token: $updatedToken")

                    val athleteResponse = withContext(Dispatchers.IO) {
                        apiService.getAthlete(
                            jwtToken = "Bearer $jwtToken",
                            stravaToken = updatedToken?.accessToken ?: token.accessToken
                        ).execute()
                    }
                    Log.d("STRAVA_DEBUG", "[handleAuthCode] getAthlete response code: ${athleteResponse.code()}")
                    Log.d("STRAVA_DEBUG", "[handleAuthCode] getAthlete error body: ${athleteResponse.errorBody()?.string()}")

                    val athlete = athleteResponse.body()
                    if (athlete != null) {
                        Log.d("STRAVA_DEBUG", "[handleAuthCode] Successfully received athlete data")
                        _stravaAthlete.value = athlete
                        _stravaUserData.value = StravaUserData(
                            userId = authManager.getUserId() ?: 0,
                            stravaId = athlete.id,
                            accessToken = token.accessToken,
                            refreshToken = token.refreshToken,
                            tokenExpiresAt = token.expiresAt
                        )
                        setStravaState(StravaState.Connected(_stravaUserData.value!!))

                        // Reset OAuth flags after successful connection
                        Log.d("STRAVA_DEBUG", "[handleAuthCode] Resetting OAuth flags after successful connection")
                        isOAuthInProgress = false
                        justCompletedOAuth = true
                        lastOAuthCompletionTime = System.currentTimeMillis() / 1000
                        Log.d("STRAVA_DEBUG", "[handleAuthCode] OAuth flags reset: isOAuthInProgress=$isOAuthInProgress, justCompletedOAuth=$justCompletedOAuth")
                    } else {
                        Log.e("STRAVA_DEBUG", "[handleAuthCode] Failed to get athlete data")
                        setStravaState(StravaState.Error("Failed to get athlete data after auth code exchange"))
                        // Reset OAuth flags on error
                        isOAuthInProgress = false
                        Log.d("STRAVA_DEBUG", "[handleAuthCode] OAuth flags reset on error: isOAuthInProgress=$isOAuthInProgress")
                    }
                } else {
                    Log.e("STRAVA_DEBUG", "[handleAuthCode] No token in response")
                    setStravaState(StravaState.Error("No token received from backend"))
                    // Reset OAuth flags on error
                    isOAuthInProgress = false
                    Log.d("STRAVA_DEBUG", "[handleAuthCode] OAuth flags reset on error: isOAuthInProgress=$isOAuthInProgress")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("STRAVA_DEBUG", "[handleAuthCode] Failed to exchange code. Error: $errorBody")
                // Reset OAuth flags on error
                isOAuthInProgress = false
                Log.d("STRAVA_DEBUG", "[handleAuthCode] OAuth flags reset on error: isOAuthInProgress=$isOAuthInProgress")
                throw Exception("Failed to exchange code: $errorBody")
            }
        } catch (e: Exception) {
            Log.e("STRAVA_DEBUG", "[handleAuthCode] Error handling auth code", e)
            setStravaState(StravaState.Error("Failed to connect: ${e.message}"))
            // Reset OAuth flags on error
            isOAuthInProgress = false
            Log.d("STRAVA_DEBUG", "[handleAuthCode] OAuth flags reset on exception: isOAuthInProgress=$isOAuthInProgress")
            throw e
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            try {
                val jwtToken = authManager.getJwtToken() ?: throw Exception("Not logged in")
                val response = apiService.disconnectStravaAccount("Bearer $jwtToken")
                if (response.isSuccessful && response.body()?.get("status") == "success") {
                authManager.clearStravaToken()
                _stravaState.value = StravaState.Initial
                _stravaUserData.value = null
                _stravaAthlete.value = null
                _stravaActivities.value = emptyList()
                expectingConnectionAfterOAuth = false
                } else {
                    val message = response.body()?.get("message") ?: response.errorBody()?.string() ?: "Unknown error"
                    Log.e("StravaViewModel", "Error disconnecting from Strava: $message")
                    _stravaState.value = StravaState.Error("Failed to disconnect: $message")
                }
            } catch (e: Exception) {
                Log.e("StravaViewModel", "Error disconnecting from Strava", e)
                _stravaState.value = StravaState.Error("Failed to disconnect: ${e.message}")
            }
        }
    }

    fun forceClearAllData() {
        Log.d("STRAVA_DEBUG", "[forceClearAllData] Force clearing all Strava data")
        viewModelScope.launch {
            try {
                authManager.clearAllStravaData()
                _stravaState.value = StravaState.Initial
                _stravaUserData.value = null
                _stravaAthlete.value = null
                _stravaActivities.value = emptyList()
                _databaseActivities.value = emptyList()
                _activitiesBySport.value = emptyMap()
                _ftpEstimate.value = null
                expectingConnectionAfterOAuth = false
                isCheckingConnection = false
                lastConnectionCheckTime = 0L
                justCompletedOAuth = false
                lastOAuthCompletionTime = 0L
                checkConnectionJob?.cancel()
                checkConnectionJob = null
                isOAuthInProgress = false
                Log.d("STRAVA_DEBUG", "[forceClearAllData] All Strava data cleared successfully")

                // Clear singleton instance
                clearInstance()
                Log.d("STRAVA_DEBUG", "[forceClearAllData] Singleton instance cleared")
            } catch (e: Exception) {
                Log.e("STRAVA_DEBUG", "[forceClearAllData] Error clearing data", e)
            }
        }
    }

    // Get activity by ID for detail screen
    suspend fun getActivityById(activityId: Long): StravaActivity? {
        return withContext(Dispatchers.IO) {
            try {
                val jwtToken = authManager.getJwtToken() ?: throw Exception("Not logged in")

                Log.d("StravaViewModel", "Looking for activity $activityId")
                Log.d("StravaViewModel", "Live activities count: ${stravaActivities.value.size}")
                Log.d(
                    "StravaViewModel",
                    "Database activities count: ${databaseActivities.value.size}"
                )

                // First check live activities
                val liveActivity = stravaActivities.value.find { it.id?.toLong() == activityId }
                if (liveActivity != null) {
                    Log.d("StravaViewModel", "Found activity $activityId in live activities")
                    return@withContext liveActivity
                }

                // If not found in live activities, check database activities
                val currentActivities = databaseActivities.value
                Log.d(
                    "StravaViewModel",
                    "Searching in ${currentActivities.size} database activities"
                )
                val dbActivity = currentActivities.find { it.id?.toLong() == activityId }
                if (dbActivity != null) {
                    Log.d("StravaViewModel", "Found activity $activityId in database activities")
                    return@withContext dbActivity
                }

                Log.w(
                    "StravaViewModel",
                    "Activity $activityId not found in cache. Available IDs in live: ${stravaActivities.value.map { it.id }}"
                )
                Log.w(
                    "StravaViewModel",
                    "Available IDs in database: ${currentActivities.map { it.id }}"
                )

                // If not found in cache, try to fetch from backend
                Log.d("StravaViewModel", "Attempting to fetch activity $activityId from backend")
                try {
                    // Try to get the activity's basic data by calling activities-db for a wider range
                    // This is a fallback - in a real app you might have a specific endpoint for single activity
                    val today =
                        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            .format(java.util.Date())
                    val oneYearAgo =
                        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            .format(
                                java.util.Date(System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000)
                            )

                    Log.d(
                        "StravaViewModel",
                        "Fetching activities from $oneYearAgo to $today to find activity $activityId"
                    )
                    val activities = getActivitiesFromDatabase(oneYearAgo, today)

                    val fetchedActivity = activities.find { it.id?.toLong() == activityId }
                    if (fetchedActivity != null) {
                        Log.d(
                            "StravaViewModel",
                            "Successfully fetched activity $activityId from backend"
                        )
                        return@withContext fetchedActivity
                    } else {
                        Log.w(
                            "StravaViewModel",
                            "Activity $activityId not found even after fetching from backend"
                        )
                    }
                } catch (e: Exception) {
                    Log.e("StravaViewModel", "Error fetching activity $activityId from backend", e)
                }

                null
            } catch (e: Exception) {
                Log.e("StravaViewModel", "Error getting activity by ID $activityId", e)
                null
            }
        }
    }

    fun refreshStravaToken() {
        Log.d("STRAVA_DEBUG", "[refreshStravaToken] Attempting to refresh Strava token")
        viewModelScope.launch {
            try {
                val jwtToken = authManager.getJwtToken() ?: throw Exception("Not logged in")
                val response = withContext(Dispatchers.IO) {
                    apiService.refreshToken("Bearer $jwtToken").execute()
                }

                if (response.isSuccessful) {
                    val newToken = response.body()
                    if (newToken != null) {
                        Log.d("STRAVA_DEBUG", "[refreshStravaToken] Token refreshed successfully")
                        authManager.saveStravaToken(newToken)
                        // Retry connection check with new token
                        checkConnectionStatus()
                    } else {
                        Log.e("STRAVA_DEBUG", "[refreshStravaToken] No token in response")
                        setStravaState(StravaState.Error("Failed to refresh token: No token in response"))
                    }
                } else {
                    Log.e("STRAVA_DEBUG", "[refreshStravaToken] Failed to refresh token: ${response.errorBody()?.string()}")
                    setStravaState(StravaState.Error("Failed to refresh token: ${response.errorBody()?.string()}"))
                }
            } catch (e: Exception) {
                Log.e("STRAVA_DEBUG", "[refreshStravaToken] Error refreshing token", e)
                setStravaState(StravaState.Error("Failed to refresh token: ${e.message}"))
            }
        }
    }

    // Method to check connection after OAuth with patience for rate limits
    fun checkConnectionAfterOAuth() {
        Log.d("STRAVA_DEBUG", "[checkConnectionAfterOAuth] Checking connection after OAuth completion")
        expectingConnectionAfterOAuth = true
        checkConnectionStatus()
    }

    // Method to set connected state immediately after successful OAuth (when we trust the backend)
    fun setConnectedAfterOAuth() {
        Log.d("STRAVA_DEBUG", "=== setConnectedAfterOAuth() START ===")
        Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Current state before: ${_stravaState.value}")
        Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] isOAuthInProgress before: $isOAuthInProgress")
        Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] checkConnectionJob before: ${checkConnectionJob?.isActive}")

        // Cancel any ongoing checkConnectionStatus job to prevent conflicts
        checkConnectionJob?.cancel()
        checkConnectionJob = null
        Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Cancelled any ongoing connection check job")

        // Set flags to prevent interference
        isOAuthInProgress = false
        justCompletedOAuth = true
        isCheckingConnection = false
        lastOAuthCompletionTime = System.currentTimeMillis() / 1000
        Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Set flags to prevent interference")
        Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] OAuth completion timestamp set to: $lastOAuthCompletionTime")

        // Try to set connected state immediately first
        try {
            val existingToken = authManager.getStravaToken()
            if (existingToken != null && !existingToken.accessToken.isNullOrEmpty()) {
                Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Found existing token, setting connected state immediately")
                _stravaUserData.value = StravaUserData(
                    userId = authManager.getUserId() ?: 0,
                    stravaId = 0,
                    accessToken = existingToken.accessToken,
                    refreshToken = existingToken.refreshToken,
                    tokenExpiresAt = existingToken.expiresAt
                )
                setStravaState(StravaState.Connected(_stravaUserData.value!!))
                Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Connected state set immediately with existing token")

                // Fetch athlete data to get the stravaId
                Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Fetching athlete data to get stravaId")
                fetchAthleteData()

                // Schedule reset of OAuth flags after cooldown period
                viewModelScope.launch {
                    delay(oAuthCooldownPeriod)
                    Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] OAuth cooldown period completed, resetting flags")
                    justCompletedOAuth = false
                    Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] justCompletedOAuth reset to: $justCompletedOAuth")
                }
                return
            }
        } catch (e: Exception) {
            Log.w("STRAVA_DEBUG", "[setConnectedAfterOAuth] Error setting immediate state: ${e.message}")
        }

        // If no existing token, try to get fresh token from backend
        viewModelScope.launch {
            try {
                Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Starting OAuth completion process")
                // Since OAuth was successful on backend, get the fresh token from backend first
                Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] OAuth successful on backend, fetching fresh token")

                val jwtToken = authManager.getJwtToken()
                Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] JWT token exists: ${!jwtToken.isNullOrEmpty()}")
                if (jwtToken == null) {
                    Log.e("STRAVA_DEBUG", "[setConnectedAfterOAuth] JWT token is null")
                    setStravaState(StravaState.Error("Not logged in"))
                    // Reset OAuth flags on error
                    isOAuthInProgress = false
                    justCompletedOAuth = false
                    Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] OAuth flags reset on error")
                    return@launch
                }

                // Get fresh token from backend
                Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Refreshing token to get latest from backend")
                val refreshResponse = withContext(Dispatchers.IO) {
                    Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Making API call to refresh token")
                    Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] JWT token: ${jwtToken.take(10)}...${jwtToken.takeLast(10)}")
                    Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Calling apiService.refreshToken()")
                    val result = apiService.refreshToken("Bearer $jwtToken").execute()
                    Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] refreshToken API call completed")
                    Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] refreshToken response code: ${result.code()}")
                    Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] refreshToken response successful: ${result.isSuccessful}")
                    Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] refreshToken response body: ${result.body()}")
                    Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] refreshToken error body: ${result.errorBody()?.string()}")
                    result
                }

                Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Refresh response code: ${refreshResponse.code()}")
                Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Refresh response successful: ${refreshResponse.isSuccessful}")

                if (refreshResponse.isSuccessful) {
                    val freshToken = refreshResponse.body()
                    Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Fresh token received: ${freshToken != null}")
                    if (freshToken != null && !freshToken.accessToken.isNullOrEmpty()) {
                        Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Fresh token received from backend")
                        Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Fresh token: ${freshToken.accessToken.take(10)}...${freshToken.accessToken.takeLast(10)}")

                        // Save the fresh token
                        authManager.saveStravaToken(freshToken)
                        Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Fresh token saved to SharedPreferences")

                        // Set connected state with fresh token
                        _stravaUserData.value = StravaUserData(
                            userId = authManager.getUserId() ?: 0,
                            stravaId = 0, // Will be updated when we get athlete data later
                            accessToken = freshToken.accessToken,
                            refreshToken = freshToken.refreshToken,
                            tokenExpiresAt = freshToken.expiresAt
                        )
                        Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] StravaUserData created with fresh token")

                        Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] About to set Connected state")
                        setStravaState(StravaState.Connected(_stravaUserData.value!!))
                        Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Connected state set successfully")

                        // Fetch athlete data to get the stravaId
                        Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Fetching athlete data to get stravaId")
                        fetchAthleteData()

                        expectingConnectionAfterOAuth = false
                        Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Flags reset - expectingConnectionAfterOAuth: $expectingConnectionAfterOAuth, justCompletedOAuth: $justCompletedOAuth, isOAuthInProgress: $isOAuthInProgress")
                        Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Connected state set successfully with fresh token")

                        // Schedule reset of OAuth flags after cooldown period
                        viewModelScope.launch {
                            delay(oAuthCooldownPeriod)
                            Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] OAuth cooldown period completed, resetting flags")
                            justCompletedOAuth = false
                            Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] justCompletedOAuth reset to: $justCompletedOAuth")
                        }
                    } else {
                        Log.e("STRAVA_DEBUG", "[setConnectedAfterOAuth] No valid fresh token from backend")
                        setStravaState(StravaState.Error("No valid token received from backend"))
                        justCompletedOAuth = false
                        isOAuthInProgress = false
                        Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Error case - flags reset")
                    }
                } else {
                    Log.e("STRAVA_DEBUG", "[setConnectedAfterOAuth] Failed to get fresh token from backend: ${refreshResponse.errorBody()?.string()}")
                    setStravaState(StravaState.Error("Failed to get fresh token from backend"))
                    justCompletedOAuth = false
                    isOAuthInProgress = false
                    Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Error case - flags reset")
                }
            } catch (e: Exception) {
                Log.e("STRAVA_DEBUG", "[setConnectedAfterOAuth] Error setting connected state", e)
                if (e is CancellationException) {
                    Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Job was cancelled, but OAuth was successful")
                    Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Attempting to set connected state anyway")

                    // Even if the job was cancelled, try to set the connected state
                    // since we know OAuth was successful on the backend
                    try {
                        val freshToken = authManager.getStravaToken()
                        if (freshToken != null && !freshToken.accessToken.isNullOrEmpty()) {
                            _stravaUserData.value = StravaUserData(
                                userId = authManager.getUserId() ?: 0,
                                stravaId = 0,
                                accessToken = freshToken.accessToken,
                                refreshToken = freshToken.refreshToken,
                                tokenExpiresAt = freshToken.expiresAt
                            )
                            setStravaState(StravaState.Connected(_stravaUserData.value!!))
                            Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Connected state set successfully despite job cancellation")

                            // Fetch athlete data to get the stravaId
                            Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Fetching athlete data to get stravaId")
                            fetchAthleteData()
                        } else {
                            Log.e("STRAVA_DEBUG", "[setConnectedAfterOAuth] No token available after job cancellation")
                            setStravaState(StravaState.Error("No token available after OAuth completion"))
                        }
                    } catch (innerException: Exception) {
                        Log.e("STRAVA_DEBUG", "[setConnectedAfterOAuth] Failed to set connected state after job cancellation", innerException)
                        setStravaState(StravaState.Error("Failed to set connected state: ${innerException.message}"))
                    }
                } else {
                    setStravaState(StravaState.Error("Failed to set connected state: ${e.message}"))
                }
                justCompletedOAuth = false
                isOAuthInProgress = false
                Log.d("STRAVA_DEBUG", "[setConnectedAfterOAuth] Exception case - flags reset")
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

    fun syncStravaActivities() {
        val jwtToken = authManager.getJwtToken() ?: return
        viewModelScope.launch {
            try {
                val response = apiService.syncStravaActivities("Bearer $jwtToken")
                if (response.isSuccessful) {
                    // Sync completed successfully
                    Log.d("StravaViewModel", "Sync completed successfully")
                } else {
                    Log.e("StravaViewModel", "Sync failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("StravaViewModel", "Error during sync", e)
            }
        }
    }

    fun syncCheck() {
        val jwtToken = authManager.getJwtToken() ?: return
        viewModelScope.launch {
            try {
                Log.d("StravaViewModel", "Starting sync check...")
                val response = apiService.syncCheck("Bearer $jwtToken")
                if (response.isSuccessful) {
                    val syncResult = response.body()
                    Log.d("StravaViewModel", "Sync check result: ${syncResult?.message}")

                    if (syncResult?.activities_synced != null && syncResult.activities_synced > 0) {
                        Log.d("StravaViewModel", "Activities synced: ${syncResult.activities_synced}")
                        // No need to refresh activities since sync-live handles this
                    } else {
                        Log.d("StravaViewModel", "No new activities to sync")
                    }
                } else {
                    Log.e("StravaViewModel", "Sync check failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("StravaViewModel", "Error during sync check", e)
            }
        }
    }

    fun estimateFtp() {
        viewModelScope.launch {
            try {
                val jwtToken = authManager.getJwtToken() ?: throw Exception("Not logged in")
                val result = withTimeout(300000) { // 5 minutes timeout for FTP estimation
                    withContext(Dispatchers.IO) {
                        apiService.estimateFtp("Bearer $jwtToken")
                    }
                }
                _ftpEstimate.value = result
            } catch (e: TimeoutCancellationException) {
                Log.e("StravaViewModel", "FTP estimation timed out after 5 minutes", e)
                _ftpEstimate.value = null
            } catch (e: Exception) {
                Log.e("StravaViewModel", "Error estimating FTP", e)
                _ftpEstimate.value = null
            }
        }
    }

    fun getLastFtpEstimate() {
        viewModelScope.launch {
            try {
                val jwtToken = authManager.getJwtToken() ?: throw Exception("Not logged in")
                val result = apiService.getLastFtpEstimateFromDb("Bearer $jwtToken")
                _ftpEstimate.value = result
            } catch (e: Exception) {
                Log.e("StravaViewModel", "Error getting last FTP estimate", e)
                _ftpEstimate.value = null
            }
        }
    }

    fun fetchFtpEstimateAfterSync() {
        viewModelScope.launch {
            try {
                Log.d("STRAVA_DEBUG", "[fetchFtpEstimateAfterSync] Fetching FTP estimate after sync")
                val jwtToken = authManager.getJwtToken() ?: throw Exception("Not logged in")

                // Use withTimeout for FTP estimation which might take longer
                val result = withTimeout(300000) { // 5 minutes timeout for FTP estimation
                    withContext(Dispatchers.IO) {
                        apiService.estimateFtp("Bearer $jwtToken")
                    }
                }

                _ftpEstimate.value = result
                Log.d("STRAVA_DEBUG", "[fetchFtpEstimateAfterSync] FTP estimate received: ${result.estimatedFTP}W")
            } catch (e: TimeoutCancellationException) {
                Log.e("STRAVA_DEBUG", "[fetchFtpEstimateAfterSync] FTP estimation timed out after 5 minutes", e)
                _ftpEstimate.value = null
            } catch (e: Exception) {
                Log.e("STRAVA_DEBUG", "[fetchFtpEstimateAfterSync] Error fetching FTP estimate", e)
                _ftpEstimate.value = null
            }
        }
    }

    private fun fetchAthleteData() {
        Log.d("STRAVA_DEBUG", "=== fetchAthleteData() STARTED ===")
        Log.d("STRAVA_DEBUG", "[fetchAthleteData] Current state: ${_stravaState.value}")
        Log.d("STRAVA_DEBUG", "[fetchAthleteData] StravaUserData: ${_stravaUserData.value}")

        viewModelScope.launch {
            try {
                Log.d("STRAVA_DEBUG", "[fetchAthleteData] Starting athlete data fetch")
                val jwtToken = authManager.getJwtToken()
                Log.d("STRAVA_DEBUG", "[fetchAthleteData] JWT token exists: ${!jwtToken.isNullOrEmpty()}")

                if (jwtToken == null) {
                    Log.e("STRAVA_DEBUG", "[fetchAthleteData] No JWT token")
                    return@launch
                }

                val stravaToken = authManager.getStravaToken()
                Log.d("STRAVA_DEBUG", "[fetchAthleteData] Strava token exists: ${stravaToken != null}")

                if (stravaToken == null) {
                    Log.e("STRAVA_DEBUG", "[fetchAthleteData] No Strava token")
                    return@launch
                }

                Log.d("STRAVA_DEBUG", "[fetchAthleteData] Making API call to get athlete data")
                val response = withContext(Dispatchers.IO) {
                    apiService.getAthlete(
                        jwtToken = "Bearer $jwtToken",
                        stravaToken = stravaToken.accessToken
                    ).execute()
                }

                Log.d("STRAVA_DEBUG", "[fetchAthleteData] Response code: ${response.code()}")
                Log.d("STRAVA_DEBUG", "[fetchAthleteData] Response successful: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    val athlete = response.body()
                    Log.d("STRAVA_DEBUG", "[fetchAthleteData] Athlete data received: ${athlete != null}")

                    if (athlete != null) {
                        Log.d("STRAVA_DEBUG", "[fetchAthleteData] Athlete ID: ${athlete.id}")
                        Log.d("STRAVA_DEBUG", "[fetchAthleteData] Athlete name: ${athlete.firstName} ${athlete.lastName}")

                        _stravaAthlete.value = athlete
                        Log.d("STRAVA_DEBUG", "[fetchAthleteData] Athlete data saved")

                        // Update StravaUserData with athlete ID
                        val updatedUserData = _stravaUserData.value?.copy(stravaId = athlete.id)
                        _stravaUserData.value = updatedUserData
                        Log.d("STRAVA_DEBUG", "[fetchAthleteData] StravaUserData updated with athlete ID")

                        // Update the StravaState to reflect the new user data with athlete ID
                        if (updatedUserData != null) {
                            Log.d("STRAVA_DEBUG", "[fetchAthleteData] Updating StravaState with updated user data")
                            setStravaState(StravaState.Connected(updatedUserData))
                            Log.d("STRAVA_DEBUG", "[fetchAthleteData] StravaState updated successfully")
                        }

                        Log.d("STRAVA_DEBUG", "[fetchAthleteData] Athlete data fetch completed successfully")
                    } else {
                        Log.e("STRAVA_DEBUG", "[fetchAthleteData] No athlete data in response")
                    }
                } else {
                    Log.e("STRAVA_DEBUG", "[fetchAthleteData] API call failed: ${response.code()}")
                    Log.e("STRAVA_DEBUG", "[fetchAthleteData] Error body: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("STRAVA_DEBUG", "[fetchAthleteData] Exception during athlete data fetch", e)
            }
        }
        Log.d("STRAVA_DEBUG", "=== fetchAthleteData() COMPLETED ===")
    }

    private fun resetOAuthFlags() {
        Log.d("STRAVA_DEBUG", "=== resetOAuthFlags() CALLED ===")
        Log.d("STRAVA_DEBUG", "[resetOAuthFlags] isOAuthInProgress before: $isOAuthInProgress")
        Log.d("STRAVA_DEBUG", "[resetOAuthFlags] justCompletedOAuth before: $justCompletedOAuth")
        Log.d("STRAVA_DEBUG", "[resetOAuthFlags] expectingConnectionAfterOAuth before: $expectingConnectionAfterOAuth")

        isOAuthInProgress = false
        justCompletedOAuth = false
        expectingConnectionAfterOAuth = false

        Log.d("STRAVA_DEBUG", "[resetOAuthFlags] isOAuthInProgress after: $isOAuthInProgress")
        Log.d("STRAVA_DEBUG", "[resetOAuthFlags] justCompletedOAuth after: $justCompletedOAuth")
        Log.d("STRAVA_DEBUG", "[resetOAuthFlags] expectingConnectionAfterOAuth after: $expectingConnectionAfterOAuth")
        Log.d("STRAVA_DEBUG", "=== resetOAuthFlags() COMPLETED ===")
    }

    // Get activities from database for specific date range
    suspend fun getActivitiesFromDatabase(startDate: String, endDate: String): List<StravaActivity> {
        return withContext(Dispatchers.IO) {
            try {
                val jwtToken = authManager.getJwtToken() ?: throw Exception("Not logged in")
                val response = apiService.getActivitiesFromDb("Bearer $jwtToken", startDate, endDate)

                if (response.isSuccessful) {
                    val activities = response.body() ?: emptyList()

                    // Update the database activities StateFlow
                    val currentDbActivities = _databaseActivities.value.toMutableList()
                    activities.forEach { newActivity ->
                        // Add if not already present
                        if (currentDbActivities.none { it.id == newActivity.id }) {
                            currentDbActivities.add(newActivity)
                        }
                    }
                    _databaseActivities.value = currentDbActivities

                    Log.d(
                        "StravaViewModel",
                        "Got ${activities.size} activities from database for $startDate to $endDate"
                    )
                    activities
                } else {
                    Log.e(
                        "StravaViewModel",
                        "Failed to get activities from database: ${response.errorBody()?.string()}"
                    )
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("StravaViewModel", "Error getting activities from database", e)
                emptyList()
            }
        }
    }

    // Get unified activities (Strava + App workouts) for specific date range
    suspend fun getUnifiedActivities(startDate: String, endDate: String): List<StravaActivity> {
        return withContext(Dispatchers.IO) {
            try {
                val jwtToken = authManager.getJwtToken() ?: throw Exception("Not logged in")
                val response =
                    apiService.getUnifiedActivities("Bearer $jwtToken", startDate, endDate)

                if (response.isSuccessful) {
                    val activities = response.body() ?: emptyList()

                    // Update a unified activities StateFlow if needed, but for now return the list
                    Log.d(
                        "StravaViewModel",
                        "Got ${activities.size} unified activities for $startDate to $endDate"
                    )
                    activities
                } else {
                    Log.e(
                        "StravaViewModel",
                        "Failed to get unified activities: ${response.errorBody()?.string()}"
                    )
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("StravaViewModel", "Error getting unified activities", e)
                emptyList()
            }
        }
    }

    // Get GPX file URL for specific activity
    suspend fun getActivityGpxUrl(activityId: Long): String? {
        return withContext(Dispatchers.IO) {
            try {
                val jwtToken = authManager.getJwtToken() ?: throw Exception("Not logged in")
                val response = apiService.getActivityGpx("Bearer $jwtToken", activityId)

                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.get("status") == "success") {
                        val url = result["url"]
                        Log.d("StravaViewModel", "Got GPX URL for activity $activityId")
                        url
                    } else {
                        Log.e("StravaViewModel", "Failed to get GPX URL: ${result?.get("message")}")
                        null
                    }
                } else {
                    Log.e("StravaViewModel", "Failed to get GPX URL: ${response.errorBody()?.string()}")
                    null
                }
            } catch (e: Exception) {
                Log.e("StravaViewModel", "Error getting GPX URL for activity $activityId", e)
                null
            }
        }
    }

    // Get map view URL for specific activity
    suspend fun getActivityMapView(activityId: Long): Map<String, String> {
        return withContext(Dispatchers.IO) {
            try {
                val jwtToken = authManager.getJwtToken() ?: throw Exception("Not logged in")
                val response = apiService.getActivityMapView("Bearer $jwtToken", activityId)

                if (response.isSuccessful) {
                    val result = response.body() ?: emptyMap()
                    Log.d("StravaViewModel", "Got map view data for activity $activityId: $result")
                    result
                } else {
                    Log.e(
                        "StravaViewModel",
                        "Failed to get map view: ${response.errorBody()?.string()}"
                    )
                    emptyMap()
                }
            } catch (e: Exception) {
                Log.e("StravaViewModel", "Error getting map view for activity $activityId", e)
                emptyMap()
            }
        }
    }

    // Get activity streams data for detailed analysis (DEPRECATED - using DB version instead)
    suspend fun getActivityStreams(activityId: Long): ActivityStreamsResponse? {
        // This method is deprecated - use getActivityStreamsFromDB instead
        return null
    }

    // Get activity streams from database
    suspend fun getActivityStreamsFromDB(activityId: Long): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            try {
                val jwtToken = authManager.getJwtToken() ?: throw Exception("Not logged in")
                Log.d("StravaViewModel", "Requesting streams from DB for activity $activityId")

                val response = apiService.getActivityStreamsFromDB("Bearer $jwtToken", activityId)
                Log.d("StravaViewModel", "Streams from DB response code: ${response.code()}")

                if (response.isSuccessful) {
                    response.body() ?: emptyMap()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(
                        "StravaViewModel",
                        "Failed to get streams from DB: HTTP ${response.code()} - $errorBody"
                    )
                    emptyMap()
                }
            } catch (e: Exception) {
                Log.e("StravaViewModel", "Error getting streams from DB for activity $activityId", e)
                emptyMap()
            }
        }
    }

    // Get power curve data for cycling activities
    suspend fun getActivityPowerCurve(activityId: Long): com.example.fitnessapp.model.PowerCurveResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val jwtToken = authManager.getJwtToken() ?: throw Exception("Not logged in")
                Log.d("StravaViewModel", "Requesting power curve for activity $activityId")

                val response = apiService.getActivityPowerCurve("Bearer $jwtToken", activityId)
                Log.d("StravaViewModel", "Power curve response code: ${response.code()}")

                if (response.isSuccessful) {
                    val powerCurve = response.body()
                    Log.d("StravaViewModel", "Power curve data received for activity $activityId")
                    powerCurve
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(
                        "StravaViewModel",
                        "Failed to get power curve: HTTP ${response.code()} - $errorBody"
                    )
                    null
                }
            } catch (e: Exception) {
                Log.e("StravaViewModel", "Error getting power curve for activity $activityId", e)
                null
            }
        }
    }

    // Get max BPM for the user
    suspend fun getMaxBpm(): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            try {
                val jwtToken = authManager.getJwtToken() ?: throw Exception("Not logged in")
                Log.d("StravaViewModel", "Requesting max BPM for user")

                val response = apiService.getMaxBpm("Bearer $jwtToken")
                Log.d("StravaViewModel", "Max BPM response code: ${response.code()}")

                if (response.isSuccessful) {
                    val maxBpmData = response.body() ?: emptyMap()
                    Log.d("StravaViewModel", "Max BPM data received: $maxBpmData")
                    maxBpmData
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(
                        "StravaViewModel",
                        "Failed to get max BPM: HTTP ${response.code()} - $errorBody"
                    )
                    emptyMap()
                }
            } catch (e: Exception) {
                Log.e("StravaViewModel", "Error getting max BPM", e)
                emptyMap()
            }
        }
    }

    // Delete Strava activity
    suspend fun deleteStravaActivity(activityId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val jwtToken = authManager.getJwtToken() ?: throw Exception("Not logged in")
                Log.d("StravaViewModel", "Deleting Strava activity $activityId")

                val response = mainApiService.deleteStravaActivity("Bearer $jwtToken", activityId)
                Log.d("StravaViewModel", "Delete Strava activity response code: ${response.code()}")

                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d("StravaViewModel", "Strava activity $activityId deleted successfully: $result")
                    true
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(
                        "StravaViewModel",
                        "Failed to delete Strava activity: HTTP ${response.code()} - $errorBody"
                    )
                    false
                }
            } catch (e: Exception) {
                Log.e("StravaViewModel", "Error deleting Strava activity $activityId", e)
                false
            }
        }
    }

    // Delete app workout
    suspend fun deleteAppWorkout(workoutId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val jwtToken = authManager.getJwtToken() ?: throw Exception("Not logged in")
                Log.d("StravaViewModel", "Deleting app workout $workoutId")

                val response = mainApiService.deleteAppWorkout("Bearer $jwtToken", workoutId)
                Log.d("StravaViewModel", "Delete app workout response code: ${response.code()}")

                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d("StravaViewModel", "App workout $workoutId deleted successfully: $result")
                    true
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(
                        "StravaViewModel",
                        "Failed to delete app workout: HTTP ${response.code()} - $errorBody"
                    )
                    false
                }
            } catch (e: Exception) {
                Log.e("StravaViewModel", "Error deleting app workout $workoutId", e)
                false
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
