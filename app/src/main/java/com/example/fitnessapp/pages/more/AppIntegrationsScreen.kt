package com.example.fitnessapp.pages.more

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import com.example.fitnessapp.R
import com.example.fitnessapp.api.ApiService
import com.example.fitnessapp.viewmodel.AuthState
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.viewmodel.HealthConnectState
import com.example.fitnessapp.viewmodel.HealthConnectViewModel
import com.example.fitnessapp.viewmodel.StravaState
import com.example.fitnessapp.viewmodel.StravaViewModel
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "AppIntegrationsScreen"

private val HC_PERMS = setOf(
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getWritePermission(StepsRecord::class),
    HealthPermission.getReadPermission(DistanceRecord::class),
    HealthPermission.getWritePermission(DistanceRecord::class),
    HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    HealthPermission.getWritePermission(ExerciseSessionRecord::class),
    HealthPermission.getReadPermission(HeartRateRecord::class),
    HealthPermission.getWritePermission(HeartRateRecord::class),
    HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppIntegrationsScreen(
    authViewModel: AuthViewModel,
    stravaViewModel: StravaViewModel,
    healthConnectViewModel: HealthConnectViewModel,
    apiService: ApiService,
    onNavigateBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val stravaState by stravaViewModel.stravaState.collectAsState()
    val stravaFtpEstimate by stravaViewModel.ftpEstimate.collectAsState()
    val stravaAthlete by stravaViewModel.stravaAthlete.collectAsState()
    val healthConnectState by healthConnectViewModel.healthConnectState.collectAsState()
    val authState = authViewModel.authState.observeAsState()
    var isConnecting by remember { mutableStateOf(false) }
    var ftpEstimate by remember { mutableStateOf<Float?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        Log.d(TAG, "Permission launcher result: granted permissions = $granted")
        if (granted.containsAll(HC_PERMS)) {
            Log.d(TAG, "All required permissions granted")
            healthConnectViewModel.onPermissionsGranted()
        } else {
            Log.d(TAG, "Some permissions were denied")
            healthConnectViewModel.onPermissionsDenied()
        }
    }

    // Function to fetch FTP estimate
    fun fetchFtpEstimate(token: String) {
        scope.launch {
            try {
                val ftpUrl = "${com.example.fitnessapp.api.ApiConfig.BASE_URL}strava/estimate-ftp"
                Log.d(TAG, "FTP estimate URL: $ftpUrl")

                val response = withContext(Dispatchers.IO) {
                    val url = URL(ftpUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Authorization", "Bearer $token")
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.connectTimeout = 60000
                    connection.readTimeout = 60000

                    if (connection.responseCode == 200) {
                        val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                        val ftpData = Gson().fromJson(responseBody, JsonObject::class.java)
                        ftpData.get("estimated_ftp")?.asFloat
                    } else {
                        null
                    }
                }

                if (response != null) {
                    ftpEstimate = response
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching FTP estimate", e)
            }
        }
    }

    // Function to open URLs
    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open URL", e)
            errorMessage = "Failed to open browser: ${e.message}"
        }
    }

    // Check initial connection status
    LaunchedEffect(Unit) {
        stravaViewModel.checkConnectionStatus()
        val token = authViewModel.getToken()
        if (!token.isNullOrEmpty()) {
            fetchFtpEstimate(token)
        }
    }

    // Handle Strava authentication
    LaunchedEffect(stravaState) {
        when (val currentState = stravaState) {
            is StravaState.Connected -> {
                isConnecting = false
                errorMessage = null
            }
            is StravaState.Error -> {
                isConnecting = false
                if (!currentState.message.contains("Rate Limit") &&
                    !currentState.message.contains("Job was cancelled") &&
                    !currentState.message.contains("Strava account not connected yet")
                ) {
                    errorMessage = currentState.message
                }
            }
            is StravaState.Connecting -> {
                isConnecting = true
                errorMessage = null
            }
            else -> {
                isConnecting = false
                errorMessage = null
            }
        }
    }

    // Handle FTP estimate results
    LaunchedEffect(stravaFtpEstimate) {
        if (stravaFtpEstimate != null) {
            ftpEstimate = stravaFtpEstimate!!.estimatedFTP
        }
    }

    fun connectToStrava() {
        isConnecting = true
        scope.launch {
            try {
                val authUrl = stravaViewModel.connect()
                openUrl(authUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting OAuth", e)
                isConnecting = false
                errorMessage = "Failed to start OAuth: ${e.message}"
            }
        }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF6366F1)),
            shape = RoundedCornerShape(0.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                androidx.compose.material3.IconButton(
                    onClick = { onNavigateBack() }
                ) {
                    androidx.compose.material3.Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Center content (logo and title)
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.iclogo),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "App Integrations",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Empty space for balance
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Authentication Banner (simplified)
            if (authState.value is AuthState.Authenticated) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFF10B981).copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check_circle),
                        contentDescription = "Authenticated",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "You are authenticated",
                        color = Color(0xFF10B981),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Strava Integration Section (simplified)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically
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
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when {
                        stravaState is StravaState.Connected -> {
                            // Connected State (simplified)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_check_circle),
                                        contentDescription = "Connected",
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Connected${if (stravaAthlete?.firstName != null) " as ${stravaAthlete?.firstName}" else ""}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF6366F1),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Action Buttons (simplified)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { stravaViewModel.refreshStravaToken() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Refresh", style = MaterialTheme.typography.bodySmall)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            stravaViewModel.disconnect()
                                            isConnecting = false
                                            errorMessage = null
                                            ftpEstimate = null
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Disconnect", style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                // Performance Metrics (simplified)
                                if (ftpEstimate != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Color(0xFFF8FAFC),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "FTP",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF1F2937)
                                        )
                                        Text(
                                            text = "${ftpEstimate!!.toInt()} W",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color(0xFF6366F1),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        isConnecting -> {
                            // Connecting State (simplified)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = Color(0xFF6366F1),
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = "Connecting to Strava...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF6366F1)
                                )
                            }
                        }
                        else -> {
                            // Not Connected State (simplified)
                            Button(
                                onClick = { connectToStrava() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6366F1)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_strava),
                                    contentDescription = "Strava Logo",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connect to Strava")
                            }
                        }
                    }
                }
            }

            // Health Connect Integration Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_health_connect),
                            contentDescription = "Health Connect",
                            modifier = Modifier.size(32.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Health Connect Integration",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (healthConnectState) {
                        is HealthConnectState.Connected -> {
                            // Connected State
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_check_circle),
                                        contentDescription = "Connected",
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Connected to Health Connect",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF6366F1),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Buton pentru sincronizare manuală
                                Button(
                                    onClick = { 
                                        scope.launch {
                                            healthConnectViewModel.syncActivitiesToBackend(
                                                apiService = apiService,
                                                getJwtToken = { authViewModel.getToken() }
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF6366F1)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Sync Activities to Backend", color = Color.White)
                                }

                                // Buton pentru sincronizare automată
                                OutlinedButton(
                                    onClick = { 
                                        scope.launch {
                                            healthConnectViewModel.setupAutoSync(
                                                apiService = apiService,
                                                getJwtToken = { authViewModel.getToken() }
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Enable Auto Sync")
                                }

                                Button(
                                    onClick = { healthConnectViewModel.verifyConnection() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Verify Connection", color = Color.White)
                                }

                                // Buton pentru a vedea activitățile
                                OutlinedButton(
                                    onClick = { 
                                        scope.launch {
                                            val activities = healthConnectViewModel.getHealthConnectActivities(
                                                apiService = apiService,
                                                getJwtToken = { authViewModel.getToken() },
                                                page = 1,
                                                perPage = 10
                                            )
                                            Log.d("HealthConnect", "Activities: $activities")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("View Activities")
                                }

                                // Buton pentru a vedea statisticile
                                OutlinedButton(
                                    onClick = { 
                                        scope.launch {
                                            val stats = healthConnectViewModel.getHealthConnectStats(
                                                apiService = apiService,
                                                getJwtToken = { authViewModel.getToken() }
                                            )
                                            Log.d("HealthConnect", "Stats: $stats")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("View Stats")
                                }

                                // Buton pentru a vedea ultima sincronizare
                                OutlinedButton(
                                    onClick = { 
                                        scope.launch {
                                            val lastSync = healthConnectViewModel.getLastSync(
                                                apiService = apiService,
                                                getJwtToken = { authViewModel.getToken() }
                                            )
                                            Log.d("HealthConnect", "Last Sync: $lastSync")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Check Last Sync")
                                }

                                OutlinedButton(
                                    onClick = { healthConnectViewModel.disconnect() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Disconnect")
                                }
                            }
                        }

                        is HealthConnectState.Syncing -> {
                            // Syncing State
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = Color(0xFF6366F1),
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = "Syncing activities to backend...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF6366F1)
                                )
                            }
                        }

                        is HealthConnectState.Connecting -> {
                            // Connecting State
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = Color(0xFF6366F1),
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = "Connecting to Health Connect...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF6366F1)
                                )
                            }
                        }

                        is HealthConnectState.PermissionRequired -> {
                            // Permission Required State
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = Color(0xFFFF9500),
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = "Waiting for Health Connect permissions...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFFF9500),
                                    fontWeight = FontWeight.Bold
                                )

                                // Step-by-step instructions
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "To complete the connection:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Text(
                                        text = "1. Health Connect should have opened automatically",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Text(
                                        text = "2. Look for our app in the permissions list",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Text(
                                        text = "3. Grant permissions for Steps, Distance, Calories, Exercise, and Heart Rate",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Text(
                                        text = "4. Return to this app",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Action buttons
                                Button(
                                    onClick = { healthConnectViewModel.openHealthConnectSettings() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFF9500)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "Open Health Connect",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Text(
                                    text = "We're also checking automatically every few seconds...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Light
                                )
                            }
                        }

                        is HealthConnectState.NotSupported -> {
                            // Not Supported State
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Health Connect is not supported on this device",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }

                        is HealthConnectState.Error -> {
                            // Error State
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = (healthConnectState as HealthConnectState.Error).message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFDC2626)
                                )
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(HC_PERMS)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF6366F1)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Retry Connection")
                                }
                            }
                        }

                        else -> {
                            // Not Connected State
                            Button(
                                onClick = {
                                    when (HealthConnectClient.getSdkStatus(context)) {
                                        HealthConnectClient.SDK_AVAILABLE -> {
                                            healthConnectViewModel.connect()
                                            permissionLauncher.launch(HC_PERMS)
                                        }

                                        HealthConnectClient.SDK_UNAVAILABLE -> {
                                            // HC is not installed
                                            errorMessage =
                                                "Health Connect is not installed on this device."
                                        }

                                        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                                            // HC needs to be updated - open Play Store
                                            errorMessage = "Health Connect requires an update."
                                            val playIntent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("market://details?id=com.google.android.apps.healthdata")
                                            )
                                            context.startActivity(playIntent)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6366F1)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Connect to Health Connect")
                            }
                        }
                    }
                }
            }

            // Error Message (simplified)
            if (errorMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color(0xFFF8FAFC),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }
    }
}
