package com.example.fitnessapp.pages

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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fitnessapp.viewmodel.AuthState
import com.example.fitnessapp.R
import com.example.fitnessapp.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController, authViewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val authState = authViewModel.authState.observeAsState()

    // Handle authentication state
    LaunchedEffect(authState.value) {
        val currentAuthState = authState.value
        when (currentAuthState) {
            is AuthState.Loading -> {
                showLoading = true
                errorMessage = null
            }
            is AuthState.Authenticated -> {
                showLoading = false
                errorMessage = null
                navController.navigate("home_screen") {
                    popUpTo("login_screen") { inclusive = true }
                }
            }
            is AuthState.Error -> {
                showLoading = false
                errorMessage = currentAuthState.message
            }
            else -> {
                showLoading = false
                errorMessage = null
            }
        }
    }

    if (showLoading) {
        // Loading Screen
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
                    painter = painterResource(id = R.drawable.iclogo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(200.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Loading indicator
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Loading text
                Text(
                    text = "Logging in...",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Please wait while we authenticate your account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        // Login Form
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
                    painter = painterResource(id = R.drawable.iclogo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(200.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Error Message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Email Input
                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        errorMessage = null // Clear error when user starts typing
                    },
                    label = { Text("Email", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier
                        .width(259.dp)
                        .height(56.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(34.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Input
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        errorMessage = null // Clear error when user starts typing
                    },
                    label = { Text("Password", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier
                        .width(259.dp)
                        .height(56.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(34.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Login Button
                Button(
                    onClick = {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            authViewModel.login(email, password)
                        } else {
                            errorMessage = "Please enter both email and password"
                        }
                    },
                    enabled = email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier
                        .width(178.dp)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (email.isNotBlank() && password.isNotBlank()) 
                            Color.Black 
                        else 
                            Color.Gray,
                        contentColor = Color.White
                    )
                ) {
                    Text("Log in", style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Forgot Password
                TextButton(onClick = {
                    navController.navigate("enter_forgot_password")
                }) {
                    Text(
                        text = "Forgot password?",
                        color = MaterialTheme.colorScheme.primary,
                    )
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
                    IconButton(
                        onClick = { /* Facebook Login */ },
                        modifier = Modifier.size(50.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.facebooklogo),
                            contentDescription = "Facebook Logo"
                        )
                    }
                    Spacer(modifier = Modifier.width(13.dp))
                    IconButton(
                        onClick = { /* Google Login */ },
                        modifier = Modifier.size(50.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.googlelogo),
                            contentDescription = "Google Logo"
                        )
                    }
                    Spacer(modifier = Modifier.width(13.dp))
                    IconButton(
                        onClick = { /* Apple Login */ },
                        modifier = Modifier.size(50.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.applelogo),
                            contentDescription = "Apple Logo"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sign Up
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Don't have an account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(
                        onClick = {
                            navController.navigate("enter_add_email")
                        }
                    ) {
                        Text(
                            "Sign in",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
} 