package com.example.fitnessapp.pages.loading

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.navigation.NavController
import com.example.fitnessapp.model.TrainingPlan
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.viewmodel.TrainingDetailViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoadingTrainingScreen(
    training: TrainingPlan,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val viewModel: TrainingDetailViewModel = viewModel()
    val ftpEstimate by viewModel.ftpEstimate.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState()
    val error by viewModel.error.observeAsState()

    // Fetch FTP estimate when the screen is first displayed
    LaunchedEffect(Unit) {
        try {
            val token = authViewModel.getToken() ?: throw Exception("Not authenticated")
            viewModel.fetchLastFtpEstimateFromDb(token)
        } catch (e: Exception) {
            // Error will be handled by the viewModel
        }
    }

    // Navigate to TrainingDetailScreen when FTP is loaded or if there's an error
    LaunchedEffect(ftpEstimate, error, isLoading) {
        if (isLoading == false && (ftpEstimate != null || error != null)) {
            // Navigate to the actual training detail screen
            navController.navigate("training_detail/${training.id}") {
                popUpTo("loading_training") { inclusive = true }
            }
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
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            Text(
                text = "Loading Training",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            // Placeholder for symmetry
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
                    text = "Loading Training Details",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = training.workout_name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF6366F1),
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Fetching FTP from database and calculating training metrics for your workout.",
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
                
                // Error display
                if (error != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFF6B6B).copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Error: $error",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD32F2F)
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Button(
                                onClick = { 
                                    val token = authViewModel.getToken()
                                    if (!token.isNullOrEmpty()) {
                                        viewModel.fetchLastFtpEstimateFromDb(token)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6366F1),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
} 