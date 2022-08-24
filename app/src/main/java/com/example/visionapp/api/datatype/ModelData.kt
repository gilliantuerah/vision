package com.example.visionapp.api.datatype

data class ModelData(
    val _id: String,
    val created_date: String,
    val updated_date: String,
    val file_path: String,
    val file_size: Int,
    val version: Int,
    val name: String
)
