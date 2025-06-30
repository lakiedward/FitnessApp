package com.example.fitnessapp.pages.signup

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.fitnessapp.mock.SharedPreferencesMock
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.viewmodel.SetupViewModel

@Composable
fun SetupStatusLoadingScreen(
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
    
    // Handle setup status response
    LaunchedEffect(setupStatus) {
        setupStatus?.let { status ->
            when {
                status.missing.isEmpty() -> {
                    // All data complete - navigate to plan length
                    navController.navigate("plan_length_screen") {
                        popUpTo("setup_status_loading_screen") { inclusive = true }
                    }
                }
                status.missing.contains("cycling_ftp") -> {
                    // Missing cycling FTP - navigate to FTP screen
                    navController.navigate("add_ftp_screen") {
                        popUpTo("setup_status_loading_screen") { inclusive = true }
                    }
                }
                status.missing.contains("running_prediction") -> {
                    // Missing running predictions - navigate to running pace screen
                    navController.navigate("add_running_pace_screen") {
                        popUpTo("setup_status_loading_screen") { inclusive = true }
                    }
                }
                status.missing.contains("swim_prediction") -> {
                    // Missing swimming predictions - navigate to swim pace screen
                    navController.navigate("add_swim_pace_screen") {
                        popUpTo("setup_status_loading_screen") { inclusive = true }
                    }
                }
            }
        }
    }
    
    // Handle error
    LaunchedEffect(error) {
        error?.let {
            // On error, go back to ChooseSports
            navController.navigateUp()
        }
    }
    
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
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated loading indicator
                val infiniteTransition = rememberInfiniteTransition(label = "loading")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )
                
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale),
                    color = Color(0xFF6366F1),
                    strokeWidth = 6.dp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Checking Your Data",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = if (isLoading) {
                        "Verifying your sports data..."
                    } else {
                        "Preparing your training plan..."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Progress dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { index ->
                        val dotScale by infiniteTransition.animateFloat(
                            initialValue = 0.5f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(
                                    durationMillis = 600,
                                    delayMillis = index * 200,
                                    easing = LinearEasing
                                ),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "dot$index"
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .graphicsLayer(scaleX = dotScale, scaleY = dotScale)
                                .background(
                                    color = Color(0xFF6366F1),
                                    shape = RoundedCornerShape(6.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SetupStatusLoadingScreenPreview() {
    SetupStatusLoadingScreen(
        navController = rememberNavController(),
        authViewModel = AuthViewModel(SharedPreferencesMock())
    )
} 