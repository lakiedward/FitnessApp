package com.example.fitnessapp.pages.home

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.example.fitnessapp.ui.theme.extendedColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.text.NumberFormat
import kotlin.math.max
import kotlin.math.roundToInt
import com.example.fitnessapp.navigation.Routes
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fitnessapp.mock.SharedPreferencesMock
import com.example.fitnessapp.viewmodel.AuthViewModel
import com.example.fitnessapp.viewmodel.HealthConnectViewModel
import com.example.fitnessapp.viewmodel.HomeViewModel
import com.example.fitnessapp.viewmodel.StravaViewModel

fun getTodayDate(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        java.time.LocalDate.now().toString()
    } else {
        "2023-01-01"
    }
}

/**
 * Converts decimal hours to hours and minutes format
 * @param decimalHours the decimal hours (e.g., 8.6)
 * @return formatted string like "8h 36m"
 */
fun formatSleepHours(decimalHours: Double): String {
    val hours = decimalHours.toInt()
    val minutes = ((decimalHours - hours) * 60).toInt()
    return "${hours}h ${minutes}m"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    stravaViewModel: StravaViewModel,
    healthConnectViewModel: HealthConnectViewModel
) {
    val today = getTodayDate()
    val trainingPlans by authViewModel.trainingPlan.observeAsState(emptyList())
    val todayWorkout = trainingPlans.find { it.date == today }
    val userTrainingData by authViewModel.userTrainingData.observeAsState()
    val homeViewModel: HomeViewModel = viewModel()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        authViewModel.getTrainingPlans()
        authViewModel.getRaces()
        authViewModel.getUserTrainingData()
        stravaViewModel.syncCheck()
        homeViewModel.fetchSleepData(context)
        homeViewModel.fetchCalorieData(context)
        healthConnectViewModel.fetchDailySteps()
    }

    val ftpEstimate by homeViewModel.ftpEstimate.observeAsState()
    val isLoading by homeViewModel.isLoading.observeAsState()
    val error by homeViewModel.error.observeAsState()
    val sleepHours by homeViewModel.sleepHours.observeAsState(0.0)
    val caloriesBurned by homeViewModel.caloriesBurned.observeAsState(0.0)
    val calorieAllowance by homeViewModel.calorieAllowance.observeAsState(0.0)
    val steps by healthConnectViewModel.totalSteps.collectAsState()

    Scaffold(
        bottomBar = {
            ModernBottomNavigation(navController = navController)
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.extendedColors.gradientPrimary,
                            MaterialTheme.extendedColors.gradientSecondary,
                            MaterialTheme.extendedColors.gradientAccent
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    WelcomeHeader()
                }

                item {
                    TodayTrainingCard(todayWorkout)
                }

                item {
                    QuickActionsSection(navController)
                }

                item {
                    MetricsSection(
                        navController = navController,
                        ftpEstimate = ftpEstimate,
                        userTrainingData = userTrainingData,
                        isLoading = isLoading,
                        sleepHours = sleepHours,
                        steps = steps,
                        caloriesBurned = caloriesBurned,
                        calorieAllowance = calorieAllowance
                    )
                }

                if (error != null) {
                    item {
                        ErrorCard(errorMessage = error!!, homeViewModel = homeViewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeHeader() {
    val colorScheme = MaterialTheme.colorScheme
    val gradientContentColor = if (isSystemInDarkTheme()) colorScheme.onSurface else colorScheme.onPrimary

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Welcome back!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = gradientContentColor
        )
        Text(
            text = "Ready for today's training?",
            style = MaterialTheme.typography.bodyLarge,
            color = gradientContentColor.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun TodayTrainingCard(todayWorkout: Any?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Today's Training",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (todayWorkout != null) {
                        Text(
                            text = "Workout Session",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Ready to start",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "Rest Day",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.extendedColors.success
                        )
                        Text(
                            text = "Recovery time",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.extendedColors.surfaceMuted),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (todayWorkout != null) Icons.Filled.FitnessCenter else Icons.Filled.SelfImprovement,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val headerColor = if (isSystemInDarkTheme()) colorScheme.onSurface else colorScheme.onPrimary

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = headerColor,
                modifier = Modifier.weight(1f)
            )
            action?.invoke()
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun QuickActionsSection(navController: NavController) {
    Column {
        SectionHeader(title = "Quick Actions")

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            item {
                QuickActionCard(
                    title = "View Calendar",
                    icon = Icons.Filled.CalendarToday,
                    onClick = { navController.navigate(Routes.CALENDAR) }
                )
            }
            item {
                QuickActionCard(
                    title = "Training Dashboard",
                    icon = Icons.Filled.Assignment,
                    onClick = { navController.navigate(Routes.TRAINING_DASHBOARD) }
                )
            }
            item {
                QuickActionCard(
                    title = "Performance",
                    icon = Icons.Filled.TrendingUp,
                    onClick = { navController.navigate(Routes.PERFORMANCE) }
                )
            }
            item {
                QuickActionCard(
                    title = "Settings",
                    icon = Icons.Filled.Settings,
                    onClick = { navController.navigate(Routes.MORE) }
                )
            }
            item {
                QuickActionCard(
                    title = "Strava Sync",
                    icon = Icons.Filled.FitnessCenter,
                    onClick = { navController.navigate(Routes.STRAVA_SYNC_LOADING) }
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MetricsSection(
    navController: NavController,
    ftpEstimate: Any?,
    userTrainingData: Any?,
    isLoading: Boolean?,
    sleepHours: Double,
    steps: Long?,
    caloriesBurned: Double,
    calorieAllowance: Double
) {
    Column {
        SectionHeader(
            title = "Your Metrics",
            action = {
                TextButton(onClick = { navController.navigate(Routes.PERFORMANCE) }) {
                    Text("View details")
                }
            }
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Sleep",
                    value = if (sleepHours > 0) formatSleepHours(sleepHours) else "--",
                    unit = "",
                    icon = Icons.Filled.Bedtime,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Steps",
                    value = steps?.let { NumberFormat.getIntegerInstance().format(it) } ?: "No data",
                    unit = "steps",
                    icon = Icons.Filled.DirectionsWalk,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Calories Burned",
                    value = if (caloriesBurned > 0) NumberFormat.getIntegerInstance().format(caloriesBurned.roundToInt()) else "No data",
                    unit = "kcal",
                    icon = Icons.Filled.LocalFireDepartment,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Daily Allowance",
                    value = if (calorieAllowance > 0) NumberFormat.getIntegerInstance().format(calorieAllowance.roundToInt()) else "2,000",
                    unit = "kcal",
                    icon = Icons.Filled.Favorite,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Heart Rate",
                    value = "72",
                    unit = "bpm",
                    icon = Icons.Filled.Favorite,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Remaining",
                    value = if (calorieAllowance > 0) {
                        val rem = max((calorieAllowance - caloriesBurned).roundToInt(), 0)
                        NumberFormat.getIntegerInstance().format(rem)
                    } else "2,000",
                    unit = "kcal",
                    icon = Icons.Filled.FitnessCenter,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(errorMessage: String, homeViewModel: HomeViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Error: $errorMessage",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { homeViewModel.clearError() }) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun ModernBottomNavigation(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navigationItems = mapOf(
        Routes.HOME to (Icons.Filled.Home to "Home"),
        Routes.CALENDAR to (Icons.Filled.CalendarToday to "Calendar"),
        Routes.TRAINING_DASHBOARD to (Icons.Filled.Assignment to "Plans"),
        Routes.MORE to (Icons.Filled.MoreHoriz to "More")
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navigationItems.forEach { (route, C) ->
                val (icon, label) = C
                BottomNavItem(
                    icon = icon,
                    label = label,
                    isSelected = currentRoute == route,
                    onClick = {
                        navController.navigate(route) {
                            navController.graph.startDestinationRoute?.let { startDestinationRoute ->
                                popUpTo(startDestinationRoute) {
                                    saveState = true
                                }
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen(
            navController = rememberNavController(),
            authViewModel = AuthViewModel(SharedPreferencesMock()),
            stravaViewModel = StravaViewModel(LocalContext.current),
            healthConnectViewModel = HealthConnectViewModel.getInstance(LocalContext.current)
        )
    }
}

