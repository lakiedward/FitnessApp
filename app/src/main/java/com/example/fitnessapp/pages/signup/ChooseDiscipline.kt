package com.example.fitnessapp.pages.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.example.fitnessapp.ui.theme.extendedColors
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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

    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = MaterialTheme.extendedColors
    val gradientContentColor = if (isSystemInDarkTheme()) colorScheme.onSurface else colorScheme.onPrimary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        extendedColors.gradientPrimary,
                        extendedColors.gradientSecondary,
                        extendedColors.gradientAccent
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
                    tint = gradientContentColor
                )
            }
            
            Text(
                text = "Training Goal",
                style = MaterialTheme.typography.headlineSmall,
                color = gradientContentColor,
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
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
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
                    color = colorScheme.onSurface
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
                                    color = if (selectedRaceType == raceType) colorScheme.primary else colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedRaceType == raceType) 
                                    colorScheme.primary.copy(alpha = 0.1f) 
                                else 
                                    colorScheme.onPrimary
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
                                    color = if (selectedRaceType == raceType) colorScheme.primary else colorScheme.onSurface
                                )
                                
                                if (selectedRaceType == raceType) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = colorScheme.primary,
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
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                    disabledContainerColor = colorScheme.surfaceVariant
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


