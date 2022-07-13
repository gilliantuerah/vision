package com.example.visionapp

import retrofit2.Call
import retrofit2.http.GET
import java.util.ArrayList

interface Api {
    // TODO: adjust to GET API PADIL
    @GET("posts")
    fun getPosts(): Call<ArrayList<PostResponse>>
}