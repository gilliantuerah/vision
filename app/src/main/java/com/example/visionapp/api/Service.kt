package com.example.visionapp.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.visionapp.api.datatype.*
import com.example.visionapp.env.Constants
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.util.*
import kotlin.collections.ArrayList


class Service {
    fun getLastModel(): String? {
        // return file path of model from server
        var modelFile: String? = null

        // retrofit
        RetrofitClient.instance.getModel(true).enqueue(object: Callback<ModelResponse> {
            override fun onResponse(call: Call<ModelResponse>, response: Response<ModelResponse>) {
                val responseCode = response.code().toString()
                Log.d(Constants.TAG, "response code$responseCode")

                val responseBody = response.body()
                Log.d("getlastmodel", responseBody.toString())

                modelFile = responseBody?.data?.file_path

            }

            override fun onFailure(call: Call<ModelResponse>, t: Throwable) {
                Log.e("Failed", t.message.toString())
            }
        })

        return modelFile
    }

    private fun bitMapToString(bitmap: Bitmap?): String {
        val baos = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    fun predictOnServer(image: Bitmap, modelName: String): ArrayList<ResultAnnotation>? {
        val fileBase64 = bitMapToString(image)
        val body = PredictImageReq(fileBase64, modelName)
        var result: ArrayList<ResultAnnotation>? = ArrayList()

        RetrofitClient.instance.predictImageServer(body).enqueue(object: Callback<PredictImageResponse> {
            override fun onResponse(
                call: Call<PredictImageResponse>,
                response: Response<PredictImageResponse>
            ) {
                val responseCode = response.code().toString()
                Log.d(Constants.TAG, "response code$responseCode")

                val responseBody = response.body()
                Log.d(Constants.TAG, responseBody.toString())

                result = responseBody?.data
            }

            override fun onFailure(call: Call<PredictImageResponse>, t: Throwable) {
                Log.e("Failed", t.message.toString())
            }
        })
        return result
    }

    fun postPredictionResult(image: Bitmap, result: ArrayList<ResultAnnotation>) {
        val fileBase64 = bitMapToString(image)
        val body = PostPredictionReq(fileBase64, result)

        RetrofitClient.instance.postPrediction(body).enqueue(object: Callback<PostPredictionResponse> {
            override fun onResponse(
                call: Call<PostPredictionResponse>,
                response: Response<PostPredictionResponse>
            ) {
                val responseCode = response.code().toString()
                Log.d("postpredict", "response code$responseCode")

                val responseBody = response.body()
                Log.d("postpredict", responseBody.toString())
            }

            override fun onFailure(call: Call<PostPredictionResponse>, t: Throwable) {
                Log.e("Failed", t.message.toString())
            }
        })
    }
}