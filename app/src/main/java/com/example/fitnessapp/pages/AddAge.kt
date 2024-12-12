package com.example.fitnessapp.pages

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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fitnessapp.AuthViewModel
import com.example.fitnessapp.R
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
fun AddAgeScreen(navController: NavHostController, authViewModel: AuthViewModel) {
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weightUnit by remember { mutableStateOf("Kg") }
    var heightUnit by remember { mutableStateOf("Cm") }
    val authState = authViewModel.authState.observeAsState()

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
                    shape = RoundedCornerShape(34.dp)
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
                    shape = RoundedCornerShape(34.dp)
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
                    shape = RoundedCornerShape(34.dp)
                )
            }

            // Continue Button
            Button(
                onClick = {
                    val convertedWeight = if (weightUnit == "Lb") weight.toDouble() * 0.453592 else weight.toDouble()
                    val convertedHeight = if (heightUnit == "Ft") height.toDouble() * 30.48 else height.toDouble()

                    authViewModel.updatePhysicalAttributes(
                        age = age.toInt(),
                        weight = convertedWeight,
                        height = convertedHeight
                    )
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
