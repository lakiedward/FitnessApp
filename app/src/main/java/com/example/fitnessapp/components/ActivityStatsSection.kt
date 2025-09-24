package com.example.fitnessapp.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitnessapp.model.StravaActivity

data class StatItem(
    val icon: ImageVector,
    val label: String,
    val value: String
)

@Composable
fun ActivityStatsSection(
    activity: StravaActivity,
    gearName: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Activity Statistics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            ActivityStatsContent(activity = activity, gearName = gearName)
        }
    }
}

@Composable
private fun ActivityStatsContent(activity: StravaActivity, gearName: String?) {
    // Create list of stats
    val stats = buildList {
        // Distance
        activity.distance?.let { distance ->
            add(
                StatItem(
                    icon = Icons.Filled.Place,
                    label = "Distance",
                    value = "${String.format("%.2f", distance / 1000)} km"
                )
            )
        }

        // Moving time
        add(
            StatItem(
                icon = Icons.Filled.Timer,
                label = "Moving Time",
                value = formatMovingTime(activity.movingTime)
            )
        )

        // Average speed or pace depending on activity type
        when (activity.type) {
            "Run", "Swim" -> {
                val pace = formatPace(activity.distance ?: 0f, activity.movingTime)
                add(
                    StatItem(
                        icon = Icons.Filled.Speed,
                        label = "Average Pace",
                        value = pace
                    )
                )
            }
            else -> {
                activity.averageSpeed?.let { speed ->
                    add(
                        StatItem(
                            icon = Icons.Filled.Speed,
                            label = "Average Speed",
                            value = "${String.format("%.2f", speed * 3.6)} km/h"
                        )
                    )
                }
            }
        }

        // Calories (prefer calories, fallback to kilojoules)
        val calories = (activity.calories ?: activity.kilojoules)?.toInt()
        calories?.let {
            add(
                StatItem(
                    icon = Icons.Filled.LocalFireDepartment,
                    label = "Calories",
                    value = "${it} kcal"
                )
            )
        }

        // Cadence (spm for Run, rpm otherwise)
        activity.averageCadence?.let { cadence ->
            val cadenceLabel = if (activity.type == "Run") "Cadence (steps/min)" else "Cadence (rpm)"
            add(
                StatItem(
                    icon = Icons.Filled.Repeat,
                    label = cadenceLabel,
                    value = cadence.toInt().toString()
                )
            )
        }

        // Temperature (Celsius)
        activity.averageTemp?.let { temp ->
            add(
                StatItem(
                    icon = Icons.Filled.Thermostat,
                    label = "Temperature",
                    value = "$temp °C"
                )
            )
        }

        // Equipment
        gearName?.let { name ->
            val icon = when (activity.type) {
                "Ride" -> Icons.Filled.DirectionsBike
                "Run" -> Icons.Filled.DirectionsRun
                else -> Icons.Filled.Label
            }
            add(
                StatItem(
                    icon = icon,
                    label = "Equipment",
                    value = name
                )
            )
        }

        // Max speed (cycling only)
        if (activity.type == "Ride") {
            activity.maxSpeed?.let { speed ->
                val formattedMaxSpeed = String.format("%.1f", speed * 3.6)
                add(
                    StatItem(
                        icon = Icons.Filled.TrendingUp,
                        label = "Max Speed",
                        value = "$formattedMaxSpeed km/h"
                    )
                )
            }
        }

        // Elevation gain
        activity.totalElevationGain?.let { elevation ->
            add(
                StatItem(
                    icon = Icons.Filled.Terrain,
                    label = "Elevation Gain",
                    value = "${String.format("%.0f", elevation)} m"
                )
            )
        }

        // Average heart rate
        activity.averageHeartrate?.let { hr ->
            add(
                StatItem(
                    icon = Icons.Filled.Favorite,
                    label = "Average Heart Rate",
                    value = "${hr.toInt()} bpm"
                )
            )
        }

        // Max heart rate
        activity.maxHeartrate?.let { hr ->
            add(
                StatItem(
                    icon = Icons.Filled.Favorite,
                    label = "Max Heart Rate",
                    value = "$hr bpm"
                )
            )
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        stats.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { stat ->
                    StatRow(
                        icon = stat.icon,
                        label = stat.label,
                        value = stat.value,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatRow(
    icon: ImageVector, 
    label: String, 
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.semantics {
            contentDescription = "$label: $value"
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun formatMovingTime(movingTime: Int): String {
    val hours = movingTime / 3600
    val minutes = (movingTime % 3600) / 60
    val seconds = movingTime % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}


private fun formatPace(distanceMeters: Float, movingTimeSec: Int): String {
    if (distanceMeters <= 0f || movingTimeSec <= 0) return "N/A"
    val paceInSecondsPerKm = movingTimeSec / (distanceMeters / 1000f)
    val minutes = (paceInSecondsPerKm / 60f).toInt()
    val seconds = (paceInSecondsPerKm % 60f).toInt()
    return String.format("%d:%02d min/km", minutes, seconds)
}
