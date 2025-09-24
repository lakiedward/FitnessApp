package com.example.fitnessapp.model

import com.google.gson.annotations.SerializedName

data class PerformanceRecalcResponse(
    @SerializedName("jobId")
    val jobId: String? = null,
    @SerializedName("job_id")
    val jobIdSnake: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("message")
    val message: String? = null
) {
    val resolvedJobId: String?
        get() = jobId ?: jobIdSnake
}

data class PerformanceMetricStatusResponse(
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("lastComputedAt")
    val lastComputedAt: String? = null,
    @SerializedName("last_computed_at")
    val lastComputedAtSnake: String? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("value")
    val value: String? = null,
    @SerializedName("unit")
    val unit: String? = null,
    @SerializedName("source")
    val source: String? = null,
    @SerializedName("cooldownUntil")
    val cooldownUntil: String? = null,
    @SerializedName("cooldown_until")
    val cooldownUntilSnake: String? = null
) {
    val resolvedLastComputedAt: String?
        get() = lastComputedAt ?: lastComputedAtSnake
    val resolvedCooldownUntil: String?
        get() = cooldownUntil ?: cooldownUntilSnake
}
