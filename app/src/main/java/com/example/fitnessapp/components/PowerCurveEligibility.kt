package com.example.fitnessapp.components

import java.util.Locale

private val POWER_CURVE_ELIGIBLE_TYPES = setOf(
    "ride",
    "virtualride",
    "cycling"
)

fun isPowerCurveEligible(activityType: String?): Boolean {
    val normalizedType = activityType?.lowercase(Locale.getDefault()) ?: return false
    return normalizedType in POWER_CURVE_ELIGIBLE_TYPES
}
