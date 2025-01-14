package com.example.fitnessapp.model

data class RacesModelResponse(
    val message: String,
    val data: List<RaceModel>
)

data class RaceModel(
    val race_date: String,
)
