package com.example.visionapp.api.datatype

data class ModelResponse(
    val status: Int,
    val message: String,
    val data: ModelData,
    val count: Int?,
    val next: Int?,
    val previous: Int?
)
