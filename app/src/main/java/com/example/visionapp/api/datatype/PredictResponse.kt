package com.example.visionapp.api.datatype

import java.util.ArrayList
import kotlin.Annotation

data class PredictResponse(
    val status: String?,
    val message: Int,
    val data: ArrayList<Annotation>,
)
