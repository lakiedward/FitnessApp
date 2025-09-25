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
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
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
import java.util.Locale

data class StatItem(
    val icon: ImageVector,
    val label: String,
    val value: String
)

@Composable
fun ActivityStatsSection(
    activity: StravaActivity,
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
            ActivityStatsContent(activity = activity)
        }
    }
}

@Composable
private fun ActivityStatsContent(activity: StravaActivity) {
    // Create list of stats
    val stats = buildList {
        val type = activity.type.lowercase(Locale.getDefault())
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

        // Average speed / pace (contextual by sport)
        if (type == "run" || type == "swim") {
            val distance = activity.distance ?: 0f
            val timeSec = activity.movingTime
            if (type == "run") {
                formatPacePerKm(distance, timeSec)?.let { pace ->
                    add(
                        StatItem(
                            icon = Icons.Filled.Speed,
                            label = "Running Pace",
                            value = pace
                        )
                    )
                }
            } else {
                formatPacePer100m(distance, timeSec)?.let { pace ->
                    add(
                        StatItem(
                            icon = Icons.Filled.Speed,
                            label = "Pace 100m",
                            value = pace
                        )
                    )
                }
            }
        } else {
            activity.averageSpeed?.let { speed ->
                if (speed.isFinite()) {
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

        // Max speed (relevant mainly for cycling)
        if (type == "ride" || type == "virtualride") {
            activity.maxSpeed?.let { speed ->
                if (speed.isFinite()) {
                    add(
                        StatItem(
                            icon = Icons.Filled.Speed,
                            label = "Max Speed",
                            value = "${String.format("%.2f", speed * 3.6)} km/h"
                        )
                    )
                }
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

        // Calories (if available), fallback to kilojoules
        val calories = activity.calories ?: activity.kilojoules
        calories?.let { kcalsLike ->
            if (kcalsLike.isFinite()) {
                add(
                    StatItem(
                        icon = Icons.Filled.Timer,
                        label = "Calories",
                        value = "${kcalsLike.toInt()} kcal"
                    )
                )
            }
        }

        // Average temperature (if available)
        activity.averageTemp?.let { temp ->
            if (temp.isFinite()) {
                add(
                    StatItem(
                        icon = Icons.Filled.Thermostat,
                        label = "Temperature",
                        value = "${temp.toInt()} °C"
                    )
                )
            }
        }    }

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

private fun formatPacePerKm(distanceMeters: Float, movingTimeSec: Int): String? {
    if (distanceMeters <= 0f || movingTimeSec <= 0) return null
    val secPerKm = movingTimeSec / (distanceMeters / 1000f)
    if (!secPerKm.isFinite() || secPerKm <= 0f) return null
    val minutes = (secPerKm / 60f).toInt()
    val seconds = ((secPerKm % 60f).coerceAtLeast(0f)).toInt()
    return String.format("%02d:%02d", minutes, seconds)
}

private fun formatPacePer100m(distanceMeters: Float, movingTimeSec: Int): String? {
    if (distanceMeters <= 0f || movingTimeSec <= 0) return null
    val secPerMeter = movingTimeSec / distanceMeters
    val secPer100 = secPerMeter * 100f
    if (!secPer100.isFinite() || secPer100 <= 0f) return null
    val minutes = (secPer100 / 60f).toInt()
    val seconds = ((secPer100 % 60f).coerceAtLeast(0f)).toInt()
    return String.format("%02d:%02d", minutes, seconds)
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
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
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



