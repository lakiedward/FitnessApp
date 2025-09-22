package com.example.fitnessapp.pages.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fitnessapp.model.TrainingPlanCreateRequest
import com.example.fitnessapp.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCreateTrainingPlanScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    defaultDate: String
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val colorScheme = MaterialTheme.colorScheme

    val dateFormatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }

    fun parseDateToMillis(value: String): Long {
        return runCatching {
            LocalDate.parse(value, dateFormatter)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrElse {
            LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
    }

    var selectedDate by remember { mutableStateOf(defaultDate) }
    var workoutName by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("00:45:00") }
    var intensity by remember { mutableStateOf("Moderate") }
    var description by remember { mutableStateOf("") }
    var workoutType by remember { mutableStateOf("cycling") }

    var showDatePicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            errorMessage = null
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = parseDateToMillis(selectedDate)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val pickedMillis = datePickerState.selectedDateMillis
                    if (pickedMillis != null) {
                        val newDate = Instant.ofEpochMilli(pickedMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        selectedDate = newDate.format(dateFormatter)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    fun handleSave() {
        val name = workoutName.trim()
        val planDuration = duration.trim()
        val planIntensity = intensity.trim()

        if (name.isEmpty()) {
            errorMessage = "Workout name is required"
            return
        }
        if (planDuration.isEmpty()) {
            errorMessage = "Duration is required"
            return
        }
        if (planIntensity.isEmpty()) {
            errorMessage = "Intensity is required"
            return
        }

        isSaving = true
        focusManager.clearFocus()

        val request = TrainingPlanCreateRequest(
            date = selectedDate,
            workoutName = name,
            duration = planDuration,
            intensity = planIntensity,
            description = description.trim(),
            workoutType = workoutType.ifBlank { null }
        )

        authViewModel.quickCreateTrainingPlan(request) { success, message ->
            isSaving = false
            if (success) {
                navController.popBackStack()
            } else {
                errorMessage = message ?: "Failed to create training"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Training") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Planned date") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = "Select date"
                            )
                        }
                    }
                )
            }

            item {
                OutlinedTextField(
                    value = workoutName,
                    onValueChange = { workoutName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Workout name") }
                )
            }

            item {
                OutlinedTextField(
                    value = workoutType,
                    onValueChange = { workoutType = it.lowercase() },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Workout type (cycling, running, swimming)") }
                )
            }

            item {
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Duration (HH:MM:SS)") }
                )
            }

            item {
                OutlinedTextField(
                    value = intensity,
                    onValueChange = { intensity = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Intensity") }
                )
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    label = { Text("Description") },
                    maxLines = 6
                )
            }

            item {
                Button(
                    onClick = {
                        if (!isSaving) {
                            coroutineScope.launch { handleSave() }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    )
                ) {
                    Text(if (isSaving) "Saving..." else "Save training")
                }
            }
        }
    }
}




