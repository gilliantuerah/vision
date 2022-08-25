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


class Service {
    fun getLastModel() {
        // retrofit
        RetrofitClient.instance.getModel(true).enqueue(object: Callback<ModelResponse> {
            override fun onResponse(call: Call<ModelResponse>, response: Response<ModelResponse>) {
                val responseCode = response.code().toString()
                Log.d(Constants.TAG, "response code$responseCode")

                val responseBody = response.body()
                Log.d("getlastmodel", responseBody.toString())

                val modelFile = responseBody?.data?.file_path
                if (modelFile != null) {
                    // if model exist
                    Log.d("getlastmodel", modelFile)
                }
            }

            override fun onFailure(call: Call<ModelResponse>, t: Throwable) {
                Log.e("Failed", t.message.toString())
            }
        })
    }

    private fun bitMapToString(bitmap: Bitmap?): String {
        val baos = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    fun predictOnServer(image: Bitmap, modelName: String) {
        val fileBase64 = bitMapToString(image)
        val body = PredictImageReq(fileBase64, modelName)

        RetrofitClient.instance.predictImageServer(body).enqueue(object: Callback<PredictImageResponse> {
            override fun onResponse(
                call: Call<PredictImageResponse>,
                response: Response<PredictImageResponse>
            ) {
                val responseCode = response.code().toString()
                Log.d(Constants.TAG, "response code$responseCode")

                val responseBody = response.body()
                Log.d(Constants.TAG, responseBody.toString())

                val result = responseBody?.data

                Log.d("prediksi", result.toString())
            }

            override fun onFailure(call: Call<PredictImageResponse>, t: Throwable) {
                Log.e("Failed", t.message.toString())
            }
        })
    }

    fun postPredictionResult(image: Bitmap, result: ArrayList<ResultAnnotation>) {
        val fileBase64 = bitMapToString(image)
        val body = PostPredictionReq(fileBase64, result)

        RetrofitClient.instance.postPrediction(body).enqueue(object: Callback<String> {
            override fun onResponse(
                call: Call<String>,
                response: Response<String>
            ) {
                val responseCode = response.code().toString()
                Log.d(Constants.TAG, "response code$responseCode")

                val response = "response $response"
                Log.d(Constants.TAG, response)
            }

            override fun onFailure(call: Call<String>, t: Throwable) {
                Log.e("Failed", t.message.toString())
            }
        })
    }
}