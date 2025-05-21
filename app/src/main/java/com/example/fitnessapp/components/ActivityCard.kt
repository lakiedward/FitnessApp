package com.example.fitnessapp.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fitnessapp.model.StravaActivity

@Composable
fun ActivityCard(activity: StravaActivity, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = activity.name, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Distanță: ${
                        activity.distance?.let { "%.2f km".format(it / 1000) } ?: "N/A"
                    }"
                )
                Text(
                    text = "${activity.movingTime / 60} min",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = activity.type,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}