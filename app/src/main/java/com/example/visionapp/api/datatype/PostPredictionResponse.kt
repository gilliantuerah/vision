package com.example.visionapp.api.datatype

import java.util.ArrayList

data class PostPredictionResponse(
    val status: Int,
    val message: String,
    val data: PostPredictionData,
)
