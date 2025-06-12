package com.example.fitnessapp.pages.signup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState

import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fitnessapp.viewmodel.AuthState
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.R
import com.example.fitnessapp.model.UserDetalis
import com.example.fitnessapp.ui.theme.SectionTitle

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

    val authState by authViewModel.authState.observeAsState()
    var navigateNext by remember { mutableStateOf(false) }
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            navigateNext = true
        }
    }

    LaunchedEffect(navigateNext) {
        if (navigateNext) {
            navController.navigate("choose_sports")
            authViewModel.resetAuthState()
            navigateNext = false
        }
    }




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
                SectionTitle(title = "Select For What Are Training For")

                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded } // Toggle dropdown visibility
                ) {
                    OutlinedTextField(
                        value = selectedRaceType,
                        onValueChange = {},
                        // label = { Text("", style = MaterialTheme.typography.bodySmall) },
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
                    authViewModel.addUserDetails(
                        userDetalis.value.varsta,
                        userDetalis.value.inaltime,
                        userDetalis.value.greutate,
                        userDetalis.value.gender,
                        userDetalis.value.discipline
                    )
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

            if (authState is AuthState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Saving your preferences...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

        }
    }
}

