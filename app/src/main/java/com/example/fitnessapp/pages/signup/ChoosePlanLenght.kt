package com.example.fitnessapp.pages.signup

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.model.ChoosedSports
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.fitnessapp.mock.SharedPreferencesMock

@Composable
fun PlanLengthScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    choosedSports: MutableState<ChoosedSports>
) {
    var raceDate by remember { mutableStateOf("") }

    Column(
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
                    tint = Color.White
                )
            }

            Text(
                text = "Plan Length",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
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
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Choose Plan Length",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Select the end date of your training plan",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                DatePickerFieldToModal(
                    modifier = Modifier.fillMaxWidth(),
                    onDateSelected = { selectedDate ->
                    raceDate = selectedDate
            }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Continue Button
            Button(
                onClick = {
                    Handler(Looper.getMainLooper()).postDelayed({
                        authViewModel.generateTrainingPlanBySport(raceDate)
                        navController.navigate("loading_screen")
                    }, 1000)
                },
                enabled = raceDate.isNotEmpty(),
                modifier = Modifier
                        .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1),
                        contentColor = Color.White,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp)
            ) {
                    Text(
                        text = "Continue",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerFieldToModal(modifier: Modifier = Modifier, onDateSelected: (String) -> Unit) {
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var showModal by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = selectedDate?.let { convertMillisToDate(it) } ?: "",
        onValueChange = { },
        label = { Text("Race Date") },
        placeholder = { Text("YYYY-MM-DD", style = MaterialTheme.typography.bodyMedium) },
        trailingIcon = {
            Icon(
                Icons.Default.DateRange, 
                contentDescription = "Select date",
                tint = Color(0xFF6366F1)
            )
        },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = Color(0xFF6366F1),
            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
            focusedLabelColor = Color(0xFF6366F1),
            unfocusedLabelColor = Color.Gray,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
        ),
        textStyle = MaterialTheme.typography.bodyMedium,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .pointerInput(selectedDate) {
                awaitEachGesture {
                    awaitFirstDown(pass = PointerEventPass.Initial)
                    val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    if (upEvent != null) {
                        showModal = true
                    }
                }
            }
    )

    if (showModal) {
        DatePickerModal(
            onDateSelected = { dateMillis ->
                selectedDate = dateMillis
                onDateSelected(selectedDate?.let { convertMillisToDate(it) } ?: "")
                showModal = false
            },
            onDismiss = { showModal = false }
        )
    }
}

fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return formatter.format(Date(millis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF6366F1)
                )
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.Gray
                )
            ) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(
                selectedDayContainerColor = Color(0xFF6366F1),
                todayContentColor = Color(0xFF6366F1),
                todayDateBorderColor = Color(0xFF6366F1)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PlanLengthScreenPreview() {
    PlanLengthScreen(
        navController = rememberNavController(),
        authViewModel = AuthViewModel(SharedPreferencesMock()),
        choosedSports = remember { mutableStateOf(ChoosedSports()) }
    )
}