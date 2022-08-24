package com.example.visionapp.api.datatype

import android.graphics.RectF

data class Annotation(
    val box: ArrayList<Float>,
    val label: String
)
