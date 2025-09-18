package com.example.fitnessapp.pages.signup

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.example.fitnessapp.ui.theme.extendedColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.fitnessapp.R
import com.example.fitnessapp.mock.SharedPreferencesMock
import com.example.fitnessapp.model.ChoosedSports
import com.example.fitnessapp.model.SportsSelectionRequest
import com.example.fitnessapp.model.UserDetalis
import com.example.fitnessapp.model.UserWeekAvailability
import com.example.fitnessapp.viewmodel.AuthViewModel

@Composable
fun ChooseSportsScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    choosedSports: MutableState<ChoosedSports>,
    userDetalis: MutableState<UserDetalis>,
    requiredSport: String? = null
) {
    val selectedSports = remember { mutableStateMapOf<String, Boolean>() }
    val selectedDays = remember { mutableStateMapOf<String, Boolean>() }
    val hours = remember { mutableStateMapOf<String, String>() }
    val minutes = remember { mutableStateMapOf<String, String>() }
    
    var isSubmitting by remember { mutableStateOf(false) }
    var submitCount by remember { mutableStateOf(0) }
    var submitError by remember { mutableStateOf<String?>(null) }
    var warningMessage by remember { mutableStateOf<String?>(null) }

    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    // Validation logic
    val isContinueEnabled = remember(selectedSports.values.toList(), selectedDays.values.toList()) {
        val hasSelectedSports = selectedSports.values.any { it }
        val hasSelectedDays = selectedDays.values.any { it }
        val hasRequiredSport = requiredSport == null || selectedSports[requiredSport] == true
        
        if (requiredSport != null && selectedSports[requiredSport] != true) {
            warningMessage = "Please select $requiredSport as it's required for your training plan"
        } else {
            warningMessage = null
        }
        
        hasSelectedSports && hasSelectedDays && hasRequiredSport
    }

    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = MaterialTheme.extendedColors

    Box(
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
                text = "Sports & Schedule",
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
                // Sports Selection Section
                Text(
                    text = "Choose Your Sports",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Warning message if required sport is not selected
                warningMessage?.let { message ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colorScheme.errorContainer.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SportsIcon(
                        iconRes = R.drawable.cycling,
                        label = "Cycling",
                        isSelected = selectedSports["Cycling"] == true,
                        isRequired = requiredSport == "Cycling",
                        onSelectionChanged = { selectedSports["Cycling"] = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    )
                    SportsIcon(
                        iconRes = R.drawable.running,
                        label = "Running",
                        isSelected = selectedSports["Running"] == true,
                        isRequired = requiredSport == "Running",
                        onSelectionChanged = { selectedSports["Running"] = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    )
                    SportsIcon(
                        iconRes = R.drawable.swimming,
                        label = "Swimming",
                        isSelected = selectedSports["Swimming"] == true,
                        isRequired = requiredSport == "Swimming",
                        onSelectionChanged = { selectedSports["Swimming"] = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Availability Section
                Text(
                    text = "Choose Your Availability",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(days) { day ->
                        AvailabilityRow(
                            day = day,
                            isSelected = selectedDays[day] ?: false,
                            onDaySelected = {
                                selectedDays[day] = !(selectedDays[day] ?: false)
                            },
                            hourValue = hours[day] ?: "",
                            onHourChange = { hours[day] = it },
                            minuteValue = minutes[day] ?: "",
                            onMinuteChange = { minutes[day] = it },
                            isInputEnabled = selectedDays[day] == true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Continue Button
                Button(
                    onClick = {
                        isSubmitting = true
                        submitCount = 0
                        submitError = null

                        val availabilityList = days.mapNotNull { day ->
                            if (selectedDays[day] == true) {
                                UserWeekAvailability(
                                    day = day,
                                    hours = hours[day]?.toIntOrNull() ?: 0,
                                    minutes = minutes[day]?.toIntOrNull() ?: 0
                                )
                            } else null
                        }

                        val selectedSportsList =
                            selectedSports.filter { it.value }.keys.toList()
                        choosedSports.value = ChoosedSports(selectedSportsList)

                        // Save availability
                        authViewModel.addWeekAvailability(
                            availabilityList,
                            onSuccess = {
                                submitCount++
                                Log.d(
                                    "ChooseSports",
                                    "Availability saved successfully. Count: $submitCount"
                                )
                                if (submitCount == 2) {
                                    navController.navigate("setup_status_loading_screen")
                                    isSubmitting = false
                                }
                            },
                            onError = { error ->
                                submitError = error
                                isSubmitting = false
                                Log.e("ChooseSports", "Error saving availability: $error")
                            }
                        )

                        // Save sports
                        authViewModel.saveUserSports(
                            SportsSelectionRequest(
                                selectedSportsList
                            )
                        )
                        submitCount++
                        Log.d("ChooseSports", "Sports save initiated. Count: $submitCount")
                        if (submitCount == 2) {
                            navController.navigate("setup_status_loading_screen")
                            isSubmitting = false
                        }
                    },
                    enabled = isContinueEnabled && !isSubmitting,
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
                    if (isSubmitting) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...")
                        }
                    } else {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                submitError?.let {
                    Text(
                        text = it,
                        color = colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SportsIcon(
    iconRes: Int,
    label: String,
    isSelected: Boolean,
    isRequired: Boolean,
    onSelectionChanged: (Boolean) -> Unit,
    modifier: Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = MaterialTheme.extendedColors

    Card(
        modifier = modifier
            .size(100.dp)
            .clickable {
                onSelectionChanged(!isSelected)
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> colorScheme.primary
                isRequired -> colorScheme.errorContainer.copy(alpha = 0.1f)
                else -> extendedColors.surfaceSubtle
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        ),
        border = if (isRequired && !isSelected) {
            BorderStroke(2.dp, colorScheme.errorContainer)
        } else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    colorFilter = ColorFilter.tint(
                        when {
                            isSelected -> colorScheme.onPrimary
                            isRequired -> colorScheme.errorContainer
                            else -> colorScheme.onSurfaceVariant
                        }
                    ),
                    modifier = Modifier.size(48.dp)
                )

                if (isRequired && !isSelected) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Required",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.errorContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AvailabilityRow(
    day: String,
    isSelected: Boolean,
    onDaySelected: () -> Unit,
    hourValue: String,
    onHourChange: (String) -> Unit,
    minuteValue: String,
    onMinuteChange: (String) -> Unit,
    isInputEnabled: Boolean
) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = MaterialTheme.extendedColors

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colorScheme.primary.copy(alpha = 0.1f) else extendedColors.surfaceSubtle
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Day selection row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = onDaySelected,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = colorScheme.primary,
                        unselectedColor = colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) colorScheme.primary else colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            // Time inputs row (only visible when selected)
            if (isSelected) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = hourValue,
                        onValueChange = onHourChange,
                        label = { Text("Hours", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = isInputEnabled,
                        shape = RoundedCornerShape(8.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            disabledBorderColor = colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )
                    OutlinedTextField(
                        value = minuteValue,
                        onValueChange = onMinuteChange,
                        label = { Text("Minutes", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = isInputEnabled,
                        shape = RoundedCornerShape(8.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            disabledBorderColor = colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChooseSportsScreenPreview() {
    ChooseSportsScreen(
        navController = rememberNavController(),
        authViewModel = AuthViewModel(SharedPreferencesMock()),
        choosedSports = remember { mutableStateOf(ChoosedSports()) },
        userDetalis = remember { mutableStateOf(UserDetalis(0, 0f, 0f, "", "")) }
    )
}

