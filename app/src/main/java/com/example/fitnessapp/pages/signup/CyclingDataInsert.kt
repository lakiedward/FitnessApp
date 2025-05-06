package com.example.fitnessapp.pages.signup

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fitnessapp.AuthViewModel
import com.example.fitnessapp.R
import com.example.fitnessapp.model.ChoosedSports
//import androidx.navigation.NavHostController
import com.example.fitnessapp.ui.theme.FitnessAppTheme
import com.example.fitnessapp.ui.theme.SectionTitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class CyclingDataInsert : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitnessAppTheme {

            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyclingDataInsertScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    choosedSports: MutableState<ChoosedSports>
) {
    var cyclingFtp by remember { mutableStateOf("") }
    var maxBpm by remember { mutableStateOf("") }
    var selectedRaceType by remember { mutableStateOf("") } // Neutilizat în codul actual
    var raceDate by remember { mutableStateOf("") } // Neutilizat în codul actual

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Back Button (rămâne în stânga sus)
            IconButton(
                onClick = { navController.navigateUp() },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.back),
                    contentDescription = "Back"
                )
            }

            // Conținutul centrat (imagine și câmpuri de text)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .align(Alignment.Center), // Centrează vertical și orizontal
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.cycling),
                    contentDescription = "Cycling Icon",
                    modifier = Modifier
                        .size(100.dp)
                        .padding(bottom = 16.dp)
                )

                SectionTitle(title = "Enter Cycling FTP")
                OutlinedTextField(
                    value = cyclingFtp,
                    onValueChange = { cyclingFtp = it },
                    label = { Text("Enter Cycling FTP", style = MaterialTheme.typography.bodySmall) },
                    placeholder = { Text("Watts", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier
                        .width(259.dp)
                        .height(56.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(34.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(16.dp)) // Spațiu între câmpuri

                SectionTitle(title = "Enter Maximum BPM")
                OutlinedTextField(
                    value = maxBpm,
                    onValueChange = { maxBpm = it },
                    label = { Text("Enter Maximum BPM", style = MaterialTheme.typography.bodySmall) },
                    placeholder = { Text("BPM", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier
                        .width(259.dp)
                        .height(56.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(34.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            // Butonul Continue (centrat orizontal, jos)
            Button(
                onClick = {
                    val ftpValue = cyclingFtp.toIntOrNull()
                    val bpmValue = maxBpm.toIntOrNull()

                    if (!choosedSports.value.running) {
                        if (ftpValue != null && bpmValue != null) {
                            authViewModel.addOrUpdateTrainingData(
                                ftp = ftpValue,
                                max_bpm = bpmValue
                            )
                            navController.navigate("plan_length_screen")
                        } else {
                            Toast.makeText(
                                navController.context,
                                "Please enter valid numbers for FTP and Max BPM.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        if (ftpValue != null && bpmValue != null) {
                            authViewModel.addOrUpdateTrainingData(
                                ftp = ftpValue,
                                max_bpm = bpmValue
                            )
                            navController.navigate("running_data_insert")
                        } else {
                            Toast.makeText(
                                navController.context,
                                "Please enter valid numbers for FTP and Max BPM.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                enabled = cyclingFtp.isNotEmpty() && maxBpm.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomCenter) // Centrat orizontal, jos
                    .padding(16.dp)
                    .fillMaxWidth(0.6f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (cyclingFtp.isNotEmpty() && maxBpm.isNotEmpty()) Color.Black else Color.Gray,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Continue")
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
        placeholder = { Text("MM/DD/YYYY" , style = MaterialTheme.typography.bodySmall) },
        trailingIcon = {
            Icon(Icons.Default.DateRange, contentDescription = "Select date")
        },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = MaterialTheme.colorScheme.primary, // Border when focused
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), // Border when unfocused
            focusedLabelColor = MaterialTheme.colorScheme.primary, // Label when focused
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), // Label when unfocused
            focusedTextColor = MaterialTheme.colorScheme.onSurface, // Ensure focused text is visible
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface // Ensure unfocused text is visible

        ),
        textStyle = MaterialTheme.typography.bodySmall,
        shape = RoundedCornerShape(34.dp),
        modifier = modifier
            .width(259.dp)
            .height(56.dp)
            .pointerInput(selectedDate) {
                awaitEachGesture {
                    // Modifier.clickable doesn't work for text fields, so we use Modifier.pointerInput
                    // in the Initial pass to observe events before the text field consumes them
                    // in the Main pass.
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
    val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
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
            TextButton(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
