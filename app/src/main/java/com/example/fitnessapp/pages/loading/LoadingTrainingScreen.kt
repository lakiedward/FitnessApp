package com.example.fitnessapp.pages.loading

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loading Training Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Loading Training Details",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = training.workout_name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Fetching FTP from database and calculating metrics...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Error: $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { 
                        val token = authViewModel.getToken()
                        if (!token.isNullOrEmpty()) {
                            viewModel.fetchLastFtpEstimateFromDb(token)
                        }
                    }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
} 