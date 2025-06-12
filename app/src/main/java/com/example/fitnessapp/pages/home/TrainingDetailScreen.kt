package com.example.fitnessapp.pages.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fitnessapp.model.TrainingPlan
import com.example.fitnessapp.components.TrainingChart
import com.example.fitnessapp.viewmodel.TrainingDetailViewModel
import com.example.fitnessapp.viewmodel.AuthViewModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Duration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun TrainingDetailScreen(
    training: TrainingPlan,
    navController: NavController,
    authViewModel: AuthViewModel,
    viewModel: TrainingDetailViewModel = viewModel()
) {
    val metrics by viewModel.calculatedMetrics.observeAsState()
    val ftpEstimate by viewModel.ftpEstimate.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState()
    var error by remember { mutableStateOf<String?>(null) }

    // Fetch FTP estimate when the screen is first displayed
    LaunchedEffect(Unit) {
        try {
            val token = authViewModel.getToken() ?: throw Exception("Not authenticated")
            viewModel.fetchLastFtpEstimateFromDb(token)
        } catch (e: Exception) {
            error = e.message
        }
    }

    // Calculate metrics when loading is complete
    LaunchedEffect(isLoading, ftpEstimate, error) {
        if (isLoading == false) {
            val currentFtpEstimate = ftpEstimate
            val ftpToUse = if (currentFtpEstimate != null) {
                currentFtpEstimate.estimatedFTP
            } else {
                // Use default FTP if no estimate is available
                200f
            }
            viewModel.calculateTrainingMetrics(training, ftpToUse)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(training.workout_name) },
                navigationIcon = {
                    IconButton(onClick = { 
                        navController.navigate("calendar_screen") {
                            popUpTo("calendar_screen") { inclusive = true }
                        }
                    }) {
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
            when {
                isLoading == true -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Calculating training metrics...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error: $error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { 
                            error = null
                            val token = authViewModel.getToken()
                            if (!token.isNullOrEmpty()) {
                                viewModel.fetchLastFtpEstimateFromDb(token)
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Display the training chart if steps are available
                        training.steps?.let { steps ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Workout Structure",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    TrainingChart(steps = steps)
                                }
                            }
                        }

                        // Display training metrics
                        metrics?.let { trainingMetrics ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Training Metrics",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        MetricItem(
                                            label = "TSS",
                                            value = String.format("%.1f", trainingMetrics.tss)
                                        )
                                        MetricItem(
                                            label = "Duration",
                                            value = formatDuration(trainingMetrics.duration)
                                        )
                                        MetricItem(
                                            label = "Calories",
                                            value = String.format("%.0f kcal", trainingMetrics.calories)
                                        )
                                    }

                                    // Display FTP estimate if available
                                    ftpEstimate?.let { ftp ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp)
                                            ) {
                                                Text(
                                                    text = "FTP Estimate",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = "Estimated FTP",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                        Text(
                                                            text = "${ftp.estimatedFTP.toInt()} watts",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    }
                                                    Column {
                                                        Text(
                                                            text = "Confidence",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                        Text(
                                                            text = "${(ftp.confidence * 100).toInt()}%",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    }
                                                }
                                                
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Method: ${ftp.method}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                
                                                ftp.notes?.let { notes ->
                                                    Text(
                                                        text = "Notes: $notes",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                                
                                                ftp.fthrValue?.let { fthr ->
                                                    Text(
                                                        text = "FTHR: ${fthr.toInt()} bpm",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                                
                                                ftp.weeklyFTP?.let { weeklyFtp ->
                                                    if (weeklyFtp.isNotEmpty()) {
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text(
                                                            text = "Weekly FTP Trend",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                        weeklyFtp.take(3).forEach { week ->
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Text(
                                                                    text = week.week,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                                )
                                                                Text(
                                                                    text = "${week.ftpFinal.toInt()}w",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                                )
                                                                Text(
                                                                    text = week.categorie,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } ?: run {
                                        // Show default FTP message if no estimate is available
                                        if (isLoading == false && error == null) {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(16.dp)
                                                ) {
                                                    Text(
                                                        text = "FTP Information",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = "Using default FTP of 200 watts for calculations",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                    Text(
                                                        text = "Connect Strava to get personalized FTP estimates",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Display the complete description
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Description",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = training.description,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> String.format("%dh %dm", hours, minutes)
        else -> String.format("%dm", minutes)
    }
} 