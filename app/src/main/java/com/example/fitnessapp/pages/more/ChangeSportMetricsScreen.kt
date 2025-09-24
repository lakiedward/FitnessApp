package com.example.fitnessapp.pages.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateTopPadding
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
import androidx.compose.material3.Surface
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.statusBarsPadding
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

    val gradientColors = listOf(
        MaterialTheme.extendedColors.gradientPrimary,
        MaterialTheme.extendedColors.gradientSecondary,
        MaterialTheme.extendedColors.gradientAccent
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.verticalGradient(colors = gradientColors))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                IconButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Text(
                    text = "Change Sport Metrics",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.verticalGradient(colors = gradientColors))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp,
                        top = paddingValues.calculateTopPadding() + 16.dp
                    ),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.extendedColors.surfaceSubtle,
                tonalElevation = 2.dp
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
                            shape = RoundedCornerShape(16.dp)
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
