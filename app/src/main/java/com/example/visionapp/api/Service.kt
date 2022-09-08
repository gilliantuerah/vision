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


class Service {
    // path model yoloV5 from server
    var modelFromServer: String? = null
    var resultModelOnline: ArrayList<ResultAnnotation>? = ArrayList()

    fun getLastModel() {
        // retrofit
        RetrofitClient.instance.getModel(true).enqueue(object: Callback<ModelResponse> {
            override fun onResponse(call: Call<ModelResponse>, response: Response<ModelResponse>) {
                val responseCode = response.code().toString()
                Log.d(Constants.TAG, "response code$responseCode")

                val responseBody = response.body()
                Log.d("getlastmodel", responseBody.toString())

                modelFromServer = responseBody?.data?.file_path

            }

            override fun onFailure(call: Call<ModelResponse>, t: Throwable) {
                Log.e("Failed", t.message.toString())
            }
        })
    }

    private fun bitMapToString(bitmap: Bitmap?): String {
        var baos = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 50, baos)
        var b = baos.toByteArray()

        var temp: String = ""

        try {
            System.gc()
            temp = Base64.encodeToString(b, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
        } catch (e: OutOfMemoryError) {
            baos = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 0, baos)
            b = baos.toByteArray()
            temp = Base64.encodeToString(b, Base64.DEFAULT)
            Log.d("EWN", "Out of memory error catched")
        }
        return temp
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
                Log.d("hasilonline", "response code$responseCode")

                val responseBody = response.body()
                Log.d("hasilonline", responseBody?.data.toString())

                resultModelOnline = responseBody?.data
                Log.d("hasilonline", resultModelOnline.toString())
            }

            override fun onFailure(call: Call<PredictImageResponse>, t: Throwable) {
                // set result to empty
                resultModelOnline = ArrayList()
                Log.e("Failed", t.message.toString())
            }
        })
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