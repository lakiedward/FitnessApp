package com.example.fitnessapp.pages.signup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fitnessapp.viewmodel.AuthState
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.R
import android.util.Patterns

// Email validation function
private fun isValidEmail(email: String): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

@Composable
fun AddEmailScreen(navController: NavHostController, authViewModel: AuthViewModel) {

    val authState by authViewModel.authState.observeAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            navController.navigate("gender_selection_screen")
            authViewModel.resetAuthState()
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
                    .align(Alignment.TopStart)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.back),
                    contentDescription = "Back"
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Sign up with Email",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Input Fields with Icons
                var name by remember { mutableStateOf("") }
                var email by remember { mutableStateOf("") }
                var password by remember { mutableStateOf("") }
                var confirmPassword by remember { mutableStateOf("") }
                var passwordVisible by remember { mutableStateOf(false) }
                var confirmPasswordVisible by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf<String?>(null) } // For error message

                // Name Field with User Icon
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Name") },
                    modifier = Modifier.width(330.dp)
                        .height(66.dp),
                    shape = RoundedCornerShape(34.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.user),
                            contentDescription = "User Icon",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email Field with User Icon
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Your Email") },
                    modifier = Modifier.width(330.dp)
                        .height(66.dp),
                    shape = RoundedCornerShape(34.dp),
                    singleLine = true,
                    isError = email.isNotEmpty() && !isValidEmail(email),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.user),
                            contentDescription = "Email Icon",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                )
                
                // Show email validation error
                if (email.isNotEmpty() && !isValidEmail(email)) {
                    Text(
                        text = "Please enter a valid email address",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field with Toggleable Eye Icon
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.width(330.dp)
                        .height(66.dp),
                    shape = RoundedCornerShape(34.dp),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.password),
                            contentDescription = "Password Icon",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                painter = painterResource(id = R.drawable.eye),
                                contentDescription = if (passwordVisible) "Hide Password" else "Show Password",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Password Field with Toggleable Eye Icon
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    modifier = Modifier.width(330.dp)
                        .height(66.dp),
                    shape = RoundedCornerShape(34.dp),
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.password),
                            contentDescription = "Password Icon",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                painter = painterResource(id = R.drawable.eye),
                                contentDescription = if (confirmPasswordVisible) "Hide Password" else "Show Password",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )

                // Display error message if passwords do not match
                if (!errorMessage.isNullOrEmpty()) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Continue Button
                Button(
                    onClick = {
                        // Reset error message
                        errorMessage = null
                        
                        // Validate email format
                        if (!isValidEmail(email)) {
                            errorMessage = "Please enter a valid email address"
                            return@Button
                        }
                        
                        // Validate password match
                        if (password != confirmPassword) {
                            errorMessage = "Passwords do not match"
                            return@Button
                        }
                        
                        // Validate required fields
                        if (name.isBlank()) {
                            errorMessage = "Please enter your name"
                            return@Button
                        }
                        
                        if (password.isBlank()) {
                            errorMessage = "Please enter a password"
                            return@Button
                        }
                        
                        // All validations passed, proceed with signup
                        authViewModel.signup(email, password)
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "Create an Account")
                }
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
                            text = "Creating your account...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}



