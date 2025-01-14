package com.example.fitnessapp.pages

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fitnessapp.AuthViewModel
import com.example.fitnessapp.R
import com.example.fitnessapp.model.UserDetalis
import com.example.fitnessapp.model.UserWeekAvailability
//import androidx.navigation.NavHostController
import com.example.fitnessapp.ui.theme.FitnessAppTheme

class ChooseDiscipline : ComponentActivity() {
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
fun ChooseDisciplineScreen(navController: NavHostController, authViewModel: AuthViewModel, userDetalis: MutableState<UserDetalis>) {

    var selectedRaceType by remember { mutableStateOf("") }
    val raceTypes = listOf("Road Cycling", "Mountain bike", "Cyclocross", "Gravel", "Triathlon", "Duathlon")

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
                    .align(Alignment.TopStart) // Align Back button to top start
            ) {
                Image(
                    painter = painterResource(id = R.drawable.back),
                    contentDescription = "Back"
                )
            }

            // Main Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .align(Alignment.Center), // Align the content to the center
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Title
                Text(
                    text = "Choose the discipline you want to train",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded } // Toggle dropdown visibility
                ) {
                    OutlinedTextField(
                        value = selectedRaceType,
                        onValueChange = {},
                        label = { Text("Select What Type Of Race You Are Training For", style = MaterialTheme.typography.bodySmall) },
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .width(259.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(34.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        raceTypes.forEach { raceType ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedRaceType = raceType
                                    expanded = false
                                },
                                text = { Text(raceType) }
                            )
                        }
                    }
                }
            }

            // Continue Button
            Button(
                onClick = {
                    userDetalis.value = userDetalis.value.copy(discipline = selectedRaceType)
                    authViewModel.addUserDetails(userDetalis.value.varsta, userDetalis.value.inaltime, userDetalis.value.greutate, userDetalis.value.fitnessLevel, userDetalis.value.gender, userDetalis.value.discipline)
                    navController.navigate("choose_sports")
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(0.6f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Continue")
            }
        }
    }
}

