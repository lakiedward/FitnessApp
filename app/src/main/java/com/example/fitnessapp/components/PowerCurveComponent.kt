package com.example.fitnessapp.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnessapp.model.PowerCurveResponse
import com.example.fitnessapp.viewmodel.StravaViewModel
import com.example.fitnessapp.viewmodel.StravaViewModelFactory
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.pow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

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
    modifier: Modifier = Modifier,
    fthr: Int? = null
) {
    if (activityType.lowercase() !in listOf("ride", "virtualride", "cycling")) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val stravaViewModel: StravaViewModel = viewModel(factory = StravaViewModelFactory(context))
    val scope = rememberCoroutineScope()

    var powerCurveData by remember { mutableStateOf<PowerCurveResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedMarker by remember { mutableStateOf<PowerCurveMarker?>(null) }
    var showFtpLine by remember { mutableStateOf(true) }
    var showComparisonLine by remember { mutableStateOf(false) }
    var chartBoxOffset by remember { mutableStateOf(Offset.Zero) }
    var chartBoxSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    // Load power curve data
    LaunchedEffect(activityId) {
        scope.launch {
            try {
                val powerCurve = stravaViewModel.getActivityPowerCurve(activityId)
                if (powerCurve != null) powerCurveData = powerCurve
                else errorMessage = "Power curve data not available"
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Failed to load power curve: ${e.message}"
                isLoading = false
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Main Card with chart and controls
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 0.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            border = BorderStroke(1.dp, Color(0xFFE5EAF2)),
        ) {
            Column(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedMarker == null) {
                    PowerCurveMarkerDisplay(
                        markerData = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
                // Controls row (switches) tightly above chart
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = showFtpLine,
                            onCheckedChange = { showFtpLine = it },
                            modifier = Modifier.size(36.dp, 20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        powerCurveData?.referenceData?.userFtp?.let { ftp ->
                            Text(
                                "FTP Line (${ftp.toInt()} W)",
                                color = Color(0xFF3B82F6),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = showComparisonLine,
                            onCheckedChange = { showComparisonLine = it },
                            modifier = Modifier.size(36.dp, 20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Previous Best",
                            color = Color(0xFF10B981),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(vertical = 2.dp),
                    thickness = 1.dp,
                    color = Color(0xFFF3F4F6)
                )
                // Chart (fills the card, compact)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .onGloballyPositioned { layoutCoordinates ->
                            val position = layoutCoordinates.localToWindow(Offset.Zero)
                            chartBoxOffset = position
                            chartBoxSize = layoutCoordinates.size.run { androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat()) }
                        }
                ) {
                    when {
                        isLoading -> LoadingState()
                        errorMessage != null -> ErrorState(message = errorMessage!!, onRetry = {
                            errorMessage = null
                            isLoading = true
                            scope.launch {
                                try {
                                    val powerCurve = stravaViewModel.getActivityPowerCurve(activityId)
                                    if (powerCurve != null) powerCurveData = powerCurve
                                    else errorMessage = "Power curve data not available"
                                    isLoading = false
                                } catch (e: Exception) {
                                    errorMessage = "Failed to load power curve: ${e.message}"
                                    isLoading = false
                                }
                            }
                        })
                        powerCurveData != null -> PowerCurveChart(
                            powerCurveData = powerCurveData!!,
                            onMarkerSelected = { marker -> selectedMarker = marker },
                            selectedMarker = selectedMarker,
                            showFtpLine = showFtpLine,
                            showComparisonLine = showComparisonLine
                        )
                    }
                }
                if (powerCurveData != null) {
                    PowerCurveStats(powerCurveData!!, fthr)
                }
            }
        }
        // Tooltip overlay rendered at the BoxWithConstraints level
        selectedMarker?.let { marker ->
            val chartX = chartBoxOffset.x
            val chartY = chartBoxOffset.y
            val chartWidth = chartBoxSize.width
            val chartHeight = chartBoxSize.height
            val tooltipWidth = with(density) { 220.dp.toPx() } // Approximate width of tooltip
            val tooltipHeight = with(density) { 140.dp.toPx() } // Approximate height of tooltip
            val x = (chartX + marker.xPosition - tooltipWidth / 2).coerceIn(0f, with(density) { maxWidth.toPx() } - tooltipWidth)
            val y = (chartY - tooltipHeight - with(density) { 16.dp.toPx() }).coerceAtLeast(0f)
            Box(
                modifier = Modifier
                    .absoluteOffset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .zIndex(10f)
            ) {
                FloatingTooltipContent(marker = marker, onClose = { selectedMarker = null })
            }
        }
    }
}

@Composable
private fun PowerCurveChart(
    powerCurveData: PowerCurveResponse,
    onMarkerSelected: (PowerCurveMarker?) -> Unit,
    selectedMarker: PowerCurveMarker?,
    showFtpLine: Boolean,
    showComparisonLine: Boolean
) {
    val density = LocalDensity.current
    val chartPadding = 24.dp
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1500, easing = EaseInOutCubic),
        label = "chart_animation"
    )
    val markerAlpha by animateFloatAsState(
        targetValue = if (selectedMarker != null) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "marker_animation"
    )
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offsetX += panChange.x
        offsetY += panChange.y
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(14.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .transformable(transformableState)
                .pointerInput(powerCurveData) {
                    detectTapGestures(
                        onTap = { offset ->
                            val paddingPx = with(density) { chartPadding.toPx() }
                            val chartWidth = size.width - 2 * paddingPx
                            if (powerCurveData.powerCurve.intervals.isNotEmpty()) {
                                val progress =
                                    ((offset.x - paddingPx) / chartWidth).coerceIn(0f, 1f)
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
                        },
                        onDoubleTap = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }
                    )
                }
        ) {
            drawEnhancedPowerCurveChart(
                powerCurveData = powerCurveData,
                selectedMarker = selectedMarker,
                chartPadding = with(density) { chartPadding.toPx() },
                animatedProgress = animatedProgress,
                showFtpLine = showFtpLine,
                showComparisonLine = showComparisonLine
            )
        }
        selectedMarker?.let { marker ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200)),
                modifier = Modifier
                    .offset(x = (marker.xPosition - 100).dp.coerceAtLeast(0.dp), y = 20.dp)
                    .alpha(markerAlpha)
            ) {
                FloatingTooltipContent(marker = marker, onClose = { onMarkerSelected(null) })
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .semantics { contentDescription = "Loading power curve data" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    Color(0xFFE5E7EB).copy(alpha = 0.3f),
                    RoundedCornerShape(8.dp)
                )
        ) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(2.dp)
                        .offset(y = (40 * index).dp)
                        .background(
                            Color(0xFFD1D5DB).copy(alpha = 0.5f),
                            RoundedCornerShape(1.dp)
                        )
                        .align(Alignment.CenterStart)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        CircularProgressIndicator(
            color = Color(0xFF6366F1),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Loading power curve...",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .semantics { contentDescription = "Error loading power curve: $message" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFF6B6B).copy(alpha = 0.1f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = "Error loading power curve data",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Failed to load power curve",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1)
                    ),
                    modifier = Modifier.semantics { 
                        contentDescription = "Retry loading power curve data"
                    }
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun PowerCurveMarkerDisplay(
    markerData: PowerCurveMarker?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E7FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Info, // Use a better info/touch icon if available
                contentDescription = "Info",
                tint = Color(0xFF6366F1),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Tap a point on the curve to see details for that interval.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF374151),
                textAlign = TextAlign.Center
            )
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
private fun FloatingTooltipContent(marker: PowerCurveMarker, onClose: () -> Unit) {
    // Position the tooltip above or below the selected point based on y-position (for now, always above)
    Box {
        // Arrow (triangle) pointing down to the selected point
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 24.dp)
        ) {
            Canvas(modifier = Modifier.size(24.dp, 12.dp)) {
                drawPath(
                    path = Path().apply {
                        moveTo(size.width / 2, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    },
                    color = Color.White
                )
            }
        }
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(12.dp),
            modifier = Modifier
                .widthIn(min = 180.dp, max = 260.dp)
                .align(Alignment.TopCenter)
                .shadow(12.dp, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Selection",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF6366F1),
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Filled.Close, // Use a close icon
                        contentDescription = "Close",
                        tint = Color(0xFF6366F1),
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFE0E7FF))
                            .padding(2.dp)
                            .clickable { onClose() }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = marker.interval,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF374151),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = marker.power,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFFFF6B35), // Brand orange for power
                    fontWeight = FontWeight.ExtraBold
                )
                marker.hr?.let { hr ->
                    Text(
                        text = hr,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )
                }
                marker.zone?.let { zone ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .background(getZoneColorFromLabel(zone), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = zone,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PowerCurveStats(powerCurveData: PowerCurveResponse, fthr: Int? = null) {
    Column {
        Text(
            text = "Power Statistics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937)
        )

        Spacer(modifier = Modifier.height(12.dp))

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

        fthr?.let { fthrValue ->
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFFEF4444).copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Estimated FTHR",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFEF4444)
                )
                Text(
                    text = "$fthrValue bpm",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
            }
        } ?: powerCurveData.referenceData.userFtp?.let { ftp ->
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

private fun DrawScope.drawEnhancedPowerCurveChart(
    powerCurveData: PowerCurveResponse,
    selectedMarker: PowerCurveMarker?,
    chartPadding: Float,
    animatedProgress: Float,
    showFtpLine: Boolean,
    showComparisonLine: Boolean
) {
    val chartWidth = size.width - 2 * chartPadding
    val chartHeight = size.height - 2 * chartPadding

    val gridColor = Color(0xFFE5E7EB)
    val axisColor = Color(0xFF9CA3AF)
    val textColor = Color(0xFF374151)

    val intervals = powerCurveData.powerCurve.intervals
    if (intervals.isNotEmpty()) {
        val logMin = log10(intervals.first().toDouble())
        val logMax = log10(intervals.last().toDouble())
        val powerMin = powerCurveData.powerCurve.powerValues.minOrNull() ?: 0.0
        val powerMax = powerCurveData.powerCurve.powerValues.maxOrNull() ?: 1.0

        for (i in 0..5) {
            val y = chartPadding + (i.toFloat() / 5) * chartHeight
            val powerValue = powerMax - (i.toFloat() / 5) * (powerMax - powerMin)

            drawLine(
                color = if (i == 0 || i == 5) axisColor else gridColor,
                start = Offset(chartPadding, y),
                end = Offset(size.width - chartPadding, y),
                strokeWidth = if (i == 0 || i == 5) 1.5f else 0.8f
            )

            drawContext.canvas.nativeCanvas.drawText(
                "${powerValue.toInt()}W",
                maxOf(24f/2 + chartPadding, chartPadding - 40f), 
                y + 5f,
                android.graphics.Paint().apply {
                    color = textColor.hashCode()
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
            )
        }

        val timeLabels = listOf("5s", "1m", "5m", "20m", "1h")
        for (i in 0..4) {
            val progress = i.toFloat() / 4
            val x = chartPadding + progress * chartWidth

            drawLine(
                color = gridColor,
                start = Offset(x, chartPadding),
                end = Offset(x, size.height - chartPadding),
                strokeWidth = 0.8f
            )

            if (i < timeLabels.size) {
                drawContext.canvas.nativeCanvas.drawText(
                    timeLabels[i],
                    x,
                    size.height - chartPadding + 30f,
                    android.graphics.Paint().apply {
                        color = textColor.hashCode()
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }

        drawContext.canvas.nativeCanvas.drawText(
            "Power (W)",
            20f,
            chartPadding - 20f,
            android.graphics.Paint().apply {
                color = textColor.hashCode()
                textSize = 28f
                isFakeBoldText = true
            }
        )

        drawContext.canvas.nativeCanvas.drawText(
            "Time",
            size.width / 2,
            size.height - 10f,
            android.graphics.Paint().apply {
                color = textColor.hashCode()
                textSize = 28f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
        )

        powerCurveData.referenceData.userFtp?.let { ftp ->
            if (showFtpLine) {
                if (ftp >= powerMin && ftp <= powerMax) {
                    val yProgress = ((ftp - powerMin) / (powerMax - powerMin)).toFloat()
                    val y = chartPadding + chartHeight - (yProgress * chartHeight)

                    drawLine(
                        color = Color(0xFF6366F1).copy(alpha = 0.7f),
                        start = Offset(chartPadding, y),
                        end = Offset(size.width - chartPadding, y),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                    )

                    drawContext.canvas.nativeCanvas.drawText(
                        "FTP: ${ftp.toInt()}W",
                        size.width - chartPadding - 100f,
                        y - 10f,
                        android.graphics.Paint().apply {
                            color = Color(0xFF6366F1).hashCode()
                            textSize = 22f
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                    )
                }
            }
        }

        if (showComparisonLine) {
            val comparisonPath = Path()
            intervals.forEachIndexed { index, interval ->
                val power = powerCurveData.powerCurve.powerValues[index] * 0.9

                val logValue = log10(interval.toDouble())
                val xProgress = ((logValue - logMin) / (logMax - logMin)).toFloat()
                val x = chartPadding + xProgress * chartWidth

                val yProgress = ((power - powerMin) / (powerMax - powerMin)).toFloat()
                val y = chartPadding + chartHeight - (yProgress * chartHeight)

                if (index == 0) {
                    comparisonPath.moveTo(x, y)
                } else {
                    comparisonPath.lineTo(x, y)
                }
            }

            drawPath(
                path = comparisonPath,
                color = Color(0xFF10B981).copy(alpha = 0.6f),
                style = Stroke(
                    width = 2f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                )
            )
        }

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

        if (intervals.isNotEmpty() && powerCurveData.powerCurve.powerValues.isNotEmpty()) {
            val logMin = log10(intervals.first().toDouble())
            val logMax = log10(intervals.last().toDouble())
            val powerMin = powerCurveData.powerCurve.powerValues.minOrNull() ?: 0.0
            val powerMax = powerCurveData.powerCurve.powerValues.maxOrNull() ?: 1.0
            val powerRange = powerMax - powerMin

            if (powerRange > 0) {
                val path = Path()
                val fillPath = Path()

                val totalPoints = intervals.size
                val animatedPointCount =
                    (totalPoints * animatedProgress).toInt().coerceAtMost(totalPoints)

                for (index in 0 until animatedPointCount) {
                    val interval = intervals[index]
                    val power = powerCurveData.powerCurve.powerValues[index]

                    val logValue = log10(interval.toDouble())
                    val xProgress = ((logValue - logMin) / (logMax - logMin)).toFloat()
                    val x = chartPadding + xProgress * chartWidth

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

                if (animatedProgress >= 1f) {
                    val lastX = chartPadding + chartWidth
                    val bottomY = size.height - chartPadding
                    fillPath.lineTo(lastX, bottomY)
                    fillPath.close()
                }

                drawPath(
                    path = fillPath,
                    color = Color(0xFFFF6B35).copy(alpha = 0.15f * animatedProgress)
                )

                drawPath(
                    path = path,
                    color = Color(0xFFFF6B35),
                    style = Stroke(
                        width = 3f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                for (index in 0 until animatedPointCount) {
                    val interval = intervals[index]
                    val power = powerCurveData.powerCurve.powerValues[index]

                    val logValue = log10(interval.toDouble())
                    val xProgress = ((logValue - logMin) / (logMax - logMin)).toFloat()
                    val x = chartPadding + xProgress * chartWidth

                    val yProgress = ((power - powerMin) / powerRange).toFloat()
                    val y = chartPadding + chartHeight - (yProgress * chartHeight)

                    val pointDelay = index.toFloat() / totalPoints
                    val pointAlpha = ((animatedProgress - pointDelay) * 2f).coerceIn(0f, 1f)

                    drawCircle(
                        color = Color.White.copy(alpha = pointAlpha),
                        radius = 6f,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = Color(0xFFFF6B35).copy(alpha = pointAlpha),
                        radius = 4f,
                        center = Offset(x, y)
                    )
                }
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

// Helper to get zone color from label
private fun getZoneColorFromLabel(zone: String): Color {
    return when {
        zone.contains("Zone 1") -> Color(0xFF6EE7B7)
        zone.contains("Zone 2") -> Color(0xFF34D399)
        zone.contains("Zone 3") -> Color(0xFF10B981)
        zone.contains("Zone 4") -> Color(0xFF059669)
        zone.contains("Zone 5") -> Color(0xFF6366F1)
        zone.contains("Zone 6") -> Color(0xFFF59E42)
        zone.contains("Zone 7") -> Color(0xFFEF4444)
        else -> Color(0xFF6366F1)
    }
}
