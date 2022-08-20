package com.example.visionapp.api

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.ArrayList

interface Api {
    // TODO: adjust to GET API PADIL
    @GET("posts")
    fun getPosts(): Call<ArrayList<PostResponse>>

    // TODO: adjust to POST API PADIL
    @FormUrlEncoded
    @POST("posts")
    fun createPosts(
        @Field("userId") userId: Int,
        @Field("title") title: String,
        @Field("body") body: String
    ): Call<CreatePostResponse>
}