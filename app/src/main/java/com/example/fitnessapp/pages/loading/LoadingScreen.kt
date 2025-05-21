package com.example.fitnessapp.pages.loading

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fitnessapp.viewmodel.AuthViewModel
import kotlinx.coroutines.delay
import androidx.compose.runtime.livedata.observeAsState
import com.example.fitnessapp.model.TrainingPlan

@Composable
fun LoadingScreen(navController: NavHostController, authViewModel: AuthViewModel) {
    val trainingPlan by authViewModel.trainingPlan.observeAsState(initial = emptyList<TrainingPlan>())
    var hasTimedOut by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authViewModel.getTrainingPlans()
    }

    LaunchedEffect(trainingPlan) {
        if (trainingPlan.isNotEmpty()) {
            navController.navigate("home_screen") {
                popUpTo("loading_screen") { inclusive = true }
            }
        }
    }

    // Timeout handler (2 minutes)
    LaunchedEffect(Unit) {
        delay(120_000) // 2 minute timeout
        if (trainingPlan.isEmpty()) {
            hasTimedOut = true
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Generating your training plan...")

                if (hasTimedOut) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "The request took too long. Please try again later.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}