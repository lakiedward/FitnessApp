package com.example.fitnessapp.pages

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.fitnessapp.AuthViewModel
import com.example.fitnessapp.R
//import androidx.navigation.NavHostController
import com.example.fitnessapp.ui.theme.FitnessAppTheme

class PhysicalActivityLevel : ComponentActivity() {
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
fun PhysicalActivityLevelScreen(navController: NavHostController, authViewModel: AuthViewModel, ) {
    var selectedLevel by remember { mutableStateOf<String?>(null) }
    val authState = authViewModel.authState.observeAsState()

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
                    text = "Physical Activity Level",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // Options
                ActivityLevelButton("Beginner", selectedLevel == "Beginner") {
                    selectedLevel = "Beginner"
                }
                Spacer(modifier = Modifier.height(16.dp))

                ActivityLevelButton("Intermediate", selectedLevel == "Intermediate") {
                    selectedLevel = "Intermediate"
                }
                Spacer(modifier = Modifier.height(16.dp))

                ActivityLevelButton("Advance", selectedLevel == "Advance") {
                    selectedLevel = "Advance"
                }
            }
            Button(
                onClick = {
                    // Navigate to the next screen
                    if (selectedLevel != null) {
                        //authViewModel.updateFitnessLevel(selectedLevel ?: "")
                        navController.navigate("choose_sports")
                    }
                },
                enabled = selectedLevel != null, // Enable only if a level is selected
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(0.6f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedLevel != null) Color.Black else Color.Gray,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Continue")
            }



        }
    }
}

@Composable
fun ActivityLevelButton(level: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color.Black else Color.Gray,
            contentColor = Color.White
        )
    ) {
        Text(text = level, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}
