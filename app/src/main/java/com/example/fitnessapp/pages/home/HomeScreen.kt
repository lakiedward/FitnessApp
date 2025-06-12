package com.example.fitnessapp.pages.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.viewmodel.StravaViewModel
import com.example.fitnessapp.viewmodel.StravaViewModelFactory
import com.example.fitnessapp.viewmodel.HomeViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import androidx.compose.ui.platform.LocalContext

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(navController: NavController, authViewModel: AuthViewModel, stravaViewModel: StravaViewModel) {
    val today = LocalDate.now().toString()
    val trainingPlans by authViewModel.trainingPlan.observeAsState(emptyList())
    val todayWorkout = trainingPlans.find { it.date == today }
    val userTrainingData by authViewModel.userTrainingData.observeAsState()
    val homeViewModel: HomeViewModel = viewModel()

    // Sync data when screen is first displayed
    LaunchedEffect(Unit) {
        authViewModel.getTrainingPlans()
        authViewModel.getRaces()
        authViewModel.getUserTrainingData()
        stravaViewModel.syncCheck()
        
        // Fetch FTP estimate
        val token = authViewModel.getToken()
        if (!token.isNullOrEmpty()) {
            homeViewModel.fetchFTPEstimate(token)
        }
    }

    val ftpEstimate by homeViewModel.ftpEstimate.observeAsState()
    val isLoading by homeViewModel.isLoading.observeAsState()
    val error by homeViewModel.error.observeAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(Color.Black)
            )
            // Top Bar
            TopBar("Home", navController)

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
                        PerformanceCard(
                            title = "FTP",
                            value = if (isLoading == true) "..." else ftpEstimate?.estimatedFTP?.toInt()?.toString() ?: userTrainingData?.ftp?.toString() ?: "-",
                            unit = "W",
                            color = Color.Yellow
                        )
                    }
                }

                // Display FTP estimate details if available
                ftpEstimate?.let { ftp ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "FTP Estimate Details",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Confidence",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "${(ftp.confidence * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.Black
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Method",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = ftp.method,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.Black
                                    )
                                }
                            }
                            
                            ftp.fthrValue?.let { fthr ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "FTHR: ${fthr.toInt()} bpm",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            
                            ftp.notes?.let { notes ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }

                // Display error if any
                error?.let { errorMessage ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Error loading FTP: $errorMessage",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Red
                            )
                            IconButton(onClick = { homeViewModel.clearError() }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color.Red
                                )
                            }
                        }
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
            .height(120.dp)
            .border(1.dp, Color.LightGray, shape = RoundedCornerShape(16.dp)),
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
fun TopBar(
    titlu: String,
    navController: NavController,
    showBackButton: Boolean = false,
    showProfileIcon: Boolean = true,
    showNotificationIcon: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(vertical = 8.dp)
    ) {
        // Back button or Profile Icon on the left
        if (showBackButton) {
            IconButton(
                onClick = {
                    navController.popBackStack()
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else if (showProfileIcon) {
            IconButton(
                onClick = {
                    navController.navigate("workout_screen")
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.iclogo),
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Gray, CircleShape)
                )
            }
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
        if (showNotificationIcon) {
            IconButton(
                onClick = {
                    navController.navigate("workout_screen")
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_record_white_no_text),
                    contentDescription = "Notifications",
                    modifier = Modifier.size(64.dp)
                )
            }
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

