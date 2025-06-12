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
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.net.URL
import java.net.HttpURLConnection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StravaSyncLoadingScreen(
    onNavigateBack: () -> Unit,
    onSyncComplete: () -> Unit,
    stravaViewModel: StravaViewModel,
    authViewModel: AuthViewModel
) {
    var syncProgress by remember { mutableFloatStateOf(0f) }
    var currentStep by remember { mutableStateOf("Pregătire sincronizare...") }
    var activitiesSynced by remember { mutableIntStateOf(0) }
    var totalActivities by remember { mutableIntStateOf(0) }
    var isCompleted by remember { mutableStateOf(false) }
    var ftpEstimate by remember { mutableStateOf<Float?>(null) }
    var syncError by remember { mutableStateOf<String?>(null) }
    var currentActivityName by remember { mutableStateOf<String?>(null) }
    
    val stravaActivities by stravaViewModel.stravaActivities.collectAsState()
    val stravaState by stravaViewModel.stravaState.collectAsState()
    
    // Monitor real activities count
    LaunchedEffect(stravaActivities) {
        if (stravaActivities.isNotEmpty()) {
            Log.d("StravaSyncLoading", "Real activities updated: ${stravaActivities.size}")
            totalActivities = stravaActivities.size
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
                    
                    Log.d("StravaSyncLoading", "Making SSE request...")
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
                    val gson = Gson()
                    
                    while (reader.readLine().also { line = it } != null) {
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
                                        activitiesSynced = activitiesCount
                                        totalActivities = activitiesCount
                                    }
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
                                    break
                                }
                                
                                // Check if it's an activity sync event
                                if (eventData.has("name") && eventData.has("start_date")) {
                                    activitiesCount++
                                    val activityName = eventData.get("name").asString
                                    val startDate = eventData.get("start_date").asString
                                    
                                    Log.d("StravaSyncLoading", "Activity synced: $activityName ($startDate)")
                                    
                                    withContext(Dispatchers.Main) {
                                        currentActivityName = activityName
                                        activitiesSynced = activitiesCount
                                        totalActivities = maxOf(totalActivities, activitiesCount)
                                        
                                        // Update progress based on activities synced
                                        if (activitiesCount <= 10) {
                                            syncProgress = 0.3f + (activitiesCount * 0.06f) // 0.3 to 0.9 for first 10 activities
                                        } else {
                                            syncProgress = 0.9f + (0.1f / maxOf(1, activitiesCount - 10)) // 0.9 to 1.0 for remaining
                                        }
                                        
                                        currentStep = "Sincronizare activitate $activitiesCount..."
                                    }
                                    
                                    delay(100) // Small delay to show progress
                                }
                                
                            } catch (e: Exception) {
                                Log.e("StravaSyncLoading", "Error parsing SSE event", e)
                            }
                        }
                    }
                    
                    Log.d("StravaSyncLoading", "SSE stream completed")
                    
                    // Refresh activities using the correct endpoint
                    Log.d("StravaSyncLoading", "Refreshing activities...")
                    withContext(Dispatchers.Main) {
                        stravaViewModel.refreshActivities()
                    }
                    
                    delay(2000)
                    withContext(Dispatchers.Main) {
                        onSyncComplete()
                    }
                    
                } finally {
                    connection.disconnect()
                }
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
            
            Text(
                text = "Strava Sync Live",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            // Placeholder pentru simetrie
            Spacer(modifier = Modifier.width(48.dp))
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
                    
                    Text(
                        text = "✗ Eroare sincronizare",
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
                        text = "$activitiesSynced activități sincronizate",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                    
                    if (ftpEstimate != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "FTP Estimat",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${ftpEstimate!!.toInt()}W",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color(0xFF2563EB),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Bazat pe ${activitiesSynced} activități",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    
                } else {
                    // Loading State
                    CircularProgressIndicator(
                        progress = { syncProgress },
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
                    
                    if (currentActivityName != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Activitate curentă: $currentActivityName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6366F1),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Progress info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Progres",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${(syncProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6366F1)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            LinearProgressIndicator(
                                progress = { syncProgress },
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF6366F1),
                                trackColor = Color(0xFF6366F1).copy(alpha = 0.2f)
                            )
                            
                            if (activitiesSynced > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Activități",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "$activitiesSynced${if (totalActivities > 0) " / $totalActivities" else ""}",
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