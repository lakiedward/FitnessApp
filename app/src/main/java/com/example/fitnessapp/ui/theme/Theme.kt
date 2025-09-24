package com.example.fitnessapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ——— Light palette (AURORA) ———
val BrandPrimary = Color(0xFF5F5EF1)
val BrandPrimaryContainer = Color(0xFFE0E7FF)
val BrandSecondary = Color(0xFF22D3EE)
val BrandSecondaryContainer = Color(0xFFCFFAFE)
val BrandTertiary = Color(0xFF10B981)
val BrandTertiaryContainer = Color(0xFFBBF7D0)
val BrandSurfaceVariantLight = Color(0xFFF3F4F6)
val BrandOutlineLight = Color(0xFF9CA3AF)
val BrandOutlineVariantLight = Color(0xFFE5E7EB)
val BrandGradientAccent = Color(0xFFA855F7)
val BrandSurfaceMutedLight = Color(0xFFF1F5F9)
val BrandSurfaceSubtleLight = Color(0xFFF8FAFC)
val BrandBorderSubtleLight = Color(0xFFE5EAF2)
val BrandBorderStrongLight = Color(0xFFE2E8F0)
val BrandWarningLight = Color(0xFFD97706)
val BrandWarningContainerLight = Color(0xFFFEF3C7)
val BrandInfoLight = Color(0xFF0EA5E9)
val BrandInfoContainerLight = Color(0xFFE0F2FE)
val BrandStrava = Color(0xFFFC4C02)
val BrandChartPower = Color(0xFFFF6B35)
val BrandChartHeartRate = Color(0xFFEF4444)
val BrandChartCadence = Color(0xFF22C55E)
val BrandChartSpeed = Color(0xFF3B82F6)
val BrandChartAltitude = Color(0xFF8B5CF6)
val BrandChartGridLight = Color(0xFFE5E7EB)
val BrandChartAxisLight = Color(0xFF9CA3AF)
val BrandChartTrackLight = Color(0xFFE5E7EB)
val BrandSuccessContainerLight = Color(0xFFECFDF5)
val BrandZone1Light = Color(0xFF6EE7B7)
val BrandZone2Light = Color(0xFF34D399)
val BrandZone3Light = Color(0xFF10B981)
val BrandZone4Light = Color(0xFF059669)
val BrandZone5Light = Color(0xFF047857)

// Dark palette (new)
val BrandBackgroundDark = Color(0xFF0E121A)
val BrandPrimaryDark = Color(0xFF9DB2FF)
val BrandPrimaryContainerDark = Color(0xFF223169)
val BrandSecondaryDark = Color(0xFFB7A4FF)
val BrandSecondaryContainerDark = Color(0xFF33295E)
val BrandTertiaryDark = Color(0xFF34D399)
val BrandTertiaryContainerDark = Color(0xFF0B3A2B)
val BrandSurfaceDark = Color(0xFF161B22)
val BrandSurfaceVariantDark = Color(0xFF1F2633)
val BrandSurfaceContrastDark = Color(0xFF273040)
val BrandOutlineDark = Color(0xFF2B3544)
val BrandOutlineVariantDark = Color(0xFF384055)
val BrandGradientPrimaryDark = Color(0xFF2C2367)
val BrandGradientSecondaryDark = Color(0xFF3F35A6)
val BrandGradientAccentDark = Color(0xFF2B306B)
val BrandSurfaceMutedDark = Color(0xFF1F2633)
val BrandSurfaceSubtleDark = Color(0xFF161B22)
val BrandBorderSubtleDark = Color(0xFF2B3544)
val BrandBorderStrongDark = Color(0xFF405066)
val BrandWarningDark = Color(0xFFF59E0B)
val BrandWarningContainerDark = Color(0xFF3A2300)
val BrandInfoDark = Color(0xFF38BDF8)
val BrandInfoContainerDark = Color(0xFF10324F)
val BrandStravaDark = Color(0xFFFF6B35)
val BrandChartGridDark = Color(0xFF2B3544)
val BrandChartAxisDark = Color(0xFFB3C1D1)
val BrandChartTrackDark = Color(0xFF273040)
val BrandSuccessContainerDark = Color(0xFF0B3A2B)
val BrandZone1Dark = Color(0xFF1C6B44)
val BrandZone2Dark = Color(0xFF218B61)
val BrandZone3Dark = Color(0xFF2AA172)
val BrandZone4Dark = Color(0xFF34D399)
val BrandZone5Dark = Color(0xFF4FD4BE)

@Immutable
data class FitnessExtendedColors(
    val gradientPrimary: Color,
    val gradientSecondary: Color,
    val gradientAccent: Color,
    val surfaceMuted: Color,
    val surfaceSubtle: Color,
    val surfaceContrast: Color,
    val borderSubtle: Color,
    val borderStrong: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val strava: Color,
    val stravaOnColor: Color,
    val chartPower: Color,
    val chartHeartRate: Color,
    val chartCadence: Color,
    val chartSpeed: Color,
    val chartAltitude: Color,
    val chartGrid: Color,
    val chartAxis: Color,
    val chartTrack: Color,
    val hrZone1: Color,
    val hrZone2: Color,
    val hrZone3: Color,
    val hrZone4: Color,
    val hrZone5: Color
)

private val LocalExtendedColors = staticCompositionLocalOf {
    FitnessExtendedColors(
        gradientPrimary = BrandPrimary,
        gradientSecondary = BrandSecondary,
        gradientAccent = BrandGradientAccent,
        surfaceMuted = BrandSurfaceMutedLight,
        surfaceSubtle = BrandSurfaceSubtleLight,
        surfaceContrast = Color.White,
        borderSubtle = BrandBorderSubtleLight,
        borderStrong = BrandBorderStrongLight,
        success = BrandTertiary,
        successContainer = BrandSuccessContainerLight,
        warning = BrandWarningLight,
        warningContainer = BrandWarningContainerLight,
        info = BrandInfoLight,
        infoContainer = BrandInfoContainerLight,
        strava = BrandStrava,
        stravaOnColor = Color.White,
        chartPower = BrandChartPower,
        chartHeartRate = BrandChartHeartRate,
        chartCadence = BrandChartCadence,
        chartSpeed = BrandChartSpeed,
        chartAltitude = BrandChartAltitude,
        chartGrid = BrandChartGridLight,
        chartAxis = BrandChartAxisLight,
        chartTrack = BrandChartTrackLight,
        hrZone1 = BrandZone1Light,
        hrZone2 = BrandZone2Light,
        hrZone3 = BrandZone3Light,
        hrZone4 = BrandZone4Light,
        hrZone5 = BrandZone5Light
    )
}

private fun lightExtendedColors(): FitnessExtendedColors = FitnessExtendedColors(
    gradientPrimary = BrandPrimary,
    gradientSecondary = BrandSecondary,
    gradientAccent = BrandGradientAccent,
    surfaceMuted = BrandSurfaceMutedLight,
    surfaceSubtle = BrandSurfaceSubtleLight,
    surfaceContrast = Color.White,
    borderSubtle = BrandBorderSubtleLight,
    borderStrong = BrandBorderStrongLight,
    success = BrandTertiary,
    successContainer = BrandSuccessContainerLight,
    warning = BrandWarningLight,
    warningContainer = BrandWarningContainerLight,
    info = BrandInfoLight,
    infoContainer = BrandInfoContainerLight,
    strava = BrandStrava,
    stravaOnColor = Color.White,
    chartPower = BrandChartPower,
    chartHeartRate = BrandChartHeartRate,
    chartCadence = BrandChartCadence,
    chartSpeed = BrandChartSpeed,
    chartAltitude = BrandChartAltitude,
    chartGrid = BrandChartGridLight,
    chartAxis = BrandChartAxisLight,
    chartTrack = BrandChartTrackLight,
    hrZone1 = BrandZone1Light,
    hrZone2 = BrandZone2Light,
    hrZone3 = BrandZone3Light,
    hrZone4 = BrandZone4Light,
    hrZone5 = BrandZone5Light
)

private fun darkExtendedColors(): FitnessExtendedColors = FitnessExtendedColors(
    gradientPrimary = BrandGradientPrimaryDark,
    gradientSecondary = BrandGradientSecondaryDark,
    gradientAccent = BrandGradientAccentDark,
    surfaceMuted = BrandSurfaceMutedDark,
    surfaceSubtle = BrandSurfaceSubtleDark,
    surfaceContrast = BrandSurfaceContrastDark,
    borderSubtle = BrandBorderSubtleDark,
    borderStrong = BrandBorderStrongDark,
    success = BrandTertiaryDark,
    successContainer = BrandTertiaryContainerDark,
    warning = BrandWarningDark,
    warningContainer = BrandWarningContainerDark,
    info = BrandInfoDark,
    infoContainer = BrandInfoContainerDark,
    strava = BrandStravaDark,
    stravaOnColor = Color.White,
    chartPower = BrandChartPower,
    chartHeartRate = BrandChartHeartRate,
    chartCadence = BrandChartCadence,
    chartSpeed = BrandChartSpeed,
    chartAltitude = BrandChartAltitude,
    chartGrid = BrandChartGridDark,
    chartAxis = BrandChartAxisDark,
    chartTrack = BrandChartTrackDark,
    hrZone1 = BrandZone1Dark,
    hrZone2 = BrandZone2Dark,
    hrZone3 = BrandZone3Dark,
    hrZone4 = BrandZone4Dark,
    hrZone5 = BrandZone5Dark
)

// Scheme de culori statice
private val lightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = BrandSecondary,
    onSecondary = Color(0xFF083344),
    secondaryContainer = BrandSecondaryContainer,
    onSecondaryContainer = Color(0xFF083344),
    tertiary = BrandTertiary,
    onTertiary = Color(0xFF052E21),
    tertiaryContainer = BrandTertiaryContainer,
    onTertiaryContainer = Color(0xFF052E21),
    background = Color.White,
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    surfaceVariant = BrandSurfaceVariantLight,
    onSurfaceVariant = Color(0xFF4B5563),
    outline = BrandOutlineLight,
    outlineVariant = BrandOutlineVariantLight
)

private val darkColorScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = Color(0xFF0F1A3A),
    primaryContainer = BrandPrimaryContainerDark,
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = BrandSecondaryDark,
    onSecondary = Color(0xFF101634),
    secondaryContainer = BrandSecondaryContainerDark,
    onSecondaryContainer = Color(0xFFDDD9FF),
    tertiary = BrandTertiaryDark,
    onTertiary = Color(0xFF052E21),
    tertiaryContainer = BrandTertiaryContainerDark,
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = BrandBackgroundDark,
    onBackground = Color(0xFFE6EDF3),
    surface = BrandSurfaceDark,
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = BrandSurfaceVariantDark,
    onSurfaceVariant = Color(0xFFB3C1D1),
    outline = BrandOutlineDark,
    outlineVariant = BrandOutlineVariantDark,
    scrim = Color.Black
)

@Composable
fun FitnessAppTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val baseScheme =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dynamicColor) {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            if (isDark) darkColorScheme else lightColorScheme
        }

    val onSecondaryFixed = if (isDark) Color(0xFF042A2D) else Color(0xFF083344)
    val colorScheme = baseScheme.copy(
        primary = BrandPrimary,
        onPrimary = Color.White,
        secondary = BrandSecondary,
        onSecondary = onSecondaryFixed,
        tertiary = BrandTertiary,
        onTertiary = Color(0xFF052E21)
    )

    val extendedColors = if (isDark) {
        darkExtendedColors()
    } else {
        lightExtendedColors()
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@get:ReadOnlyComposable
val MaterialTheme.extendedColors: FitnessExtendedColors
    @Composable
    get() = LocalExtendedColors.current
