package com.example.visionapp.api.datatype

import java.util.ArrayList
import kotlin.Annotation

data class PredictImageResponse(
    val status: Int,
    val message: String,
    val data: ArrayList<ResultAnnotation>?,
)
