package com.example.visionapp.api

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*
import java.io.File
import java.util.*

interface Api {
    @GET("api/ml")
    fun getModel(): Call<ModelResponse>

    @FormUrlEncoded
    @POST("api/ml/predict")
    fun predictImageServer(
        @Field("image") image: Bitmap?,
        @Field("model") model: String
    ): Call<PredictResponse>

    @FormUrlEncoded
    @POST("api/objects")
    fun postPrediction(
        @Field("annotations") annotations: ArrayList<Annotation>,
        @Field("file") file: Bitmap
    ): Call<String>
}