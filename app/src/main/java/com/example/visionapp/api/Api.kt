package com.example.visionapp.api

import android.graphics.Bitmap
import retrofit2.Call
import retrofit2.http.*
import java.util.*

interface Api {
    // TODO: adjust to GET API PADIL
//    @GET("posts")
//    fun getPosts(): Call<ArrayList<PostResponse>>

    @GET("api/ml/{id}")
    fun getModelById(@Path("id") id: String): Call<String>

    // TODO: adjust to POST API PADIL
//    @FormUrlEncoded
//    @POST("posts")
//    fun createPosts(
//        @Field("userId") userId: Int,
//        @Field("title") title: String,
//        @Field("body") body: String
//    ): Call<CreatePostResponse>

    @FormUrlEncoded
    @POST("api/ml/predict")
    fun predictImageServer(
        @Field("image") image: Bitmap,
        @Field("model") model: String
    ): Call<PredictResponse>

    @FormUrlEncoded
    @POST("api/objects")
    fun postPrediction(
        @Field("annotations") annotations: ArrayList<Annotation>,
        @Field("file") file: Bitmap
    ): Call<String>
}