package com.example.fitnessapp.components

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnessapp.model.PowerCurveResponse
import com.example.fitnessapp.viewmodel.StravaViewModel
import com.example.fitnessapp.viewmodel.StravaViewModelFactory
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.pow

data class PowerCurveMarker(
    val interval: String,
    val power: String,
    val hr: String?,
    val zone: String?,
    val xPosition: Float
)

@Composable
fun PowerCurveComponent(
    activityId: Long,
    activityType: String,
    modifier: Modifier = Modifier
) {
    // Only show for cycling activities
    if (activityType.lowercase() !in listOf("ride", "virtualride", "cycling")) {
        return
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val stravaViewModel: StravaViewModel = viewModel(factory = StravaViewModelFactory(context))
    val scope = rememberCoroutineScope()

    var powerCurveData by remember { mutableStateOf<PowerCurveResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedMarker by remember { mutableStateOf<PowerCurveMarker?>(null) }

    // Load power curve data
    LaunchedEffect(activityId) {
        scope.launch {
            try {
                Log.d("PowerCurveComponent", "Loading power curve for activity $activityId")
                val powerCurve = stravaViewModel.getActivityPowerCurve(activityId)

                if (powerCurve != null) {
                    powerCurveData = powerCurve
                    Log.d("PowerCurveComponent", "Power curve loaded successfully")
                } else {
                    errorMessage = "Power curve data not available"
                    Log.w("PowerCurveComponent", "No power curve data available")
                }

                isLoading = false
            } catch (e: Exception) {
                Log.e("PowerCurveComponent", "Error loading power curve", e)
                errorMessage = "Failed to load power curve: ${e.message}"
                isLoading = false
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Text(
            text = "Power Curve Analysis",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Marker display
        selectedMarker?.let { marker ->
            PowerCurveMarkerDisplay(
                markerData = marker,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Main chart card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                when {
                    isLoading -> {
                        LoadingState()
                    }

                    errorMessage != null -> {
                        ErrorState(errorMessage!!)
                    }

                    powerCurveData != null -> {
                        PowerCurveContent(
                            powerCurveData = powerCurveData!!,
                            onMarkerSelected = { marker -> selectedMarker = marker },
                            selectedMarker = selectedMarker
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = Color(0xFF6366F1))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Loading power curve...",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
private fun ErrorState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PowerCurveContent(
    powerCurveData: PowerCurveResponse,
    onMarkerSelected: (PowerCurveMarker?) -> Unit,
    selectedMarker: PowerCurveMarker?
) {
    val density = LocalDensity.current
    val chartPadding = 16.dp

    // Chart
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(powerCurveData) {
                    detectTapGestures { offset ->
                        val paddingPx = with(density) { chartPadding.toPx() }
                        val chartWidth = size.width - 2 * paddingPx

                        if (powerCurveData.powerCurve.intervals.isNotEmpty()) {
                            // Find the closest interval based on logarithmic scale
                            val progress = ((offset.x - paddingPx) / chartWidth).coerceIn(0f, 1f)
                            val logMin =
                                log10(powerCurveData.powerCurve.intervals.first().toDouble())
                            val logMax =
                                log10(powerCurveData.powerCurve.intervals.last().toDouble())
                            val logValue = logMin + progress * (logMax - logMin)
                            val targetInterval = 10.0.pow(logValue).toInt()

                            val index = powerCurveData.powerCurve.intervals
                                .mapIndexed { i, interval -> i to kotlin.math.abs(interval - targetInterval) }
                                .minByOrNull { it.second }
                                ?.first ?: 0

                            val interval = powerCurveData.powerCurve.intervals[index]
                            val power = powerCurveData.powerCurve.powerValues[index]
                            val hr = powerCurveData.powerCurve.hrValues.getOrNull(index)
                            val label = powerCurveData.powerCurve.labels[index]

                            // Calculate power zone
                            val zone = powerCurveData.referenceData.userFtp?.let { ftp ->
                                calculatePowerZone(power, ftp)
                            }

                            onMarkerSelected(
                                PowerCurveMarker(
                                    interval = label,
                                    power = "${power.toInt()} W",
                                    hr = hr?.let { "${it.toInt()} bpm" },
                                    zone = zone,
                                    xPosition = offset.x
                                )
                            )
                        }
                    }
                }
        ) {
            drawPowerCurveChart(
                powerCurveData = powerCurveData,
                selectedMarker = selectedMarker,
                chartPadding = with(density) { chartPadding.toPx() }
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Statistics summary
    PowerCurveStats(powerCurveData)
}

@Composable
private fun PowerCurveMarkerDisplay(
    markerData: PowerCurveMarker,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Interval: ${markerData.interval}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                markerData.zone?.let { zone ->
                    Text(
                        text = zone,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6366F1),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .background(
                                Color(0xFF6366F1).copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PowerMetricCard("Power", markerData.power, Color(0xFFFF6B35))
                markerData.hr?.let { hr ->
                    PowerMetricCard("Heart Rate", hr, Color(0xFFEF4444))
                }
            }
        }
    }
}

@Composable
private fun PowerMetricCard(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(
                color.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PowerCurveStats(powerCurveData: PowerCurveResponse) {
    Column {
        Text(
            text = "Power Statistics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Key intervals
        val keyIntervals = listOf(
            Triple("5s", 1, "Neuromuscular"),
            Triple("1m", 5, "Anaerobic"),
            Triple("5m", 8, "VO2 Max"),
            Triple("20m", 10, "Threshold")
        )

        keyIntervals.forEach { (label, index, description) ->
            if (index < powerCurveData.powerCurve.powerValues.size) {
                val power = powerCurveData.powerCurve.powerValues[index]

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "$label Peak Power",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF374151)
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280)
                        )
                    }
                    Text(
                        text = "${power.toInt()} W",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                }
            }
        }

        // FTP comparison
        powerCurveData.referenceData.userFtp?.let { ftp ->
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFF6366F1).copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Current FTP",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6366F1)
                )
                Text(
                    text = "${ftp.toInt()} W",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6366F1)
                )
            }
        }
    }
}

private fun DrawScope.drawPowerCurveChart(
    powerCurveData: PowerCurveResponse,
    selectedMarker: PowerCurveMarker?,
    chartPadding: Float
) {
    val chartWidth = size.width - 2 * chartPadding
    val chartHeight = size.height - 2 * chartPadding

    // Grid
    val gridColor = Color(0xFFE5E7EB)
    val axisColor = Color(0xFF9CA3AF)

    // Horizontal grid lines
    for (i in 0..5) {
        val y = chartPadding + (i.toFloat() / 5) * chartHeight
        drawLine(
            color = if (i == 0 || i == 5) axisColor else gridColor,
            start = Offset(chartPadding, y),
            end = Offset(size.width - chartPadding, y),
            strokeWidth = if (i == 0 || i == 5) 1.5f else 0.8f
        )
    }

    // Vertical grid lines (logarithmic scale)
    val intervals = powerCurveData.powerCurve.intervals
    if (intervals.isNotEmpty()) {
        val logMin = log10(intervals.first().toDouble())
        val logMax = log10(intervals.last().toDouble())

        for (i in 0..5) {
            val progress = i.toFloat() / 5
            val x = chartPadding + progress * chartWidth
            drawLine(
                color = gridColor,
                start = Offset(x, chartPadding),
                end = Offset(x, size.height - chartPadding),
                strokeWidth = 0.8f
            )
        }
    }

    // Draw selected marker line
    selectedMarker?.let { marker ->
        val x = marker.xPosition.coerceIn(chartPadding, size.width - chartPadding)
        drawLine(
            color = Color(0xFF6366F1).copy(alpha = 0.8f),
            start = Offset(x, chartPadding),
            end = Offset(x, size.height - chartPadding),
            strokeWidth = 2.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
        )
    }

    // Draw power curve
    if (intervals.isNotEmpty() && powerCurveData.powerCurve.powerValues.isNotEmpty()) {
        val logMin = log10(intervals.first().toDouble())
        val logMax = log10(intervals.last().toDouble())
        val powerMin = powerCurveData.powerCurve.powerValues.minOrNull() ?: 0.0
        val powerMax = powerCurveData.powerCurve.powerValues.maxOrNull() ?: 1.0
        val powerRange = powerMax - powerMin

        if (powerRange > 0) {
            val path = Path()
            val fillPath = Path()

            intervals.forEachIndexed { index, interval ->
                val power = powerCurveData.powerCurve.powerValues[index]

                // Logarithmic x position
                val logValue = log10(interval.toDouble())
                val xProgress = ((logValue - logMin) / (logMax - logMin)).toFloat()
                val x = chartPadding + xProgress * chartWidth

                // Linear y position
                val yProgress = ((power - powerMin) / powerRange).toFloat()
                val y = chartPadding + chartHeight - (yProgress * chartHeight)

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, size.height - chartPadding)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }

            // Close fill path
            val lastX = chartPadding + chartWidth
            val bottomY = size.height - chartPadding
            fillPath.lineTo(lastX, bottomY)
            fillPath.close()

            // Draw fill
            drawPath(
                path = fillPath,
                color = Color(0xFFFF6B35).copy(alpha = 0.15f)
            )

            // Draw curve
            drawPath(
                path = path,
                color = Color(0xFFFF6B35),
                style = Stroke(
                    width = 3f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Draw points
            intervals.forEachIndexed { index, interval ->
                val power = powerCurveData.powerCurve.powerValues[index]

                val logValue = log10(interval.toDouble())
                val xProgress = ((logValue - logMin) / (logMax - logMin)).toFloat()
                val x = chartPadding + xProgress * chartWidth

                val yProgress = ((power - powerMin) / powerRange).toFloat()
                val y = chartPadding + chartHeight - (yProgress * chartHeight)

                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color(0xFFFF6B35),
                    radius = 4f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

private fun calculatePowerZone(power: Double, ftp: Double): String {
    val percentage = (power / ftp) * 100
    return when {
        percentage >= 150 -> "Zone 7 (Neuromuscular)"
        percentage >= 120 -> "Zone 6 (Anaerobic)"
        percentage >= 105 -> "Zone 5 (VO2 Max)"
        percentage >= 90 -> "Zone 4 (Threshold)"
        percentage >= 76 -> "Zone 3 (Tempo)"
        percentage >= 56 -> "Zone 2 (Endurance)"
        else -> "Zone 1 (Recovery)"
    }
}