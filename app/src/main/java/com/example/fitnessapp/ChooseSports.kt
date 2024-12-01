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

class ChooseSports : ComponentActivity() {
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
fun AppNavigationChooseSports(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "choose_sports") {
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
        composable("cycling_data_insert") {
            CyclingDataInsertScreen(navController)
        }

    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseSportsScreen(navController: NavHostController) {
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val selectedDays = remember { mutableStateMapOf<String, Boolean>() }
    val hours = remember { mutableStateMapOf<String, String>() }
    val minutes = remember { mutableStateMapOf<String, String>() }
    var isSportSelected by remember { mutableStateOf(false) }

    // Initialize the states for all days
    days.forEach { day ->
        if (selectedDays[day] == null) {
            selectedDays[day] = false
            hours[day] = ""
            minutes[day] = ""
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                Text(
                    text = "Choose Your Sports",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SportsIcon(R.drawable.cycling, "Cycling") { isSelected ->
                        isSportSelected = isSelected
                    }
                    SportsIcon(R.drawable.running, "Running") { isSelected ->
                        isSportSelected = isSelected
                    }
                    SportsIcon(R.drawable.swimming, "Swimming") { isSelected ->
                        isSportSelected = isSelected
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Choose Your Availability",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    days.forEach { day ->
                        AvailabilityRow(
                            day = day,
                            isSelected = selectedDays[day] ?: false,
                            onDaySelected = { selectedDays[day] = !selectedDays[day]!! },
                            hourValue = hours[day] ?: "",
                            onHourChange = { hours[day] = it },
                            minuteValue = minutes[day] ?: "",
                            onMinuteChange = { minutes[day] = it },
                            isInputEnabled = selectedDays[day] == true
                        )
                    }
                }
            }

            Button(
                onClick = {
                    navController.navigate("cycling_data_insert")
                },
                enabled = selectedDays.values.any { it } && isSportSelected, // Check both conditions
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(0.6f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedDays.values.any { it } && isSportSelected) Color.Black else Color.Gray,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Continue")
            }
        }
    }
}


@Composable
fun SportsIcon(iconRes: Int, contentDescription: String, onSelectionChanged: (Boolean) -> Unit) {
    var isSelected by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(100.dp)
            .clickable {
                isSelected = !isSelected
                onSelectionChanged(isSelected) // Notify the parent of the selection change
            }
            .background(
                color = if (isSelected) Color.Black else Color.Gray,
                shape = CircleShape
            )
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(if (isSelected) Color.White else Color.LightGray),
            modifier = Modifier.fillMaxSize()
        )
    }
}






@OptIn(ExperimentalMaterial3Api::class)
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onDaySelected() }
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onDaySelected
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = day,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = hourValue,
                onValueChange = onHourChange,
                label = { Text("hours") },
                modifier = Modifier.width(80.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = isInputEnabled // Enable only if the day is selected
            )
            OutlinedTextField(
                value = minuteValue,
                onValueChange = onMinuteChange,
                label = { Text("min") },
                modifier = Modifier.width(80.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = isInputEnabled // Enable only if the day is selected
            )
        }
    }
}
