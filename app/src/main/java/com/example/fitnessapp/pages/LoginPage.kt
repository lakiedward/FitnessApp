package com.example.fitnessapp.pages

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fitnessapp.R
import com.example.fitnessapp.AuthViewModel
//import com.example.fitnessapp.api.CyclingPlanGenerator



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController, authViewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current

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
            // Logo
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Logo",
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email Input
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier
                    .width(259.dp)
                    .height(56.dp), // Adjusted height for better usability
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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


            Spacer(modifier = Modifier.height(16.dp))

            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier
                    .width(259.dp)
                    .height(56.dp), // Adjusted height for better usability
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Login Button
            Button(
                onClick = {
                    val userData = mapOf(
                        "weight" to 70,
                        "height" to 175,
                        "age" to 30,
                        "fitness_level" to "Intermediate",
                        "training_goal" to "Improve endurance",
                        "race_type" to "Road"
                    )

                    // Apel către API

                    /* Handle login logic */
                },
                modifier = Modifier
                    .width(178.dp)
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black, // Set background color to black
                    contentColor = Color.White   // Set text color to white
                )
            ) {
                Text("Log in", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Forgot Password
            TextButton(onClick = {
                navController.navigate("enter_forgot_password")
                /* Navigate to Forgot Password */ }) {
                Text("Forgot password?")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // OR Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "OR",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Social Media Login
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { /* Facebook Login */ },
                    modifier = Modifier.size(50.dp)) {
                    //Icon(painterResource(id = R.drawable.facebooklogo), contentDescription = "Facebook")
                    Image(
                        painter = painterResource(id = R.drawable.facebooklogo),
                        contentDescription = "Facebook Logo"
                    )
                }
                Spacer(modifier = Modifier.width(13.dp))
                IconButton(onClick = { /* Google Login */ },
                    modifier = Modifier.size(50.dp)) {
                    //Icon(painterResource(id = R.drawable.googlelogo), contentDescription = "Google")
                    Image(
                        painter = painterResource(id = R.drawable.googlelogo),
                        contentDescription = "Google Logo",
                    )
                }
                Spacer(modifier = Modifier.width(13.dp))
                IconButton(onClick = { /* Apple Login */ },
                    modifier = Modifier.size(50.dp)) {
                    //Icon(painterResource(id = R.drawable.applelogo), contentDescription = "Apple")
                    Image(
                        painter = painterResource(id = R.drawable.applelogo),
                        contentDescription = "Apple Logo",
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sign Up
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Don’t have an account?", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary)
                val context = LocalContext.current

                TextButton(
                    onClick = {
                        navController.navigate("enter_add_email")
                    },

                    ) {
                    Text("Sign in", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary)
                }

            }
        }
    }
}