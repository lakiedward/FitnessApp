package com.example.fitnessapp.pages.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import com.example.fitnessapp.ui.theme.extendedColors
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.fitnessapp.R
import com.example.fitnessapp.components.ActivityChartsComponent
import com.example.fitnessapp.components.ActivityStatsSection
import com.example.fitnessapp.components.PerformanceChartsSection
import com.example.fitnessapp.components.PowerCurveSection
import com.example.fitnessapp.components.isPowerCurveEligible
import com.example.fitnessapp.components.RouteMapSection
import com.example.fitnessapp.ui.theme.FitnessAppTheme
import com.example.fitnessapp.model.ActivityStreamsResponse
import com.example.fitnessapp.model.StravaActivity
import com.example.fitnessapp.viewmodel.StravaViewModel
import com.example.fitnessapp.viewmodel.StravaViewModelFactory
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.em
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShimmerPlaceholder(modifier: Modifier = Modifier, height: Int = 32) {
    val extendedColors = MaterialTheme.extendedColors

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "shimmerAlpha"
    )
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -300f, targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "shimmerTranslate"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            extendedColors.surfaceMuted,
                            extendedColors.borderStrong,
                            extendedColors.surfaceMuted
                        ),
                        startX = shimmerTranslate - 100f,
                        endX = shimmerTranslate + 100f
                    )
                )
                .alpha(alpha)
        )
    }
}

// Reusable Section Header Composable
@Composable
fun SectionHeader(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(bottom = 8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.1.em
        )
    }
}

@Composable
private fun ActivityOverviewCard(activity: StravaActivity) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = MaterialTheme.extendedColors
    val onGradient = colorScheme.onPrimary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.12f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            extendedColors.gradientPrimary.copy(alpha = 0.95f),
                            extendedColors.gradientSecondary.copy(alpha = 0.95f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color = onGradient.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (activity.type.lowercase(Locale.getDefault())) {
                                "ride", "virtualride" -> Icons.Default.DirectionsBike
                                "run" -> Icons.Default.DirectionsRun
                                "swim" -> Icons.Default.Pool
                                else -> Icons.Default.Speed
                            },
                            contentDescription = "Activity type ${activity.type}",
                            tint = onGradient,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Strava Activity",
                            style = MaterialTheme.typography.labelMedium,
                            color = onGradient.copy(alpha = 0.75f)
                        )
                        Text(
                            text = activity.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = onGradient,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatActivityType(activity.type),
                            style = MaterialTheme.typography.bodyMedium,
                            color = onGradient.copy(alpha = 0.85f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = null,
                        tint = onGradient.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = formatStartDate(activity.startDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = onGradient.copy(alpha = 0.85f)
                    )
                }

                val location = listOfNotNull(
                    activity.location_city,
                    activity.location_state,
                    activity.location_country
                )
                    .filter { it.isNotBlank() }
                    .joinToString(separator = ", ")

                if (location.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Place,
                            contentDescription = null,
                            tint = onGradient.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = location,
                            style = MaterialTheme.typography.bodySmall,
                            color = onGradient.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrainingTypeCard(activity: StravaActivity) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Training Type",
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (activity.type.lowercase(Locale.getDefault())) {
                        "ride", "virtualride" -> Icons.Default.DirectionsBike
                        "run" -> Icons.Default.DirectionsRun
                        "swim" -> Icons.Default.Pool
                        else -> Icons.Default.Speed
                    },
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = formatActivityType(activity.type),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = if (activity.manual) {
                            "Logged in FitSense"
                        } else {
                            "Synced automatically from Strava"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private data class KeyMetric(
    val label: String,
    val value: String,
    val icon: ImageVector
)

@Composable
private fun ActivityCoreStatsRow(activity: StravaActivity) {
    val metrics = buildList {
        activity.distance?.let { distance ->
            add(
                KeyMetric(
                    label = "Distance",
                    value = "${String.format(Locale.getDefault(), "%.1f", distance / 1000f)} km",
                    icon = Icons.Filled.Place
                )
            )
        }

        add(
            KeyMetric(
                label = "Moving Time",
                value = formatMovingTime(activity.movingTime),
                icon = Icons.Filled.Timer
            )
        )

        activity.averageSpeed?.let { speed ->
            add(
                KeyMetric(
                    label = "Avg Speed",
                    value = "${String.format(Locale.getDefault(), "%.1f", speed * 3.6f)} km/h",
                    icon = Icons.Filled.Speed
                )
            )
        }

        activity.totalElevationGain?.let { gain ->
            add(
                KeyMetric(
                    label = "Elevation Gain",
                    value = "${String.format(Locale.getDefault(), "%.0f", gain)} m",
                    icon = Icons.Filled.Terrain
                )
            )
        }
    }

    if (metrics.isEmpty()) {
        return
    }

    val displayedMetrics = metrics.take(3)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        displayedMetrics.forEach { metric ->
            KeyMetricChip(metric = metric, modifier = Modifier.weight(1f))
        }
        repeat(3 - displayedMetrics.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun KeyMetricChip(metric: KeyMetric, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = metric.icon,
                contentDescription = metric.label,
                tint = colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = metric.value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            Text(
                text = metric.label,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WorkoutStructureCard(
    activity: StravaActivity,
    userMaxBpm: Int?,
    heartRateData: List<Int>?
) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Workout Structure",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )
            Text(
                text = "Zone distribution",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant
            )

            val averageHr = activity.averageHeartrate
            val resolvedMaxBpm = userMaxBpm

            if (averageHr != null && resolvedMaxBpm != null) {
                val activityMaxHr = activity.maxHeartrate?.toFloat() ?: averageHr
                HeartRateZoneDistribution(
                    avgHr = averageHr,
                    maxHr = activityMaxHr,
                    maxBpm = resolvedMaxBpm,
                    heartRateData = heartRateData
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Text(
                        text = "Sync a heart rate monitor to see detailed zone insights.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

private fun formatActivityType(type: String): String {
    return when (type.lowercase(Locale.getDefault())) {
        "ride", "virtualride" -> "Cycling"
        "run", "trailrun" -> "Running"
        "swim" -> "Swimming"
        "walk" -> "Walking"
        "hike" -> "Hiking"
        "workout" -> "Workout"
        else -> type.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}

@Preview(showBackground = true)
@Composable
fun StravaActivityDetailScreenPreview() {
    FitnessAppTheme {
        StravaActivityDetailScreen(navController = rememberNavController(), activityId = 123456L)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StravaActivityDetailScreen(
    navController: NavController,
    activityId: Long
) {
    val context = LocalContext.current
    val stravaViewModel: StravaViewModel = viewModel(factory = StravaViewModelFactory(context))

    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = MaterialTheme.extendedColors
    val isDarkTheme = isSystemInDarkTheme()
    val gradientContentColor = if (isDarkTheme) {
        colorScheme.onSurface
    } else {
        colorScheme.onPrimary
    }
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = !isDarkTheme

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = useDarkIcons
        )
    }

    var activity by remember { mutableStateOf<StravaActivity?>(null) }
    var mapViewData by remember { mutableStateOf<Map<String, String>?>(null) }
    var streams by remember { mutableStateOf<ActivityStreamsResponse?>(null) }
    var streamsFromDB by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var maxBpm by remember { mutableStateOf<Int?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Pull-to-refresh state
    val pullToRefreshState = rememberPullToRefreshState()

    suspend fun loadActivityData(showLoading: Boolean) {
        if (showLoading) {
            isLoading = true
        }
        try {
            val activityDetails = stravaViewModel.getActivityById(activityId)
            activity = activityDetails

            mapViewData = if (activityDetails != null) {
                stravaViewModel.getActivityMapView(activityId)
            } else {
                null
            }

            if (activityDetails != null) {
                streamsFromDB = stravaViewModel.getActivityStreamsFromDB(activityId)
                streams = stravaViewModel.getActivityStreams(activityId)
            } else {
                streamsFromDB = null
                streams = null
            }

            val maxBpmData = stravaViewModel.getMaxBpm()
            maxBpm = maxBpmData["max_bpm"] as? Int
            Log.d("StravaActivityDetail", "Max BPM loaded: $maxBpm")
        } catch (e: Exception) {
            Log.e("StravaActivityDetail", "Error loading activity $activityId", e)
            if (activity == null) {
                mapViewData = null
                streamsFromDB = null
                streams = null
                maxBpm = null
            }
        } finally {
            isLoading = false
            if (pullToRefreshState.isRefreshing) {
                pullToRefreshState.endRefresh()
            }
        }
    }

    // Load activity details when the screen opens or the activity ID changes
    LaunchedEffect(activityId) {
        loadActivityData(showLoading = true)
    }

    // Handle pull-to-refresh gestures
    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            loadActivityData(showLoading = false)
        }
    }

    // Main layout with pull-to-refresh support
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        extendedColors.gradientPrimary,
                        extendedColors.gradientSecondary,
                        extendedColors.gradientAccent
                    )
                )
            )
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(pullToRefreshState.nestedScrollConnection),
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
            // --- Header (accessibility: guarantee 48dp min icons) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = gradientContentColor
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Navigate back to previous screen",
                        tint = gradientContentColor
                    )
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_strava),
                        contentDescription = "Strava logo",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = when (activity?.type?.lowercase()) {
                            "ride", "virtualride" -> Icons.Default.DirectionsBike
                            "run" -> Icons.Default.DirectionsRun
                            "swim" -> Icons.Default.Pool
                            else -> Icons.Default.Speed
                        },
                        contentDescription = "Activity type: ${activity?.type}",
                        tint = gradientContentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activity?.name ?: "Activity Details",
                        color = gradientContentColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(48.dp))
            }
            // --- End Header ---

            // --- Content Card ---
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .border(BorderStroke(1.dp, extendedColors.borderSubtle), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                when {
                    isLoading -> {
                        // --- Enhanced SHIMMER SKELETONS WHILE LOADING ---
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            contentPadding = PaddingValues(vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            item {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(animationSpec = tween(300))
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        ShimmerPlaceholder(height = 80) // Activity Summary
                                        ShimmerPlaceholder(height = 32) // Section Header
                                        ShimmerPlaceholder(height = 120) // Statistics
                                        ShimmerPlaceholder(height = 32) // Section Header
                                        ShimmerPlaceholder(height = 200) // Map area
                                        ShimmerPlaceholder(height = 32) // Section Header
                                        ShimmerPlaceholder(height = 160) // Heart Rate
                                        ShimmerPlaceholder(height = 32) // Section Header
                                        ShimmerPlaceholder(height = 180) // Charts
                                        ShimmerPlaceholder(height = 56) // Button
                                    }
                                }
                            }
                        }
                    }
                    activity == null -> {
                        // --- Enhanced error state with animation ---
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(500)) + slideInVertically { it/2 }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val infiniteTransition = rememberInfiniteTransition(label = "errorPulse")
                                val pulseScale by infiniteTransition.animateFloat(
                                    initialValue = 1f, targetValue = 1.1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ), label = "errorPulseScale"
                                )

                                Card(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                                        .shadow(elevation = 8.dp, shape = CircleShape),
                                    shape = CircleShape,
                                    colors = CardDefaults.cardColors(
                                        containerColor = colorScheme.errorContainer
                                    ),
                                    elevation = CardDefaults.cardElevation(0.dp)
                                ) {
                                    Column(
                                        Modifier.fillMaxSize(), 
                                        horizontalAlignment = Alignment.CenterHorizontally, 
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Warning,
                                            contentDescription = "Error illustration",
                                            tint = colorScheme.error,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "!",
                                            color = colorScheme.error,
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Oops! Activity Not Found",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "We couldn't load this activity. Please check your connection and try again.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 24.dp.value.em
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            loadActivityData(showLoading = true)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = ButtonDefaults.buttonElevation(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Try Again",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        // --- Main Content with AnimatedVisibility per section card ---
                        val currentActivity = activity!!
                        val userMaxBpm = maxBpm?.takeIf { it > 0 }
                            ?: currentActivity.maxHeartrate?.takeIf { it > 0 }
                        val heartRateData = remember(streamsFromDB) {
                            getHeartRateDataFromStreams(streamsFromDB)
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            contentPadding = PaddingValues(bottom = 36.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            item {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(animationSpec = tween(300, delayMillis = 0)) + slideInVertically { it/4 }
                                ) {
                                    ActivityOverviewCard(activity = currentActivity)
                                }
                            }
                            item {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(animationSpec = tween(300, delayMillis = 100)) + slideInVertically { it/4 }
                                ) {
                                    TrainingTypeCard(activity = currentActivity)
                                }
                            }
                            item {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(animationSpec = tween(300, delayMillis = 150)) + slideInVertically { it/4 }
                                ) {
                                    ActivityCoreStatsRow(activity = currentActivity)
                                }
                            }
                            if (currentActivity.averageHeartrate != null || userMaxBpm != null) {
                                item {
                                    AnimatedVisibility(
                                        visible = true,
                                        enter = fadeIn(animationSpec = tween(300, delayMillis = 200)) + slideInVertically { it/4 }
                                    ) {
                                        WorkoutStructureCard(
                                            activity = currentActivity,
                                            userMaxBpm = userMaxBpm,
                                            heartRateData = heartRateData
                                        )
                                    }
                                }
                            }
                            // Detailed statistics card
                            item {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(animationSpec = tween(300, delayMillis = 250)) + slideInVertically { it/4 }
                                ) {
                                    ActivityStatsSection(activity = currentActivity)
                                }
                            }
                            // --- Section Header with Icon: Route Map ---
                            item {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(animationSpec = tween(300, delayMillis = 300)) + slideInVertically { it/4 }
                                ) {
                                    Column {
                                        SectionHeader(Icons.Filled.Place, "Route Map")
                                // ... existing RouteMapSection code ... remain unchanged
                                val mapData = mapViewData
                                when {
                                    mapData?.get("gpx_data") != null && mapData["gpx_data"]!!.isNotEmpty() -> {
                                        val gpxData = mapData["gpx_data"]!!
                                        RouteMapSection(gpxData = gpxData)
                                    }

                                    mapData?.get("html_url") != null && mapData["html_url"]!!.isNotEmpty() -> {
                                        RouteMapSection(htmlUrl = mapData["html_url"]!!)
                                    }

                                    mapData?.get("map_html") != null && mapData["map_html"]!!.isNotEmpty() -> {
                                        RouteMapSection(mapHtml = mapData["map_html"]!!)
                                    }

                                    activity?.map?.summaryPolyline != null && activity?.map?.summaryPolyline!!.isNotEmpty() -> {
                                        RouteMapSection(polyline = activity!!.map!!.summaryPolyline!!)
                                    }

                                    activity?.map?.polyline != null && activity?.map?.polyline!!.isNotEmpty() -> {
                                        RouteMapSection(polyline = activity!!.map!!.polyline!!)
                                    }
                                    else -> {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                                .shadow(
                                                    elevation = 4.dp,
                                                    shape = RoundedCornerShape(20.dp)
                                                ),
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(1.dp, extendedColors.borderSubtle),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                            elevation = CardDefaults.cardElevation(0.dp)
                                        ) {
                                            Text(
                                                text = "No route data available for this activity",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(20.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                                    }
                                }
                            }
                            // --- Section Header with Icon: Charts ---
                            item {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(animationSpec = tween(300, delayMillis = 400)) + slideInVertically { it/4 }
                                ) {
                                    Column {
                                        SectionHeader(Icons.Filled.Speed, "Performance Charts")
                                        PerformanceChartsSection(
                                            activityId = activityId,
                                            activity = activity
                                        )
                                    }
                                }
                            }
                            val showPowerCurve = isPowerCurveEligible(activity?.type)
                            if (showPowerCurve) {
                                // --- Section Header with Icon: Power Curve ---
                                item {
                                    AnimatedVisibility(
                                        visible = true,
                                        enter = fadeIn(animationSpec = tween(300, delayMillis = 500)) + slideInVertically { it/4 }
                                    ) {
                                        Column {
                                            SectionHeader(Icons.Filled.Terrain, "Power Curve")
                                            PowerCurveSection(
                                                activityId = activityId,
                                                activityType = activity!!.type,
                                                activity = activity
                                            )
                                        }
                                    }
                                }
                            }
                            // --- Enhanced Floating Bottom Button ---
                            item {
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(animationSpec = tween(300, delayMillis = 600)) + slideInVertically { it/4 }
                                ) {
                                    Column {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val stravaButtonInteractionSource = remember { MutableInteractionSource() }
                                        val isStravaButtonPressed by stravaButtonInteractionSource.collectIsPressedAsState()
                                        val stravaButtonScale by animateFloatAsState(
                                            targetValue = if (isStravaButtonPressed) 0.95f else 1f,
                                            animationSpec = tween(durationMillis = 100),
                                            label = "stravaButtonScale"
                                        )

                                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                            Button(
                                                onClick = {
                                                    val stravaUri = Uri.parse("strava://activities/$activityId")
                                                    val fallbackUri = Uri.parse("https://www.strava.com/activities/$activityId")
                                                    val stravaIntent = Intent(Intent.ACTION_VIEW, stravaUri)
                                                    val packageManager = context.packageManager

                                                    try {
                                                        if (stravaIntent.resolveActivity(packageManager) != null) {
                                                            context.startActivity(stravaIntent)
                                                        } else {
                                                            try {
                                                                CustomTabsIntent.Builder()
                                                                    .build()
                                                                    .launchUrl(context, fallbackUri)
                                                            } catch (browserException: ActivityNotFoundException) {
                                                                val webIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
                                                                if (webIntent.resolveActivity(packageManager) != null) {
                                                                    context.startActivity(webIntent)
                                                                } else {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "No application available to open Strava link",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                            }
                                                        }
                                                    } catch (e: ActivityNotFoundException) {
                                                        Toast.makeText(
                                                            context,
                                                            "Unable to open Strava",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    } catch (e: Exception) {
                                                        Log.e("StravaActivityDetail", "Failed to open Strava activity", e)
                                                        Toast.makeText(
                                                            context,
                                                            "Unable to open Strava",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(56.dp)
                                                    .graphicsLayer(
                                                        scaleX = stravaButtonScale,
                                                        scaleY = stravaButtonScale
                                                    ),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = extendedColors.gradientPrimary,
                                                    contentColor = colorScheme.onPrimary
                                                ),
                                                elevation = ButtonDefaults.buttonElevation(12.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                interactionSource = stravaButtonInteractionSource
                                            ) {
                                                Icon(
                                                    Icons.Default.Link,
                                                    contentDescription = "Open activity in Strava app",
                                                    modifier = Modifier.size(24.dp),
                                                    tint = MaterialTheme.colorScheme.onPrimary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    "View in Strava",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    Icons.Default.ArrowForward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // Pull-to-refresh container
        PullToRefreshContainer(
            state = pullToRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun ActivityStatsCard(activity: StravaActivity) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Activity Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
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
    val colorScheme = MaterialTheme.colorScheme

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActivityStatsGrid(activity: StravaActivity) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Activity Statistics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Create list of stats
            val stats = listOfNotNull(
                activity.distance?.let { "Distance" to "${String.format("%.2f", it / 1000)} km" },
                "Moving Time" to formatMovingTime(activity.movingTime),
                activity.averageSpeed?.let { "Avg Speed" to "${String.format("%.2f", it * 3.6)} km/h" },
                activity.maxSpeed?.let { "Max Speed" to "${String.format("%.2f", it * 3.6)} km/h" },
                activity.totalElevationGain?.let { "Elevation" to "${String.format("%.0f", it)} m" },
                activity.averageHeartrate?.let { "Avg HR" to "${it.toInt()} bpm" },
                activity.maxHeartrate?.let { "Max HR" to "$it bpm" }
            )

            // Display stats in 2-column grid
            stats.chunked(2).forEach { rowStats ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    rowStats.forEach { (label, value) ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // Add spacer if odd number of stats in row
                    if (rowStats.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

// Enhanced ActivityStatsGridContent using StatRow for better visual hierarchy
@Composable
private fun ActivityStatsGridContent(activity: StravaActivity) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Distance
        activity.distance?.let { distance ->
            StatRow(
                icon = Icons.Filled.Place,
                label = "Distance",
                value = "${String.format("%.2f", distance / 1000)} km"
            )
        }

        // Moving time
        StatRow(
            icon = Icons.Filled.Timer,
            label = "Moving Time",
            value = formatMovingTime(activity.movingTime)
        )

        // Average speed
        activity.averageSpeed?.let { speed ->
            StatRow(
                icon = Icons.Filled.Speed,
                label = "Average Speed",
                value = "${String.format("%.2f", speed * 3.6)} km/h"
            )
        }

        // Max speed
        activity.maxSpeed?.let { speed ->
            StatRow(
                icon = Icons.Filled.Speed,
                label = "Max Speed",
                value = "${String.format("%.2f", speed * 3.6)} km/h"
            )
        }

        // Elevation gain
        activity.totalElevationGain?.let { elevation ->
            StatRow(
                icon = Icons.Filled.Terrain,
                label = "Elevation Gain",
                value = "${String.format("%.0f", elevation)} m"
            )
        }

        // Average heart rate
        activity.averageHeartrate?.let { hr ->
            StatRow(
                icon = Icons.Filled.Favorite,
                label = "Average Heart Rate",
                value = "${hr.toInt()} bpm"
            )
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

// Enhanced FullInteractiveWebViewMapContent with zoom controls, error handling, and fullscreen toggle
@Composable
private fun FullInteractiveWebViewMapContent(htmlMapUrl: String) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = MaterialTheme.extendedColors

    var isMapLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isMapLoading = false
                            hasError = false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?
                        ) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            isMapLoading = false
                            hasError = true
                        }
                    }
                    settings.apply {
                        javaScriptEnabled = true
                        builtInZoomControls = true
                        displayZoomControls = false // Hide default zoom controls
                        setSupportZoom(true)
                    }
                    loadUrl(htmlMapUrl)
                    webView = this
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        )

        // Loading indicator
        if (isMapLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = colorScheme.primary
            )
        }

        // Error state with retry button
        if (hasError) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Map loading error",
                        tint = extendedColors.warning,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Failed to load map",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Check your connection",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            hasError = false
                            isMapLoading = true
                            webView?.reload()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Retry loading map",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Control overlay (top-right corner)
        if (!isMapLoading && !hasError) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Zoom In button
                IconButton(
                    onClick = { webView?.zoomIn() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            colorScheme.surface.copy(alpha = 0.9f),
                            RoundedCornerShape(6.dp)
                        )
                ) {
                    Icon(
                        Icons.Filled.ZoomIn,
                        contentDescription = "Zoom in on map",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Zoom Out button
                IconButton(
                    onClick = { webView?.zoomOut() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            colorScheme.surface.copy(alpha = 0.9f),
                            RoundedCornerShape(6.dp)
                        )
                ) {
                    Icon(
                        Icons.Filled.ZoomOut,
                        contentDescription = "Zoom out on map",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Fullscreen toggle button
                IconButton(
                    onClick = {
                        // TODO: Implement fullscreen functionality
                        // This would require navigation to a fullscreen map view
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            colorScheme.surface.copy(alpha = 0.9f),
                            RoundedCornerShape(6.dp)
                        )
                ) {
                    Icon(
                        Icons.Filled.Fullscreen,
                        contentDescription = "View map in fullscreen",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Enhanced HeartRateZoneDistribution with actual zone calculation using user's maxBPM
@Composable
private fun HeartRateZoneDistribution(
    avgHr: Float,
    maxHr: Float,
    maxBpm: Int,
    heartRateData: List<Int>? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = MaterialTheme.extendedColors

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Redesigned HR Metrics Header (Suggestion 2 & 8)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HrStat(label = "Average HR", value = "${avgHr.toInt()}", unit = "bpm")
            HrStat(label = "Max Activity HR", value = "${maxHr.toInt()}", unit = "bpm")
            HrStat(label = "Your Max BPM", value = if (maxBpm > 0) "$maxBpm" else "Not Set", unit = "bpm")
        }


        // HR Zone Distribution Bars with actual calculation
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Heart Rate Zones Distribution",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurfaceVariant
            )

            // Calculate correct zones based on user's maxBPM
            val zone1Max = (maxBpm * 0.6).toInt()
            val zone2Min = zone1Max + 1
            val zone2Max = (maxBpm * 0.7).toInt()
            val zone3Min = zone2Max + 1
            val zone3Max = (maxBpm * 0.8).toInt()
            val zone4Min = zone3Max + 1
            val zone4Max = (maxBpm * 0.9).toInt()
            val zone5Min = zone4Max + 1

            // Calculate actual zone percentages from heart rate data
            val (zonePercentages, timeSpentInZones) = calculateActualZonePercentages(heartRateData, maxBpm)
            val maxPercentage = zonePercentages.maxOrNull() ?: 0f

            val zones = listOf(
                ZoneInfo(
                    "Zone 1 (Recovery)",
                    "Up to $zone1Max",
                    zonePercentages[0],
                    timeSpentInZones[0]
                ),
                ZoneInfo(
                    "Zone 2 (Aerobic)",
                    "$zone2Min-$zone2Max",
                    zonePercentages[1],
                    timeSpentInZones[1]
                ),
                ZoneInfo(
                    "Zone 3 (Tempo)",
                    "$zone3Min-$zone3Max",
                    zonePercentages[2],
                    timeSpentInZones[2]
                ),
                ZoneInfo(
                    "Zone 4 (Threshold)", 
                    "$zone4Min-$zone4Max", 
                    zonePercentages[3], 
                    timeSpentInZones[3]
                ),
                ZoneInfo(
                    "Zone 5 (VO2 Max)", 
                    "$zone5Min+", 
                    zonePercentages[4], 
                    timeSpentInZones[4]
                )
            )

            zones.forEachIndexed { index, zoneInfo ->
                val isMostActiveZone = zoneInfo.percentage == maxPercentage && maxPercentage > 0
                val animatedProgress by animateFloatAsState(
                    targetValue = (zoneInfo.percentage / 100f).coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
                    label = "zoneProgress"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (isMostActiveZone) Modifier.border(
                                1.dp,
                                getZoneColor(index + 1),
                                RoundedCornerShape(8.dp)
                            )
                            else Modifier
                        )
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Zone color indicator (Suggestion #4)
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                getZoneColor(index + 1),
                                RoundedCornerShape(2.dp)
                            )
                    )

                    // Zone name and progress
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = zoneInfo.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${zoneInfo.range} bpm",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                // Combined Percentage and Duration (Suggestion #5)
                                Text(
                                    text = if (zoneInfo.timeSpent.isNotEmpty()) {
                                        "${zoneInfo.percentage.toInt()}% (${zoneInfo.timeSpent})"
                                    } else {
                                        "${zoneInfo.percentage.toInt()}%"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = animatedProgress, // Animated progress (Suggestion #10)
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = getZoneColor(index + 1),
                            trackColor = extendedColors.chartGrid
                        )
                    }
                }
            }

            // Show message about data accuracy
            if (heartRateData.isNullOrEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = extendedColors.warningContainer),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = extendedColors.warning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Zone distribution is an estimate. For accurate results, sync activities with detailed heart rate data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = extendedColors.warning,
                            lineHeight = 16.dp.value.em
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = extendedColors.successContainer),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = extendedColors.success,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Zones calculated from ${heartRateData.size} heart rate data points",
                            style = MaterialTheme.typography.bodySmall,
                            color = extendedColors.success.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HrStat(label: String, value: String, unit: String) {
    val colorScheme = MaterialTheme.colorScheme

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurfaceVariant
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant
        )
    }
}

// Data class for zone information
data class ZoneInfo(
    val name: String,
    val range: String,
    val percentage: Float,
    val timeSpent: String
)

// Try to extract heart rate data from streams DB (tries multiple keys and nested cases)
fun getHeartRateDataFromStreams(streams: Map<String, Any>?): List<Int>? {
    if (streams == null) {
        Log.d("HeartRateZones", "Streams data is null")
        return null
    }

    Log.d("HeartRateZones", "Available streams keys: ${streams.keys}")

    // Try top level "heartrate" as List<Int>
    (streams["heartrate"] as? List<Int>)?.let {
        Log.d("HeartRateZones", "Found heartrate as List<Int> with ${it.size} data points")
        return it
    }

    // Try top level "heartrate" as List<Float> (convert to Int)
    (streams["heartrate"] as? List<Float>)?.map { it.toInt() }?.let {
        if (it.isNotEmpty()) {
            Log.d("HeartRateZones", "Found heartrate as List<Float> with ${it.size} data points")
            return it
        }
    }

    // Try nested "streams" object
    val nestedStreams = streams["streams"] as? Map<*, *>
    if (nestedStreams != null) {
        Log.d("HeartRateZones", "Found nested streams with keys: ${nestedStreams.keys}")

        // Try "heartrate" as List<Int>
        (nestedStreams["heartrate"] as? List<Int>)?.let {
            Log.d(
                "HeartRateZones",
                "Found nested heartrate as List<Int> with ${it.size} data points"
            )
            return it
        }

        // Try "heartrate" as List<Float> (convert to Int)
        (nestedStreams["heartrate"] as? List<Float>)?.map { it.toInt() }?.let {
            if (it.isNotEmpty()) {
                Log.d(
                    "HeartRateZones",
                    "Found nested heartrate as List<Float> with ${it.size} data points"
                )
                return it
            }
        }
    }

    // Try possible alternate key names (e.g. "heart_rate", "hr")
    (streams["heart_rate"] as? List<Int>)?.let {
        Log.d("HeartRateZones", "Found heart_rate as List<Int> with ${it.size} data points")
        return it
    }

    (streams["hr"] as? List<Int>)?.let {
        Log.d("HeartRateZones", "Found hr as List<Int> with ${it.size} data points")
        return it
    }

    (streams["heart_rate"] as? List<Float>)?.map { it.toInt() }?.let {
        if (it.isNotEmpty()) {
            Log.d("HeartRateZones", "Found heart_rate as List<Float> with ${it.size} data points")
            return it
        }
    }

    (streams["hr"] as? List<Float>)?.map { it.toInt() }?.let {
        if (it.isNotEmpty()) {
            Log.d("HeartRateZones", "Found hr as List<Float> with ${it.size} data points")
            return it
        }
    }

    Log.d("HeartRateZones", "No heart rate data found in any expected format")
    return null
}

// Helper function to calculate actual zone percentages from heart rate data
private fun calculateActualZonePercentages(
    heartRateData: List<Int>?,
    maxBpm: Int
): Pair<List<Float>, List<String>> {
    if (heartRateData.isNullOrEmpty()) {
        // Return estimated percentages if no data available
        return Pair(
            listOf(25f, 35f, 25f, 10f, 5f),
            listOf("", "", "", "", "")
        )
    }

    val totalDataPoints = heartRateData.size
    val zone1Max = (maxBpm * 0.6).toInt()
    val zone2Max = (maxBpm * 0.7).toInt()
    val zone3Max = (maxBpm * 0.8).toInt()
    val zone4Max = (maxBpm * 0.9).toInt()

    val zoneCounts = intArrayOf(0, 0, 0, 0, 0)

    heartRateData.forEach { hr ->
        when {
            hr <= zone1Max -> zoneCounts[0]++
            hr <= zone2Max -> zoneCounts[1]++
            hr <= zone3Max -> zoneCounts[2]++
            hr <= zone4Max -> zoneCounts[3]++
            else -> zoneCounts[4]++
        }
    }

    val percentages = zoneCounts.map { (it.toFloat() / totalDataPoints) * 100f }

    // Calculate time spent in each zone (assuming 1 data point per second)
    val timeSpentList = zoneCounts.map { count ->
        if (count == 0) ""
        else formatSecondsToTime(count)
    }

    return Pair(percentages, timeSpentList)
}

// Helper function to format seconds to readable time
private fun formatSecondsToTime(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${secs}s"
        else -> "${secs}s"
    }
}

// Helper for zone colors (from TrainingDetailScreen)
@Composable
private fun getZoneColor(zone: Int): Color {
    val extendedColors = MaterialTheme.extendedColors
    return when (zone) {
        1 -> extendedColors.hrZone1
        2 -> extendedColors.hrZone2
        3 -> extendedColors.success
        4 -> extendedColors.hrZone4
        else -> extendedColors.hrZone5
    }
}

// Helper function for formatting moving time
private fun formatMovingTime(movingTime: Int): String {
    val hours = movingTime / 3600
    val minutes = (movingTime % 3600) / 60
    val seconds = movingTime % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

@Composable
private fun FullInteractiveWebViewMapCard(htmlMapUrl: String) {
    val colorScheme = MaterialTheme.colorScheme

    var isMapLoading by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = {
                    WebView(it).apply {
                        layoutParams = android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isMapLoading = false
                            }
                        }
                        settings.javaScriptEnabled = true
                        loadUrl(htmlMapUrl)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )

            if (isMapLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colorScheme.primary
                )
            }
        }
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
        isoDate.substringBefore("T") // Fallback
    }
}
