package com.example.fitnessapp.pages.signup


import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.R
import com.example.fitnessapp.model.ChoosedSports
import com.example.fitnessapp.ui.theme.SectionTitle




@Composable
fun PlanLengthScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    choosedSports: MutableState<ChoosedSports>
) {
    var selectedRaceType by remember { mutableStateOf("") } // Neutilizat în codul actual
    var raceDate by remember { mutableStateOf("") }

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

            // Conținutul centrat (titlu și DatePicker)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .align(Alignment.Center), // Centrează vertical și orizontal
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SectionTitle(title = "Enter Race Day")
                DatePickerFieldToModal(onDateSelected = { selectedDate ->
                    raceDate = selectedDate
                })
            }

            // Butonul Continue (centrat orizontal, jos)
            Button(
                onClick = {
                    Handler(Looper.getMainLooper()).postDelayed({
                        authViewModel.generateTrainingPlanBySport(raceDate)
                        navController.navigate("loading_screen")
                    }, 1000)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter) // Centrat orizontal, jos
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
        }
    }
}