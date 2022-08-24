package com.example.visionapp.api

data class ModelResponse(
    val status: Int,
    val message: String,
    val data: ArrayList<ModelData>,
    val count: Int?,
    val next: Int?,
    val previous: Int?
)
