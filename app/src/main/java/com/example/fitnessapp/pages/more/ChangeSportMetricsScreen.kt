package com.example.fitnessapp.pages.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.fitnessapp.ui.theme.extendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeSportMetricsScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    setupViewModel: SetupViewModel = viewModel()
) {
    var ftpValue by remember { mutableStateOf("") }
    var maxBpmValue by remember { mutableStateOf("") }

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
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.extendedColors.gradientPrimary,
                                MaterialTheme.extendedColors.gradientSecondary,
                                MaterialTheme.extendedColors.gradientAccent
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Change Sport Metrics",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.extendedColors.gradientPrimary,
                            MaterialTheme.extendedColors.gradientSecondary,
                            MaterialTheme.extendedColors.gradientAccent
                        )
                    )
                )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Cycling FTP Section
                    item {
                        ModernSection(title = "Cycling FTP") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = ftpValue,
                                    onValueChange = { ftpValue = it },
                                    label = { Text("FTP Value (watts)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }

                    // Running Max BPM Section
                    item {
                        ModernSection(title = "Running Max BPM") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = maxBpmValue,
                                    onValueChange = { maxBpmValue = it },
                                    label = { Text("Max BPM (beats per minute)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }

                    // Running Paces Section
                    item {
                        ModernSection(title = "Running Paces") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
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
                    }

                    // Swimming Paces Section
                    item {
                        ModernSection(title = "Swimming Paces") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
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

                                    // Max BPM
                                    val maxBpmInt = maxBpmValue.toIntOrNull()
                                    if (maxBpmInt != null && maxBpmInt > 0) {
                                        // TODO: Add setupViewModel method for Max BPM
                                        // setupViewModel.submitMaxBpm(token, maxBpmInt)
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
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Save Changes",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
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
