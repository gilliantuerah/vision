package com.example.visionapp.api

import com.example.visionapp.api.datatype.*
import retrofit2.Call
import retrofit2.http.*


interface Api {
    @GET("api/ml")
    fun getModel(
        @Query("last") getLast: Boolean
    ): Call<ModelResponse>

    @POST("api/ml/predict/")
    fun predictImageServer(
        @Body predictRequest: PredictImageReq
    ): Call<PredictImageResponse>

    @POST("api/objects/")
    fun postPrediction(
        @Body postPreidctReq: PostPredictionReq
    ): Call<PostPredictionResponse>
}