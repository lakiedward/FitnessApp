package com.example.fitnessapp.pages.signup

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import com.example.fitnessapp.ui.theme.extendedColors
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.fitnessapp.mock.SharedPreferencesMock
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.viewmodel.SetupViewModel

@Composable
fun SetupStatusScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    setupViewModel: SetupViewModel = viewModel()
) {
    val setupStatus by setupViewModel.setupStatus.observeAsState()
    val isLoading by setupViewModel.isLoading.observeAsState(false)
    val error by setupViewModel.error.observeAsState()
    
    // Check setup status when screen loads
    LaunchedEffect(Unit) {
        val token = authViewModel.getToken()
        if (token != null) {
            setupViewModel.checkSetupStatus(token)
        }
    }
    
    // Navigate based on setup status
    LaunchedEffect(setupStatus) {
        setupStatus?.let { status ->
            Log.d("SetupStatusScreen", "Processing setup status: ${status.missing}")
            when {
                status.missing.isEmpty() -> {
                    Log.d("SetupStatusScreen", "✅ All data complete - navigating to plan_length_screen")
                    navController.navigate("plan_length_screen") {
                        popUpTo("setup_status") { inclusive = true }
                    }
                }
                status.missing.contains("cycling_ftp") -> {
                    Log.d("SetupStatusScreen", "⚠️ Missing cycling FTP - navigating to add_ftp_screen")
                    navController.navigate("add_ftp_screen") {
                        popUpTo("setup_status") { inclusive = true }
                    }
                }
                status.missing.contains("running_prediction") -> {
                    Log.d("SetupStatusScreen", "⚠️ Missing running predictions - navigating to add_running_pace_screen")
                    navController.navigate("add_running_pace_screen") {
                        popUpTo("setup_status") { inclusive = true }
                    }
                }
                status.missing.contains("swim_prediction") -> {
                    Log.d("SetupStatusScreen", "⚠️ Missing swimming predictions - navigating to add_swim_pace_screen")
                    navController.navigate("add_swim_pace_screen") {
                        popUpTo("setup_status") { inclusive = true }
                    }
                }
            }
        }
    }
    
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = MaterialTheme.extendedColors
    val gradientContentColor = if (isSystemInDarkTheme()) colorScheme.onSurface else colorScheme.onPrimary

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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Înapoi",
                    tint = gradientContentColor
                )
            }
            
            Text(
                text = "Verificare configurare",
                style = MaterialTheme.typography.headlineSmall,
                color = gradientContentColor,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.width(48.dp))
        }

        // Content
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = colorScheme.primary,
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Se verifică configurarea sportivă...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    error != null -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Eroare la verificare",
                                style = MaterialTheme.typography.headlineSmall,
                                color = colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = error!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    setupViewModel.clearError()
                                    val token = authViewModel.getToken()
                                    if (token != null) {
                                        setupViewModel.checkSetupStatus(token)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Reîncearcă")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SetupStatusScreenPreview() {
    MaterialTheme {
        SetupStatusScreen(
            navController = NavHostController(LocalContext.current),
            authViewModel = AuthViewModel(SharedPreferencesMock())
        )
    }
}