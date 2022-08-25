package com.example.visionapp.api.datatype

data class PostPredictionData(
    val created_date: String,
    val updated_date: String,
    val img_url: String,
    val img_name: String,
    val img_size: Int,
    val img_dimension: String,
    val annotations: ArrayList<ResultAnnotation>
)
