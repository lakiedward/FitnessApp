package com.example.fitnessapp.pages.signup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fitnessapp.R
import com.example.fitnessapp.model.UserDetalis
//import androidx.navigation.NavHostController
import com.example.fitnessapp.ui.theme.FitnessAppTheme
import com.example.fitnessapp.ui.theme.SectionTitle


class AddAge : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitnessAppTheme {
                //GenderSelectionScreen()

            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAgeScreen(navController: NavHostController, userDetalis: MutableState<UserDetalis>) {
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weightUnit by remember { mutableStateOf("Kg") }
    var heightUnit by remember { mutableStateOf("Cm") }

    val isFormComplete = age.isNotBlank() && weight.isNotBlank() && height.isNotBlank()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Back Button
            IconButton(
                onClick = { navController.navigateUp() },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.back),
                    contentDescription = "Back",
                )
            }

            // Main Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Age Input
                SectionTitle(title = "How Old Are You?")
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Enter Age") },
                    modifier = Modifier
                        .width(259.dp)
                        .height(56.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(34.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, // Border when focused
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), // Border when unfocused
                        focusedLabelColor = MaterialTheme.colorScheme.primary, // Label when focused
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), // Label when unfocused
                        focusedTextColor = MaterialTheme.colorScheme.onSurface, // Ensure focused text is visible
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface // Ensure unfocused text is visible

                    )
                )

                // Weight Input
                SectionTitle(title = "What Is Your Weight?")
                ToggleButtonsRow(
                    options = listOf("Kg", "Lb"),
                    selectedOption = weightUnit,
                    onOptionSelected = { weightUnit = it }
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Enter Weight ($weightUnit)") },
                    modifier = Modifier
                        .width(259.dp)
                        .height(56.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(34.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, // Border when focused
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), // Border when unfocused
                        focusedLabelColor = MaterialTheme.colorScheme.primary, // Label when focused
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), // Label when unfocused
                        focusedTextColor = MaterialTheme.colorScheme.onSurface, // Ensure focused text is visible
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface // Ensure unfocused text is visible

                    )
                )

                // Height Input
                SectionTitle(title = "What Is Your Height?")
                ToggleButtonsRow(
                    options = listOf("Cm", "Ft"),
                    selectedOption = heightUnit,
                    onOptionSelected = { heightUnit = it }
                )
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text("Enter Height ($heightUnit)") },
                    modifier = Modifier
                        .width(259.dp)
                        .height(56.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(34.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary, // Border when focused
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), // Border when unfocused
                        focusedLabelColor = MaterialTheme.colorScheme.primary, // Label when focused
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), // Label when unfocused
                        focusedTextColor = MaterialTheme.colorScheme.onSurface, // Ensure focused text is visible
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface // Ensure unfocused text is visible

                    )
                )
            }

            // Continue Button
            Button(
                onClick = {
                    val convertedWeight = if (weightUnit == "Lb")
                        (weight.toFloat() * 0.453592f).toFloat() // Explicitly cast to Float
                    else
                        weight.toFloat() // Already a Float
                    val convertedHeight = if (heightUnit == "Ft")
                        (height.toFloat() * 30.48f).toFloat() // Explicitly cast to Float
                    else
                        height.toFloat() // Already a Float
                    var varsta = age.toInt()

                    userDetalis.value = userDetalis.value.copy(varsta = varsta, greutate = convertedWeight, inaltime = convertedHeight)

                    // Navigate to the next screen
                    navController.navigate("physical_activity_level_screen")
                },
                enabled = isFormComplete,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(0.6f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFormComplete) Color.Black else Color.Gray,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Continue")
            }
        }
    }
}


@Composable
fun ToggleButtonsRow(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        options.forEach { option ->
            Button(
                onClick = { onOptionSelected(option) },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (option == selectedOption) Color.Black else Color.Gray,
                    contentColor = if (option == selectedOption) Color.White else Color.Black
                )
            ) {
                Text(option)
            }
        }
    }
}
