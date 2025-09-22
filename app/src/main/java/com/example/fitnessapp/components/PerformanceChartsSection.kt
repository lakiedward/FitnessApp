package com.example.fitnessapp.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import com.example.fitnessapp.ui.theme.extendedColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnessapp.model.StravaActivity
import com.example.fitnessapp.viewmodel.StravaViewModel
import com.example.fitnessapp.viewmodel.StravaViewModelFactory

@Composable
fun PerformanceChartsSection(
    activityId: Long,
    activity: StravaActivity?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val stravaViewModel: StravaViewModel = viewModel(factory = StravaViewModelFactory(context))
    val ftpEstimate by stravaViewModel.ftpEstimate.collectAsState()

    // State for max BPM
    var maxBpm by remember { mutableStateOf<Float?>(null) }

    // Fetch max BPM when the activity changes
    LaunchedEffect(activityId) {
        maxBpm = null
        try {
            val maxBpmData = stravaViewModel.getMaxBpm()
            val maxBpmValue = maxBpmData["max_bpm"] as? Number
            maxBpm = maxBpmValue?.toFloat()
        } catch (e: Exception) {
            // Fallback to activity max heart rate if API call fails
            maxBpm = activity?.maxHeartrate?.toFloat()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Performance charts section showing activity analysis"
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ActivityChartsComponent(
                activityId = activityId,
                ftp = ftpEstimate?.estimatedFTP ?: 0f,
                fthr = maxBpm ?: activity?.maxHeartrate?.toFloat() ?: 0f
            )
        }
    }
}

@Composable
fun PowerCurveSection(
    activityId: Long,
    activityType: String,
    activity: StravaActivity?,
    modifier: Modifier = Modifier
) {
    if (!isPowerCurveEligible(activityType)) {
        return
    }

    val context = LocalContext.current
    val stravaViewModel: StravaViewModel = viewModel(factory = StravaViewModelFactory(context))

    // State for max BPM
    var maxBpm by remember { mutableStateOf<Int?>(null) }

    // Fetch max BPM when component loads
    LaunchedEffect(activityId) {
        maxBpm = null
        try {
            val maxBpmData = stravaViewModel.getMaxBpm()
            val maxBpmValue = maxBpmData["max_bpm"] as? Number
            maxBpm = maxBpmValue?.toInt()
        } catch (e: Exception) {
            // Fallback to activity average heart rate if API call fails
            maxBpm = activity?.averageHeartrate?.toInt()
        }
    }

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics {
                contentDescription = "Power curve analysis section"
            }
    ) {
        androidx.compose.material3.HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.extendedColors.chartGrid
        )
        PowerCurveComponent(
            activityId = activityId,
            activityType = activityType,
            fthr = maxBpm ?: activity?.averageHeartrate?.toInt()
        )
    }
}
