package com.example.visionapp.api.datatype

import kotlin.Annotation

data class PostPredictionReq(
    val image: String,
    val annotations: ArrayList<Annotation>,
)
