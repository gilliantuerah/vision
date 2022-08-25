package com.example.visionapp.api.datatype

data class PostPredictionReq(
    val image: String,
    val annotations: ArrayList<ResultAnnotation>,
)
