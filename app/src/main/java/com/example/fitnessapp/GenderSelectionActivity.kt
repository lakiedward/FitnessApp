package com.example.fitnessapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnessapp.ui.theme.FitnessAppTheme

class GenderSelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitnessAppTheme {
                GenderSelectionScreen()
            }
        }
    }
}

@Composable
fun GenderSelectionScreen() {
    var selectedGender by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
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
                    // Handle Continue Logic
                },
                modifier = Modifier.fillMaxWidth(),
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
            .size(120.dp)
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
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = label,
                color = if (isSelected) Color.White else Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}