package com.example.fitnessapp.pages.signup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.example.fitnessapp.ui.theme.extendedColors
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import com.example.fitnessapp.R
import com.example.fitnessapp.model.UserDetalis
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.viewmodel.StravaViewModel
 
import com.example.fitnessapp.viewmodel.HomeViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.util.Log
 
 
import com.example.fitnessapp.mock.SharedPreferencesMock

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
    var isSyncing by remember { 
        Log.d("StravaAuthScreen", "Creating isSyncingActivities state")
        mutableStateOf(false) 
    }
    var syncMessage by remember { 
        Log.d("StravaAuthScreen", "Creating syncMessage state")
        mutableStateOf<String?>(null) 
    }
    
    val stravaState by stravaViewModel.stravaState.collectAsState()
    val ftpEstimateData by homeViewModel.ftpEstimate.observeAsState()
    val isLoading by homeViewModel.isLoading.observeAsState()
    val error by homeViewModel.error.observeAsState()
    val stravaFtpEstimate by stravaViewModel.ftpEstimate.collectAsState()
    val stravaFthrEstimate by stravaViewModel.fthrEstimate.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // FTHR estimation handled by StravaViewModel

    // Function to open URLs
    fun openUrl(url: String) {
        Log.d("StravaAuthScreen", "Opening URL: $url")
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
            Log.d("StravaAuthScreen", "URL opened successfully in browser")
        } catch (e: Exception) {
            Log.e("StravaAuthScreen", "Failed to open URL", e)
            errorMessage = "Failed to open browser: ${e.message}"
        }
    }

    // Debug logging for state changes
    LaunchedEffect(stravaState) {
        Log.d("StravaAuthScreen", "New stravaState: $stravaState")
        Log.d("StravaAuthScreen", "Current isConnecting: $isConnecting")
    }

    // Check initial connection status
    LaunchedEffect(Unit) {
        Log.d("StravaAuthScreen", "Screen loaded, checking initial connection status")
        stravaViewModel.checkConnectionStatus()
        val token = authViewModel.getToken()
        if (!token.isNullOrEmpty()) {
            Log.d("StravaAuthScreen", "Fetching FTHR estimate on screen load via ViewModel")
            stravaViewModel.estimateCyclingFthr()
        }
    }

    // Handle Strava authentication
    LaunchedEffect(stravaState) {
        Log.d("StravaAuthScreen", "Strava state changed: $stravaState")
        val currentState = stravaState
        when (currentState) {
            is com.example.fitnessapp.viewmodel.StravaState.Connected -> {
                Log.d("StravaAuthScreen", "Strava connected successfully")
                isConnecting = false
                errorMessage = null
                syncMessage = "Connected! You can now sync your activities."
            }
            is com.example.fitnessapp.viewmodel.StravaState.Error -> {
                Log.e("StravaAuthScreen", "Strava connection error: ${currentState.message}")
                isConnecting = false
                isSyncing = false
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
                Log.d("StravaAuthScreen", "Strava connecting...")
                isConnecting = true
                errorMessage = null
                syncMessage = null
            }
            else -> {
                Log.d("StravaAuthScreen", "Strava state: $currentState")
                isConnecting = false
                errorMessage = null
                syncMessage = null
            }
        }
    }
    
    // Force reset isConnecting
    LaunchedEffect(stravaState) {
        val currentState = stravaState
        if (currentState is com.example.fitnessapp.viewmodel.StravaState.Connected) {
            Log.d("StravaAuthScreen", "Connected state detected - forcing isConnecting = false")
            isConnecting = false
        }
    }

    // Additional reset for isConnecting
    LaunchedEffect(stravaState) {
        val currentState = stravaState
        if (currentState is com.example.fitnessapp.viewmodel.StravaState.Connected && isConnecting) {
            Log.d("StravaAuthScreen", "AGGRESSIVE RESET: Connected state with isConnecting=true, forcing reset")
            isConnecting = false
        }
    }

    // Handle FTP estimate results
    LaunchedEffect(stravaFtpEstimate) {
        if (stravaFtpEstimate != null) {
            ftpEstimate = stravaFtpEstimate!!.estimatedFTP
            Log.d("StravaAuthScreen", "FTP estimate received from StravaViewModel: ${ftpEstimate}W")
        }
    }

    // Handle FTHR estimate results from ViewModel
    LaunchedEffect(stravaFthrEstimate) {
        if (stravaFthrEstimate != null) {
            fthrEstimate = stravaFthrEstimate
            Log.d("StravaAuthScreen", "FTHR estimate received from ViewModel: ${fthrEstimate} bpm")
        }
    }

    // Handle FTP results from HomeViewModel
    LaunchedEffect(ftpEstimateData, error) {
        if (ftpEstimateData != null) {
            ftpEstimate = ftpEstimateData!!.estimatedFTP
            Log.d("StravaAuthScreen", "FTP estimate received from HomeViewModel: ${ftpEstimate}W")
        }
        if (error != null) {
            errorMessage = error
        }
    }

    // Monitor state changes
    LaunchedEffect(stravaState, isConnecting) {
        Log.d("StravaAuthScreen", "stravaState changed to: $stravaState")
        Log.d("StravaAuthScreen", "isConnecting changed to: $isConnecting")
    }

    fun connectToStrava() {
        Log.d("StravaAuthScreen", "Starting OAuth flow...")
        isConnecting = true
        coroutineScope.launch {
            try {
                val authUrl = stravaViewModel.connect()
                Log.d("StravaAuthScreen", "OAuth URL received: $authUrl")
                openUrl(authUrl)
            } catch (e: Exception) {
                Log.e("StravaAuthScreen", "Error starting OAuth", e)
                isConnecting = false
                errorMessage = "Failed to start OAuth: ${e.message}"
            }
        }
    }

    fun syncActivities() {
        Log.d("StravaAuthScreen", "Navigating to sync loading screen...")
        navController.navigate("strava_sync_loading")
    }

    // Log recomposition
    Log.d("StravaAuthScreen", "stravaState: $stravaState")
    Log.d("StravaAuthScreen", "isConnecting: $isConnecting")
    Log.d("StravaAuthScreen", "isSyncing: $isSyncing")
    Log.d("StravaAuthScreen", "errorMessage: $errorMessage")
    Log.d("StravaAuthScreen", "syncMessage: $syncMessage")

    val currentStravaState = stravaState

    // UI
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = MaterialTheme.extendedColors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        extendedColors.gradientPrimary,
                        extendedColors.gradientSecondary,
                        extendedColors.gradientAccent
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
                text = "Connect Strava",
                style = MaterialTheme.typography.titleLarge,
                color = colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        // Content
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Strava Logo and Title - Made larger and clearer
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_strava),
                            contentDescription = "Strava",
                            modifier = Modifier.size(72.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Integrare Strava",
                            style = MaterialTheme.typography.headlineSmall,
                            color = colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    when {
                        currentStravaState is com.example.fitnessapp.viewmodel.StravaState.Connected -> {
                            // Connected State
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check_circle),
                                    contentDescription = "Conectat",
                                    tint = extendedColors.success,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "✓ Conectat cu succes",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = extendedColors.success,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // Activity Sync
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Sincronizare activități",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        if (syncMessage != null) {
                                            Text(
                                                text = syncMessage!!,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (syncMessage!!.startsWith("✓")) extendedColors.success else colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                        Button(
                                            onClick = {
                                                isSyncing = true
                                                syncMessage = "Pornire sincronizare..."
                                                coroutineScope.launch {
                                                    try {
                                                        syncActivities()
                                                    } catch (e: Exception) {
                                                        Log.e("StravaAuthScreen", "Error during sync", e)
                                                        syncMessage = "Eroare: ${e.message}"
                                                    } finally {
                                                        isSyncing = false
                                                    }
                                                }
                                            },
                                            enabled = !isSyncing,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = colorScheme.primary
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            if (isSyncing) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    color = colorScheme.onPrimary,
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                            Text(if (isSyncing) "Sincronizare..." else "Sincronizează activități")
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { 
                                                stravaViewModel.disconnect()
                                                isConnecting = false
                                                errorMessage = null
                                                syncMessage = null
                                                ftpEstimate = null
                                                fthrEstimate = null
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = colorScheme.error
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Deconectează Strava")
                                        }
                                    }
                                }
                                
                                // Performance Metrics
                                if (ftpEstimate != null || fthrEstimate != null) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = "Metrici de performanță",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    if (ftpEstimate != null) {
                                        val currentFtp = ftpEstimate
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "FTP (Putere prag funcțional)",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Prag de putere pentru antrenament",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "${currentFtp!!.toInt()} W",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = extendedColors.info,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "FTHR (Frecvență cardiacă prag funcțional)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Prag de frecvență cardiacă",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = if (fthrEstimate != null) "${fthrEstimate} bpm" else "Indisponibil",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = extendedColors.warning,
                                            fontWeight = if (fthrEstimate != null) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                        isConnecting -> {
                            // Connecting State
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = colorScheme.primary,
                                    strokeWidth = 4.dp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Conectare în curs...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Vă rugăm să completați autorizarea în browser",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        else -> {
                            // Not Connected State - Simplified
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Sincronizează activitățile și obține detalii despre performanță",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        connectToStrava()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = extendedColors.strava),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Conectare la Strava")
                                }
                            }
                        }
                    }
                    
                    // Error Message
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colorScheme.errorContainer)
                        ) {
                            Text(
                                text = errorMessage!!,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                
                // Fixed bottom area with Continue button
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Continue Button
                    if (currentStravaState is com.example.fitnessapp.viewmodel.StravaState.Connected) {
                        Button(
                            onClick = onContinue,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = extendedColors.success),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Continuă")
                        }
                    } else {
                        OutlinedButton(
                            onClick = onContinue,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Continuă fără Strava")
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StravaAuthScreenPreview() {
    MaterialTheme {
        StravaAuthScreen(
            onContinue = {},
            authViewModel = AuthViewModel(SharedPreferencesMock()),
            stravaViewModel = StravaViewModel(LocalContext.current),
            navController = NavHostController(LocalContext.current)
        )
    }
}




