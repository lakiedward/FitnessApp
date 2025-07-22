package com.example.fitnessapp.pages.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
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

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF6366F1),
                                Color(0xFF8B5CF6),
                                Color(0xFFA855F7)
                            )
                        )
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Training Zones",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentPadding = PaddingValues(
                top = 8.dp + WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current Status Section
            item {
                CurrentStatusSection(
                    currentPower = currentPower,
                    currentHeartRate = currentHeartRate,
                    userFtp = userFtp,
                    userMaxBpm = userMaxBpm
                )
            }

            // Cycling Zones Section
            item {
                CyclingZonesSection(userFtp = userFtp)
            }

            // Running Zones Section
            item {
                RunningZonesSection(userMaxBpm = userMaxBpm)
            }

            // Swimming Zones Section
            item {
                SwimmingZonesSection()
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
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
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CurrentMetricCard(
                title = "Power Zone",
                value = calculatePowerZone(currentPower.toDouble(), userFtp.toDouble()),
                subtitle = "$currentPower W",
                icon = Icons.Default.DirectionsBike,
                color = getPowerZoneColor(currentPower.toDouble(), userFtp.toDouble())
            )

            CurrentMetricCard(
                title = "HR Zone",
                value = calculateHeartRateZone(currentHeartRate, userMaxBpm),
                subtitle = "$currentHeartRate BPM",
                icon = Icons.Default.DirectionsRun,
                color = getHeartRateZoneColor(currentHeartRate, userMaxBpm)
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
    color: Color
) {
    Card(
        modifier = Modifier.size(width = 160.dp, height = 120.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun CyclingZonesSection(userFtp: Int) {
    TrainingSection(title = "Cycling Power Zones (FTP: ${userFtp}W)") {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val zones = listOf(
                ZoneInfo("Zone 1", "Recovery", "< 56%", "< ${(userFtp * 0.56).toInt()}W", Color(0xFF10B981)),
                ZoneInfo("Zone 2", "Endurance", "56-75%", "${(userFtp * 0.56).toInt()}-${(userFtp * 0.75).toInt()}W", Color(0xFF3B82F6)),
                ZoneInfo("Zone 3", "Tempo", "76-90%", "${(userFtp * 0.76).toInt()}-${(userFtp * 0.90).toInt()}W", Color(0xFF8B5CF6)),
                ZoneInfo("Zone 4", "Threshold", "91-105%", "${(userFtp * 0.91).toInt()}-${(userFtp * 1.05).toInt()}W", Color(0xFFF59E0B)),
                ZoneInfo("Zone 5", "VO2 Max", "106-120%", "${(userFtp * 1.06).toInt()}-${(userFtp * 1.20).toInt()}W", Color(0xFFEF4444)),
                ZoneInfo("Zone 6", "Anaerobic", "121-150%", "${(userFtp * 1.21).toInt()}-${(userFtp * 1.50).toInt()}W", Color(0xFFDC2626)),
                ZoneInfo("Zone 7", "Neuromuscular", "> 150%", "> ${(userFtp * 1.50).toInt()}W", Color(0xFF991B1B))
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val zones = listOf(
                ZoneInfo("Zone 1", "Recovery", "< 60%", "< ${(userMaxBpm * 0.60).toInt()} BPM", Color(0xFF10B981)),
                ZoneInfo("Zone 2", "Extensive Endurance", "60-70%", "${(userMaxBpm * 0.60).toInt()}-${(userMaxBpm * 0.70).toInt()} BPM", Color(0xFF3B82F6)),
                ZoneInfo("Zone 3", "Aerobic", "70-80%", "${(userMaxBpm * 0.70).toInt()}-${(userMaxBpm * 0.80).toInt()} BPM", Color(0xFF8B5CF6)),
                ZoneInfo("Zone 4", "Lactate Threshold", "80-90%", "${(userMaxBpm * 0.80).toInt()}-${(userMaxBpm * 0.90).toInt()} BPM", Color(0xFFF59E0B)),
                ZoneInfo("Zone 5", "Neuromuscular", "> 90%", "> ${(userMaxBpm * 0.90).toInt()} BPM", Color(0xFFEF4444))
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val zones = listOf(
                ZoneInfo("EN1", "Aerobic Base", "Easy", "Conversational pace", Color(0xFF10B981)),
                ZoneInfo("EN2", "Aerobic Threshold", "Moderate", "Comfortably hard", Color(0xFF3B82F6)),
                ZoneInfo("EN3", "Lactate Threshold", "Hard", "Sustainable effort", Color(0xFF8B5CF6)),
                ZoneInfo("SP1", "Lactate Production", "Very Hard", "Short intervals", Color(0xFFF59E0B)),
                ZoneInfo("SP2", "Neuromuscular", "Maximum", "Sprint pace", Color(0xFFEF4444))
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
        shape = RoundedCornerShape(8.dp)
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
            Spacer(modifier = Modifier.padding(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${zone.zone} - ${zone.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = zone.color
                )
                Text(
                    text = zone.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
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

private fun getPowerZoneColor(power: Double, ftp: Double): Color {
    val percentage = (power / ftp) * 100
    return when {
        percentage >= 150 -> Color(0xFF991B1B)
        percentage >= 120 -> Color(0xFFDC2626)
        percentage >= 105 -> Color(0xFFEF4444)
        percentage >= 90 -> Color(0xFFF59E0B)
        percentage >= 76 -> Color(0xFF8B5CF6)
        percentage >= 56 -> Color(0xFF3B82F6)
        else -> Color(0xFF10B981)
    }
}

private fun getHeartRateZoneColor(currentHR: Int, maxBPM: Int): Color {
    val percentage = (currentHR.toDouble() / maxBPM) * 100
    return when {
        percentage >= 90 -> Color(0xFFEF4444)
        percentage >= 80 -> Color(0xFFF59E0B)
        percentage >= 70 -> Color(0xFF8B5CF6)
        percentage >= 60 -> Color(0xFF3B82F6)
        else -> Color(0xFF10B981)
    }
}

@Composable
fun TrainingSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), content = content)
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
