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
    private fun getModelByName(modelName: String, responseData: ArrayList<ModelData>) : String? {
        responseData.forEach {
            // check if model exist in array
            if(it.name == modelName) {
                return it.file_path
            }
        }

        return null
    }

    fun getModel() {
        // retrofit
        RetrofitClient.instance.getModel().enqueue(object: Callback<ModelResponse> {
            override fun onResponse(call: Call<ModelResponse>, response: Response<ModelResponse>) {
                val responseCode = response.code().toString()
                Log.d(Constants.TAG, "response code$responseCode")

                val responseBody = response.body()
                Log.d(Constants.TAG, responseBody.toString())

                val modelFile = responseBody?.data?.let { getModelByName("MobileNet", it) }
                if (modelFile != null) {
                    // if model exist
                    Log.d(Constants.TAG, modelFile)
                }
            }

            override fun onFailure(call: Call<ModelResponse>, t: Throwable) {
                Log.e("Failed", t.message.toString())
            }
        })
    }

    private fun bitMapToString(bitmap: Bitmap?): String {
        val baos = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 0, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    fun predictOnServer(image: Bitmap, modelName: String) {
        val fileBase64 = bitMapToString(image)
        val body = PredictImageReq(fileBase64, modelName)

        RetrofitClient.instance.predictImageServer(body).enqueue(object: Callback<PredictResponse> {
            override fun onResponse(
                call: Call<PredictResponse>,
                response: Response<PredictResponse>
            ) {
                val responseCode = response.code().toString()
                Log.d(Constants.TAG, "response code$responseCode")

                val response = "response $response"
                Log.d(Constants.TAG, response)
            }

            override fun onFailure(call: Call<PredictResponse>, t: Throwable) {
                Log.e("Failed", t.message.toString())
            }
        })
    }

    fun postPredictionResult(image: Bitmap, result: ArrayList<Annotation>) {
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