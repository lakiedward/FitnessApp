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
import com.example.fitnessapp.ui.theme.SectionTitle


class CyclingDataInsert : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitnessAppTheme {
                val navController = rememberNavController()
                AppNavigationAddAge(navController)
            }
        }
    }
}

@Composable
fun AppNavigationCyclingDataInsert(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "choose_sports") {
//        composable("gender_selection_screen") {
//            GenderSelectionScreen(navController)
//        }
//        composable("add_age_screen") {
//            AddAgeScreen(navController)
//        }
//        composable("physical_activity_level_screen") {
//            PhysicalActivityLevelScreen(navController)
//        }
        composable("choose_sports") {
            ChooseSportsScreen(navController)
        }
//        composable("cycling_data_insert") {
//            CyclingDataInsertScreen(navController)
//        }
        composable("enter_race_day") {
            EnterRaceDayScreen(navController)
        }

    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyclingDataInsertScreen(navController: NavHostController) {
    var cyclingFtp by remember { mutableStateOf("") }
    var maxBpm by remember { mutableStateOf("") }
    var selectedRaceType by remember { mutableStateOf("") }
    val raceTypes = listOf("XCM", "XCO", "Cyclocross", "Gravel", "TT")

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
                    contentDescription = "Back"
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cycling Icon
                Image(
                    painter = painterResource(id = R.drawable.cycling), // Replace with your cycling icon resource
                    contentDescription = "Cycling Icon",
                    modifier = Modifier
                        .size(100.dp)
                        .padding(bottom = 16.dp)
                )
                SectionTitle(title = "Enter Cycling Ftp")
                OutlinedTextField(
                    value = cyclingFtp,
                    onValueChange = { cyclingFtp = it },
                    label = { Text("Enter Cycling Ftp", style = MaterialTheme.typography.bodySmall) },
                    placeholder = { Text("Watts" , style = MaterialTheme.typography.bodySmall) },
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

                SectionTitle(title = "Enter Maximum Bpm")

                OutlinedTextField(
                    value = maxBpm,
                    onValueChange = { maxBpm = it },
                    label = { Text("Enter Maximum Bpm", style = MaterialTheme.typography.bodySmall) },
                    placeholder = { Text("Bpm", style = MaterialTheme.typography.bodySmall) },
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


                SectionTitle(title = "Select What Type Of Race You Are Training For")

                // Dropdown for Race Type
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded } // Toggle dropdown visibility
                ) {
                    OutlinedTextField(
                        value = selectedRaceType,
                        onValueChange = {},
                        label = { Text("Select What Type Of Race You Are Training For", style = MaterialTheme.typography.bodySmall) },
                        //placeholder = { Text("Select Race Type", style = MaterialTheme.typography.bodySmall) },
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .width(259.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(34.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary, // Border when focused
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), // Border when unfocused
                            focusedLabelColor = MaterialTheme.colorScheme.primary, // Label when focused
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), // Label when unfocused
                            focusedTextColor = MaterialTheme.colorScheme.onSurface, // Ensure focused text is visible
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface // Ensure unfocused text is visible

                        )// Required for proper anchoring
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




                Spacer(modifier = Modifier.weight(1f))

                // Continue Button
                Button(
                    onClick = {
                        // Navigate to the next screen
                        navController.navigate("enter_race_day")
                    },
                    enabled = cyclingFtp.isNotEmpty() && maxBpm.isNotEmpty() && selectedRaceType.isNotEmpty(),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(0.6f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (cyclingFtp.isNotEmpty() && maxBpm.isNotEmpty() && selectedRaceType.isNotEmpty()) Color.Black else Color.Gray,
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "Continue")
                }
            }
        }
    }
}


