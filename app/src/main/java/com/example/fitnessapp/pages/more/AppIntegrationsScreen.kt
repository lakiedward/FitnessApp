package com.example.fitnessapp.pages.more

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.health.connect.client.records.SleepSessionRecord
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "AppIntegrationsScreen"

// Status indicator components
@Composable
private fun StatusBadge(
    isConnected: Boolean,
    text: String,
    modifier: Modifier = Modifier
) {
    val successColor = Color(0xFF10B981)
    val errorColor = MaterialTheme.colorScheme.error

    Row(
        modifier = modifier
            .background(
                color = if (isConnected) successColor.copy(alpha = 0.1f) else errorColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (isConnected) successColor else errorColor,
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isConnected) successColor else errorColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LastSyncIndicator(
    lastSyncTime: String?,
    modifier: Modifier = Modifier
) {
    if (lastSyncTime != null) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check_circle),
                contentDescription = "Last sync",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Last sync: $lastSyncTime",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DataSummaryCard(
    title: String,
    value: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1F2937),
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF)
                )
            }
        }
    }
}

@Composable
private fun LoadingStateCard(
    title: String,
    message: String,
    progress: Float? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (progress != null) {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun IntegrationHeader(
    title: String,
    icon: Int,
    isConnected: Boolean,
    connectionText: String,
    iconTint: androidx.compose.ui.graphics.Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = "$title integration icon - ${if (isConnected) "Connected" else "Disconnected"}",
                modifier = Modifier.size(32.dp),
                tint = iconTint
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }

        // Status badge
        StatusBadge(
            isConnected = isConnected,
            text = connectionText
        )
    }
}

@Composable
private fun ErrorStateCard(
    title: String,
    message: String,
    troubleshootingTips: List<String>? = null,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_help),
                contentDescription = "Error",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (troubleshootingTips != null && troubleshootingTips.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Troubleshooting tips:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                    troubleshootingTips.forEach { tip ->
                        Text(
                            text = "• $tip",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (onRetry != null && onDismiss != null) Arrangement.spacedBy(8.dp) else Arrangement.Center
            ) {
                if (onRetry != null) {
                    Button(
                        onClick = onRetry,
                        modifier = if (onDismiss != null) Modifier.weight(1f) else Modifier,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Retry", color = Color.White)
                    }
                }
                if (onDismiss != null) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = if (onRetry != null) Modifier.weight(1f) else Modifier,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}

private val HC_PERMS = setOf(
    HealthPermission.getReadPermission(SleepSessionRecord::class),
    HealthPermission.getWritePermission(SleepSessionRecord::class),
    HealthPermission.getReadPermission(StepsRecord::class),
    HealthPermission.getWritePermission(StepsRecord::class)
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

    // Dynamic last sync times
    var stravaLastSync by remember { mutableStateOf<String?>(null) }
    var healthConnectLastSync by remember { mutableStateOf<String?>(null) }

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

    // Check initial connection status and fetch last sync times
    LaunchedEffect(Unit) {
        stravaViewModel.checkConnectionStatus()
        val token = authViewModel.getToken()
        if (!token.isNullOrEmpty()) {
            fetchFtpEstimate(token)
        }

        // Fetch Health Connect last sync
        try {
            val lastSyncResponse = healthConnectViewModel.getLastSync(
                apiService = apiService,
                getJwtToken = { authViewModel.getToken() }
            )
            if (lastSyncResponse?.lastSync != null) {
                healthConnectLastSync = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                    .format(java.util.Date.from(java.time.Instant.parse(lastSyncResponse.lastSync)))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Health Connect last sync", e)
        }

        // For Strava, we'll use a placeholder since there's no direct lastSyncDate method
        // In a real implementation, you might want to add this to StravaViewModel
        if (stravaState is StravaState.Connected) {
            stravaLastSync = "Recently synced"
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

    // Force reset isConnecting when connected (additional safety check)
    LaunchedEffect(stravaState) {
        val currentState = stravaState
        if (currentState is StravaState.Connected) {
            Log.d(TAG, "Connected state detected - forcing isConnecting = false")
            isConnecting = false
        }
    }

    // Additional aggressive reset for isConnecting
    LaunchedEffect(stravaState) {
        val currentState = stravaState
        if (currentState is StravaState.Connected && isConnecting) {
            Log.d(TAG, "AGGRESSIVE RESET: Connected state with isConnecting=true, forcing reset")
            isConnecting = false
        }
    }

    // Handle FTP estimate results
    LaunchedEffect(stravaFtpEstimate) {
        if (stravaFtpEstimate != null) {
            ftpEstimate = stravaFtpEstimate!!.estimatedFTP
        }
    }

    fun connectToStrava() {
        Log.d(TAG, "Attempting to connect to Strava")
        isConnecting = true
        scope.launch {
            try {
                Log.d(TAG, "Fetching auth URL from ViewModel")
                val authUrl = stravaViewModel.connect()
                Log.d(TAG, "Opening auth URL: $authUrl")
                openUrl(authUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting OAuth: ${e.message}", e)
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
                    // Header with status
                    IntegrationHeader(
                        title = "Strava Integration",
                        icon = R.drawable.ic_strava,
                        isConnected = stravaState is StravaState.Connected,
                        connectionText = when {
                            stravaState is StravaState.Connected -> "Connected"
                            isConnecting -> "Connecting"
                            else -> "Disconnected"
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    when {
                        stravaState is StravaState.Connected -> {
                            // Connected State with enhanced info
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Connection status with user info
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

                                // Last sync indicator
                                LastSyncIndicator(
                                    lastSyncTime = stravaLastSync,
                                    modifier = Modifier.fillMaxWidth()
                                )

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

                                // Performance Metrics with enhanced cards
                                if (ftpEstimate != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        DataSummaryCard(
                                            title = "FTP",
                                            value = "${ftpEstimate!!.toInt()} W",
                                            subtitle = "Functional Threshold Power",
                                            modifier = Modifier.weight(1f)
                                        )
                                        DataSummaryCard(
                                            title = "Status",
                                            value = "Active",
                                            subtitle = "Data syncing",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                        isConnecting -> {
                            // Enhanced Connecting State
                            LoadingStateCard(
                                title = "Connecting to Strava",
                                message = "Please complete the authorization in your browser and return to the app",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        else -> {
                            // Not Connected State (simplified)
                            if (authState.value is AuthState.Authenticated) {
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
                            } else {
                                Text(
                                    text = "Please log in to your account to connect to Strava.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFEF4444),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                )
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
                    // Header with status
                    IntegrationHeader(
                        title = "Health Connect Integration",
                        icon = R.drawable.ic_health_connect,
                        isConnected = healthConnectState is HealthConnectState.Connected,
                        connectionText = when (healthConnectState) {
                            is HealthConnectState.Connected -> "Connected"
                            is HealthConnectState.Connecting -> "Connecting"
                            is HealthConnectState.Syncing -> "Syncing"
                            is HealthConnectState.PermissionRequired -> "Permissions"
                            else -> "Disconnected"
                        },
                        iconTint = Color(0xFF4CAF50)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    when (healthConnectState) {
                        is HealthConnectState.Connected -> {
                            // Connected State with enhanced info
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Connection status
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

                                // Last sync indicator
                                LastSyncIndicator(
                                    lastSyncTime = healthConnectLastSync,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Data summary cards
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DataSummaryCard(
                                        title = "Sleep Data",
                                        value = "Active",
                                        subtitle = "Syncing enabled",
                                        modifier = Modifier.weight(1f)
                                    )
                                    DataSummaryCard(
                                        title = "Permissions",
                                        value = "Granted",
                                        subtitle = "Read & Write",
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Sync Actions Section
                                SectionHeader("Sync Actions")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { 
                                            scope.launch {
                                                healthConnectViewModel.syncActivitiesToBackend(
                                                    apiService = apiService,
                                                    getJwtToken = { authViewModel.getToken() }
                                                )
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Sync Sleep", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                healthConnectViewModel.syncAllActivitiesToBackend(
                                                    apiService = apiService,
                                                    getJwtToken = { authViewModel.getToken() }
                                                )
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF10B981)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Full Sync", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                    }
                                }

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
                                    Text("Enable Auto Sync for Sleep")
                                }

                                androidx.compose.material3.Divider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )

                                // Data Management Section
                                SectionHeader("Data Management")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
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
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("View Sessions", style = MaterialTheme.typography.bodySmall)
                                    }
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
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("View Stats", style = MaterialTheme.typography.bodySmall)
                                    }
                                }

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

                                androidx.compose.material3.Divider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )

                                // Connection Management Section
                                SectionHeader("Connection")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { healthConnectViewModel.verifyConnection() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF10B981)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Verify", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                    }
                                    OutlinedButton(
                                        onClick = { healthConnectViewModel.disconnect() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Disconnect", style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                androidx.compose.material3.Divider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )

                                // Debug Tools Section
                                SectionHeader("Debug Tools")
                                Button(
                                    onClick = {
                                        scope.launch {
                                            healthConnectViewModel.debugHealthConnectIssues()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("🔍 Debug Health Connect Issues", color = Color.White)
                                }
                            }
                        }

                        is HealthConnectState.Syncing -> {
                            // Enhanced Syncing State
                            LoadingStateCard(
                                title = "Syncing Sleep Data",
                                message = "Uploading your sleep sessions to the backend. This may take a few moments...",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        is HealthConnectState.Connecting -> {
                            // Enhanced Connecting State
                            LoadingStateCard(
                                title = "Connecting to Health Connect",
                                message = "Establishing connection with Health Connect services...",
                                modifier = Modifier.fillMaxWidth()
                            )
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
                                        text = "3. Grant permissions for Sleep",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Text(
                                        text = "4. Return to this app (tap on Recent Apps button and select this app)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF6366F1),
                                        fontWeight = FontWeight.Medium
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

                                // Add a button to manually bring user back
                                OutlinedButton(
                                    onClick = {
                                        // Try to bring the app to foreground
                                        healthConnectViewModel.verifyConnection()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "I granted permissions - Check connection",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Text(
                                    text = "We're checking automatically every few seconds...",
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
                            // Enhanced Error State
                            ErrorStateCard(
                                title = "Health Connect Connection Failed",
                                message = (healthConnectState as HealthConnectState.Error).message,
                                troubleshootingTips = listOf(
                                    "Make sure Health Connect is installed and updated",
                                    "Check if you have granted the necessary permissions",
                                    "Try restarting the Health Connect app",
                                    "Ensure your device supports Health Connect"
                                ),
                                onRetry = {
                                    permissionLauncher.launch(HC_PERMS)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        else -> {
                            // Not Connected State
                            Button(
                                onClick = {
                                    when (HealthConnectClient.getSdkStatus(context)) {
                                        HealthConnectClient.SDK_AVAILABLE -> {
                                            // Use the improved connection flow that opens Health Connect directly
                                            healthConnectViewModel.initiateConnection(permissionLauncher)
                                        }

                                        HealthConnectClient.SDK_UNAVAILABLE -> {
                                            // HC is not installed
                                            errorMessage =
                                                "Health Connect is not installed on this device."
                                        }

                                        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                                            // HC needs an update - open Play Store
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

            // Enhanced Error Message
            if (errorMessage != null) {
                ErrorStateCard(
                    title = "Connection Error",
                    message = errorMessage!!,
                    troubleshootingTips = listOf(
                        "Check your internet connection",
                        "Make sure the service is available",
                        "Try again in a few moments"
                    ),
                    onRetry = {
                        errorMessage = null
                        // Retry the last failed operation
                        if (stravaState !is StravaState.Connected) {
                            connectToStrava()
                        }
                    },
                    onDismiss = {
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
