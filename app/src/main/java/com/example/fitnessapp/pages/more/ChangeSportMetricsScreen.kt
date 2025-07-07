package com.example.fitnessapp.pages.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.fitnessapp.mock.SharedPreferencesMock
import com.example.fitnessapp.model.RunningPrediction
import com.example.fitnessapp.model.SwimPrediction
import com.example.fitnessapp.pages.signup.RunningDistance
import com.example.fitnessapp.pages.signup.RunningPaceData
import com.example.fitnessapp.pages.signup.RunningPaceEntryCard
import com.example.fitnessapp.pages.signup.SwimDistance
import com.example.fitnessapp.pages.signup.SwimPaceData
import com.example.fitnessapp.pages.signup.SwimPaceEntryCard
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.viewmodel.SetupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeSportMetricsScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    setupViewModel: SetupViewModel = viewModel()
) {
    var ftpValue by remember { mutableStateOf("") }

    val commonRunningDistances = listOf(
        RunningDistance(1f, "1 km"),
        RunningDistance(5f, "5 km"),
        RunningDistance(10f, "10 km"),
        RunningDistance(21.0975f, "Half Marathon"),
        RunningDistance(42.195f, "Marathon")
    )
    var runningEntries by remember {
        mutableStateOf(
            commonRunningDistances.map { distance ->
                RunningPaceData(
                    distance = distance,
                    minutes = "",
                    seconds = ""
                )
            }
        )
    }

    val commonSwimDistances = listOf(
        SwimDistance(50, "50m"),
        SwimDistance(100, "100m"),
        SwimDistance(200, "200m"),
        SwimDistance(400, "400m"),
        SwimDistance(800, "800m"),
        SwimDistance(1500, "1500m")
    )
    var swimEntries by remember {
        mutableStateOf(
            commonSwimDistances.map { distance ->
                SwimPaceData(
                    distance = distance,
                    minutes = "",
                    seconds = ""
                )
            }
        )
    }

    // TODO: Add state for loading, error, and success from viewmodel

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF6366F1),
                                Color(0xFF8B5CF6),
                                Color(0xFFA855F7)
                            )
                        )
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Change Sport Metrics",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cycling FTP Section
            item {
                ModernSection(title = "Cycling FTP") {
                    OutlinedTextField(
                        value = ftpValue,
                        onValueChange = { ftpValue = it },
                        label = { Text("FTP Value (watts)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Running Paces Section
            item {
                ModernSection(title = "Running Paces") {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        runningEntries.forEachIndexed { index, entry ->
                            RunningPaceEntryCard(
                                entry = entry,
                                onEntryChange = { newEntry ->
                                    runningEntries = runningEntries.toMutableList().apply {
                                        set(index, newEntry)
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Swimming Paces Section
            item {
                ModernSection(title = "Swimming Paces") {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        swimEntries.forEachIndexed { index, entry ->
                            SwimPaceEntryCard(
                                entry = entry,
                                onEntryChange = { newEntry ->
                                    swimEntries = swimEntries.toMutableList().apply {
                                        set(index, newEntry)
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Submit Button
            item {
                Button(
                    onClick = {
                        val token = authViewModel.getToken()
                        if (token != null) {
                            // FTP
                            val ftpInt = ftpValue.toIntOrNull()
                            if (ftpInt != null && ftpInt > 0) {
                                setupViewModel.submitCyclingData(token, ftpInt)
                            }

                            // Running
                            val validRunningEntries = runningEntries.mapNotNull { entry ->
                                val minutes = entry.minutes.toIntOrNull()
                                val seconds = entry.seconds.toIntOrNull()
                                if (minutes != null && seconds != null && (minutes > 0 || seconds > 0)) {
                                    val timeString = String.format("%d:%02d", minutes, seconds)
                                    RunningPrediction(
                                        distanceKm = entry.distance.distanceKm,
                                        time = timeString
                                    )
                                } else null
                            }
                            if (validRunningEntries.isNotEmpty()) {
                                setupViewModel.submitRunningPacePredictions(
                                    token,
                                    validRunningEntries
                                )
                            }

                            // Swimming
                            val validSwimEntries = swimEntries.mapNotNull { entry ->
                                val minutes = entry.minutes.toIntOrNull()
                                val seconds = entry.seconds.toIntOrNull()
                                if (minutes != null && seconds != null && (minutes > 0 || seconds > 0)) {
                                    val timeString = String.format("%d:%02d", minutes, seconds)
                                    SwimPrediction(
                                        distanceM = entry.distance.distanceMeters,
                                        time = timeString
                                    )
                                } else null
                            }
                            if (validSwimEntries.isNotEmpty()) {
                                setupViewModel.submitSwimPacePredictions(token, validSwimEntries)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Save Changes",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChangeSportMetricsScreenPreview() {
    ChangeSportMetricsScreen(
        navController = rememberNavController(),
        authViewModel = AuthViewModel(SharedPreferencesMock())
    )
}
