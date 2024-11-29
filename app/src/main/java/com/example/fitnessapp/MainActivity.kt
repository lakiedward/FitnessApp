package com.example.fitnessapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavHostController
import com.example.fitnessapp.ui.theme.FitnessAppTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitnessAppTheme {
                val navController = rememberNavController()
                AppNavigation(navController)
                //LoginScreen()
            }
        }
    }
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "login_screen") {
        composable("login_screen") {
            LoginScreen(navController)
        }
        composable("gender_selection_screen") {
            GenderSelectionScreen(navController)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
                onClick = { /* Handle login logic */ },
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
            TextButton(onClick = { /* Navigate to Forgot Password */ }) {
                Text("Forgot password?")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // OR Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.weight(1f),
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.primary)

                Text(
                    text = "OR",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                Divider(modifier = Modifier.weight(1f),
                    thickness = 2.dp,
                    color = MaterialTheme.colorScheme.primary)
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
                        navController.navigate("gender_selection_screen")
                    },

                ) {
                    Text("Sign in", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary)
                }

            }
        }
    }
}
