package com.example.visionapp.api

import android.graphics.RectF

data class Annotation(
    val box: ArrayList<Float>,
    val label: String
)
