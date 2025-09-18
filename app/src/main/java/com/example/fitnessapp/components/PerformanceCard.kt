package com.example.fitnessapp.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import com.example.fitnessapp.ui.theme.extendedColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Data classes and enums for enhanced functionality
enum class TrendDirection { UP, DOWN, STABLE }

enum class Orientation { HORIZONTAL, VERTICAL }

data class TrendData(
    val direction: TrendDirection,
    val percentage: Float? = null
)

data class PerformanceThresholds(
    val minThreshold: Float,
    val maxThreshold: Float
)

// Helper function to determine performance color based on thresholds
@Composable
fun getPerformanceColor(
    value: Float,
    thresholds: PerformanceThresholds?
): Color {
    return thresholds?.let { 
        when {
            value >= it.maxThreshold -> MaterialTheme.extendedColors.success // Green
            value <= it.minThreshold -> MaterialTheme.colorScheme.error // Red
            else -> MaterialTheme.colorScheme.primary
        }
    } ?: MaterialTheme.colorScheme.primary
}

// Helper function to get trend icon
fun getTrendIcon(direction: TrendDirection): ImageVector {
    return when (direction) {
        TrendDirection.UP -> Icons.Default.KeyboardArrowUp
        TrendDirection.DOWN -> Icons.Default.KeyboardArrowDown
        TrendDirection.STABLE -> Icons.Default.KeyboardArrowUp // Could use a different icon for stable
    }
}

// Skeleton placeholder for loading states
@Composable
fun PerformanceCardSkeleton(
    cardHeight: Dp = 100.dp,
    orientation: Orientation = Orientation.HORIZONTAL,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val shimmerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val backgroundColor = MaterialTheme.colorScheme.surface

    Card(
        modifier = modifier
            .height(cardHeight)
            .let { if (testTag != null) it.testTag("${testTag}_skeleton") else it },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemInDarkTheme()) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (orientation == Orientation.HORIZONTAL) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon placeholder
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(shimmerColor, RoundedCornerShape(4.dp))
                )

                // Content placeholder
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(16.dp)
                            .background(shimmerColor, RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(24.dp)
                            .background(shimmerColor, RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(30.dp)
                            .height(12.dp)
                            .background(shimmerColor, RoundedCornerShape(4.dp))
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Icon placeholder
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(shimmerColor, RoundedCornerShape(4.dp))
                )

                // Content placeholder
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(16.dp)
                            .background(shimmerColor, RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(24.dp)
                            .background(shimmerColor, RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(30.dp)
                            .height(12.dp)
                            .background(shimmerColor, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PerformanceCard(
    title: String,
    value: String,
    unit: String,
    icon: Int? = null,
    numericValue: Float? = null,
    thresholds: PerformanceThresholds? = null,
    trendData: TrendData? = null,
    orientation: Orientation = Orientation.HORIZONTAL,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null,
    // New parameters for additional enhancements
    cardHeight: Dp = if (orientation == Orientation.VERTICAL) 120.dp else 100.dp,
    iconSize: Dp = 24.dp,
    containerColor: Color = Color.Unspecified,
    containerBrush: Brush? = null,
    testTag: String? = null,
    onClick: (() -> Unit)? = null
) {
    var showTooltip by remember { mutableStateOf(false) }

    // Haptic feedback and layout direction support
    val hapticFeedback = LocalHapticFeedback.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    // Calculate performance color based on thresholds
    val performanceColor = numericValue?.let { 
        getPerformanceColor(it, thresholds) 
    } ?: MaterialTheme.colorScheme.primary

    // Animate color changes (motion-reduce support can be added later with system settings)
    val animatedColor by animateFloatAsState(
        targetValue = if (performanceColor == MaterialTheme.extendedColors.success) 1f 
                     else if (performanceColor == MaterialTheme.colorScheme.error) -1f 
                     else 0f,
        animationSpec = tween(300),
        label = "colorAnimation"
    )

    val finalColor = when {
        animatedColor > 0.5f -> MaterialTheme.extendedColors.success
        animatedColor < -0.5f -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    // Determine container color with dark theme support
    val finalContainerColor = when {
        containerColor != Color.Unspecified -> containerColor
        isSystemInDarkTheme() -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .height(cardHeight)
            .let { if (testTag != null) it.testTag(testTag) else it }
            .clip(RoundedCornerShape(12.dp))
            .let { cardModifier ->
                if (containerBrush != null) {
                    cardModifier.background(containerBrush)
                } else {
                    cardModifier
                }
            }
            .combinedClickable(
                indication = rememberRipple(bounded = true),
                interactionSource = remember { MutableInteractionSource() },
                onLongClick = {
                    if (trendData != null && onLongPress != null) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        showTooltip = !showTooltip
                        onLongPress()
                    }
                },
                onClick = { onClick?.invoke() }
            )
            .semantics {
                val trendText = trendData?.let { trend ->
                    val direction = when (trend.direction) {
                        TrendDirection.UP -> "increasing"
                        TrendDirection.DOWN -> "decreasing"
                        TrendDirection.STABLE -> "stable"
                    }
                    val percentageText = trend.percentage?.let { " by ${it}%" } ?: ""
                    ", trend $direction$percentageText"
                } ?: ""

                contentDescription = "Performance: $title $value $unit$trendText"
                role = Role.Button
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (containerBrush == null) finalContainerColor else Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (orientation == Orientation.HORIZONTAL) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (icon == null) Arrangement.Center else Arrangement.SpaceBetween
            ) {
                // Icon (optional)
                icon?.let {
                    Image(
                        painter = painterResource(id = it),
                        contentDescription = title,
                        modifier = Modifier.size(iconSize),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                    )
                }

                // Content
                PerformanceContent(
                    title = title,
                    value = value,
                    unit = unit,
                    color = finalColor,
                    trendData = trendData,
                    showTooltip = showTooltip,
                    motionReduceEnabled = false // Can be enhanced with system settings detection
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Icon (optional)
                icon?.let {
                    Image(
                        painter = painterResource(id = it),
                        contentDescription = title,
                        modifier = Modifier.size(iconSize),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                    )
                }

                // Content
                PerformanceContent(
                    title = title,
                    value = value,
                    unit = unit,
                    color = finalColor,
                    trendData = trendData,
                    showTooltip = showTooltip,
                    motionReduceEnabled = false // Can be enhanced with system settings detection
                )
            }
        }
    }
}

@Composable
private fun PerformanceContent(
    title: String,
    value: String,
    unit: String,
    color: Color,
    trendData: TrendData?,
    showTooltip: Boolean,
    motionReduceEnabled: Boolean = false
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Animated value text with motion-reduce support
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    if (motionReduceEnabled) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        fadeIn(animationSpec = tween(300)) togetherWith 
                        fadeOut(animationSpec = tween(300))
                    }
                },
                label = "valueAnimation"
            ) { animatedValue ->
                Text(
                    text = animatedValue,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                    fontWeight = FontWeight.Bold,
                    color = color,
                    textAlign = TextAlign.Center
                )
            }

            // Trend indicator with RTL support
            trendData?.let { trend ->
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = getTrendIcon(trend.direction),
                    contentDescription = "Trend ${trend.direction.name.lowercase()}",
                    modifier = Modifier.size(16.dp),
                    tint = when (trend.direction) {
                        TrendDirection.UP -> MaterialTheme.extendedColors.success
                        TrendDirection.DOWN -> MaterialTheme.colorScheme.error
                        TrendDirection.STABLE -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }

        if (unit.isNotEmpty()) {
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Tooltip for trend data
        if (showTooltip && trendData?.percentage != null) {
            Text(
                text = "${trendData.percentage}% ${trendData.direction.name.lowercase()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
