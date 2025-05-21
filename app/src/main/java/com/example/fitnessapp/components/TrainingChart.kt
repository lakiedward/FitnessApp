package com.example.fitnessapp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.fitnessapp.model.WorkoutStep

@Composable
fun TrainingChart(steps: List<WorkoutStep>) {
    val totalDuration = steps.sumOf {
        when (it) {
            is WorkoutStep.SteadyState -> it.duration
            is WorkoutStep.IntervalsT -> it.repeat * (it.on_duration + it.off_duration)
        }
    }.toFloat()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(horizontal = 8.dp)
    ) {
        steps.forEach { step ->
            when (step) {
                is WorkoutStep.SteadyState -> {
                    val weight = step.duration / totalDuration

                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(weight)
                            .padding(horizontal = 1.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((step.power * 100).dp)
                                .background(Color.Blue.copy(alpha = step.power.coerceIn(0.3f, 1f)))
                        )
                    }
                }

                is WorkoutStep.IntervalsT -> {
                    repeat(step.repeat) {
                        val onWeight = step.on_duration / totalDuration
                        val offWeight = step.off_duration / totalDuration

                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(onWeight)
                                .padding(horizontal = 1.dp),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((step.on_power * 100).dp)
                                    .background(Color.Red.copy(alpha = step.on_power.coerceIn(0.3f, 1f)))
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(offWeight)
                                .padding(horizontal = 1.dp),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((step.off_power * 100).dp)
                                    .background(Color.Gray.copy(alpha = step.off_power.coerceIn(0.3f, 1f)))
                            )
                        }
                    }
                }
            }
        }
    }
}
