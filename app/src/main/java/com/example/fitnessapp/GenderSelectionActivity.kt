package com.example.fitnessapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
//import androidx.navigation.NavHostController
import com.example.fitnessapp.ui.theme.FitnessAppTheme

class GenderSelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitnessAppTheme {
                val navController = rememberNavController()
                AppNavigationGenderSelectionActivity(navController)
            }
        }
    }
}

@Composable
fun AppNavigationGenderSelectionActivity(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "login_screen") {
        composable("login_screen") {
            LoginScreen(navController)
        }
        composable("gender_selection_screen") {
            GenderSelectionScreen(navController)
        }
        composable("add_age_screen") {
            AddAgeScreen(navController)
        }
    }
}


@Composable
fun GenderSelectionScreen(navController: NavHostController) {
    var selectedGender by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()
    ) {
        IconButton(
            onClick = { navController.navigateUp()
            },
            modifier = Modifier
                //.padding(16.dp)
                .align(Alignment.TopStart)
        ) {
           // Text(text = "Back", color = Color.Black)
            Image(
                painter = painterResource(id = R.drawable.back),
                contentDescription = "Back",
            )

        }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "What's Your Gender",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Male Selection
                GenderOption(
                    icon = R.drawable.male,
                    label = "Male",
                    isSelected = selectedGender == "Male",
                    onClick = { selectedGender = "Male" }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Female Selection
                GenderOption(
                    icon = R.drawable.female,
                    label = "Female",
                    isSelected = selectedGender == "Female",
                    onClick = { selectedGender = "Female" }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Continue Button
                Button(
                    onClick = {
                        navController.navigate("add_age_screen")
                    },
                    modifier = Modifier
                        .width(178.dp)
                        .height(44.dp),
                    enabled = selectedGender != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedGender != null) Color.Black else Color.Gray,
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "Continue")
                }
            }

    }

}

@Composable
fun GenderOption(icon: Int, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(162.dp)
            .clickable { onClick() },
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color.Black else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // Ensures centering
        ) {
            // Icon (e.g., male or female)
            Image(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(78.dp) // Adjust size as needed
            )
            Spacer(modifier = Modifier.height(8.dp)) // Space between icon and label
            // Label (e.g., "Male" or "Female")
            Text(
                text = label,
                color = if (isSelected) Color.White else Color.Black,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

