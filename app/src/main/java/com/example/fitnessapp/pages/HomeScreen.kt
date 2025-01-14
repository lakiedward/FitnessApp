package com.example.fitnessapp.pages

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fitnessapp.R
import com.example.fitnessapp.AuthViewModel
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(navController: NavController, authViewModel: AuthViewModel) {
    val today = LocalDate.now().toString()
    val trainingPlans by authViewModel.trainingPlan.observeAsState(emptyList())
    val todayWorkout = trainingPlans.find { it.date == today }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Top Bar
            TopBar("Home")

            Spacer(modifier = Modifier.height(16.dp))

            // Content with Padding
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                // Upcoming Section
                Text(
                    text = "Upcoming",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0FF))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Workout",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (todayWorkout != null) {
                                Text(
                                    text = todayWorkout.workout_name,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black)
                                )
                                Text(
                                    text = todayWorkout.description,
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                                    maxLines = 2
                                )
                            } else {
                                Text(
                                    text = "No workout planned for today",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                )
                            }
                        }
                        Image(
                            painter = painterResource(id = R.drawable.runningilustration), // Replace with your workout image
                            contentDescription = "Workout Image",
                            modifier = Modifier.size(100.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Performance Section
                Text(
                    text = "Performance",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Cards with spacing
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PerformanceCard(title = "HRV", value = "38", unit = "bpm", color = Color.Red)
                        PerformanceCard(title = "Sleep", value = "8:50", unit = "Hours", color = Color.Blue)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PerformanceCard(title = "Fitness score", value = "46", unit = "", color = Color.Green)
                        PerformanceCard(title = "Calories", value = "456", unit = "Kcal", color = Color.Yellow)
                    }
                }
            }

            // Bottom Navigation Bar
            BottomNavigationBar(navController)
        }
    }
}

// Other components remain unchanged (TopBar, PerformanceCard, BottomNavigationBar, etc.)



@Composable
fun PerformanceCard(title: String, value: String, unit: String, color: Color) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0FF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
            Text(
                text = "$value $unit",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
fun TopBar(titlu: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(vertical = 8.dp)
    ) {
        // Profile Icon on the left
        IconButton(
            onClick = { /* Profile action */ },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo), // Replace with your profile icon
                contentDescription = "Profile",
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Gray, CircleShape)
            )
        }

        // Title in the center
        Text(
            text = titlu,
            color = Color.White,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.align(Alignment.Center)
        )

        // Notifications Icon on the right
        IconButton(
            onClick = { /* Notifications action */ },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.bell), // Replace with your notification icon
                contentDescription = "Notifications",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(vertical = 12.dp), // Adjust padding as needed
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomBarItem(
            navController,
            "home_screen",
            R.drawable.ic_home,
            "home"
        )
        BottomBarItem(
            navController,
            "calendar_screen",
            R.drawable.ic_calendar,
            "calendar"
        )
        BottomBarItem(
            navController,
            "season_screen",
            R.drawable.ic_season,
            "Season"
        )
        BottomBarItem(
            navController,
            "more",
            R.drawable.ic_more,
            "More"
        )
    }
}

@Composable
fun BottomBarItem(
    navController: NavController,
    route: String,
    iconId: Int,
    description: String
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp) // Add padding around each item
            .size(56.dp) // Ensure uniform size for each item
            .background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = { navController.navigate(route) },
            modifier = Modifier.size(40.dp) // Adjust icon size as needed
        ) {
            Image(
                painter = painterResource(id = iconId),
                contentDescription = description,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(Color.White)
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = Color.White
        )
    }
}

//@Preview(showBackground = true)
//@Composable
//fun HomeScreenPreview() {
//    val mockNavController = rememberNavController()
//    HomeScreen(navController = mockNavController)
//}

