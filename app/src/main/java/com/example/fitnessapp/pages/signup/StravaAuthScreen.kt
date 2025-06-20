package com.example.fitnessapp.pages.signup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.fitnessapp.R
import com.example.fitnessapp.model.UserDetalis
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.viewmodel.StravaViewModel
import com.example.fitnessapp.viewmodel.StravaViewModelFactory
import com.example.fitnessapp.viewmodel.HomeViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.net.HttpURLConnection
import java.net.URL
import com.google.gson.Gson
import com.google.gson.JsonObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StravaAuthScreen(
    onContinue: () -> Unit = {},
    authViewModel: AuthViewModel,
    stravaViewModel: StravaViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = viewModel()
    
    var isConnecting by remember { 
        Log.d("StravaAuthScreen", "Creating isConnecting state")
        mutableStateOf(false) 
    }
    var ftpEstimate by remember { 
        Log.d("StravaAuthScreen", "Creating ftpEstimate state")
        mutableStateOf<Float?>(null) 
    }
    var fthrEstimate by remember { 
        Log.d("StravaAuthScreen", "Creating fthrEstimate state")
        mutableStateOf<Int?>(null) 
    }
    var errorMessage by remember { 
        Log.d("StravaAuthScreen", "Creating errorMessage state")
        mutableStateOf<String?>(null) 
    }
    var isSyncingActivities by remember { 
        Log.d("StravaAuthScreen", "Creating isSyncingActivities state")
        mutableStateOf(false) 
    }
    var syncMessage by remember { 
        Log.d("StravaAuthScreen", "Creating syncMessage state")
        mutableStateOf<String?>(null) 
    }
    var activitiesCount by remember { 
        Log.d("StravaAuthScreen", "Creating activitiesCount state")
        mutableStateOf<Int?>(null) 
    }
    
    val stravaState by stravaViewModel.stravaState.collectAsState()
    val ftpEstimateData by homeViewModel.ftpEstimate.observeAsState()
    val isLoading by homeViewModel.isLoading.observeAsState()
    val error by homeViewModel.error.observeAsState()
    val stravaActivities by stravaViewModel.stravaActivities.collectAsState()
    val stravaFtpEstimate by stravaViewModel.ftpEstimate.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Function to fetch FTHR estimate - defined early for scope access
    fun fetchFthrEstimate(token: String) {
        coroutineScope.launch {
            try {
                val fthrUrl = "${com.example.fitnessapp.api.ApiConfig.BASE_URL}strava/estimate-cycling-fthr"
                Log.d("StravaAuthScreen", "FTHR estimate URL: $fthrUrl")
                
                val response = withContext(Dispatchers.IO) {
                    val url = URL(fthrUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Authorization", "Bearer $token")
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.connectTimeout = 60000
                    connection.readTimeout = 60000
                    
                    Log.d("StravaAuthScreen", "Making FTHR estimate request...")
                    Log.d("StravaAuthScreen", "FTHR response code: ${connection.responseCode}")
                    
                    if (connection.responseCode == 200) {
                        val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                        Log.d("StravaAuthScreen", "FTHR estimate response: $responseBody")
                        
                        val fthrData = Gson().fromJson(responseBody, JsonObject::class.java)
                        val estimatedFthr = fthrData.get("estimated_fthr")?.asInt
                        
                        Log.d("StravaAuthScreen", "FTHR estimate parsed: ${estimatedFthr} bpm")
                        estimatedFthr
                    } else {
                        val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e("StravaAuthScreen", "FTHR estimate failed: ${connection.responseCode} - $errorBody")
                        null
                    }
                }
                
                if (response != null) {
                    fthrEstimate = response
                    Log.d("StravaAuthScreen", "FTHR estimate set to: ${fthrEstimate} bpm")
                }
            } catch (e: Exception) {
                Log.e("StravaAuthScreen", "Error fetching FTHR estimate", e)
            }
        }
    }

    // Local function to open URLs - must be defined before other functions
    fun openUrl(url: String) {
        Log.d("StravaAuthScreen", "=== openUrl() CALLED ===")
        Log.d("StravaAuthScreen", "Opening URL: $url")
        Log.d("StravaAuthScreen", "Context: $context")
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            Log.d("StravaAuthScreen", "Created intent: $intent")
            context.startActivity(intent)
            Log.d("StravaAuthScreen", "URL opened successfully in browser")
        } catch (e: Exception) {
            Log.e("StravaAuthScreen", "Failed to open URL", e)
            errorMessage = "Failed to open browser: ${e.message}"
        }
    }

    // Debug logging for state changes
    LaunchedEffect(stravaState) {
        Log.d("StravaAuthScreen", "=== STATE CHANGE DEBUG ===")
        Log.d("StravaAuthScreen", "New stravaState: $stravaState")
        Log.d("StravaAuthScreen", "Current isConnecting: $isConnecting")
        Log.d("StravaAuthScreen", "ViewModel instance: ${stravaViewModel.hashCode()}")
    }

    // Check initial connection status when screen loads
    LaunchedEffect(Unit) {
        Log.d("StravaAuthScreen", "Screen loaded, checking initial connection status")
        Log.d("StravaAuthScreen", "ViewModel instance: ${stravaViewModel.hashCode()}")
        stravaViewModel.checkConnectionStatus()
        
        // Also fetch FTHR estimate if we have activities and token
        val token = authViewModel.getToken()
        if (!token.isNullOrEmpty() && stravaActivities.isNotEmpty()) {
            Log.d("StravaAuthScreen", "Fetching FTHR estimate on screen load")
            fetchFthrEstimate(token)
        }
    }

    // Handle Strava authentication
    LaunchedEffect(stravaState) {
        Log.d("StravaAuthScreen", "=== LaunchedEffect(stravaState) TRIGGERED ===")
        val currentState = stravaState
        Log.d("StravaAuthScreen", "=== HANDLING STRAVA STATE ===")
        Log.d("StravaAuthScreen", "Strava state changed: $currentState")
        Log.d("StravaAuthScreen", "Previous isConnecting: $isConnecting")
        Log.d("StravaAuthScreen", "Current isConnecting: $isConnecting")
        Log.d("StravaAuthScreen", "State type: ${currentState::class.simpleName}")
        Log.d("StravaAuthScreen", "State hashCode: ${currentState.hashCode()}")
        
        when (currentState) {
            is com.example.fitnessapp.viewmodel.StravaState.Connected -> {
                Log.d("StravaAuthScreen", "=== CONNECTED STATE DETECTED ===")
                Log.d("StravaAuthScreen", "Strava connected successfully")
                Log.d("StravaAuthScreen", "Setting isConnecting = false")
                Log.d("StravaAuthScreen", "isConnecting before: $isConnecting")
                isConnecting = false
                Log.d("StravaAuthScreen", "isConnecting after: $isConnecting")
                errorMessage = null
                syncMessage = "Connected! You can now sync your activities."
                Log.d("StravaAuthScreen", "After setting isConnecting = false: $isConnecting")
                
                // After successful connection, fetch FTP estimate
                val token = authViewModel.getToken()
                if (!token.isNullOrEmpty()) {
                    Log.d("StravaAuthScreen", "Fetching FTP estimate after successful connection")
                    homeViewModel.fetchFTPEstimate(token)
                }
                
                // Refresh activities count
                stravaViewModel.refreshActivities()
            }
            is com.example.fitnessapp.viewmodel.StravaState.Error -> {
                Log.d("StravaAuthScreen", "=== ERROR STATE DETECTED ===")
                Log.e("StravaAuthScreen", "Strava connection error: ${currentState.message}")
                Log.d("StravaAuthScreen", "Setting isConnecting = false")
                Log.d("StravaAuthScreen", "isConnecting before: $isConnecting")
                isConnecting = false
                Log.d("StravaAuthScreen", "isConnecting after: $isConnecting")
                isSyncingActivities = false
                // Don't show rate limiting errors or temporary connection issues to user
                if (!currentState.message.contains("Rate Limit") && 
                    !currentState.message.contains("Job was cancelled") &&
                    !currentState.message.contains("Strava account not connected yet")) {
                    errorMessage = currentState.message
                    syncMessage = null
                } else {
                    Log.d("StravaAuthScreen", "Suppressing temporary error: ${currentState.message}")
                    errorMessage = null
                }
            }
            is com.example.fitnessapp.viewmodel.StravaState.Connecting -> {
                Log.d("StravaAuthScreen", "=== CONNECTING STATE DETECTED ===")
                Log.d("StravaAuthScreen", "Strava connecting...")
                Log.d("StravaAuthScreen", "Setting isConnecting = true")
                Log.d("StravaAuthScreen", "isConnecting before: $isConnecting")
                isConnecting = true
                Log.d("StravaAuthScreen", "isConnecting after: $isConnecting")
                errorMessage = null
                syncMessage = null
            }
            else -> {
                Log.d("StravaAuthScreen", "=== OTHER STATE DETECTED ===")
                Log.d("StravaAuthScreen", "Strava state: $currentState")
                Log.d("StravaAuthScreen", "Setting isConnecting = false")
                Log.d("StravaAuthScreen", "isConnecting before: $isConnecting")
                isConnecting = false
                Log.d("StravaAuthScreen", "isConnecting after: $isConnecting")
                errorMessage = null
                syncMessage = null
            }
        }
        Log.d("StravaAuthScreen", "Final isConnecting: $isConnecting")
        Log.d("StravaAuthScreen", "=== LaunchedEffect(stravaState) COMPLETED ===")
    }
    
    // Force reset isConnecting when Connected state is detected
    LaunchedEffect(stravaState) {
        if (stravaState is com.example.fitnessapp.viewmodel.StravaState.Connected) {
            Log.d("StravaAuthScreen", "Connected state detected - forcing isConnecting = false")
            isConnecting = false
        }
    }

    // Additional aggressive reset for isConnecting
    LaunchedEffect(stravaState) {
        if (stravaState is com.example.fitnessapp.viewmodel.StravaState.Connected && isConnecting) {
            Log.d("StravaAuthScreen", "AGGRESSIVE RESET: Connected state with isConnecting=true, forcing reset")
            isConnecting = false
        }
    }

    // Handle activities count when activities are fetched
    LaunchedEffect(stravaActivities) {
        if (stravaActivities.isNotEmpty()) {
            activitiesCount = stravaActivities.size
            syncMessage = "✓ Activities loaded successfully"
            
            // Fetch FTP estimate after activities are loaded
            val token = authViewModel.getToken()
            if (!token.isNullOrEmpty()) {
                Log.d("StravaAuthScreen", "Fetching FTP estimate after activities loaded")
                stravaViewModel.fetchFtpEstimateAfterSync()
                
                // Also fetch FTHR estimate
                Log.d("StravaAuthScreen", "Fetching FTHR estimate after activities loaded")
                fetchFthrEstimate(token)
            }
        }
    }

    // Handle FTP estimate results from StravaViewModel
    LaunchedEffect(stravaFtpEstimate) {
        if (stravaFtpEstimate != null) {
            ftpEstimate = stravaFtpEstimate!!.estimatedFTP
            Log.d("StravaAuthScreen", "FTP estimate received from StravaViewModel: ${ftpEstimate}W")
        }
    }

    // Refresh FTHR estimate when activities are refreshed (after sync)
    LaunchedEffect(stravaActivities) {
        if (stravaActivities.isNotEmpty()) {
            val token = authViewModel.getToken()
            if (!token.isNullOrEmpty()) {
                Log.d("StravaAuthScreen", "Activities refreshed, fetching FTHR estimate")
                fetchFthrEstimate(token)
            }
        }
    }

    // Handle FTP estimate results from HomeViewModel (fallback)
    LaunchedEffect(ftpEstimateData, error) {
        if (ftpEstimateData != null) {
            ftpEstimate = ftpEstimateData!!.estimatedFTP
            Log.d("StravaAuthScreen", "FTP estimate received from HomeViewModel: ${ftpEstimate}W")
        }
        if (error != null) {
            errorMessage = error
        }
    }

    // Monitor state changes for debugging
    LaunchedEffect(stravaState, isConnecting) {
        Log.d("StravaAuthScreen", "=== STATE MONITORING ===")
        Log.d("StravaAuthScreen", "stravaState changed to: $stravaState")
        Log.d("StravaAuthScreen", "isConnecting changed to: $isConnecting")
        Log.d("StravaAuthScreen", "Timestamp: ${System.currentTimeMillis()}")
    }

    fun connectToStrava() {
        Log.d("StravaAuthScreen", "=== connectToStrava() FUNCTION CALLED ===")
        Log.d("StravaAuthScreen", "Current isConnecting: $isConnecting")
        Log.d("StravaAuthScreen", "Current stravaState: $stravaState")
        Log.d("StravaAuthScreen", "Starting OAuth flow from function...")
        isConnecting = true
        Log.d("StravaAuthScreen", "isConnecting set to: $isConnecting")
        Log.d("StravaAuthScreen", "Calling stravaViewModel.connect() from function")
        coroutineScope.launch {
            try {
                val authUrl = stravaViewModel.connect()
                Log.d("StravaAuthScreen", "OAuth URL received from function: $authUrl")
                Log.d("StravaAuthScreen", "Opening OAuth URL in browser from function")
                openUrl(authUrl)
            } catch (e: Exception) {
                Log.e("StravaAuthScreen", "Error starting OAuth from function", e)
                isConnecting = false
                Log.d("StravaAuthScreen", "Error occurred in function, isConnecting reset to: $isConnecting")
                errorMessage = "Failed to start OAuth: ${e.message}"
            }
        }
    }

    fun syncActivities() {
        Log.d("StravaAuthScreen", "Navigating to sync loading screen...")
        navController.navigate("strava_sync_loading")
    }

    // Debug logging for recomposition
    Log.d("StravaAuthScreen", "=== RECOMPOSITION ===")
    Log.d("StravaAuthScreen", "stravaState: $stravaState")
    Log.d("StravaAuthScreen", "isConnecting: $isConnecting")
    Log.d("StravaAuthScreen", "isSyncingActivities: $isSyncingActivities")
    Log.d("StravaAuthScreen", "activitiesCount: $activitiesCount")
    Log.d("StravaAuthScreen", "errorMessage: $errorMessage")
    Log.d("StravaAuthScreen", "syncMessage: $syncMessage")

    // Get current Strava state for UI decisions
    val currentStravaState = stravaState

    // UI
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
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Setup Complete",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
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
                modifier = Modifier.fillMaxSize()
            ) {
                // Scrollable content area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Strava Logo and Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_strava),
                            contentDescription = "Strava",
                            modifier = Modifier.size(32.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Strava Integration",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Log.d("StravaAuthScreen", "=== UI RENDERING DECISION ===")
                    Log.d("StravaAuthScreen", "currentStravaState: $currentStravaState")
                    Log.d("StravaAuthScreen", "isConnecting: $isConnecting")
                    Log.d("StravaAuthScreen", "currentStravaState is Connected: ${currentStravaState is com.example.fitnessapp.viewmodel.StravaState.Connected}")
                    when {
                        currentStravaState is com.example.fitnessapp.viewmodel.StravaState.Connected -> {
                            Log.d("StravaAuthScreen", "=== RENDERING CONNECTED STATE ===")
                            Log.d("StravaAuthScreen", "isConnecting: $isConnecting")
                            Log.d("StravaAuthScreen", "stravaState: $currentStravaState")
                            // Connected State
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check_circle),
                                    contentDescription = "Connected",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(48.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "✓ Connected to Strava",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Activity Sync Controls
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Activity Sync",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        if (syncMessage != null) {
                                            Text(
                                                text = syncMessage!!,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (syncMessage!!.startsWith("✓")) Color(0xFF10B981) else Color.Gray
                                            )
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                        
                                        Button(
                                            onClick = {
                                                Log.d("StravaAuthScreen", "Sync button clicked")
                                                Log.d("StravaAuthScreen", "Current stravaState: $currentStravaState")
                                                Log.d("StravaAuthScreen", "isConnecting: $isConnecting")
                                                isSyncingActivities = true
                                                syncMessage = "Pornire sincronizare..."
                                                
                                                coroutineScope.launch {
                                                    try {
                                                        Log.d("StravaAuthScreen", "Starting sync process...")
                                                        // Navigate to sync loading screen
                                                        Log.d("StravaAuthScreen", "Navigating to sync loading screen")
                                                        navController.navigate("strava_sync_loading")
                                                    } catch (e: Exception) {
                                                        Log.e("StravaAuthScreen", "Error during sync", e)
                                                        syncMessage = "Eroare: ${e.message}"
                                                    } finally {
                                                        isSyncingActivities = false
                                                    }
                                                }
                                            },
                                            enabled = !isSyncingActivities,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF6366F1),
                                                contentColor = Color.White
                                            )
                                        ) {
                                            if (isSyncingActivities) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    color = Color.White,
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                            Text(if (isSyncingActivities) "Sincronizare..." else "Sincronizează activități")
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        OutlinedButton(
                                            onClick = { 
                                                stravaViewModel.forceClearAllData()
                                                isConnecting = false
                                                errorMessage = null
                                                syncMessage = null
                                                activitiesCount = null
                                                ftpEstimate = null
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                                        ) {
                                            Text("Disconnect & Clear Cache")
                                        }
                                    }
                                }
                            }
                        }
                        
                        isConnecting -> {
                            Log.d("StravaAuthScreen", "=== RENDERING CONNECTING STATE ===")
                            Log.d("StravaAuthScreen", "isConnecting: $isConnecting")
                            Log.d("StravaAuthScreen", "stravaState: $currentStravaState")
                            // Connecting State
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = Color(0xFF6366F1),
                                    strokeWidth = 4.dp
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "Connecting to Strava...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFF6366F1),
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Text(
                                    text = "Please complete authorization in browser",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        
                        else -> {
                            // Not Connected State
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_strava),
                                    contentDescription = "Strava",
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.Unspecified
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "Connect to Strava",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Text(
                                    text = "Sync your activities and get detailed performance insights",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Button(
                                    onClick = {
                                        Log.d("StravaAuthScreen", "=== CONNECT BUTTON CLICKED ===")
                                        Log.d("StravaAuthScreen", "Current isConnecting: $isConnecting")
                                        Log.d("StravaAuthScreen", "Current stravaState: $stravaState")
                                        Log.d("StravaAuthScreen", "Starting OAuth flow...")
                                        isConnecting = true
                                        Log.d("StravaAuthScreen", "isConnecting set to: $isConnecting")
                                        Log.d("StravaAuthScreen", "Calling stravaViewModel.connect()")
                                        coroutineScope.launch {
                                            try {
                                                val authUrl = stravaViewModel.connect()
                                                Log.d("StravaAuthScreen", "OAuth URL received: $authUrl")
                                                Log.d("StravaAuthScreen", "Opening OAuth URL in browser")
                                                openUrl(authUrl)
                                            } catch (e: Exception) {
                                                Log.e("StravaAuthScreen", "Error starting OAuth", e)
                                                isConnecting = false
                                                Log.d("StravaAuthScreen", "Error occurred, isConnecting reset to: $isConnecting")
                                                errorMessage = "Failed to start OAuth: ${e.message}"
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC4C02))
                                ) {
                                    Text("Connect to Strava")
                                }
                            }
                        }
                    }
                    
                    // Error Message
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
                        ) {
                            Text(
                                text = errorMessage!!,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                    
                    // FTP and FTHR Estimates Display
                    if (ftpEstimate != null || stravaFtpEstimate != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Performance Metrics Section
                        Text(
                            text = "Performance Metrics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // FTP Card
                        if (ftpEstimate != null || stravaFtpEstimate != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // FTP Icon/Label
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "FTP (Functional Threshold Power)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Black
                                        )
                                        Text(
                                            text = "Power threshold for training",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                    
                                    // FTP Value
                                    Text(
                                        text = "${(ftpEstimate ?: stravaFtpEstimate?.estimatedFTP)?.toInt() ?: "N/A"}W",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = Color(0xFF2563EB),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // FTHR Card - show actual value when available
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // FTHR Icon/Label
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "FTHR (Functional Threshold Heart Rate)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "Heart rate threshold for training",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                
                                // FTHR Value - show actual value or "Sync activities"
                                Text(
                                    text = if (fthrEstimate != null) "${fthrEstimate} bpm" else "Sync activities",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (fthrEstimate != null) Color(0xFFD97706) else Color(0xFFD97706),
                                    fontWeight = if (fthrEstimate != null) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                
                // Add some bottom padding for the scrollable content
                Spacer(modifier = Modifier.height(16.dp))
                
                // Fixed bottom area with Continue button
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Continue Button
                    if (currentStravaState is com.example.fitnessapp.viewmodel.StravaState.Connected) {
                        Button(
                            onClick = onContinue,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("Continue")
                        }
                    } else {
                        OutlinedButton(
                            onClick = onContinue,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Continue Without Strava")
                        }
                    }
                }
            }
        }
    }
}
