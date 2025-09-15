package com.example.fitnessapp.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnessapp.viewmodel.StravaViewModel
import com.example.fitnessapp.viewmodel.StravaViewModelFactory
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

// Data class pentru metricile combinare
data class CombinedMetrics(
    val time: List<Float>,           // timp în secunde
    val power: List<Float>?,         // putere în W
    val heartRate: List<Float>?,     // HR în bpm
    val cadence: List<Float>?,       // cadență în rpm
    val speed: List<Float>?,         // viteză în km/h
    val altitude: List<Float>?       // altitudine în m
)

// Data class pentru markerul interactiv
data class AdvancedMarkerData(
    val time: String,
    val power: String?,
    val heartRate: String?,
    val cadence: String?,
    val xPosition: Float
)

@Composable
fun ActivityChartsComponent(
    activityId: Long,
    modifier: Modifier = Modifier,
    ftp: Float? = null,
    fthr: Float? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val stravaViewModel: StravaViewModel = viewModel(factory = StravaViewModelFactory(context))
    val scope = rememberCoroutineScope()

    var streamsData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var combinedMetrics by remember { mutableStateOf<CombinedMetrics?>(null) }
    var dataSource by remember { mutableStateOf<String?>(null) }
    var selectedMarker by remember { mutableStateOf<AdvancedMarkerData?>(null) }

    // Filter states for chart visibility (persisted)
    var showPower by rememberSaveable { mutableStateOf(true) }
    var showHr by rememberSaveable { mutableStateOf(true) }
    var showCad by rememberSaveable { mutableStateOf(true) }

    // Marker locking state for long-press functionality
    var isMarkerLocked by remember { mutableStateOf(false) }

    // Animation states for color transitions (smoother than alpha)
    val powerColor by animateColorAsState(
        targetValue = if (showPower && combinedMetrics?.power != null) 
            Color(0xFFFF6B35) else Color(0xFFFF6B35).copy(alpha = 0.3f),
        animationSpec = tween(durationMillis = 800),
        label = "powerColor"
    )
    val hrColor by animateColorAsState(
        targetValue = if (showHr && combinedMetrics?.heartRate != null) 
            Color(0xFFEF4444) else Color(0xFFEF4444).copy(alpha = 0.3f),
        animationSpec = tween(durationMillis = 800),
        label = "hrColor"
    )
    val cadenceColor by animateColorAsState(
        targetValue = if (showCad && combinedMetrics?.cadence != null) 
            Color(0xFF22C55E) else Color(0xFF22C55E).copy(alpha = 0.3f),
        animationSpec = tween(durationMillis = 800),
        label = "cadenceColor"
    )

    // Zoom and pan states (persisted)
    var scale by rememberSaveable { mutableStateOf(1f) }
    var offsetX by rememberSaveable { mutableStateOf(0f) }
    var offsetY by rememberSaveable { mutableStateOf(0f) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    val chartPadding = 24.dp

    // Dynamic height calculation for better small screen support
    val screenWidthDp = configuration.screenWidthDp.dp
    val chartHeight = max(200f, screenWidthDp.value * 0.6f).dp

    // Dark theme support
    val isDarkTheme = isSystemInDarkTheme()
    val cardBackgroundColor = if (isDarkTheme) Color(0xFF1F2937) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1F2937)
    val gridColor = if (isDarkTheme) Color(0xFF374151) else Color(0xFFD1D5DB)
    val axisColor = if (isDarkTheme) Color(0xFF6B7280) else Color(0xFF9CA3AF)

    // Load streams data on component creation
    LaunchedEffect(activityId) {
        try {
            Log.d("ActivityChartsComponent", "Loading streams for activity $activityId")
            val streams = stravaViewModel.getActivityStreamsFromDB(activityId)
            Log.d("ActivityChartsComponent", "Received streams: ${streams.keys}")
            streamsData = streams
            dataSource = streams["source"] as? String

            // Extract and combine metrics
            val actualStreams = streams["streams"] as? Map<*, *>
            if (actualStreams != null) {
                combinedMetrics = extractCombinedMetrics(actualStreams)
                Log.d("ActivityChartsComponent", "Combined metrics extracted successfully")
            }

            isLoading = false
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Composition disposed; don't treat as error
            Log.d("ActivityChartsComponent", "Streams load cancelled for activity $activityId")
        } catch (e: Exception) {
            Log.e("ActivityChartsComponent", "Error loading streams", e)
            isLoading = false
        }
    }

    // Use Column layout to ensure proper ordering: Chart first, then card content
    Column(modifier = modifier.fillMaxWidth()) {
        // Minimal marker row — NO CARD, just same-height pill chips
        if (selectedMarker != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MinimalMetricPill("Power", selectedMarker?.power, Color(0xFFFF6B35))
                MinimalMetricPill("HR", selectedMarker?.heartRate, Color(0xFFEF4444))
                MinimalMetricPill("Cadence", selectedMarker?.cadence, Color(0xFF22C55E))
                Spacer(Modifier.weight(1f))
                Text(
                    text = selectedMarker?.time ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280),
                    maxLines = 1,
                    softWrap = false
                )
            }
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                thickness = 1.dp,
                color = Color(0xFFE5EAF2)
            )
        }
        // Tight chart, no extra card or vertical space
        if (!isLoading && combinedMetrics != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .semantics {
                            contentDescription = "Interactive activity chart"
                            role = Role.Image
                        }
                        .transformable(transformableState)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { offset ->
                                    if (isMarkerLocked) {
                                        isMarkerLocked = false
                                        return@detectTapGestures
                                    }
                                    if (combinedMetrics == null || combinedMetrics!!.time.isEmpty()) return@detectTapGestures
                                    val paddingPx = with(density) { chartPadding.toPx() }
                                    val chartWidth = size.width - 2 * paddingPx
                                    val progress =
                                        ((offset.x - paddingPx) / chartWidth).coerceIn(0f, 1f)
                                    val dataSize = combinedMetrics!!.time.size
                                    val index = (progress * (dataSize - 1)).roundToInt()
                                        .coerceIn(0, dataSize - 1)
                                    val originalTimeSec =
                                        combinedMetrics!!.time.getOrNull(index) ?: 0f
                                    val timeSec = (originalTimeSec / 5f).roundToInt() * 5f
                                    val minutes = (timeSec / 60).toInt()
                                    val seconds = (timeSec % 60).toInt()
                                    val timeFormatted = String.format("%d:%02d", minutes, seconds)
                                    val powerVal = combinedMetrics!!.power?.getOrNull(index)
                                    val hrVal = combinedMetrics!!.heartRate?.getOrNull(index)
                                    val cadVal = combinedMetrics!!.cadence?.getOrNull(index)
                                    selectedMarker = AdvancedMarkerData(
                                        time = timeFormatted,
                                        power = powerVal?.let { "${it.toInt()} W" },
                                        heartRate = hrVal?.let { "${it.toInt()} bpm" },
                                        cadence = cadVal?.let { "${it.toInt()} rpm" },
                                        xPosition = offset.x
                                    )
                                },
                                onLongPress = { offset ->
                                    if (combinedMetrics == null || combinedMetrics!!.time.isEmpty()) return@detectTapGestures
                                    isMarkerLocked = true
                                    val paddingPx = with(density) { chartPadding.toPx() }
                                    val chartWidth = size.width - 2 * paddingPx
                                    val progress =
                                        ((offset.x - paddingPx) / chartWidth).coerceIn(0f, 1f)
                                    val dataSize = combinedMetrics!!.time.size
                                    val index = (progress * (dataSize - 1)).roundToInt()
                                        .coerceIn(0, dataSize - 1)
                                    val originalTimeSec =
                                        combinedMetrics!!.time.getOrNull(index) ?: 0f
                                    val timeSec = (originalTimeSec / 5f).roundToInt() * 5f
                                    val minutes = (timeSec / 60).toInt()
                                    val seconds = (timeSec % 60).toInt()
                                    val timeFormatted = String.format("%d:%02d", minutes, seconds)
                                    val powerVal = combinedMetrics!!.power?.getOrNull(index)
                                    val hrVal = combinedMetrics!!.heartRate?.getOrNull(index)
                                    val cadVal = combinedMetrics!!.cadence?.getOrNull(index)
                                    selectedMarker = AdvancedMarkerData(
                                        time = timeFormatted,
                                        power = powerVal?.let { "${it.toInt()} W" },
                                        heartRate = hrVal?.let { "${it.toInt()} bpm" },
                                        cadence = cadVal?.let { "${it.toInt()} rpm" },
                                        xPosition = offset.x
                                    )
                                },
                                onDoubleTap = {
                                    scale = 1f; offsetX = 0f; offsetY = 0f
                                }
                            )
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    // Drag
                                    event.changes.firstOrNull { it.pressed }?.let { change ->
                                        if (isMarkerLocked && change.pressed && event.type == androidx.compose.ui.input.pointer.PointerEventType.Move) {
                                            // Scrub on drag
                                            if (combinedMetrics == null || combinedMetrics!!.time.isEmpty()) return@awaitPointerEventScope
                                            val paddingPx = with(density) { chartPadding.toPx() }
                                            val chartWidth = size.width - 2 * paddingPx
                                            val progress =
                                                ((change.position.x - paddingPx) / chartWidth).coerceIn(
                                                    0f,
                                                    1f
                                                )
                                            val dataSize = combinedMetrics!!.time.size
                                            val index = (progress * (dataSize - 1)).roundToInt()
                                                .coerceIn(0, dataSize - 1)
                                            val originalTimeSec =
                                                combinedMetrics!!.time.getOrNull(index) ?: 0f
                                            val timeSec = (originalTimeSec / 5f).roundToInt() * 5f
                                            val minutes = (timeSec / 60).toInt()
                                            val seconds = (timeSec % 60).toInt()
                                            val timeFormatted =
                                                String.format("%d:%02d", minutes, seconds)
                                            val powerVal = combinedMetrics!!.power?.getOrNull(index)
                                            val hrVal =
                                                combinedMetrics!!.heartRate?.getOrNull(index)
                                            val cadVal = combinedMetrics!!.cadence?.getOrNull(index)
                                            selectedMarker = AdvancedMarkerData(
                                                time = timeFormatted,
                                                power = powerVal?.let { "${it.toInt()} W" },
                                                heartRate = hrVal?.let { "${it.toInt()} bpm" },
                                                cadence = cadVal?.let { "${it.toInt()} rpm" },
                                                xPosition = change.position.x
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        .transformable(transformableState)
                ) {
                    drawTrainerRoadStyleChart(
                        metrics = combinedMetrics!!,
                        showPower = showPower,
                        showHeartRate = showHr,
                        showCadence = showCad,
                        selectedMarker = selectedMarker,
                        chartPadding = with(density) { chartPadding.toPx() },
                        gridColor = gridColor,
                        axisColor = axisColor,
                        powerColor = powerColor,
                        hrColor = hrColor,
                        cadenceColor = cadenceColor,
                        scale = scale,
                        offsetX = offsetX,
                        offsetY = offsetY,
                        ftp = ftp,
                        fthr = fthr
                    )
                }
            }
        }
        // Analysis row below, minimal spacing
        if (!isLoading && combinedMetrics != null) {
            Spacer(modifier = Modifier.height(12.dp))
            // Use a Row or simple Column, no additional card
            Text(
                text = "TrainerRoad-Style Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            // ... rest of analysis content or call ChartContent, as before ...
            ChartContent(
                metrics = combinedMetrics!!,
                showPower = showPower,
                showHeartRate = showHr,
                showCadence = showCad,
                onTogglePower = { showPower = !showPower },
                onToggleHr = { showHr = !showHr },
                onToggleCad = { showCad = !showCad },
                selectedMarker = selectedMarker,
                onDismissMarker = { selectedMarker = null },
                textColor = textColor
            )
        }
    }
}

@Composable
private fun MinimalMetricPill(label: String, value: String?, color: Color) {
    if (value != null) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.14f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .height(36.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LoadingState(dataSource: String?) {
    val isDarkTheme = isSystemInDarkTheme()
    val shimmerColors = if (isDarkTheme) {
        listOf(
            Color(0xFF374151),
            Color(0xFF4B5563),
            Color(0xFF374151)
        )
    } else {
        listOf(
            Color(0xFFE5E7EB),
            Color(0xFFF3F4F6),
            Color(0xFFE5E7EB)
        )
    }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Shimmer chart placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(brush, RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Shimmer text placeholders
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f - index * 0.1f)
                    .height(16.dp)
                    .background(brush, RoundedCornerShape(8.dp))
            )
            if (index < 2) Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (dataSource == "api") "Loading data from API..." else "Loading analysis...",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDarkTheme) Color(0xFF9CA3AF) else Color(0xFF6B7280)
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .semantics {
                contentDescription =
                    "No analysis data available for this activity. Chart cannot be displayed."
                role = Role.Image
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No analysis data available for this activity",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280) // Light gray for light theme
        )
    }
}

@Composable
private fun ChartContent(
    metrics: CombinedMetrics,
    showPower: Boolean,
    showHeartRate: Boolean,
    showCadence: Boolean,
    onTogglePower: () -> Unit,
    onToggleHr: () -> Unit,
    onToggleCad: () -> Unit,
    selectedMarker: AdvancedMarkerData?,
    onDismissMarker: () -> Unit,
    textColor: Color
) {
    // Legend with colored dots
    MetricLegend(
        showPower = showPower,
        showHr = showHeartRate,
        showCad = showCadence,
        onTogglePower = onTogglePower,
        onToggleHr = onToggleHr,
        onToggleCad = onToggleCad,
        textColor = textColor
    )

    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun MetricLegend(
    showPower: Boolean,
    showHr: Boolean,
    showCad: Boolean,
    onTogglePower: () -> Unit,
    onToggleHr: () -> Unit,
    onToggleCad: () -> Unit,
    textColor: Color
) {
    // Define all available metrics for horizontal scrolling
    val availableMetrics = listOf(
        LegendMetric("Power", Color(0xFFFF6B35), showPower, onTogglePower, "legend-power"),
        LegendMetric("HR", Color(0xFFEF4444), showHr, onToggleHr, "legend-hr"),
        LegendMetric("Cadence", Color(0xFF22C55E), showCad, onToggleCad, "legend-cadence"),
        // Additional metrics that could be enabled in the future
        LegendMetric("Speed", Color(0xFF3B82F6), false, {}, "legend-speed"),
        LegendMetric("Altitude", Color(0xFF8B5CF6), false, {}, "legend-altitude")
    ).filter { it.label in listOf("Power", "HR", "Cadence") } // Only show main metrics for now

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(availableMetrics) { metric ->
            LegendItem(
                color = metric.color,
                label = metric.label,
                isVisible = metric.isVisible,
                onClick = metric.onClick,
                textColor = textColor,
                testTag = metric.testTag
            )
        }
    }
}

// Data class for legend metrics
private data class LegendMetric(
    val label: String,
    val color: Color,
    val isVisible: Boolean,
    val onClick: () -> Unit,
    val testTag: String
)

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    isVisible: Boolean,
    onClick: () -> Unit,
    textColor: Color,
    testTag: String = ""
) {
    // Add animated color for smooth transitions
    val animatedDotColor by animateColorAsState(
        targetValue = if (isVisible) color else color.copy(alpha = 0.3f),
        animationSpec = tween(durationMillis = 300),
        label = "legendDotColor"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
            .testTag(testTag)
            .semantics {
                contentDescription = "${if (isVisible) "Hide" else "Show"} $label data on chart"
                role = Role.Switch
            }
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(animatedDotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isVisible) textColor else textColor.copy(alpha = 0.5f),
            fontWeight = if (isVisible) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun EnhancedMarkerTooltip(
    markerData: AdvancedMarkerData,
    textColor: Color,
    cardBackgroundColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .animateContentSize(animationSpec = tween(300))
            .semantics {
                contentDescription = "Data point at ${markerData.time}. ${
                    listOfNotNull(
                        markerData.power?.let { "Power: $it" },
                        markerData.heartRate?.let { "Heart rate: $it" },
                        markerData.cadence?.let { "Cadence: $it" }
                    ).joinToString(", ")
                }"
                role = Role.Image
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Data Point",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = markerData.time,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AnimatedMetricDisplay("Power", markerData.power, Color(0xFFFF6B35))
                AnimatedMetricDisplay("HR", markerData.heartRate, Color(0xFFEF4444))
                AnimatedMetricDisplay("Cadence", markerData.cadence, Color(0xFF22C55E))
            }
        }
    }
}

@Composable
private fun MinimalistMarkerDisplay(
    markerData: AdvancedMarkerData
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Marker Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Text(
                    text = markerData.time,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricDisplay("Power", markerData.power, Color(0xFFFF6B35))
                MetricDisplay("HR", markerData.heartRate, Color(0xFFEF4444))
                MetricDisplay("Cadence", markerData.cadence, Color(0xFF22C55E))
            }
        }
    }
}

@Composable
private fun AnimatedMetricDisplay(
    label: String,
    value: String?,
    color: Color
) {
    if (value != null) {
        val animatedAlpha by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400, delayMillis = 100),
            label = "metricAlpha"
        )

        Card(
            modifier = Modifier
                .padding(4.dp)
                .animateContentSize(animationSpec = tween(300)),
            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .graphicsLayer(alpha = animatedAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MetricDisplay(
    label: String,
    value: String?,
    color: Color
) {
    if (value != null) {
        Card(
            modifier = Modifier.padding(4.dp),
            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color
                )
            }
        }
    }
}

private fun DrawScope.drawTrainerRoadStyleChart(
    metrics: CombinedMetrics,
    showPower: Boolean,
    showHeartRate: Boolean,
    showCadence: Boolean,
    selectedMarker: AdvancedMarkerData?,
    chartPadding: Float,
    gridColor: Color,
    axisColor: Color,
    powerColor: Color,
    hrColor: Color,
    cadenceColor: Color,
    scale: Float = 1f,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    ftp: Float? = null,
    fthr: Float? = null
) {
    val chartWidth = size.width - 2 * chartPadding
    val chartHeight = size.height - 2 * chartPadding

    // Draw power zone bands if power data exists and FTP is provided
    if (showPower && metrics.power != null && ftp != null) {
        val powerMax = metrics.power.maxOrNull() ?: 1f
        val powerMin = metrics.power.minOrNull() ?: 0f
        val powerRange = powerMax - powerMin

        if (powerRange > 0f) {
            val zones = listOf(
                0.0f to 0.55f,  // Recovery (Z1)
                0.55f to 0.75f, // Endurance (Z2)
                0.75f to 0.90f, // Tempo (Z3)
                0.90f to 1.05f, // Threshold (Z4)
                1.05f to 1.20f, // VO2 Max (Z5)
                1.20f to 1.50f, // Anaerobic (Z6)
                1.50f to 3.0f   // Neuromuscular (Z7)
            )

            val zoneColors = listOf(
                Color(0xFF22C55E).copy(alpha = 0.05f), // Green - Recovery
                Color(0xFF3B82F6).copy(alpha = 0.05f), // Blue - Endurance
                Color(0xFFFBBF24).copy(alpha = 0.05f), // Yellow - Tempo
                Color(0xFFF97316).copy(alpha = 0.05f), // Orange - Threshold
                Color(0xFFEF4444).copy(alpha = 0.05f), // Red - VO2 Max
                Color(0xFF8B5CF6).copy(alpha = 0.05f), // Purple - Anaerobic
                Color(0xFF1F2937).copy(alpha = 0.05f)  // Dark - Neuromuscular
            )

            zones.forEachIndexed { index, (lowerPercent, upperPercent) ->
                val lowerPower = ftp * lowerPercent
                val upperPower = ftp * upperPercent

                if (lowerPower <= powerMax && upperPower >= powerMin) {
                    val normalizedLower = 0.1f + ((lowerPower.coerceIn(powerMin, powerMax) - powerMin) / powerRange) * 0.8f
                    val normalizedUpper = 0.1f + ((upperPower.coerceIn(powerMin, powerMax) - powerMin) / powerRange) * 0.8f

                    val lowerY = chartPadding + chartHeight - (normalizedLower * chartHeight)
                    val upperY = chartPadding + chartHeight - (normalizedUpper * chartHeight)

                    drawRect(
                        color = zoneColors[index],
                        topLeft = Offset(chartPadding, upperY),
                        size = androidx.compose.ui.geometry.Size(chartWidth, lowerY - upperY)
                    )
                }
            }
        }
    }

    // Draw HR zone bands if HR data exists and FTHR is provided
    if (showHeartRate && metrics.heartRate != null && fthr != null) {
        val hrMax = metrics.heartRate.maxOrNull() ?: 1f
        val hrMin = metrics.heartRate.minOrNull() ?: 0f
        val hrRange = hrMax - hrMin

        if (hrRange > 0f) {
            val hrZones = listOf(
                0.0f to 0.68f,  // Z1 - Active Recovery
                0.68f to 0.83f, // Z2 - Aerobic Base
                0.83f to 0.94f, // Z3 - Aerobic Build
                0.94f to 1.05f, // Z4 - Lactate Threshold
                1.05f to 1.15f  // Z5 - VO2 Max
            )

            val hrZoneColors = listOf(
                Color(0xFF22C55E).copy(alpha = 0.05f), // Green - Z1
                Color(0xFF3B82F6).copy(alpha = 0.05f), // Blue - Z2
                Color(0xFFFBBF24).copy(alpha = 0.05f), // Yellow - Z3
                Color(0xFFF97316).copy(alpha = 0.05f), // Orange - Z4
                Color(0xFFEF4444).copy(alpha = 0.05f)  // Red - Z5
            )

            hrZones.forEachIndexed { index, (lowerPercent, upperPercent) ->
                val lowerHR = fthr * lowerPercent
                val upperHR = fthr * upperPercent

                if (lowerHR <= hrMax && upperHR >= hrMin) {
                    val normalizedLower = 0.1f + ((lowerHR.coerceIn(hrMin, hrMax) - hrMin) / hrRange) * 0.8f
                    val normalizedUpper = 0.1f + ((upperHR.coerceIn(hrMin, hrMax) - hrMin) / hrRange) * 0.8f

                    val lowerY = chartPadding + chartHeight - (normalizedLower * chartHeight)
                    val upperY = chartPadding + chartHeight - (normalizedUpper * chartHeight)

                    drawRect(
                        color = hrZoneColors[index],
                        topLeft = Offset(chartPadding, upperY),
                        size = androidx.compose.ui.geometry.Size(chartWidth, lowerY - upperY)
                    )
                }
            }
        }
    }

    // Draw horizontal grid lines with Y-axis labels
    for (i in 0..4) {
        val y = chartPadding + (i.toFloat() / 4) * chartHeight
        drawLine(
            color = if (i == 0 || i == 4) axisColor else gridColor,
            start = Offset(chartPadding, y),
            end = Offset(size.width - chartPadding, y),
            strokeWidth = if (i == 0 || i == 4) 1f else 0.5f
        )

        // Draw Y-axis labels (showing percentage values)
        val labelValue = (100 - (i * 25)).toString() + "%"
        drawContext.canvas.nativeCanvas.drawText(
            labelValue,
            chartPadding + 10.dp.toPx(),
            y + 4.dp.toPx(),
            android.graphics.Paint().apply {
                color = axisColor.toArgb()
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.LEFT
                isAntiAlias = true
            }
        )
    }

    // Draw axis titles
    // Y-axis title (rotated)
    drawContext.canvas.nativeCanvas.save()
    drawContext.canvas.nativeCanvas.rotate(-90f, 12.dp.toPx(), size.height / 2)
    drawContext.canvas.nativeCanvas.drawText(
        "Value",
        12.dp.toPx(),
        size.height / 2,
        android.graphics.Paint().apply {
            color = axisColor.toArgb()
            textSize = 12.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
    )
    drawContext.canvas.nativeCanvas.restore()

    // X-axis title
    drawContext.canvas.nativeCanvas.drawText(
        "Time (min)",
        size.width / 2,
        size.height - 8.dp.toPx(),
        android.graphics.Paint().apply {
            color = axisColor.toArgb()
            textSize = 12.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
    )

    val timeData = metrics.time
    if (timeData.isNotEmpty()) {
        val maxTime = timeData.maxOrNull() ?: 0f
        // Adaptive grid intervals: 10% of total duration, with reasonable bounds
        val timeStep = when {
            maxTime <= 600f -> 60f      // 1 minute for short activities
            maxTime <= 1800f -> 300f    // 5 minutes for medium activities
            maxTime <= 3600f -> 600f    // 10 minutes for hour-long activities
            else -> maxTime * 0.1f      // 10% for longer activities
        }
        var currentTime = 0f

        while (currentTime <= maxTime) {
            val progress = currentTime / maxTime
            val x = chartPadding + progress * chartWidth
            drawLine(
                color = gridColor.copy(alpha = 0.6f),
                start = Offset(x, chartPadding),
                end = Offset(x, size.height - chartPadding),
                strokeWidth = 0.5f
            )
            currentTime += timeStep
        }
    }

    selectedMarker?.let { marker ->
        val x = marker.xPosition.coerceIn(chartPadding, size.width - chartPadding)
        drawLine(
            color = Color(0xFF1F2937).copy(alpha = 0.8f),
            start = Offset(x, chartPadding),
            end = Offset(x, size.height - chartPadding),
            strokeWidth = 2.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        )
    }

    if (showPower && metrics.power != null) {
        drawEnhancedMetricCurve(
            data = metrics.power,
            color = powerColor,
            fillColor = powerColor.copy(alpha = 0.1f),
            chartWidth = chartWidth,
            chartHeight = chartHeight,
            padding = chartPadding,
            drawFill = true,
            strokeAlpha = powerColor.alpha
        )

        // Draw FTP threshold line (use provided FTP or default)
        val ftpValue = ftp ?: 250f
        val powerMax = metrics.power.maxOrNull() ?: 1f
        val powerMin = metrics.power.minOrNull() ?: 0f
        val powerRange = powerMax - powerMin
        if (powerRange > 0f && ftpValue >= powerMin && ftpValue <= powerMax) {
            val normalizedFtp = 0.1f + ((ftpValue - powerMin) / powerRange) * 0.8f
            val ftpY = chartPadding + chartHeight - (normalizedFtp * chartHeight)

            drawLine(
                color = Color(0xFFFF6B35).copy(alpha = 0.7f),
                start = Offset(chartPadding, ftpY),
                end = Offset(size.width - chartPadding, ftpY),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
            )

            // FTP label at right edge
            drawContext.canvas.nativeCanvas.drawText(
                "FTP ${ftpValue.toInt()}W",
                size.width - chartPadding - 2.dp.toPx(),
                ftpY + 4.dp.toPx(),
                android.graphics.Paint().apply {
                    color = Color(0xFFFF6B35).toArgb()
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.LEFT
                    isAntiAlias = true
                }
            )
        }
    }

    if (showHeartRate && metrics.heartRate != null) {
        drawEnhancedMetricCurve(
            data = metrics.heartRate,
            color = hrColor,
            fillColor = hrColor.copy(alpha = 0.08f),
            chartWidth = chartWidth,
            chartHeight = chartHeight,
            padding = chartPadding,
            drawFill = false,
            strokeAlpha = hrColor.alpha
        )

        // Draw FTHR threshold line (use provided FTHR or default)
        val fthrValue = fthr ?: 170f
        val hrMax = metrics.heartRate.maxOrNull() ?: 1f
        val hrMin = metrics.heartRate.minOrNull() ?: 0f
        val hrRange = hrMax - hrMin
        if (hrRange > 0f && fthrValue >= hrMin && fthrValue <= hrMax) {
            val normalizedFthr = 0.1f + ((fthrValue - hrMin) / hrRange) * 0.8f
            val fthrY = chartPadding + chartHeight - (normalizedFthr * chartHeight)

            drawLine(
                color = Color(0xFFEF4444).copy(alpha = 0.7f),
                start = Offset(chartPadding, fthrY),
                end = Offset(size.width - chartPadding, fthrY),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
            )

            // FTHR label at right edge
            drawContext.canvas.nativeCanvas.drawText(
                "FTHR ${fthrValue.toInt()}bpm",
                size.width - chartPadding - 2.dp.toPx(),
                fthrY + 4.dp.toPx(),
                android.graphics.Paint().apply {
                    color = Color(0xFFEF4444).toArgb()
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.LEFT
                    isAntiAlias = true
                }
            )
        }
    }

    if (showCadence && metrics.cadence != null) {
        drawEnhancedMetricCurve(
            data = metrics.cadence,
            color = cadenceColor,
            fillColor = cadenceColor.copy(alpha = 0.08f),
            chartWidth = chartWidth,
            chartHeight = chartHeight,
            padding = chartPadding,
            drawFill = false,
            strokeAlpha = cadenceColor.alpha
        )
    }
}

private fun DrawScope.drawEnhancedMetricCurve(
    data: List<Float>,
    color: Color,
    fillColor: Color,
    chartWidth: Float,
    chartHeight: Float,
    padding: Float,
    drawFill: Boolean = false,
    strokeAlpha: Float = 1f
) {
    val maxPoints = 400
    val smoothWindow = 15

    if (data.size < 2) return

    val smoothed = data.windowed(size = smoothWindow, step = 1, partialWindows = true) {
        it.average().toFloat()
    }

    val factor = (smoothed.size.toFloat() / maxPoints).coerceAtLeast(1f).toInt()
    val sampled = smoothed.chunked(factor) { it.average().toFloat() }

    val minVal = sampled.minOrNull() ?: 0f
    val maxVal = sampled.maxOrNull() ?: 1f
    val range = maxVal - minVal

    if (range == 0f) return

    val path = Path()
    val fillPath = Path()

    sampled.forEachIndexed { index, value ->
        val normalizedValue = 0.1f + ((value - minVal) / range) * 0.8f
        val x = padding + (index.toFloat() / (sampled.size - 1).coerceAtLeast(1)) * chartWidth
        val y = padding + chartHeight - (normalizedValue * chartHeight)

        if (index == 0) {
            path.moveTo(x, y)
            if (drawFill) {
                fillPath.moveTo(x, padding + chartHeight)
                fillPath.lineTo(x, y)
            }
        } else {
            path.lineTo(x, y)
            if (drawFill) {
                fillPath.lineTo(x, y)
            }
        }
    }

    if (drawFill && sampled.isNotEmpty()) {
        val lastX = padding + chartWidth
        val bottomY = padding + chartHeight
        fillPath.lineTo(lastX, bottomY)
        fillPath.close()

        drawPath(
            path = fillPath,
            color = fillColor
        )
    }

    drawPath(
        path = path,
        color = color.copy(alpha = strokeAlpha * 0.9f),
        style = Stroke(
            width = 2.2f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

// Helper function to extract and combine metrics from stream data
private fun extractCombinedMetrics(streams: Map<*, *>): CombinedMetrics {
    var timeData: List<Float>? = null
    var powerData: List<Float>? = null
    var heartRateData: List<Float>? = null
    var cadenceData: List<Float>? = null
    var speedData: List<Float>? = null
    var altitudeData: List<Float>? = null

    streams.forEach { (key, value) ->
        val stringKey = key.toString().lowercase()
        if (value is List<*> && value.isNotEmpty()) {
            when (stringKey) {
                "time", "time_stream" -> {
                    timeData = value.filterIsInstance<Number>().map { it.toFloat() }
                }

                "watts", "power", "power_stream" -> {
                    powerData = value.filterIsInstance<Number>().map { it.toFloat() }
                }

                "heartrate", "hr", "heartrate_stream" -> {
                    heartRateData = value.filterIsInstance<Number>().map { it.toFloat() }
                }

                "cadence", "cadence_stream" -> {
                    cadenceData = value.filterIsInstance<Number>().map { it.toFloat() }
                }

                "velocity_smooth", "speed", "velocity_stream" -> {
                    speedData = value.filterIsInstance<Number>().map { it.toFloat() * 3.6f }
                }

                "altitude", "altitude_stream" -> {
                    altitudeData = value.filterIsInstance<Number>().map { it.toFloat() }
                }
            }
        }
    }

    // Generate time data if not available
    if (timeData == null) {
        val maxSize = listOfNotNull(powerData, heartRateData, cadenceData, speedData, altitudeData)
            .maxOfOrNull { it.size } ?: 0
        timeData = (0 until maxSize).map { it.toFloat() }
    }

    return CombinedMetrics(
        time = timeData ?: emptyList(),
        power = powerData,
        heartRate = heartRateData,
        cadence = cadenceData,
        speed = speedData,
        altitude = altitudeData
    )
}
