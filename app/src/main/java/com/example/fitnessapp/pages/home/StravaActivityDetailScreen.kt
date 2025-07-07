package com.example.fitnessapp.pages.home

import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.fitnessapp.components.ActivityChartsComponent
import com.example.fitnessapp.components.PowerCurveComponent
import com.example.fitnessapp.model.ActivityStreamsResponse
import com.example.fitnessapp.model.StravaActivity
import com.example.fitnessapp.viewmodel.StravaViewModel
import com.example.fitnessapp.viewmodel.StravaViewModelFactory
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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
                    Column {
                        Text(
                            text = activity?.name ?: "Activity Details",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        activity?.let {
                            val formattedDate = formatStartDate(it.startDate)
                            Text(
                                text = "${it.type} on $formattedDate",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
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
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
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

                    item {
                        // Power Curve Component (for cycling activities only)
                        PowerCurveComponent(
                            activityId = activityId,
                            activityType = activity!!.type
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Error",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Failed to load activity",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { navController.popBackStack() }) {
                            Text(text = "Go Back")
                        }
                    }
                }
            }
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
            Spacer(modifier = Modifier.height(16.dp))

            // Distance
            activity.distance?.let { distance ->
                StatRow(
                    icon = Icons.Filled.Place,
                    label = "Distance",
                    value = "${String.format("%.2f", distance / 1000)} km"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Moving time
            val hours = activity.movingTime / 3600
            val minutes = (activity.movingTime % 3600) / 60
            val seconds = activity.movingTime % 60
            StatRow(
                icon = Icons.Filled.Timer,
                label = "Moving Time",
                value = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Average speed
            activity.averageSpeed?.let { speed ->
                StatRow(
                    icon = Icons.Filled.Speed,
                    label = "Average Speed",
                    value = "${String.format("%.2f", speed * 3.6)} km/h"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Max speed
            activity.maxSpeed?.let { speed ->
                StatRow(
                    icon = Icons.Filled.Speed,
                    label = "Max Speed",
                    value = "${String.format("%.2f", speed * 3.6)} km/h"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Elevation gain
            activity.totalElevationGain?.let { elevation ->
                StatRow(
                    icon = Icons.Filled.Terrain,
                    label = "Elevation Gain",
                    value = "${String.format("%.0f", elevation)} m"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Average heart rate
            activity.averageHeartrate?.let { hr ->
                StatRow(
                    icon = Icons.Filled.Favorite,
                    label = "Average Heart Rate",
                    value = "${hr.toInt()} bpm"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Max heart rate
            activity.maxHeartrate?.let { hr ->
                StatRow(
                    icon = Icons.Filled.Favorite,
                    label = "Max Heart Rate",
                    value = "$hr bpm"
                )
            }
        }
    }
}

@Composable
private fun StatRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF6366F1),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF374151)
            )
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

private fun formatStartDate(isoDate: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(isoDate)
        val formatter = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        date?.let { formatter.format(it) } ?: isoDate.substringBefore("T")
    } catch (e: Exception) {
        Log.e("StravaActivityDetail", "Failed to parse date: $isoDate", e)
        isoDate.substringBefore("T") // Fallback
    }
}