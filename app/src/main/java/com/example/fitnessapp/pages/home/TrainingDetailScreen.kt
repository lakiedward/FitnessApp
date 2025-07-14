package com.example.fitnessapp.pages.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.fitnessapp.components.TrainingChart
import com.example.fitnessapp.mock.SharedPreferencesMock
import com.example.fitnessapp.model.TrainingPlan
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.viewmodel.TrainingDetailViewModel

@Composable
fun TrainingDetailScreen(
    training: TrainingPlan,
    navController: NavController,
    authViewModel: AuthViewModel,
    viewModel: TrainingDetailViewModel = viewModel()
) {
    val metrics by viewModel.calculatedMetrics.observeAsState()
    val ftpEstimate by viewModel.ftpEstimate.observeAsState()
    val cyclingFtp by viewModel.cyclingFtp.observeAsState()
    val runningFtp by viewModel.runningFtp.observeAsState()
    val swimmingPace by viewModel.swimmingPace.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState()
    var error by remember { mutableStateOf<String?>(null) }

    // Fetch FTP estimate when the screen is first displayed
    LaunchedEffect(Unit) {
        try {
            val token = authViewModel.getToken() ?: throw Exception("Not authenticated")
            val workoutType = training.workout_type ?: "cycling"

            // Use the new method to ensure data is loaded only once
            viewModel.ensureDataLoaded(token, workoutType)
        } catch (e: Exception) {
            error = e.message
        }
    }

    // Calculate metrics when the appropriate data is loaded
    LaunchedEffect(isLoading, cyclingFtp, runningFtp, swimmingPace, error) {
        android.util.Log.d("TrainingDetailScreen", "=== CALCULATION TRIGGER DEBUG ===")
        android.util.Log.d("TrainingDetailScreen", "isLoading: $isLoading")
        android.util.Log.d("TrainingDetailScreen", "error: $error")
        android.util.Log.d("TrainingDetailScreen", "cyclingFtp: $cyclingFtp")
        android.util.Log.d("TrainingDetailScreen", "runningFtp: $runningFtp")
        android.util.Log.d("TrainingDetailScreen", "swimmingPace: $swimmingPace")
        android.util.Log.d("TrainingDetailScreen", "workout_type: ${training.workout_type}")

        if (isLoading == false && error == null) {
            val workoutType = training.workout_type?.lowercase() ?: "cycling"

            // Wait for the appropriate data to be loaded before calculating
            val shouldCalculate = when (workoutType) {
                "cycling" -> cyclingFtp != null
                "running" -> runningFtp != null
                "swimming" -> swimmingPace != null
                else -> cyclingFtp != null
            }

            android.util.Log.d("TrainingDetailScreen", "workoutType: $workoutType")
            android.util.Log.d("TrainingDetailScreen", "shouldCalculate: $shouldCalculate")

            if (shouldCalculate) {
                // Use appropriate FTP value or default
                val ftpToUse = when (workoutType) {
                    "cycling" -> cyclingFtp?.cyclingFtp?.toFloat() ?: 200f
                    "running" -> paceToSpeed(runningFtp?.runningFtp ?: "5:00")
                    "swimming" -> 200f // Swimming doesn't use FTP directly
                    else -> cyclingFtp?.cyclingFtp?.toFloat() ?: 200f
                }

                android.util.Log.d("TrainingDetailScreen", "ftpToUse: $ftpToUse")
                android.util.Log.d("TrainingDetailScreen", "Calling calculateTrainingMetrics...")

                viewModel.calculateTrainingMetrics(training, ftpToUse)
            } else {
                android.util.Log.d("TrainingDetailScreen", "Not calculating - waiting for data")
            }
        } else {
            android.util.Log.d(
                "TrainingDetailScreen",
                "Not calculating - isLoading: $isLoading, error: $error"
            )
        }
        android.util.Log.d("TrainingDetailScreen", "===================================")
    }

    Box(
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
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    navController.navigate("calendar_screen") {
                        popUpTo("calendar_screen") { inclusive = true }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = training.workout_name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                )

                // Placeholder for symmetry
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Content Card
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                when {
                    isLoading == true -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF6366F1)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Calculating training metrics...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                    error != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Error: $error",
                                color = Color(0xFFEF4444),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    error = null
                                    val token = authViewModel.getToken()
                                    if (!token.isNullOrEmpty()) {
                                        // Retry with the appropriate method based on workout type
                                        when (training.workout_type?.lowercase()) {
                                            "running" -> {
                                                viewModel.fetchRunningFtp(token)
                                            }

                                            "swimming" -> {
                                                viewModel.fetchSwimmingPace(token)
                                            }

                                            "cycling", null -> {
                                                viewModel.fetchCyclingFtp(token)
                                            }

                                            else -> {
                                                viewModel.fetchCyclingFtp(token) // Default to cycling
                                            }
                                        }
                                    }
                                },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6366F1)
                                )
                            ) {
                                Text("Retry", color = Color.White)
                            }
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Training Type Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = when (training.workout_type?.lowercase()) {
                                        "cycling" -> Color(0xFFF0F9FF)
                                        "running" -> Color(0xFFF0FDF4)
                                        "swimming" -> Color(0xFFFFF7ED)
                                        else -> Color(0xFFF8FAFC)
                                    }
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Training Type",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF6B7280),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = training.workout_type?.replaceFirstChar { it.uppercase() }
                                                    ?: "Unknown",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = when (training.workout_type?.lowercase()) {
                                                "cycling" -> Color(0xFF2563EB)
                                                "running" -> Color(0xFF059669)
                                                "swimming" -> Color(0xFFD97706)
                                                else -> Color(0xFF1F2937)
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Display the training chart if steps are available
                            training.steps?.let { steps ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp)
                                    ) {
                                        Text(
                                            text = "Workout Structure",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1F2937),
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )
                                        TrainingChart(
                                            steps = steps,
                                            workoutType = training.workout_type ?: "cycling",
                                            cyclingFtp = cyclingFtp?.cyclingFtp?.toFloat(),
                                            runningFtp = runningFtp?.runningFtp?.let {
                                                paceToSpeed(
                                                    it
                                                )
                                            },
                                            swimmingPace = swimmingPace?.pace100m
                                        )
                                    }
                                }
                            }

                            // Display training metrics
                            metrics?.let { trainingMetrics ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            ModernMetricItem(
                                                label = "TSS",
                                                value = String.format("%.1f", trainingMetrics.tss)
                                            )
                                            ModernMetricItem(
                                                label = "Duration",
                                                value = formatDuration(trainingMetrics.duration)
                                            )
                                            ModernMetricItem(
                                                label = "Calories",
                                                value = String.format("%.0f", trainingMetrics.calories)
                                            )
                                        }

                                        // Display sport-specific data
                                        when (training.workout_type?.lowercase()) {
                                            "cycling" -> {
                                                cyclingFtp?.let { cycling ->
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = Color(0xFFF0F9FF)
                                                        ),
                                                        elevation = CardDefaults.cardElevation(
                                                            defaultElevation = 2.dp
                                                        )
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(16.dp)
                                                        ) {
                                                            Text(
                                                                text = "Cycling FTP",
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF1F2937)
                                                            )
                                                            Spacer(modifier = Modifier.height(8.dp))

                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Column {
                                                                    Text(
                                                                        text = "FTP",
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        color = Color(0xFF6B7280)
                                                                    )
                                                                    Text(
                                                                        text = "${cycling.cyclingFtp} watts",
                                                                        style = MaterialTheme.typography.titleMedium,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color(0xFF2563EB)
                                                                    )
                                                                }
                                                                Column {
                                                                    Text(
                                                                        text = "FTHR",
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        color = Color(0xFF6B7280)
                                                                    )
                                                                    Text(
                                                                        text = "${cycling.fthrCycling} bpm",
                                                                        style = MaterialTheme.typography.titleMedium,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color(0xFF2563EB)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            "running" -> {
                                                runningFtp?.let { running ->
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = Color(0xFFF0FDF4)
                                                        ),
                                                        elevation = CardDefaults.cardElevation(
                                                            defaultElevation = 2.dp
                                                        )
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(16.dp)
                                                        ) {
                                                            Text(
                                                                text = "Running Data",
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF1F2937)
                                                            )
                                                            Spacer(modifier = Modifier.height(8.dp))

                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Column {
                                                                    Text(
                                                                        text = "Running Pace",
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        color = Color(0xFF6B7280)
                                                                    )
                                                                    Text(
                                                                        text = formatRunningPace(running.runningFtp),
                                                                        style = MaterialTheme.typography.titleMedium,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color(0xFF059669)
                                                                    )
                                                                }
                                                                Column {
                                                                    Text(
                                                                        text = "FTHR",
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        color = Color(0xFF6B7280)
                                                                    )
                                                                    Text(
                                                                        text = "${running.fthrRunning} bpm",
                                                                        style = MaterialTheme.typography.titleMedium,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color(0xFF059669)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            "swimming" -> {
                                                swimmingPace?.let { swimming ->
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = Color(0xFFFFF7ED)
                                                        ),
                                                        elevation = CardDefaults.cardElevation(
                                                            defaultElevation = 2.dp
                                                        )
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(16.dp)
                                                        ) {
                                                            Text(
                                                                text = "Swimming Data",
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF1F2937)
                                                            )
                                                            Spacer(modifier = Modifier.height(8.dp))

                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween
                                                            ) {
                                                                Column {
                                                                    Text(
                                                                        text = "Pace 100m",
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        color = Color(0xFF6B7280)
                                                                    )
                                                                    Text(
                                                                        text = swimming.pace100m,
                                                                        style = MaterialTheme.typography.titleMedium,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color(0xFFD97706)
                                                                    )
                                                                }
                                                                Column {
                                                                    Text(
                                                                        text = "FTHR Swimming",
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        color = Color(0xFF6B7280)
                                                                    )
                                                                    Text(
                                                                        text = "${swimming.fthrSwimming} bpm",
                                                                        style = MaterialTheme.typography.titleMedium,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color(0xFFD97706)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            else -> {
                                                // Show default message for unknown workout types
                                                if (isLoading == false && error == null) {
                                                    Spacer(modifier = Modifier.height(16.dp))
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = Color(0xFFF8FAFC)
                                                        ),
                                                        elevation = CardDefaults.cardElevation(
                                                            defaultElevation = 2.dp
                                                        )
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(16.dp)
                                                        ) {
                                                            Text(
                                                                text = "Training Information",
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF1F2937)
                                                            )
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                            Text(
                                                                text = "Using default values for calculations",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = Color(0xFF6B7280)
                                                            )
                                                        }
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
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp)
                                ) {
                                    Text(
                                        text = "Description",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F2937),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    Text(
                                        text = training.description,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color(0xFF374151),
                                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
                                    )
                                }
                            }

                            // Buton pentru a începe antrenamentul
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { 
                                    navController.navigate("workout_execution/${training.id}")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6366F1)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Începe antrenamentul",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
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
private fun ModernMetricItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6366F1)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280),
            fontWeight = FontWeight.Medium
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

private fun paceToSpeed(pace: String): Float {
    return try {
        if (pace.contains(":")) {
            // Format like "5:00" (min:sec per km)
            val paceParts = pace.split(":")
            val minutes = paceParts[0].toInt()
            val seconds = paceParts.getOrElse(1) { "0" }.toInt()
            val totalSeconds = minutes * 60 + seconds
            1000f / totalSeconds // Convert to m/s
        } else {
            // Format like "4.98" (decimal minutes per km)
            val decimalMinutes = pace.toFloat()
            val totalSeconds = decimalMinutes * 60f // Convert to seconds per km
            1000f / totalSeconds // Convert to m/s
        }
    } catch (e: Exception) {
        4.0f // Default fallback
    }
}

private fun formatRunningPace(pace: String): String {
    return try {
        if (pace.contains(":")) {
            // Already in pace format like "5:00"
            "$pace/km"
        } else {
            // It's decimal minutes per km (e.g., 4.98 = 4 minutes 58.8 seconds)
            val decimalMinutes = pace.toFloat()
            val minutes = decimalMinutes.toInt()
            val seconds = ((decimalMinutes - minutes) * 60).toInt()
            String.format("%d:%02d/km", minutes, seconds)
        }
    } catch (e: Exception) {
        "5:00/km" // Default fallback
    }
}

@Preview(showBackground = true)
@Composable
fun TrainingDetailScreenPreview() {
    TrainingDetailScreen(
        training = TrainingPlan(
            id = 1,
            user_id = 1,
            date = "2024-06-01",
            workout_name = "Test Workout",
            duration = "01:00:00",
            intensity = "Medium",
            description = "Test description",
            workout_type = "cycling",
            zwo_path = null,
            stepsJson = null
        ),
        navController = rememberNavController(),
        authViewModel = AuthViewModel(SharedPreferencesMock())
    )
}
