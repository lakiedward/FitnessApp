package com.example.fitnessapp.model

import com.google.gson.annotations.SerializedName

data class TrainingDateUpdate(
    @SerializedName("new_date")
    val newDate: String // Format: "YYYY-MM-DD"
)