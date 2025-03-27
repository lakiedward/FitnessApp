package com.example.fitnessapp.pages.signup

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fitnessapp.AuthViewModel
import com.example.fitnessapp.R
import com.example.fitnessapp.model.ChoosedSports
import com.example.fitnessapp.ui.theme.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunningDataInsertScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    choosedSports: MutableState<ChoosedSports>
) {
    var runningRaceTime5Km by remember { mutableStateOf("") }
    var runningRaceTime10Km by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Back Button (rămâne în stânga sus)
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

            // Conținutul centrat (imagine și câmpuri de text)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .align(Alignment.Center), // Centrează vertical și orizontal
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.running),
                    contentDescription = "RunningIcon",
                    modifier = Modifier
                        .size(100.dp)
                        .padding(bottom = 16.dp)
                )

                SectionTitle(title = "Enter Best Race Time For 5Km")
                OutlinedTextField(
                    value = runningRaceTime5Km,
                    onValueChange = { runningRaceTime5Km = it },
                    label = { Text("Enter Best Race Time For 5Km", style = MaterialTheme.typography.bodySmall) },
                    placeholder = { Text("hh:mm", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier
                        .width(259.dp)
                        .height(56.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                Spacer(modifier = Modifier.height(16.dp)) // Spațiu între câmpuri

                SectionTitle(title = "Enter Best Race Time For 10Km")
                OutlinedTextField(
                    value = runningRaceTime10Km,
                    onValueChange = { runningRaceTime10Km = it },
                    label = { Text("Enter Best Race Time For 10Km", style = MaterialTheme.typography.bodySmall) }, // Corectat label
                    placeholder = { Text("hh:mm", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier
                        .width(259.dp)
                        .height(56.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
            }

            // Butonul Continue (centrat orizontal, jos)
            Button(
                onClick = {
                    val runningRaceTime5KmInt = runningRaceTime5Km.toIntOrNull()
                    val runningRaceTime10KmInt = runningRaceTime10Km.toIntOrNull()

                    if (runningRaceTime5KmInt != null || runningRaceTime10KmInt != null) {
                        navController.navigate("plan_length_screen")
                    } else {
                        Toast.makeText(
                            navController.context,
                            "Please enter valid numbers for race times.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                enabled = runningRaceTime5Km.isNotEmpty() || runningRaceTime10Km.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomCenter) // Centrat orizontal, jos
                    .padding(16.dp)
                    .fillMaxWidth(0.6f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (runningRaceTime5Km.isNotEmpty() || runningRaceTime10Km.isNotEmpty()) Color.Black else Color.Gray,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Continue")
            }
        }
    }
}