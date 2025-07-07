package com.example.fitnessapp.components

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnessapp.viewmodel.StravaViewModel
import com.example.fitnessapp.viewmodel.StravaViewModelFactory
import kotlinx.coroutines.launch
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val stravaViewModel: StravaViewModel = viewModel(factory = StravaViewModelFactory(context))
    val scope = rememberCoroutineScope()
    
    var streamsData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var combinedMetrics by remember { mutableStateOf<CombinedMetrics?>(null) }
    var dataSource by remember { mutableStateOf<String?>(null) }
    var selectedMarker by remember { mutableStateOf<AdvancedMarkerData?>(null) }
    
    // Filter states for chart visibility
    var showPower by remember { mutableStateOf(true) }
    var showHr by remember { mutableStateOf(true) }
    var showCad by remember { mutableStateOf(true) }

    val chartPadding = 16.dp

    // Load streams data on component creation
    LaunchedEffect(activityId) {
        scope.launch {
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
            } catch (e: Exception) {
                Log.e("ActivityChartsComponent", "Error loading streams", e)
                isLoading = false
            }
        }
    }

    // Use Column layout to ensure proper ordering: Chart first, then card content
    Column(modifier = modifier.fillMaxWidth()) {
        selectedMarker?.let { marker ->
            MinimalistMarkerDisplay(
                markerData = marker
            )
        }

        // Chart section with highest z-index
        if (!isLoading && combinedMetrics != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .zIndex(1f)
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp
                    ),
                    colors = CardDefaults.cardColors(containerColor = Color.White), // Light background
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(combinedMetrics) {
                                detectTapGestures { offset ->
                                    if (combinedMetrics == null || combinedMetrics!!.time.isEmpty()) {
                                        return@detectTapGestures
                                    }

                                    val paddingPx = with(density) { chartPadding.toPx() }
                                    val chartWidth = size.width - 2 * paddingPx

                                    // Find the index in the original data corresponding to the tap position
                                    val progress =
                                        ((offset.x - paddingPx) / chartWidth).coerceIn(0f, 1f)
                                    val dataSize = combinedMetrics!!.time.size
                                    val index = (progress * (dataSize - 1))
                                        .roundToInt()
                                        .coerceIn(0, dataSize - 1)

                                    // Get data for the selected index
                                    val timeSec = combinedMetrics!!.time.getOrNull(index) ?: 0f
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
                                }
                            }
                    ) {
                        drawTrainerRoadStyleChart(
                            metrics = combinedMetrics!!,
                            showPower = showPower,
                            showHeartRate = showHr,
                            showCadence = showCad,
                            selectedMarker = selectedMarker,
                            chartPadding = with(density) { chartPadding.toPx() }
                        )
                    }
                }
            }
        }

        // Card content below chart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(0f),
            shape = RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 0.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = Color.White), // Light background
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TrainerRoad-Style Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937) // Dark text for light theme
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    LoadingState(dataSource)
                } else if (combinedMetrics == null) {
                    EmptyState()
                } else {
                    ChartContent(
                        metrics = combinedMetrics!!,
                        showPower = showPower,
                        showHeartRate = showHr,
                        showCadence = showCad,
                        onTogglePower = { showPower = !showPower },
                        onToggleHr = { showHr = !showHr },
                        onToggleCad = { showCad = !showCad },
                        selectedMarker = selectedMarker,
                        onDismissMarker = { selectedMarker = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState(dataSource: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = Color(0xFF6366F1))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (dataSource == "api") "Loading data from API..." else "Loading analysis...",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280) // Light gray for light theme
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
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
    onDismissMarker: () -> Unit
) {
    // Filter chips row
    MetricFilterRow(
        showPower = showPower,
        showHr = showHeartRate,
        showCad = showCadence,
        onTogglePower = onTogglePower,
        onToggleHr = onToggleHr,
        onToggleCad = onToggleCad
    )

    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun MetricFilterRow(
    showPower: Boolean,
    showHr: Boolean,
    showCad: Boolean,
    onTogglePower: () -> Unit,
    onToggleHr: () -> Unit,
    onToggleCad: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        FilterChip(
            selected = showPower,
            onClick = onTogglePower,
            label = { Text("Power") },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = Color(0xFFF3F4F6),
                labelColor = Color(0xFF374151),
                selectedContainerColor = Color(0xFFFF6B35),
                selectedLabelColor = Color.White
            )
        )
        FilterChip(
            selected = showHr,
            onClick = onToggleHr,
            label = { Text("HR") },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = Color(0xFFF3F4F6),
                labelColor = Color(0xFF374151),
                selectedContainerColor = Color(0xFFEF4444),
                selectedLabelColor = Color.White
            )
        )
        FilterChip(
            selected = showCad,
            onClick = onToggleCad,
            label = { Text("Cadence") },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = Color(0xFFF3F4F6),
                labelColor = Color(0xFF374151),
                selectedContainerColor = Color(0xFF22C55E),
                selectedLabelColor = Color.White
            )
        )
    }
}

@Composable
private fun MinimalistMarkerDisplay(
    markerData: AdvancedMarkerData
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
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
    chartPadding: Float
) {
    val chartWidth = size.width - 2 * chartPadding
    val chartHeight = size.height - 2 * chartPadding

    val gridColor = Color(0xFFD1D5DB)
    val axisColor = Color(0xFF9CA3AF)

    for (i in 0..4) {
        val y = chartPadding + (i.toFloat() / 4) * chartHeight
        drawLine(
            color = if (i == 0 || i == 4) axisColor else gridColor,
            start = Offset(chartPadding, y),
            end = Offset(size.width - chartPadding, y),
            strokeWidth = if (i == 0 || i == 4) 1f else 0.5f
        )
    }

    val timeData = metrics.time
    if (timeData.isNotEmpty()) {
        val maxTime = timeData.maxOrNull() ?: 0f
        val timeStep = 600f
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
            color = Color(0xFFFF6B35),
            fillColor = Color(0xFFFF6B35).copy(alpha = 0.1f),
            chartWidth = chartWidth,
            chartHeight = chartHeight,
            padding = chartPadding,
            drawFill = true
        )
    }

    if (showHeartRate && metrics.heartRate != null) {
        drawEnhancedMetricCurve(
            data = metrics.heartRate,
            color = Color(0xFFEF4444),
            fillColor = Color(0xFFEF4444).copy(alpha = 0.08f),
            chartWidth = chartWidth,
            chartHeight = chartHeight,
            padding = chartPadding,
            drawFill = false
        )
    }

    if (showCadence && metrics.cadence != null) {
        drawEnhancedMetricCurve(
            data = metrics.cadence,
            color = Color(0xFF22C55E),
            fillColor = Color(0xFF22C55E).copy(alpha = 0.08f),
            chartWidth = chartWidth,
            chartHeight = chartHeight,
            padding = chartPadding,
            drawFill = false
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
    drawFill: Boolean = false
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
        color = color.copy(alpha = 0.9f),
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
