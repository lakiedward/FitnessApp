package com.example.fitnessapp.pages.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import com.example.fitnessapp.ui.theme.extendedColors
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.fitnessapp.model.SwimPrediction
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.viewmodel.SetupViewModel
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.fitnessapp.mock.SharedPreferencesMock

data class SwimDistance(
    val distanceMeters: Int,
    val displayName: String
)

@Composable
fun AddSwimPaceScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    setupViewModel: SetupViewModel = viewModel()
) {
    val commonDistances = listOf(
        SwimDistance(50, "50m"),
        SwimDistance(100, "100m"),
        SwimDistance(200, "200m"),
        SwimDistance(400, "400m"),
        SwimDistance(800, "800m"),
        SwimDistance(1500, "1500m")
    )
    
    var selectedEntries by remember { 
        mutableStateOf(
            commonDistances.map { distance ->
                SwimPaceData(
                    distance = distance,
                    minutes = "",
                    seconds = ""
                )
            }
        )
    }
    
    val isLoading by setupViewModel.isLoading.observeAsState(false)
    val error by setupViewModel.error.observeAsState()
    val submitSuccess by setupViewModel.submitSuccess.observeAsState(false)
    
    // Handle successful submission
    LaunchedEffect(submitSuccess) {
        if (submitSuccess) {
            setupViewModel.clearSubmitSuccess()
            // Navigate back to loading screen to re-check setup status
            navController.navigate("setup_status_loading_screen") {
                popUpTo("add_swim_pace_screen") { inclusive = true }
            }
        }
    }
    
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = MaterialTheme.extendedColors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        extendedColors.gradientPrimary,
                        extendedColors.gradientSecondary,
                        extendedColors.gradientAccent
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
                    tint = colorScheme.onPrimary
                )
            }
            
            Text(
                text = "Swimming Times",
                style = MaterialTheme.typography.headlineSmall,
                color = colorScheme.onPrimary,
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
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Personal Best Times",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Enter your personal best times for different swimming distances. We'll use these to create your swimming pace predictions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error display
                error?.let {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colorScheme.errorContainer.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Swimming entries list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(selectedEntries) { index, entry ->
                        SwimPaceEntryCard(
                            entry = entry,
                            onEntryChange = { newEntry ->
                                selectedEntries = selectedEntries.toMutableList().apply {
                                    set(index, newEntry)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit Button
                Button(
                    onClick = {
                        val token = authViewModel.getToken()
                        if (token != null) {
                            val validEntries = selectedEntries.mapNotNull { entry ->
                                val minutes = entry.minutes.toIntOrNull()
                                val seconds = entry.seconds.toIntOrNull()
                                if (minutes != null && seconds != null && (minutes > 0 || seconds > 0)) {
                                    // Convert to MM:SS format
                                    val timeString = String.format("%d:%02d", minutes, seconds)
                                    SwimPrediction(
                                        distanceM = entry.distance.distanceMeters,
                                        time = timeString
                                    )
                                } else null
                            }
                            
                            if (validEntries.isNotEmpty()) {
                                setupViewModel.submitSwimPacePredictions(token, validEntries)
                            }
                        }
                    },
                    enabled = !isLoading && selectedEntries.any { 
                        val minutes = it.minutes.toIntOrNull()
                        val seconds = it.seconds.toIntOrNull()
                        (minutes != null && minutes > 0) || (seconds != null && seconds > 0)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary,
                        disabledContainerColor = colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submitting...")
                        }
                    } else {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

data class SwimPaceData(
    val distance: SwimDistance,
    val minutes: String,
    val seconds: String
)

@Composable
fun SwimPaceEntryCard(
    entry: SwimPaceData,
    onEntryChange: (SwimPaceData) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = MaterialTheme.extendedColors

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = extendedColors.surfaceSubtle
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = entry.distance.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = entry.minutes,
                    onValueChange = { minutes ->
                        onEntryChange(entry.copy(minutes = minutes))
                    },
                    label = { Text("Minutes") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                )
                
                OutlinedTextField(
                    value = entry.seconds,
                    onValueChange = { seconds ->
                        onEntryChange(entry.copy(seconds = seconds))
                    },
                    label = { Text("Seconds") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddSwimPaceScreenPreview() {
    AddSwimPaceScreen(
        navController = rememberNavController(),
        authViewModel = AuthViewModel(SharedPreferencesMock())
    )
} 