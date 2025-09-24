package com.example.fitnessapp.components

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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.ColorScheme
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import android.graphics.Paint

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
    if (!isPowerCurveEligible(activityType)) return

    val context = LocalContext.current
    val density = LocalDensity.current
    val stravaViewModel: StravaViewModel = viewModel(factory = StravaViewModelFactory(context))
    val scope = rememberCoroutineScope()
    val colorScheme = MaterialTheme.colorScheme

    var powerCurveData by remember { mutableStateOf<PowerCurveResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedMarker by remember { mutableStateOf<PowerCurveMarker?>(null) }
    var showFtpLine by remember { mutableStateOf(true) }
    var showComparisonLine by remember { mutableStateOf(false) }
    // Tooltip overlay removed; no need to track absolute chart position

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
            border = BorderStroke(1.dp, colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
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
                // Adaptive controls (no overlap on compact screens)
                PowerCurveControls(
                    ftp = powerCurveData?.referenceData?.userFtp,
                    showFtpLine = showFtpLine,
                    onToggleFtp = { showFtpLine = it },
                    showComparisonLine = showComparisonLine,
                    onToggleComparison = { showComparisonLine = it }
                )
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    thickness = 1.dp,
                    color = colorScheme.outlineVariant
                )
                // Chart (fills the card, compact)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
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
                // Inline selection details bar (replaces floating overlay to avoid overlap)
                selectedMarker?.let { marker ->
                    SelectionInfoBar(
                        marker = marker,
                        onClose = { selectedMarker = null }
                    )
                }

                if (powerCurveData != null) {
                    PowerCurveStats(powerCurveData!!, fthr)
                }
            }
        }
    }
}

@Composable
private fun PowerCurveControls(
    ftp: Double?,
    showFtpLine: Boolean,
    onToggleFtp: (Boolean) -> Unit,
    showComparisonLine: Boolean,
    onToggleComparison: (Boolean) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 360.dp
        val colorScheme = MaterialTheme.colorScheme
        if (compact) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = showFtpLine,
                        onCheckedChange = onToggleFtp,
                        modifier = Modifier.size(44.dp, 24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = buildString {
                            append("FTP Line")
                            ftp?.toInt()?.let { append(" ($it W)") }
                        },
                        color = colorScheme.tertiary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = showComparisonLine,
                        onCheckedChange = onToggleComparison,
                        modifier = Modifier.size(44.dp, 24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Previous Best",
                        color = colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = showFtpLine,
                        onCheckedChange = onToggleFtp,
                        modifier = Modifier.size(44.dp, 24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = buildString {
                            append("FTP Line")
                            ftp?.toInt()?.let { append(" ($it W)") }
                        },
                        color = colorScheme.tertiary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = showComparisonLine,
                        onCheckedChange = onToggleComparison,
                        modifier = Modifier.size(44.dp, 24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Previous Best",
                        color = colorScheme.secondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
    val chartPaddingPx = with(density) { chartPadding.toPx() }
    val colorScheme = MaterialTheme.colorScheme
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
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = newScale

        val maxOffsetX = if (canvasSize.width > 0) (canvasSize.width * (scale - 1f)) else 0f
        val maxOffsetY = if (canvasSize.height > 0) (canvasSize.height * (scale - 1f)) else 0f

        val proposedOffsetX = offsetX + panChange.x
        val proposedOffsetY = offsetY + panChange.y

        offsetX = proposedOffsetX.coerceIn(-maxOffsetX, 0f)
        offsetY = proposedOffsetY.coerceIn(-maxOffsetY, 0f)

        if (scale == 1f) {
            offsetX = 0f
            offsetY = 0f
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(14.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
                .transformable(transformableState)
                .pointerInput(powerCurveData, scale, offsetX, offsetY) {
                    detectTapGestures(
                        onTap = { offset ->
                            val chartWidth = size.width - 2 * chartPaddingPx
                            if (chartWidth <= 0f || powerCurveData.powerCurve.intervals.isEmpty()) {
                                onMarkerSelected(null)
                                return@detectTapGestures
                            }

                            val adjustedX = ((offset.x - offsetX) / scale)
                            val clampedX = adjustedX.coerceIn(
                                chartPaddingPx,
                                size.width - chartPaddingPx
                            )
                            val progress = ((clampedX - chartPaddingPx) / chartWidth).coerceIn(0f, 1f)
                            if (powerCurveData.powerCurve.intervals.isNotEmpty()) {
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
                                val selectedLog = log10(interval.toDouble())
                                val xProgress = ((selectedLog - logMin) / (logMax - logMin)).toFloat()
                                val markerX = chartPaddingPx + xProgress * chartWidth
                                val zone = powerCurveData.referenceData.userFtp?.let { ftp ->
                                    calculatePowerZone(power, ftp)
                                }
                                onMarkerSelected(
                                    PowerCurveMarker(
                                        interval = label,
                                        power = "${power.toInt()} W",
                                        hr = hr?.let { "${it.toInt()} bpm" },
                                        zone = zone,
                                        xPosition = markerX
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
            withTransform({
                scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
                translate(left = offsetX, top = offsetY)
            }) {
                drawEnhancedPowerCurveChart(
                    powerCurveData = powerCurveData,
                    selectedMarker = selectedMarker,
                    markerAlpha = markerAlpha,
                    chartPadding = chartPaddingPx,
                    animatedProgress = animatedProgress,
                    showFtpLine = showFtpLine,
                    showComparisonLine = showComparisonLine,
                    colorScheme = colorScheme
                )
            }
        }
        // No floating tooltip; selection is shown below chart as an info bar
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectionInfoBar(
    marker: PowerCurveMarker,
    onClose: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FlowRow(
                modifier = Modifier
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    label = marker.interval,
                    containerColor = colorScheme.surfaceVariant,
                    contentColor = colorScheme.onSurfaceVariant
                )
                MetricChip(
                    label = marker.power,
                    containerColor = colorScheme.primaryContainer,
                    contentColor = colorScheme.onPrimaryContainer
                )
                marker.hr?.let {
                    MetricChip(
                        label = it,
                        containerColor = colorScheme.errorContainer,
                        contentColor = colorScheme.onErrorContainer
                    )
                }
                marker.zone?.let {
                    val zoneColors = zoneColorsForLabel(it)
                    MetricPill(
                        label = it,
                        containerColor = zoneColors.containerColor,
                        contentColor = zoneColors.contentColor
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Clear selection",
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onClose() }
            )
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    containerColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MetricPill(label: String, containerColor: Color, contentColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LoadingState() {
    val colorScheme = MaterialTheme.colorScheme
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
                    colorScheme.surfaceVariant.copy(alpha = 0.3f),
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
                            colorScheme.outlineVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(1.dp)
                        )
                        .align(Alignment.CenterStart)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        CircularProgressIndicator(
            color = colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Loading power curve...",
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
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
                containerColor = colorScheme.errorContainer
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
                    tint = colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Failed to load power curve",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary
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
                    Text("Retry", color = colorScheme.onPrimary)
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
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.primaryContainer),
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
                tint = colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Tap a point on the curve to see details for that interval.",
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onPrimaryContainer,
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

// FloatingTooltipContent removed in favor of inline SelectionInfoBar to avoid overlap

@Composable
private fun PowerCurveStats(powerCurveData: PowerCurveResponse, fthr: Int? = null) {
    val colorScheme = MaterialTheme.colorScheme
    Column {
        Text(
            text = "Power Statistics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colorScheme.onSurface
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
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${power.toInt()} W",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
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
                        colorScheme.errorContainer,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Estimated FTHR",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onErrorContainer
                )
                Text(
                    text = "$fthrValue bpm",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onErrorContainer
                )
            }
        } ?: powerCurveData.referenceData.userFtp?.let { ftp ->
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        colorScheme.primaryContainer,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Current FTP",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onPrimaryContainer
                )
                Text(
                    text = "${ftp.toInt()} W",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

private fun DrawScope.drawEnhancedPowerCurveChart(
    powerCurveData: PowerCurveResponse,
    selectedMarker: PowerCurveMarker?,
    markerAlpha: Float,
    chartPadding: Float,
    animatedProgress: Float,
    showFtpLine: Boolean,
    showComparisonLine: Boolean,
    colorScheme: ColorScheme
) {
    val chartWidth = size.width - 2 * chartPadding
    val chartHeight = size.height - 2 * chartPadding

    val gridColor = colorScheme.outlineVariant
    val axisColor = colorScheme.outline
    val textColor = colorScheme.onSurfaceVariant

    val tickTextSizePx = 12.sp.toPx()
    val axisTitleTextSizePx = 14.sp.toPx()
    val ftpLabelTextSizePx = 12.sp.toPx()
    val tickLabelSpacing = 6.dp.toPx()
    val axisTitleSpacing = 8.dp.toPx()
    val yAxisLabelPadding = 8.dp.toPx()
    val ftpLabelVerticalPadding = 6.dp.toPx()
    val ftpLabelHorizontalPadding = 8.dp.toPx()

    val textPaint = Paint().apply {
        isAntiAlias = true
        color = textColor.toArgb()
        textSize = tickTextSizePx
    }

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

            textPaint.apply {
                color = textColor.toArgb()
                textSize = tickTextSizePx
                textAlign = Paint.Align.RIGHT
                isFakeBoldText = false
            }
            val yMetrics = textPaint.fontMetrics
            val yLabelBaseline = y - (yMetrics.ascent + yMetrics.descent) / 2f
            drawContext.canvas.nativeCanvas.drawText(
                "${powerValue.toInt()} W",
                chartPadding - yAxisLabelPadding,
                yLabelBaseline,
                textPaint
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
                textPaint.apply {
                    color = textColor.toArgb()
                    textSize = tickTextSizePx
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = false
                }
                val xMetrics = textPaint.fontMetrics
                val xBaseline = size.height - chartPadding + tickLabelSpacing - xMetrics.ascent
                drawContext.canvas.nativeCanvas.drawText(
                    timeLabels[i],
                    x,
                    xBaseline,
                    textPaint
                )
            }
        }

        textPaint.apply {
            color = textColor.toArgb()
            textSize = axisTitleTextSizePx
            textAlign = Paint.Align.LEFT
            isFakeBoldText = true
        }
        var axisMetrics = textPaint.fontMetrics
        val powerTitleBaseline = chartPadding - axisTitleSpacing - axisMetrics.ascent
        drawContext.canvas.nativeCanvas.drawText(
            "Power (W)",
            chartPadding,
            powerTitleBaseline,
            textPaint
        )

        textPaint.textAlign = Paint.Align.CENTER
        axisMetrics = textPaint.fontMetrics
        val timeTitleBaseline = size.height - chartPadding + axisTitleSpacing - axisMetrics.ascent
        drawContext.canvas.nativeCanvas.drawText(
            "Time",
            size.width / 2f,
            timeTitleBaseline,
            textPaint
        )
        textPaint.isFakeBoldText = false
        textPaint.textSize = tickTextSizePx

        powerCurveData.referenceData.userFtp?.let { ftp ->
            if (showFtpLine) {
                if (ftp >= powerMin && ftp <= powerMax) {
                    val yProgress = ((ftp - powerMin) / (powerMax - powerMin)).toFloat()
                    val y = chartPadding + chartHeight - (yProgress * chartHeight)

                    drawLine(
                        color = colorScheme.tertiary.copy(alpha = 0.7f),
                        start = Offset(chartPadding, y),
                        end = Offset(size.width - chartPadding, y),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                    )

                    textPaint.apply {
                        color = colorScheme.tertiary.toArgb()
                        textSize = ftpLabelTextSizePx
                        textAlign = Paint.Align.RIGHT
                        isFakeBoldText = true
                    }
                    val ftpMetrics = textPaint.fontMetrics
                    val ftpBaseline = y - ftpLabelVerticalPadding - ftpMetrics.descent
                    drawContext.canvas.nativeCanvas.drawText(
                        "FTP ${ftp.toInt()}W",
                        size.width - chartPadding - ftpLabelHorizontalPadding,
                        ftpBaseline,
                        textPaint
                    )
                    textPaint.apply {
                        color = textColor.toArgb()
                        textSize = tickTextSizePx
                        textAlign = Paint.Align.LEFT
                        isFakeBoldText = false
                    }
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
                color = colorScheme.secondary.copy(alpha = 0.6f),
                style = Stroke(
                    width = 2f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                )
            )
        }

        if (markerAlpha > 0f) {
            selectedMarker?.let { marker ->
                val x = marker.xPosition.coerceIn(chartPadding, size.width - chartPadding)
                drawLine(
                    color = colorScheme.primary.copy(alpha = 0.8f * markerAlpha),
                    start = Offset(x, chartPadding),
                    end = Offset(x, size.height - chartPadding),
                    strokeWidth = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                )
            }
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
                    color = colorScheme.primary.copy(alpha = 0.15f * animatedProgress)
                )

                drawPath(
                    path = path,
                    color = colorScheme.primary,
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

                    // Smaller point markers to reduce clutter
                    drawCircle(
                        color = colorScheme.surface.copy(alpha = pointAlpha),
                        radius = 4.5f,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = colorScheme.primary.copy(alpha = pointAlpha),
                        radius = 3f,
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
private data class ZoneColorPalette(val containerColor: Color, val contentColor: Color)

@Composable
private fun zoneColorsForLabel(zone: String): ZoneColorPalette {
    val colorScheme = MaterialTheme.colorScheme
    return when {
        zone.contains("Zone 1", ignoreCase = true) ->
            ZoneColorPalette(colorScheme.secondaryContainer, colorScheme.onSecondaryContainer)

        zone.contains("Zone 2", ignoreCase = true) ->
            ZoneColorPalette(colorScheme.tertiaryContainer, colorScheme.onTertiaryContainer)

        zone.contains("Zone 3", ignoreCase = true) ->
            ZoneColorPalette(colorScheme.primaryContainer, colorScheme.onPrimaryContainer)

        zone.contains("Zone 4", ignoreCase = true) ->
            ZoneColorPalette(colorScheme.secondary, colorScheme.onSecondary)

        zone.contains("Zone 5", ignoreCase = true) ->
            ZoneColorPalette(colorScheme.tertiary, colorScheme.onTertiary)

        zone.contains("Zone 6", ignoreCase = true) ->
            ZoneColorPalette(colorScheme.primary, colorScheme.onPrimary)

        zone.contains("Zone 7", ignoreCase = true) ->
            ZoneColorPalette(colorScheme.error, colorScheme.onError)

        else -> ZoneColorPalette(colorScheme.primaryContainer, colorScheme.onPrimaryContainer)
    }
}
