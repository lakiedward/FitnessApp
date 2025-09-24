package com.example.fitnessapp.pages.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.fitnessapp.mock.SharedPreferencesMock
import com.example.fitnessapp.ui.theme.extendedColors
import com.example.fitnessapp.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingZonesScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    // Mock data - in real implementation, these would come from the viewmodel/database
    var userFtp by remember { mutableStateOf(250) } // Default FTP value
    var userMaxBpm by remember { mutableStateOf(190) } // Default Max BPM value
    var currentHeartRate by remember { mutableStateOf(150) } // Current HR for demo
    var currentPower by remember { mutableStateOf(200) } // Current power for demo

    val gradientColors = listOf(
        MaterialTheme.extendedColors.gradientPrimary,
        MaterialTheme.extendedColors.gradientSecondary,
        MaterialTheme.extendedColors.gradientAccent
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.verticalGradient(colors = gradientColors))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                IconButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                Text(
                    text = "Training Zones",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.verticalGradient(colors = gradientColors))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.extendedColors.surfaceSubtle,
                tonalElevation = 2.dp
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        CurrentStatusSection(
                            currentPower = currentPower,
                            currentHeartRate = currentHeartRate,
                            userFtp = userFtp,
                            userMaxBpm = userMaxBpm
                        )
                    }

                    item {
                        CyclingZonesSection(userFtp)
                    }

                    item {
                        RunningZonesSection(userMaxBpm)
                    }

                    item {
                        SwimmingZonesSection()
                    }

                    item {
                        TrainingSection(title = "Training Tips") {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TrainingTip(
                                    icon = Icons.Default.FitnessCenter,
                                    title = "Balance your Training",
                                    description = "Include recovery days and easy sessions to avoid overtraining."
                                )
                                TrainingTip(
                                    icon = Icons.Default.DirectionsBike,
                                    title = "Mix Intensities",
                                    description = "Combine different zones in your weekly plan for optimal progress."
                                )
                                TrainingTip(
                                    icon = Icons.Default.DirectionsRun,
                                    title = "Monitor Heart Rate",
                                    description = "Track your HR zones to ensure you're hitting the right effort."
                                )
                                TrainingTip(
                                    icon = Icons.Default.Pool,
                                    title = "Technique Matters",
                                    description = "Focus on form, especially in high-intensity sessions."
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentStatusSection(
    currentPower: Int,
    currentHeartRate: Int,
    userFtp: Int,
    userMaxBpm: Int
) {
    TrainingSection(title = "Current Status") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CurrentMetricCard(
                title = "Power Zone",
                value = calculatePowerZone(currentPower.toDouble(), userFtp.toDouble()),
                subtitle = "$currentPower W",
                icon = Icons.Default.DirectionsBike,
                color = getPowerZoneColor(currentPower.toDouble(), userFtp.toDouble()),
                modifier = Modifier.weight(1f)
            )

            CurrentMetricCard(
                title = "HR Zone",
                value = calculateHeartRateZone(currentHeartRate, userMaxBpm),
                subtitle = "$currentHeartRate BPM",
                icon = Icons.Default.DirectionsRun,
                color = getHeartRateZoneColor(currentHeartRate, userMaxBpm),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun CurrentMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(136.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CyclingZonesSection(userFtp: Int) {
    TrainingSection(title = "Cycling Power Zones (FTP: ${userFtp}W)") {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val zones = listOf(
                ZoneInfo("Zone 1", "Recovery", "< 56%", "< ${(userFtp * 0.56).toInt()}W", MaterialTheme.extendedColors.success),
                ZoneInfo("Zone 2", "Endurance", "56-75%", "${(userFtp * 0.56).toInt()}-${(userFtp * 0.75).toInt()}W", MaterialTheme.extendedColors.chartSpeed),
                ZoneInfo("Zone 3", "Tempo", "76-90%", "${(userFtp * 0.76).toInt()}-${(userFtp * 0.90).toInt()}W", MaterialTheme.extendedColors.chartAltitude),
                ZoneInfo("Zone 4", "Threshold", "91-105%", "${(userFtp * 0.91).toInt()}-${(userFtp * 1.05).toInt()}W", MaterialTheme.extendedColors.warning),
                ZoneInfo("Zone 5", "VO2 Max", "106-120%", "${(userFtp * 1.06).toInt()}-${(userFtp * 1.20).toInt()}W", MaterialTheme.extendedColors.chartHeartRate),
                ZoneInfo("Zone 6", "Anaerobic", "121-150%", "${(userFtp * 1.21).toInt()}-${(userFtp * 1.50).toInt()}W", MaterialTheme.colorScheme.error),
                ZoneInfo("Zone 7", "Neuromuscular", "> 150%", "> ${(userFtp * 1.50).toInt()}W", MaterialTheme.colorScheme.error)
            )

            zones.forEach { zone ->
                ZoneCard(zone = zone, icon = Icons.Default.DirectionsBike)
            }
        }
    }
}

@Composable
fun RunningZonesSection(userMaxBpm: Int) {
    TrainingSection(title = "Running Heart Rate Zones (Max: ${userMaxBpm} BPM)") {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val zones = listOf(
                ZoneInfo("Zone 1", "Recovery", "< 60%", "< ${(userMaxBpm * 0.60).toInt()} BPM", MaterialTheme.extendedColors.success),
                ZoneInfo("Zone 2", "Extensive Endurance", "60-70%", "${(userMaxBpm * 0.60).toInt()}-${(userMaxBpm * 0.70).toInt()} BPM", MaterialTheme.extendedColors.chartSpeed),
                ZoneInfo("Zone 3", "Aerobic", "70-80%", "${(userMaxBpm * 0.70).toInt()}-${(userMaxBpm * 0.80).toInt()} BPM", MaterialTheme.extendedColors.chartAltitude),
                ZoneInfo("Zone 4", "Lactate Threshold", "80-90%", "${(userMaxBpm * 0.80).toInt()}-${(userMaxBpm * 0.90).toInt()} BPM", MaterialTheme.extendedColors.warning),
                ZoneInfo("Zone 5", "Neuromuscular", "> 90%", "> ${(userMaxBpm * 0.90).toInt()} BPM", MaterialTheme.extendedColors.chartHeartRate)
            )

            zones.forEach { zone ->
                ZoneCard(zone = zone, icon = Icons.Default.DirectionsRun)
            }
        }
    }
}

@Composable
fun SwimmingZonesSection() {
    TrainingSection(title = "Swimming Training Zones") {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val zones = listOf(
                ZoneInfo("EN1", "Aerobic Base", "Easy", "Conversational pace", MaterialTheme.extendedColors.success),
                ZoneInfo("EN2", "Aerobic Threshold", "Moderate", "Comfortably hard", MaterialTheme.extendedColors.chartSpeed),
                ZoneInfo("EN3", "Lactate Threshold", "Hard", "Sustainable effort", MaterialTheme.extendedColors.chartAltitude),
                ZoneInfo("SP1", "Lactate Production", "Very Hard", "Short intervals", MaterialTheme.extendedColors.warning),
                ZoneInfo("SP2", "Neuromuscular", "Maximum", "Sprint pace", MaterialTheme.extendedColors.chartHeartRate)
            )

            zones.forEach { zone ->
                ZoneCard(zone = zone, icon = Icons.Default.Pool)
            }
        }
    }
}

@Composable
fun ZoneCard(zone: ZoneInfo, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = zone.color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = zone.name,
                tint = zone.color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${zone.zone} - ${zone.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = zone.color
                )
                Text(
                    text = zone.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = zone.range,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = zone.color
            )
        }
    }
}

data class ZoneInfo(
    val zone: String,
    val name: String,
    val description: String,
    val range: String,
    val color: Color
)

// Zone calculation functions
private fun calculatePowerZone(power: Double, ftp: Double): String {
    val percentage = (power / ftp) * 100
    return when {
        percentage >= 150 -> "Zone 7"
        percentage >= 120 -> "Zone 6"
        percentage >= 105 -> "Zone 5"
        percentage >= 90 -> "Zone 4"
        percentage >= 76 -> "Zone 3"
        percentage >= 56 -> "Zone 2"
        else -> "Zone 1"
    }
}

private fun calculateHeartRateZone(currentHR: Int, maxBPM: Int): String {
    val percentage = (currentHR.toDouble() / maxBPM) * 100
    return when {
        percentage >= 90 -> "Zone 5"
        percentage >= 80 -> "Zone 4"
        percentage >= 70 -> "Zone 3"
        percentage >= 60 -> "Zone 2"
        else -> "Zone 1"
    }
}

@Composable
private fun getPowerZoneColor(power: Double, ftp: Double): Color {
    val percentage = (power / ftp) * 100
    return when {
        percentage >= 150 -> MaterialTheme.colorScheme.error
        percentage >= 120 -> MaterialTheme.colorScheme.error
        percentage >= 105 -> MaterialTheme.extendedColors.chartHeartRate
        percentage >= 90 -> MaterialTheme.extendedColors.warning
        percentage >= 76 -> MaterialTheme.extendedColors.chartAltitude
        percentage >= 56 -> MaterialTheme.extendedColors.chartSpeed
        else -> MaterialTheme.extendedColors.success
    }
}

@Composable
private fun getHeartRateZoneColor(currentHR: Int, maxBPM: Int): Color {
    val percentage = (currentHR.toDouble() / maxBPM) * 100
    return when {
        percentage >= 90 -> MaterialTheme.extendedColors.chartHeartRate
        percentage >= 80 -> MaterialTheme.extendedColors.warning
        percentage >= 70 -> MaterialTheme.extendedColors.chartAltitude
        percentage >= 60 -> MaterialTheme.extendedColors.chartSpeed
        else -> MaterialTheme.extendedColors.success
    }
}

@Composable
fun TrainingSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.surfaceSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun TrainingTip(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TrainingZonesScreenPreview() {
    TrainingZonesScreen(
        navController = rememberNavController(),
        authViewModel = AuthViewModel(SharedPreferencesMock())
    )
}
