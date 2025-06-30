package com.example.fitnessapp.pages.home

import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.fitnessapp.components.ActivityChartsComponent
import com.example.fitnessapp.model.ActivityStreamsResponse
import com.example.fitnessapp.model.StravaActivity
import com.example.fitnessapp.viewmodel.StravaViewModel
import com.example.fitnessapp.viewmodel.StravaViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StravaActivityDetailScreen(
    navController: NavController,
    activityId: Long
) {
    val context = LocalContext.current
    val stravaViewModel: StravaViewModel = viewModel(factory = StravaViewModelFactory(context))

    var activity by remember { mutableStateOf<StravaActivity?>(null) }
    var htmlMapUrl by remember { mutableStateOf<String?>(null) }
    var streams by remember { mutableStateOf<ActivityStreamsResponse?>(null) }
    var streamsFromDB by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Load activity details
    LaunchedEffect(activityId) {
        try {
            Log.d("StravaActivityDetail", "Starting to load activity $activityId")

            // Get activity details from ViewModel
            val activityDetails = stravaViewModel.getActivityById(activityId)
            Log.d("StravaActivityDetail", "Activity details result: $activityDetails")
            activity = activityDetails

            if (activityDetails != null) {
                // Load map view and extract html_url for full interactive map
                Log.d("StravaActivityDetail", "Loading map view for activity $activityId")
                val mapViewResponse = stravaViewModel.getActivityMapView(activityId)
                Log.d("StravaActivityDetail", "Map view response: $mapViewResponse")

                // Extract html_url for full interactive map
                htmlMapUrl = mapViewResponse["html_url"]
                Log.d("StravaActivityDetail", "Extracted HTML map URL: $htmlMapUrl")
            } else {
                Log.w("StravaActivityDetail", "Activity details is null for ID $activityId")
            }

            // Load streams data from database
            Log.d("StravaActivityDetail", "Loading streams from database for activity $activityId")
            val streamsData = stravaViewModel.getActivityStreamsFromDB(activityId)
            streamsFromDB = streamsData
            Log.d("StravaActivityDetail", "Loaded streams from DB: $streamsData")
            Log.d("StravaActivityDetail", "Streams data keys: ${streamsData.keys}")
            Log.d("StravaActivityDetail", "Streams data size: ${streamsData.size}")

            // Log each key-value pair for debugging
            streamsData.forEach { (key, value) ->
                Log.d(
                    "StravaActivityDetail",
                    "Stream key: $key, value type: ${value::class.simpleName}, value: ${
                        value.toString().take(200)
                    }"
                )
                if (value is List<*>) {
                    Log.d("StravaActivityDetail", "List size for $key: ${value.size}")
                    if (value.isNotEmpty()) {
                        Log.d(
                            "StravaActivityDetail",
                            "First item type for $key: ${value.first()?.javaClass?.simpleName}"
                        )
                        Log.d("StravaActivityDetail", "First few items for $key: ${value.take(5)}")
                    }
                }
            }

            // Load legacy streams data (if still needed)
            Log.d("StravaActivityDetail", "Loading legacy streams for activity $activityId")
            val legacyStreamsData = stravaViewModel.getActivityStreams(activityId)
            streams = legacyStreamsData

            isLoading = false
            Log.d("StravaActivityDetail", "Finished loading activity: ${activityDetails?.name}")
        } catch (e: Exception) {
            Log.e("StravaActivityDetail", "Error loading activity details for $activityId", e)
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = activity?.name ?: "Activity Details",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6366F1)
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF6366F1),
                            Color(0xFF8B55F6),
                            Color(0xFFA855F7)
                        )
                    )
                )
        ) {
            if (isLoading) {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Loading activity...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }
                }
            } else if (activity != null) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        // Activity Header Card
                        ActivityHeaderCard(activity = activity!!)
                    }

                    item {
                        // Statistics Card
                        ActivityStatsCard(activity = activity!!)
                    }

                    item {
                        // Full Interactive WebView Map Card (using html_url)
                        if (htmlMapUrl != null) {
                            FullInteractiveWebViewMapCard(htmlMapUrl = htmlMapUrl!!)
                        }
                    }

                    item {
                        // New Chart Component
                        ActivityChartsComponent(
                            activityId = activityId
                        )
                    }
                }
            } else {
                // Error state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Failed to load activity",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please try again",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.clickable { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityHeaderCard(activity: StravaActivity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = activity.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Type: ${activity.type}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Date: ${activity.startDate}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
private fun ActivityStatsCard(activity: StravaActivity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Activity Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Distance
            activity.distance?.let { distance ->
                Text(
                    text = "Distance: ${String.format("%.2f", distance / 1000)} km",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF374151)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Moving time
            val hours = activity.movingTime / 3600
            val minutes = (activity.movingTime % 3600) / 60
            val seconds = activity.movingTime % 60
            Text(
                text = "Moving Time: ${String.format("%02d:%02d:%02d", hours, minutes, seconds)}",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF374151)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Average speed
            activity.averageSpeed?.let { speed ->
                Text(
                    text = "Average Speed: ${String.format("%.2f", speed * 3.6)} km/h",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF374151)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Max speed
            activity.maxSpeed?.let { speed ->
                Text(
                    text = "Max Speed: ${String.format("%.2f", speed * 3.6)} km/h",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF374151)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Elevation gain
            activity.totalElevationGain?.let { elevation ->
                Text(
                    text = "Elevation Gain: ${String.format("%.0f", elevation)} m",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF374151)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Average heart rate
            activity.averageHeartrate?.let { hr ->
                Text(
                    text = "Average Heart Rate: ${hr.toInt()} bpm",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF374151)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Max heart rate
            activity.maxHeartrate?.let { hr ->
                Text(
                    text = "Max Heart Rate: $hr bpm",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF374151)
                )
            }
        }
    }
}

@Composable
private fun FullInteractiveWebViewMapCard(htmlMapUrl: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        AndroidView(
            factory = {
                WebView(it).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    loadUrl(htmlMapUrl)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        )
    }
}
