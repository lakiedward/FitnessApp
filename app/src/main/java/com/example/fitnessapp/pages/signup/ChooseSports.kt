package com.example.fitnessapp.pages.signup

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fitnessapp.AuthViewModel
import com.example.fitnessapp.R
import com.example.fitnessapp.model.ChoosedSports
import com.example.fitnessapp.model.UserWeekAvailability

@Composable
fun ChooseSportsScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    choosedSports: MutableState<ChoosedSports>
) {
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val selectedDays = remember { mutableStateMapOf<String, Boolean>() }
    val hours = remember { mutableStateMapOf<String, String>() }
    val minutes = remember { mutableStateMapOf<String, String>() }
    val selectedSports = remember { mutableStateMapOf<String, Boolean>() }

    days.forEach { day ->
        selectedDays.putIfAbsent(day, false)
        hours.putIfAbsent(day, "")
        minutes.putIfAbsent(day, "")
    }

    val sports = listOf("Cycling", "Running", "Swimming")
    sports.forEach { sport ->
        selectedSports.putIfAbsent(sport, false)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = { navController.navigateUp() },
                modifier = Modifier.padding(16.dp)
            ) {
                Image(painter = painterResource(id = R.drawable.back), contentDescription = "Back")
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .fillMaxWidth()
            ) {
                Column {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Choose Your Sports",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SportsIcon(R.drawable.cycling, "Cycling") { selectedSports["Cycling"] = it }
                        SportsIcon(R.drawable.running, "Running") { selectedSports["Running"] = it }
                        SportsIcon(R.drawable.swimming, "Swimming") { selectedSports["Swimming"] = it }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Choose Your Availability",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(days) { day ->
                    AvailabilityRow(
                        day = day,
                        isSelected = selectedDays[day] ?: false,
                        onDaySelected = { selectedDays[day] = !(selectedDays[day] ?: false) },
                        hourValue = hours[day] ?: "",
                        onHourChange = { hours[day] = it },
                        minuteValue = minutes[day] ?: "",
                        onMinuteChange = { minutes[day] = it },
                        isInputEnabled = selectedDays[day] == true
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedArrow(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(
                    onClick = {
                        val availabilityList = days.mapNotNull { day ->
                            if (selectedDays[day] == true) {
                                UserWeekAvailability(
                                    day = day,
                                    hours = hours[day]?.toIntOrNull() ?: 0,
                                    minutes = minutes[day]?.toIntOrNull() ?: 0
                                )
                            } else null
                        }
                        val selectedSportsList = selectedSports.filter { it.value }.keys.toList()
                        choosedSports.value = ChoosedSports(selectedSportsList)
                        authViewModel.addWeekAvailability(availabilityList)
                        if (choosedSports.value.cycling) navController.navigate("cycling_data_insert")
                        else if (choosedSports.value.running) navController.navigate("running_data_insert")
                    },
                    enabled = selectedSports.values.any { it } && selectedDays.values.any { it },
                    modifier = Modifier
                        .padding(16.dp)
                        .width(200.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedSports.values.any { it } && selectedDays.values.any { it }) Color.Black else Color.Gray,
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "Continue")
                }
            }
        }
    }
}

@Composable
fun AnimatedArrow(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "arrow_animation")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "offsetY"
    )

    Icon(
        painter = painterResource(id = R.drawable.ic_down_arrow),
        contentDescription = "Scroll down",
        tint = Color.Gray,
        modifier = modifier
            .padding(bottom = 12.dp)
            .size(24.dp)
            .offset(y = offsetY.dp)
    )
}

@Composable
fun SportsIcon(iconRes: Int, contentDescription: String, onSelectionChanged: (Boolean) -> Unit) {
    var isSelected by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(100.dp)
            .clickable {
                isSelected = !isSelected
                onSelectionChanged(isSelected)
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
            RadioButton(selected = isSelected, onClick = onDaySelected)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = day, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = hourValue,
                onValueChange = onHourChange,
                label = { Text("hours") },
                modifier = Modifier.width(80.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = isInputEnabled
            )
            OutlinedTextField(
                value = minuteValue,
                onValueChange = onMinuteChange,
                label = { Text("min") },
                modifier = Modifier.width(80.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                enabled = isInputEnabled
            )
        }
    }
}