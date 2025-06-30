package com.example.fitnessapp.pages.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.model.UserDetalis
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.fitnessapp.mock.SharedPreferencesMock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseDisciplineScreen(navController: NavHostController, authViewModel: AuthViewModel, userDetalis: MutableState<UserDetalis>) {

    var selectedRaceType by remember { mutableStateOf("") }
    val raceTypes = listOf(
        "Road Cycling",
        "Mountain bike",
        "Cyclocross",
        "Gravel",
        "Triathlon",
        "Duathlon",
        "Swimming",
        "10 Km Running",
        "5 Km Running"
    )

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
                    // TODO: Update to Icons.AutoMirrored.Filled.ArrowBack when available
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            Text(
                text = "Training Goal",
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
                    text = "What Are You Training For?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(24.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(raceTypes) { raceType ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRaceType = raceType }
                                .border(
                                    width = if (selectedRaceType == raceType) 2.dp else 1.dp,
                                    color = if (selectedRaceType == raceType) Color(0xFF6366F1) else Color.Gray.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedRaceType == raceType) 
                                    Color(0xFF6366F1).copy(alpha = 0.1f) 
                                else 
                                    Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                        modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = raceType,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (selectedRaceType == raceType) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selectedRaceType == raceType) Color(0xFF6366F1) else Color.Black
                                )
                                
                                if (selectedRaceType == raceType) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

            // Continue Button
            Button(
                onClick = {
                    userDetalis.value = userDetalis.value.copy(discipline = selectedRaceType)
                    authViewModel.addUserDetails(
                        userDetalis.value.varsta,
                        userDetalis.value.inaltime,
                        userDetalis.value.greutate,
                        userDetalis.value.gender,
                        userDetalis.value.discipline
                    )
                    // Navigate to loading screen
                    navController.navigate("discipline_loading_screen")
                },
                enabled = selectedRaceType.isNotBlank(),
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

@Preview(showBackground = true)
@Composable
fun ChooseDisciplineScreenPreview() {
    ChooseDisciplineScreen(
        navController = rememberNavController(),
        authViewModel = AuthViewModel(SharedPreferencesMock()),
        userDetalis = remember { mutableStateOf(UserDetalis(0, 0f, 0f, "", "")) }
    )
}

