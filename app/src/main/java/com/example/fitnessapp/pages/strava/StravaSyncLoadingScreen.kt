package com.example.fitnessapp.pages.strava

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnessapp.R
import com.example.fitnessapp.viewmodel.StravaViewModel
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.api.ApiConfig
import kotlinx.coroutines.delay
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.GlobalScope
import java.io.BufferedReader
import java.io.InputStreamReader
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.net.URL
import java.net.HttpURLConnection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import com.example.fitnessapp.ui.theme.FitnessAppTheme
import com.example.fitnessapp.mock.SharedPreferencesMock
import com.example.fitnessapp.utils.StravaPrefs

// Function to estimate all FTHR types - defined outside Composable
suspend fun estimateAllFthrTypes(jwtToken: String) {
    // Estimate running FTHR
    try {
        val runningFthrResponse = withContext(Dispatchers.IO) {
            val url = URL("${ApiConfig.BASE_URL}strava/estimate-running-fthr")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $jwtToken")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 60000
            conn.readTimeout = 60000
            if (conn.responseCode == 200) {
                val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
                Log.d("StravaSyncLoading", "Running FTHR estimate: $responseBody")
                responseBody
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("StravaSyncLoading", "Running FTHR estimate failed: ${conn.responseCode} - $errorBody")
                null
            }
        }
    } catch (e: Exception) {
        Log.e("StravaSyncLoading", "Error estimating running FTHR", e)
    }
    
    // Estimate swimming FTHR
    try {
        val swimmingFthrResponse = withContext(Dispatchers.IO) {
            val url = URL("${ApiConfig.BASE_URL}strava/estimate-swimming-fthr")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $jwtToken")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 60000
            conn.readTimeout = 60000
            if (conn.responseCode == 200) {
                val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
                Log.d("StravaSyncLoading", "Swimming FTHR estimate: $responseBody")
                responseBody
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("StravaSyncLoading", "Swimming FTHR estimate failed: ${conn.responseCode} - $errorBody")
                null
            }
        }
    } catch (e: Exception) {
        Log.e("StravaSyncLoading", "Error estimating swimming FTHR", e)
    }
    
    // Estimate other FTHR
    try {
        val otherFthrResponse = withContext(Dispatchers.IO) {
            val url = URL("${ApiConfig.BASE_URL}strava/estimate-other-fthr")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $jwtToken")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 60000
            conn.readTimeout = 60000
            if (conn.responseCode == 200) {
                val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
                Log.d("StravaSyncLoading", "Other FTHR estimate: $responseBody")
                responseBody
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("StravaSyncLoading", "Other FTHR estimate failed: ${conn.responseCode} - $errorBody")
                null
            }
        }
    } catch (e: Exception) {
        Log.e("StravaSyncLoading", "Error estimating other FTHR", e)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StravaSyncLoadingScreen(
    onNavigateBack: () -> Unit,
    onSyncComplete: () -> Unit,
    onViewActivities: () -> Unit,
    stravaViewModel: StravaViewModel,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val cancelFlag = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    var syncProgress by remember { mutableFloatStateOf(0f) }
    var currentStep by remember { mutableStateOf("Pregătire sincronizare...") }
    var activitiesSynced by remember { mutableIntStateOf(0) }
    var isCompleted by remember { mutableStateOf(false) }
    var ftpEstimate by remember { mutableStateOf<Float?>(null) }
    var fthrEstimate by remember { mutableStateOf<Int?>(null) }
    var syncError by remember { mutableStateOf<String?>(null) }
    var currentActivityName by remember { mutableStateOf<String?>(null) }
    
    val stravaActivities by stravaViewModel.stravaActivities.collectAsState()
    val stravaState by stravaViewModel.stravaState.collectAsState()
    
    // Monitor real activities count
    LaunchedEffect(stravaActivities) {
        if (stravaActivities.isNotEmpty()) {
            Log.d("StravaSyncLoading", "Real activities updated: ${stravaActivities.size}")
            activitiesSynced = stravaActivities.size
        }
    }
    
    // SSE Streaming sync process using /strava/sync-live
    LaunchedEffect(Unit) {
        Log.d("StravaSyncLoading", "Starting SSE streaming sync with /strava/sync-live")
        
        try {
            // Etapa 1: Pregătire
            currentStep = "Conectare la Strava..."
            syncProgress = 0.1f
            delay(500)
            
            // Etapa 2: Verificare conexiune
            currentStep = "Verificare conexiune Strava..."
            syncProgress = 0.2f
            
            Log.d("StravaSyncLoading", "Current stravaState: $stravaState")
            if (stravaState !is com.example.fitnessapp.viewmodel.StravaState.Connected) {
                Log.e("StravaSyncLoading", "Not connected to Strava. Current state: $stravaState")
                throw Exception("Nu sunteți conectat la Strava. Starea curentă: $stravaState")
            }
            
            delay(500)
            
            // Etapa 3: Pornire sincronizare SSE
            currentStep = "Pornire sincronizare în timp real..."
            syncProgress = 0.3f
            
            val jwtToken = authViewModel.getToken()
            Log.d("StravaSyncLoading", "JWT token exists: ${!jwtToken.isNullOrEmpty()}")
            if (jwtToken.isNullOrEmpty()) {
                Log.e("StravaSyncLoading", "JWT token is null or empty")
                throw Exception("Token de autentificare lipsă")
            }
            
            Log.d("StravaSyncLoading", "Starting SSE sync with /strava/sync-live")
            Log.d("StravaSyncLoading", "Base URL: ${ApiConfig.BASE_URL}")
            val fullUrl = "${ApiConfig.BASE_URL}strava/sync-live"
            Log.d("StravaSyncLoading", "Full URL: $fullUrl")
            
            // Variables for reconnection logic and progress modelling
            var totalActivitiesSynced = 0
            var maxRetries = 3
            var currentRetry = 0
            var isSyncCompleted = false
            var expectedTotal: Int? = null // will be filled if server sends total/remaining
            var lastUiUpdateMs = 0L
            val gson = Gson() // Define gson here so it's accessible in all blocks
            
            while (!isSyncCompleted && currentRetry < maxRetries) {
                if (currentRetry > 0) {
                    Log.d("StravaSyncLoading", "Reconnection attempt $currentRetry/$maxRetries")
                    withContext(Dispatchers.Main) {
                        currentStep = "Reconectare... (încercarea $currentRetry/$maxRetries)"
                        syncProgress = 0.3f + (currentRetry * 0.1f)
                    }
                    delay(2000) // Wait before reconnecting
                }
                
                // Folosesc HttpURLConnection pentru SSE streaming
                withContext(Dispatchers.IO) {
                    val url = URL(fullUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    
                    try {
                        connection.requestMethod = "GET"
                        connection.setRequestProperty("Authorization", "Bearer $jwtToken")
                        connection.setRequestProperty("Accept", "text/event-stream")
                        connection.setRequestProperty("Cache-Control", "no-cache")
                        connection.setRequestProperty("Connection", "keep-alive")
                        connection.connectTimeout = 30000 // 30 seconds
                        connection.readTimeout = 0 // No timeout for streaming
                        
                        Log.d("StravaSyncLoading", "Making SSE request... (attempt ${currentRetry + 1})")
                        Log.d("StravaSyncLoading", "Response code: ${connection.responseCode}")
                        
                        if (connection.responseCode != 200) {
                            val errorStream = connection.errorStream
                            val errorBody = errorStream?.bufferedReader()?.use { it.readText() }
                            Log.e("StravaSyncLoading", "SSE request failed: ${connection.responseCode}")
                            Log.e("StravaSyncLoading", "Error body: $errorBody")
                            throw Exception("Sincronizare eșuată: ${connection.responseCode} - $errorBody")
                        }
                        
                        val inputStream = connection.inputStream
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        
                        Log.d("StravaSyncLoading", "SSE stream started, reading events...")
                        
                        var line: String?
                        var activitiesCount = 0

                        while (reader.readLine().also { line = it } != null) {
                            if (cancelFlag.get()) {
                                withContext(Dispatchers.Main) {
                                    syncError = "Anulat de utilizator"
                                    currentStep = "Sincronizare anulată"
                                }
                                break
                            }
                            Log.d("StravaSyncLoading", "SSE line: $line")
                            
                            val currentLine = line // Store in local variable for smart cast
                            if (currentLine?.startsWith("data: ") == true) {
                                val jsonData = currentLine.substring(6) // Remove "data: " prefix
                                
                                try {
                                    val eventData = gson.fromJson(jsonData, JsonObject::class.java)
                                    Log.d("StravaSyncLoading", "Parsed event data: $eventData")
                                    
                                    // Check if it's a sync completion event
                                    if (eventData.has("status") && eventData.get("status").asString == "done") {
                                        Log.d("StravaSyncLoading", "Sync completed!")
                                        withContext(Dispatchers.Main) {
                                            currentStep = "Finalizare sincronizare..."
                                            syncProgress = 1.0f
                                            isCompleted = true
                                            currentStep = "Sincronizare completă!"
                                            activitiesSynced = totalActivitiesSynced + activitiesCount
                                        }
                                        isSyncCompleted = true
                                        break
                                    }
                                    
                                    // Check if it's an error event
                                    if (eventData.has("error")) {
                                        val errorMessage = eventData.get("error").asString
                                        Log.e("StravaSyncLoading", "SSE error: $errorMessage")
                                        withContext(Dispatchers.Main) {
                                            syncError = errorMessage
                                            currentStep = "Eroare: $errorMessage"
                                        }
                                        throw Exception(errorMessage)
                                    }
                                    
                                    // Detect total if provided
                                    if (eventData.has("total")) {
                                        expectedTotal = try { eventData.get("total").asInt } catch (_: Exception) { expectedTotal }
                                        Log.d("StravaSyncLoading", "Expected total activities: $expectedTotal")
                                    }
                                    if (eventData.has("remaining") && expectedTotal == null) {
                                        val remaining = runCatching { eventData.get("remaining").asInt }.getOrNull()
                                        if (remaining != null) {
                                            expectedTotal = (totalActivitiesSynced + activitiesCount) + remaining
                                            Log.d("StravaSyncLoading", "Derived expected total: $expectedTotal from remaining=$remaining")
                                        }
                                    }

                                    // Check if it's an activity sync event
                                    if (eventData.has("name") && eventData.has("start_date")) {
                                        activitiesCount++
                                        val activityName = eventData.get("name").asString
                                        val startDate = eventData.get("start_date").asString

                                        Log.d("StravaSyncLoading", "Activity synced: $activityName ($startDate)")

                                        val now = System.currentTimeMillis()
                                        val totalNow = totalActivitiesSynced + activitiesCount

                                        // Compute progress
                                        val progress = if (expectedTotal != null && expectedTotal!! > 0) {
                                            val ratio = (totalNow.toFloat() / expectedTotal!!.toFloat()).coerceIn(0f, 1f)
                                            0.1f + 0.8f * ratio // keep headroom for finalization steps
                                        } else {
                                            // Smoothly approach 0.9 as count grows (good for hundreds of activities)
                                            val k = 60f // smoothing constant
                                            val p = 1 - kotlin.math.exp(-totalNow.toFloat() / k)
                                            (0.1f + 0.8f * p).coerceAtMost(0.95f)
                                        }

                                        // Throttle UI updates (time-based OR by every 5 activities)
                                        val shouldUpdateUi = (now - lastUiUpdateMs) >= 300 || (totalNow % 5 == 0)
                                        if (shouldUpdateUi) {
                                            lastUiUpdateMs = now
                                            withContext(Dispatchers.Main) {
                                                currentActivityName = activityName
                                                activitiesSynced = totalNow
                                                syncProgress = progress
                                                currentStep = if (expectedTotal != null) {
                                                    "Sincronizare activitate $totalNow din $expectedTotal"
                                                } else {
                                                    "Sincronizare activitate $totalNow"
                                                }
                                            }
                                        }
                                    }
                                    
                                } catch (e: Exception) {
                                    Log.e("StravaSyncLoading", "Error parsing SSE event", e)
                                }
                            }
                        }
                        
                        // If we reach here without getting "done" status, connection was dropped
                        if (!isSyncCompleted) {
                            Log.w("StravaSyncLoading", "Connection dropped after syncing $activitiesCount activities")
                            totalActivitiesSynced += activitiesCount
                            currentRetry++
                            
                            withContext(Dispatchers.Main) {
                                currentStep = "Conexiunea s-a întrerupt. Reconectare..."
                                syncProgress = 0.3f + (currentRetry * 0.1f)
                            }
                        }
                        
                    } catch (e: Exception) {
                        Log.e("StravaSyncLoading", "Error during SSE sync (attempt ${currentRetry + 1})", e)
                        
                        // If it's a connection error, try to reconnect
                        val msg = (e.message ?: "").lowercase()
                        val isTransient = msg.contains("connection") || msg.contains("timeout") || msg.contains("protocol error") || msg.contains("bad_decrypt") || msg.contains("bad record mac") || msg.contains("ssl") || msg.contains("read error")
                        if (isTransient) {
                            Log.w("StravaSyncLoading", "Connection error detected, will retry")
                            currentRetry++
                            
                            withContext(Dispatchers.Main) {
                                currentStep = "Eroare de conexiune. Reconectare..."
                                syncProgress = 0.3f + (currentRetry * 0.1f)
                            }
                        } else {
                            // For other errors, don't retry
                            throw e
                        }
                    } finally {
                        connection.disconnect()
                    }
                }
            }
            
            // If we've exhausted retries without completion
            if (!isSyncCompleted) {
                throw Exception("Sincronizarea s-a întrerupt după $maxRetries încercări. $totalActivitiesSynced activități sincronizate.")
            }
            
            Log.d("StravaSyncLoading", "SSE stream completed")
            
            // Estimate FTHR after successful sync
            Log.d("StravaSyncLoading", "Estimating FTHR after successful sync...")
            withContext(Dispatchers.Main) {
                currentStep = "Estimare FTHR din activități recente..."
                syncProgress = 0.95f
            }
            
            // Make direct FTHR estimation request
            try {
                val fthrResponse = withContext(Dispatchers.IO) {
                    val fthrUrl = "${ApiConfig.BASE_URL}strava/estimate-cycling-fthr"
                    Log.d("StravaSyncLoading", "FTHR estimate URL: $fthrUrl")
                    
                    val fthrConnection = URL(fthrUrl).openConnection() as HttpURLConnection
                    fthrConnection.requestMethod = "GET"
                    fthrConnection.setRequestProperty("Authorization", "Bearer $jwtToken")
                    fthrConnection.setRequestProperty("Content-Type", "application/json")
                    fthrConnection.connectTimeout = 60000 // 60 seconds
                    fthrConnection.readTimeout = 60000 // 60 seconds
                    
                    Log.d("StravaSyncLoading", "Making FTHR estimate request...")
                    Log.d("StravaSyncLoading", "FTHR response code: ${fthrConnection.responseCode}")
                    
                    if (fthrConnection.responseCode == 200) {
                        val responseBody = fthrConnection.inputStream.bufferedReader().use { it.readText() }
                        Log.d("StravaSyncLoading", "FTHR estimate response: $responseBody")
                        
                        val fthrData = gson.fromJson(responseBody, JsonObject::class.java)
                        val estimatedFthr = fthrData.get("estimated_fthr")?.asInt
                        val activitiesUsed = fthrData.get("activities_used")?.asInt
                        val maxHrObserved = fthrData.get("max_hr_observed")?.asInt
                        
                        Log.d("StravaSyncLoading", "FTHR estimate parsed: ${estimatedFthr} bpm (activities: ${activitiesUsed}, max HR: ${maxHrObserved})")
                        
                        withContext(Dispatchers.Main) {
                            fthrEstimate = estimatedFthr
                            currentStep = "FTHR estimat: ${estimatedFthr} bpm"
                        }
                        
                        fthrData
                    } else {
                        val errorBody = fthrConnection.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e("StravaSyncLoading", "FTHR estimate failed: ${fthrConnection.responseCode} - $errorBody")
                        null
                    }
                }
                
                if (fthrResponse != null) {
                    Log.d("StravaSyncLoading", "FTHR estimation completed successfully")
                } else {
                    Log.w("StravaSyncLoading", "FTHR estimation failed, but sync was successful")
                }
            } catch (e: Exception) {
                Log.e("StravaSyncLoading", "Error estimating FTHR", e)
                // Don't fail the sync if FTHR estimation fails
            }
            
            delay(1000) // Small delay to show FTHR estimation step

            // Also estimate running, swimming, and other FTHR (non-blocking, log errors)
            estimateAllFthrTypes(jwtToken)

            // Persist last sync time for UI surfaces
            StravaPrefs.setLastSyncNow(context)

            // Call /running/pace-prediction to generate running pace predictions
            try {
                withContext(Dispatchers.Main) {
                    currentStep = "Generare predicții viteză alergare..."
                    syncProgress = 0.97f
                }
                
                val runningPaceResponse = withContext(Dispatchers.IO) {
                    val url = URL("${ApiConfig.BASE_URL}running/pace-prediction")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("Authorization", "Bearer $jwtToken")
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.connectTimeout = 60000
                    conn.readTimeout = 60000
                    
                    Log.d("StravaSyncLoading", "Making running pace prediction request...")
                    Log.d("StravaSyncLoading", "Running pace response code: ${conn.responseCode}")
                    
                    if (conn.responseCode == 200) {
                        val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
                        Log.d("StravaSyncLoading", "Running pace prediction response: $responseBody")
                        
                        // Parse the response to show predictions
                        try {
                            val predictionsArray = gson.fromJson(responseBody, Array<JsonObject>::class.java)
                            Log.d("StravaSyncLoading", "Running pace predictions generated: ${predictionsArray.size} predictions")
                            
                            predictionsArray.forEach { prediction ->
                                val distance = prediction.get("distance_km")?.asFloat
                                val time = prediction.get("time")?.asString
                                val pace = prediction.get("pace_min_per_km")?.asString
                                val avgHr = prediction.get("avg_hr")?.asInt
                                val adjustedForHr = prediction.get("adjusted_for_hr")?.asFloat
                                
                                Log.d("StravaSyncLoading", "Prediction: ${distance}km in $time (pace: $pace, HR: ${avgHr}bpm, adjusted: ${adjustedForHr})")
                            }
                        } catch (e: Exception) {
                            Log.e("StravaSyncLoading", "Error parsing running pace predictions", e)
                        }
                        
                        responseBody
                    } else {
                        val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e("StravaSyncLoading", "Running pace prediction failed: ${conn.responseCode} - $errorBody")
                        null
                    }
                }
                
                if (runningPaceResponse != null) {
                    Log.d("StravaSyncLoading", "Running pace predictions completed successfully")
                    withContext(Dispatchers.Main) {
                        currentStep = "Predicții viteză alergare generate"
                    }
                } else {
                    Log.w("StravaSyncLoading", "Running pace prediction failed, but sync was successful")
                }
            } catch (e: Exception) {
                Log.e("StravaSyncLoading", "Error calling running pace prediction endpoint", e)
                // Don't fail the sync if running pace prediction fails
            }

            // Call /swim/best-time-prediction to generate swimming best time predictions
            try {
                withContext(Dispatchers.Main) {
                    currentStep = "Generare predicții timp înot..."
                    syncProgress = 0.975f
                }
                
                val swimBestTimeResponse = withContext(Dispatchers.IO) {
                    val url = URL("${ApiConfig.BASE_URL}swim/best-time-prediction")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("Authorization", "Bearer $jwtToken")
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.connectTimeout = 60000
                    conn.readTimeout = 60000
                    
                    Log.d("StravaSyncLoading", "Making swimming best time prediction request...")
                    Log.d("StravaSyncLoading", "Swimming best time response code: ${conn.responseCode}")
                    
                    if (conn.responseCode == 200) {
                        val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
                        Log.d("StravaSyncLoading", "Swimming best time prediction response: $responseBody")
                        
                        // Parse the response to show predictions
                        try {
                            val predictionsArray = gson.fromJson(responseBody, Array<JsonObject>::class.java)
                            Log.d("StravaSyncLoading", "Swimming best time predictions generated: ${predictionsArray.size} predictions")
                            
                            predictionsArray.forEach { prediction ->
                                val distance = prediction.get("distance_m")?.asInt
                                val time = prediction.get("time")?.asString
                                
                                Log.d("StravaSyncLoading", "Swimming prediction: ${distance}m in $time")
                            }
                        } catch (e: Exception) {
                            Log.e("StravaSyncLoading", "Error parsing swimming best time predictions", e)
                        }
                        
                        responseBody
                    } else {
                        val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e("StravaSyncLoading", "Swimming best time prediction failed: ${conn.responseCode} - $errorBody")
                        null
                    }
                }
                
                if (swimBestTimeResponse != null) {
                    Log.d("StravaSyncLoading", "Swimming best time predictions completed successfully")
                    withContext(Dispatchers.Main) {
                        currentStep = "Predicții timp înot generate"
                    }
                } else {
                    Log.w("StravaSyncLoading", "Swimming best time prediction failed, but sync was successful")
                }
            } catch (e: Exception) {
                Log.e("StravaSyncLoading", "Error calling swimming best time prediction endpoint", e)
                // Don't fail the sync if swimming best time prediction fails
            }

            // Call /strava/calculate-hrtss to recompute missing HrTSS values
            try {
                val hrtssResponse = withContext(Dispatchers.IO) {
                    val url = URL("${ApiConfig.BASE_URL}strava/calculate-hrtss")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Authorization", "Bearer $jwtToken")
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.connectTimeout = 60000
                    conn.readTimeout = 60000
                    if (conn.responseCode == 200) {
                        val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
                        Log.d("StravaSyncLoading", "HrTSS calculation response: $responseBody")
                        responseBody
                    } else {
                        val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e("StravaSyncLoading", "HrTSS calculation failed: ${conn.responseCode} - $errorBody")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("StravaSyncLoading", "Error calling calculate-hrtss endpoint", e)
            }
            
            // Estimate FTP after FTHR estimation
            Log.d("StravaSyncLoading", "Estimating FTP after FTHR estimation...")
            withContext(Dispatchers.Main) {
                currentStep = "Estimare FTP din datele de putere..."
                syncProgress = 0.98f
            }
            
            withContext(Dispatchers.Main) {
                stravaViewModel.fetchFtpEstimateAfterSync()
            }
            
            delay(2000)
            withContext(Dispatchers.Main) {
                onSyncComplete()
            }
            
        } catch (e: Exception) {
            Log.e("StravaSyncLoading", "Error during SSE sync", e)
            Log.e("StravaSyncLoading", "Error message: ${e.message}")
            Log.e("StravaSyncLoading", "Error cause: ${e.cause}")
            syncError = e.message ?: "Eroare necunoscută"
            currentStep = "Eroare: $syncError"
        }
    }
    
    // UI cu aceleași culori ca StravaAuthScreen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6366F1),
                        Color(0xFF8B5CF6),
                        Color(0xFFA855F7)
                    )
                )
            )
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                enabled = isCompleted || syncError != null
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            // Strava Logo and Title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_strava),
                    contentDescription = "Strava",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sync Live",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Cancel action while syncing
            TextButton(
                onClick = {
                    cancelFlag.set(true)
                    onNavigateBack()
                },
                enabled = !isCompleted && syncError == null
            ) {
                Text("Cancel", color = Color.White)
            }
        }

        // Content
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                
                if (syncError != null) {
                    // Error State
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(72.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    /*
                    
                    Text(
                        text = "✗ Eroare sincronizare",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )
                    
                    */
                    Text(
                        text = "Eroare sincronizare",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = syncError!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    
                } else if (isCompleted) {
                    // Success State
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check_circle),
                        contentDescription = "Success",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(72.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "✓ Sincronizare completă!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "$activitiesSynced activities synchronized",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onViewActivities,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6366F1),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("View Activities")
                        }
                        OutlinedButton(
                            onClick = onSyncComplete,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Done")
                        }
                    }
                    
                } else {
                    // Loading State
                    CircularProgressIndicator(
                        modifier = Modifier.size(72.dp),
                        color = Color(0xFF6366F1),
                        strokeWidth = 6.dp,
                        trackColor = Color(0xFF6366F1).copy(alpha = 0.2f)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Sincronizare în timp real...",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color(0xFF6366F1),
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = currentStep,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = syncProgress.coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        color = Color(0xFF6366F1)
                    )
                    
                    if (currentActivityName != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Activitate curentă: $currentActivityName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6366F1),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    if (fthrEstimate != null && currentStep.contains("FTHR estimat")) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "FTHR estimat: ${fthrEstimate} bpm",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD97706),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Progress info without progress bar (hidden to avoid duplicate info)
                    if (false) Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Status Sincronizare",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            if (activitiesSynced > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Synced Activities",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "$activitiesSynced",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Sincronizare în timp real cu Strava...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StravaSyncLoadingScreenPreview() {
    val context = LocalContext.current
    val stravaViewModel = com.example.fitnessapp.viewmodel.StravaViewModel.getInstance(context)
    val authViewModel = com.example.fitnessapp.viewmodel.AuthViewModel(SharedPreferencesMock())
    FitnessAppTheme {
            StravaSyncLoadingScreen(
                onNavigateBack = {},
                onSyncComplete = {},
                onViewActivities = {},
                stravaViewModel = stravaViewModel,
                authViewModel = authViewModel
            )
    }
}
