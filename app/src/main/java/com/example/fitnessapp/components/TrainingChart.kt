package com.example.fitnessapp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import com.example.fitnessapp.ui.theme.extendedColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fitnessapp.model.WorkoutStep

@Composable
fun TrainingChart(
    steps: List<WorkoutStep>,
    workoutType: String = "cycling", // cycling, running, swimming
    cyclingFtp: Float? = null,
    runningFtp: Float? = null,
    swimmingPace: String? = null
) {
    var selectedSegment by remember { mutableStateOf<SegmentInfo?>(null) }
    var touchPosition by remember { mutableStateOf<Offset?>(null) }

    val density = LocalDensity.current
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = MaterialTheme.extendedColors

    val chartColors = TrainingChartColors(
        steadyState = colorScheme.primary,
        intervalWork = extendedColors.chartPower,
        intervalRest = colorScheme.surfaceVariant,
        ramp = extendedColors.chartSpeed,
        freeRide = extendedColors.warning,
        powerIntervalWork = extendedColors.chartHeartRate,
        powerIntervalRest = extendedColors.surfaceMuted,
        pyramid = extendedColors.chartAltitude
    )

    val totalDuration = calculateTotalDuration(steps)
    val segments = createSegments(steps, totalDuration, chartColors)
    val gridLineColor = colorScheme.outlineVariant.copy(alpha = 0.35f)

    Box(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(horizontal = 8.dp)
        ) {
            val levels = listOf(0.55f, 0.75f, 0.90f, 1.05f)
            levels.forEach { level ->
                val y = size.height * (1f - level.coerceIn(0f, 1f))
                drawLine(
                    color = gridLineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(horizontal = 8.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        touchPosition = offset
                        selectedSegment =
                            findSegmentAtPosition(segments, offset.x, size.width.toFloat())
                    }
                }
        ) {
            segments.forEach { segment ->
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(segment.weight)
                        .padding(horizontal = 1.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(with(density) { (segment.intensity * 100).dp })
                            .background(
                                segment.color.copy(
                                    alpha = segment.intensity.coerceIn(0.3f, 1f)
                                ),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }

        // Tooltip
        selectedSegment?.let { segment ->
            touchPosition?.let { position ->
                Card(
                    modifier = Modifier
                        .offset(
                            x = with(density) { (position.x - 60).toDp() },
                            y = 8.dp
                        )
                        .width(120.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = formatDuration(segment.duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Medium
                        )

                        val sportValue = calculateSportSpecificValue(
                            segment.intensity,
                            workoutType,
                            cyclingFtp,
                            runningFtp,
                            swimmingPace
                        )

                        Text(
                            text = sportValue,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "${(segment.intensity * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = chartColors.intervalRest
                        )
                    }
                }
            }
        }
    }
}

private fun calculateTotalDuration(steps: List<WorkoutStep>): Float {
    return steps.sumOf {
        when (it) {
            is WorkoutStep.SteadyState -> it.duration
            is WorkoutStep.IntervalsT -> it.repeat * (it.on_duration + it.off_duration)
            is WorkoutStep.Ramp -> it.duration
            is WorkoutStep.FreeRide -> it.duration
            is WorkoutStep.IntervalsP -> it.repeat * (it.on_duration + it.off_duration)
            is WorkoutStep.Pyramid -> it.repeat * it.step_duration * 2
        }
    }.toFloat()
}

private fun createSegments(
    steps: List<WorkoutStep>,
    totalDuration: Float,
    colors: TrainingChartColors
): List<SegmentInfo> {
    val segments = mutableListOf<SegmentInfo>()

    steps.forEach { step ->
        when (step) {
            is WorkoutStep.SteadyState -> {
                segments.add(
                    SegmentInfo(
                        duration = step.duration,
                        intensity = step.power,
                        weight = step.duration / totalDuration,
                        color = colors.steadyState,
                        type = "Steady"
                    )
                )
            }

            is WorkoutStep.IntervalsT -> {
                repeat(step.repeat) {
                    segments.add(
                        SegmentInfo(
                            duration = step.on_duration,
                            intensity = step.on_power,
                            weight = step.on_duration / totalDuration,
                            color = colors.intervalWork,
                            type = "Work"
                        )
                    )
                    segments.add(
                        SegmentInfo(
                            duration = step.off_duration,
                            intensity = step.off_power,
                            weight = step.off_duration / totalDuration,
                            color = colors.intervalRest,
                            type = "Rest"
                        )
                    )
                }
            }

            is WorkoutStep.Ramp -> {
                // Simplificat pentru ramp - folosim intensitatea medie
                val avgIntensity = (step.start_power + step.end_power) / 2f
                segments.add(
                    SegmentInfo(
                        duration = step.duration,
                        intensity = avgIntensity,
                        weight = step.duration / totalDuration,
                        color = colors.ramp,
                        type = "Ramp"
                    )
                )
            }

            is WorkoutStep.FreeRide -> {
                val avgIntensity = (step.power_low + step.power_high) / 2f
                segments.add(
                    SegmentInfo(
                        duration = step.duration,
                        intensity = avgIntensity,
                        weight = step.duration / totalDuration,
                        color = colors.freeRide,
                        type = "Free"
                    )
                )
            }

            is WorkoutStep.IntervalsP -> {
                repeat(step.repeat) {
                    segments.add(
                        SegmentInfo(
                            duration = step.on_duration,
                            intensity = step.on_power,
                            weight = step.on_duration / totalDuration,
                            color = colors.powerIntervalWork,
                            type = "Work"
                        )
                    )
                    segments.add(
                        SegmentInfo(
                            duration = step.off_duration,
                            intensity = step.off_power,
                            weight = step.off_duration / totalDuration,
                            color = colors.powerIntervalRest,
                            type = "Rest"
                        )
                    )
                }
            }

            is WorkoutStep.Pyramid -> {
                repeat(step.repeat) {
                    segments.add(
                        SegmentInfo(
                            duration = step.step_duration,
                            intensity = step.peak_power,
                            weight = step.step_duration / totalDuration,
                            color = colors.pyramid,
                            type = "Up"
                        )
                    )
                    segments.add(
                        SegmentInfo(
                            duration = step.step_duration,
                            intensity = step.end_power,
                            weight = step.step_duration / totalDuration,
                            color = colors.pyramid,
                            type = "Down"
                        )
                    )
                }
            }
        }
    }

    return segments
}

private fun findSegmentAtPosition(
    segments: List<SegmentInfo>,
    touchX: Float,
    totalWidth: Float
): SegmentInfo? {
    var currentX = 0f

    segments.forEach { segment ->
        val segmentWidth = segment.weight * totalWidth
        if (touchX >= currentX && touchX <= currentX + segmentWidth) {
            return segment
        }
        currentX += segmentWidth
    }

    return null
}

private fun calculateSportSpecificValue(
    intensity: Float,
    workoutType: String,
    cyclingFtp: Float?,
    runningFtp: Float?,
    swimmingPace: String?
): String {
    return when (workoutType.lowercase()) {
        "cycling" -> {
            val ftp = cyclingFtp ?: 200f
            val watts = (intensity * ftp).toInt()
            "${watts}W"
        }

        "running" -> {
            val ftpSpeed = runningFtp ?: 4.0f // m/s (already converted from decimal minutes)
            // Pentru running, intensitatea mai micÄƒ Ã®nseamnÄƒ pace mai lent
            val targetSpeed =
                ftpSpeed * intensity // La intensitate mai micÄƒ = vitezÄƒ mai micÄƒ = pace mai lent
            val targetPaceSeconds = 1000f / targetSpeed // secunde per km
            val paceMin = (targetPaceSeconds / 60).toInt()
            val paceSec = (targetPaceSeconds % 60).toInt()
            val kmh = targetSpeed * 3.6f // km/h
            "${String.format("%.1f", kmh)} km/h\n${paceMin}:${String.format("%02d", paceSec)}/km"
        }

        "swimming" -> {
            val basePace = swimmingPace ?: "1:30" // 100m pace
            val basePaceSeconds = parseSwimPace(basePace)
            val adjustedPace = basePaceSeconds / intensity
            val minutes = (adjustedPace / 60).toInt()
            val seconds = (adjustedPace % 60).toInt()
            "${minutes}:${String.format("%02d", seconds)}/100m"
        }

        else -> "${(intensity * 100).toInt()}%"
    }
}

private fun parseSwimPace(pace: String): Float {
    return try {
        val parts = pace.split(":")
        val minutes = parts[0].toFloat()
        val seconds = parts[1].toFloat()
        minutes * 60 + seconds
    } catch (e: Exception) {
        90f // Default 1:30
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "${minutes}:${String.format("%02d", remainingSeconds)}"
}

private data class TrainingChartColors(
    val steadyState: Color,
    val intervalWork: Color,
    val intervalRest: Color,
    val ramp: Color,
    val freeRide: Color,
    val powerIntervalWork: Color,
    val powerIntervalRest: Color,
    val pyramid: Color
)
data class SegmentInfo(
    val duration: Int,
    val intensity: Float,
    val weight: Float,
    val color: Color,
    val type: String
)


