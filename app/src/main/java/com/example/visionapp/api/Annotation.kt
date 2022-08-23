package com.example.visionapp.api

import android.graphics.RectF

data class Annotation(
    val coordinates: ArrayList<RectF?>,
    val label: String
)
