package com.example.fitnessapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
//import androidx.navigation.NavHostController
import com.example.fitnessapp.ui.theme.FitnessAppTheme


class AddAge : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitnessAppTheme {
                //GenderSelectionScreen()
                val navController = rememberNavController()
                AppNavigationAddAge(navController)
            }
        }
    }
}

@Composable
fun AppNavigationAddAge(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "add_age_screen") {
        composable("gender_selection_screen") {
            GenderSelectionScreen(navController)
        }
        composable("add_age_screen") {
            AddAgeScreen(navController)
        }
        composable("physical_activity_level_screen") {
            PhysicalActivityLevelScreen(navController)
        }
        composable("choose_sports") {
            ChooseSportsScreen(navController)
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAgeScreen(navController: NavHostController) {
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
                onClick = {
                    navController.navigateUp()
                },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart) // Proper usage of align within Box
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
                    .padding(horizontal = 16.dp, vertical = 32.dp)
                    .align(Alignment.TopCenter), // Align the content to the top center
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Age Input Section
                SectionTitle(title = "How Old Are You?")
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Enter Age", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier
                        .width(259.dp)
                        .height(56.dp), // Adjusted height for better usability
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

                // Weight Input Section
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

                // Height Input Section
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
                    // Navigate to the next screen
                    navController.navigate("physical_activity_level_screen")
                },
                enabled = isFormComplete, // Enable only if all fields are filled
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
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(vertical = 16.dp)
    )
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
