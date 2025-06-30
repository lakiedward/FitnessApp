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
import androidx.navigation.NavHostController
import com.example.fitnessapp.viewmodel.AuthState
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.model.UserDetalis

@Composable
fun DisciplineLoadingScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    userDetalis: UserDetalis
) {
    val authState by authViewModel.authState.observeAsState()
    
    // Handle auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                // Success - navigate to next screen
                navController.navigate("choose_sports") {
                    popUpTo("discipline_loading_screen") { inclusive = true }
                }
            }
            is AuthState.Error -> {
                // Error - go back to discipline selection
                navController.navigateUp()
            }
            else -> {
                // Still loading or other states - do nothing
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
                    text = "Saving Your Discipline",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Saving your training discipline: ${userDetalis.discipline}",
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