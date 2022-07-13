package com.example.visionapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import androidx.camera.core.Camera
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.visionapp.databinding.ActivityMainBinding
import org.tensorflow.lite.task.vision.detector.Detection
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// TODO: delete later

class MainActivity : AppCompatActivity(), ObjectDetectorHelper.DetectorListener {
    private lateinit var binding: ActivityMainBinding

    private lateinit var tts : TextToSpeech

    private lateinit var cameraM: CameraManager
    private lateinit var camera: Camera
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraId: String
    private var isFlashOn = false

    // make it public, accessible to other file (ObjectDetectorHelper file)
    private var modelInUse: MutableLiveData<Int> = MutableLiveData<Int>(0)
    var modelName: String = when (modelInUse.value) {
        MODEL_MOBILENETV1 -> "mobilenetv1.tflite"
        MODEL_EFFICIENTDETV0 -> "efficientdet-lite0.tflite"
        MODEL_EFFICIENTDETV1 -> "efficientdet-lite1.tflite"
        MODEL_EFFICIENTDETV2 -> "efficientdet-lite2.tflite"
        else -> "mobilenetv1.tflite"
    }

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var bitmapBuffer: Bitmap
    private lateinit var objectDetectorHelper: ObjectDetectorHelper

    // TODO: data dummy for testing retrofit
    private val list = ArrayList<PostResponse>()

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // before do the initialization for camera, check if device has camera first
        if(checkCameraHardware(this)) {
            // init camera executor
            cameraExecutor = Executors.newSingleThreadExecutor()
            // init camera manager
            cameraM = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            // check camera permission
            checkCameraAccess()
        } else {
            textMessage("Your device has no camera", this)
        }


        // init tts bahasa
        tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
            if (it == TextToSpeech.SUCCESS) {
                tts.language = Locale("id", "ID")
                tts.setSpeechRate(1.0f)

                // welcome speech
                tts.speak(getWelcomeSpeech(), TextToSpeech.QUEUE_FLUSH, null)
            }
        })

        // init object detector helper
        objectDetectorHelper = ObjectDetectorHelper(
            context = applicationContext,
            objectDetectorListener = this)

        // TODO: on click button help
        binding.imgBtnHelp.setOnClickListener{
            if(isFlashOn) {
                if(modelInUse.value == 0){
                    textToSpeech(Constants.HELP_MODE_0_FLASH_ON)
                } else {
                    // model 1
                    textToSpeech(Constants.HELP_MODE_1_FLASH_ON)
                }
            } else {
                if(modelInUse.value == 0){
                    textToSpeech(Constants.HELP_MODE_0_FLASH_OFF)
                } else {
                    // model 1
                    textToSpeech(Constants.HELP_MODE_1_FLASH_OFF)
                }
            }
        }

        binding.imgBtnFlash.setOnClickListener{
            flashlightOnClick(it)
            Log.d(Constants.TAG, "helo flash")
        }

        binding.switchModel.setOnCheckedChangeListener{ _, isChecked ->
            modelSwitchOnClick(isChecked)
        }

        // listen to modelInUse on change
        modelInUse.observe(this, Observer{
            modelName = when (modelInUse.value) {
                MODEL_MOBILENETV1 -> "mobilenetv1.tflite"
                MODEL_EFFICIENTDETV0 -> "efficientdet-lite0.tflite"
                MODEL_EFFICIENTDETV1 -> "efficientdet-lite1.tflite"
                MODEL_EFFICIENTDETV2 -> "efficientdet-lite2.tflite"
                else -> "mobilenetv1.tflite"
            }
        })

        // TODO: change with real func with GET padul later
        getPostsDummy()

        // TODO: change with real func with POST padul later
        ceratePostsDummy()
    }

    override fun onStop() {
        super.onStop()

        // stop text to speech on application close
        if(tts != null){
            tts.shutdown();
        }
    }

    private fun modelSwitchOnClick(isChecked: Boolean) {
        if (isChecked) {
            // change to mode 1
            modelInUse.value = 1
            // text to speech
            textToSpeech(Constants.SWITCH_TO_MODE_1)

        } else {
            // change to mode 0
            modelInUse.value = 0
            // text to speech
            textToSpeech(Constants.SWITCH_TO_MODE_0)
        }
    }

    @SuppressLint("ResourceAsColor")
    @RequiresApi(Build.VERSION_CODES.M)
    private fun flashlightOnClick(v: View?) {
        val isFlashAvailable = applicationContext.packageManager
            .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

        if (!isFlashAvailable) {
            Log.d(Constants.TAG, "gapunya flash")
        } else {
            Log.d(Constants.TAG, "flash masuk")
            try {
                val cameraIdList = cameraM.cameraIdList

                Log.d(Constants.TAG, cameraIdList.toString())

                cameraId = cameraM.cameraIdList[0]

                Log.d(Constants.TAG, cameraId)

            } catch (e: CameraAccessException) {
                Log.d(Constants.TAG, "error ni ges si flash ges$e")
                e.printStackTrace()
            }

            if (!isFlashOn) { // ACTION: TURN ON FLASH
                camera.cameraControl.enableTorch(true)

                // flashlight turned on
                isFlashOn = true

                // change icon color
                binding.imgBtnFlash.setBackgroundResource(R.drawable.ic_flash_on)

                // text to speech
                textToSpeech(Constants.FLASH_ON)
            } else { // ACTION: TURN OFF FLASH
                camera.cameraControl.enableTorch(false)

                // flashlight turned off
                isFlashOn = false

                // change icon color
                binding.imgBtnFlash.setBackgroundResource(R.drawable.ic_flash_off)

                // text to speech
                textToSpeech(Constants.FLASH_OFF)
            }
        }
    }

    private fun textMessage(s: String, c: Context) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show()
    }

    private fun textToSpeech(s: String) {
        tts.speak(s, TextToSpeech.QUEUE_FLUSH, null)
    }

    private fun textToSpeechObjectDetected(s: String) {
        tts.speak(s, TextToSpeech.QUEUE_ADD, null)
    }

    /** Check if this device has a camera */
    private fun checkCameraHardware(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }

    private fun checkCameraAccess() {
        if(allPermissionGranted()){
            startCamera()
        }else{
            ActivityCompat.requestPermissions(
                this,
                Constants.REQUIRED_PERMISSIONS,
                Constants.REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS){
            if(allPermissionGranted()){
                startCamera()
            }else{
                textMessage("permission not granted by the user", this)
                textToSpeech(Constants.NO_CAMERA_ACCESS)
            }
        }
    }

    private fun allPermissionGranted() =
        Constants.REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                baseContext, it
            ) == PackageManager.PERMISSION_GRANTED
        }

    private fun startCamera(){
        // init camera provider
        val cameraProviderFuture = ProcessCameraProvider
            .getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            preview = Preview.Builder()
                .build()
                .also { mPreview ->
                    mPreview.setSurfaceProvider(
                        binding.viewFinder.surfaceProvider
                    )
                }

            // use back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // ImageAnalysis. Using RGBA 8888 to match how our models work
            imageAnalyzer =
                ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                    .setTargetRotation(binding.viewFinder.display.rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    // The analyzer can then be assigned to the instance
                    .also {
                        it.setAnalyzer(cameraExecutor) { image ->
                            if (!::bitmapBuffer.isInitialized) {
                                // The image rotation and RGB image buffer are initialized only once
                                // the analyzer has started running
                                bitmapBuffer = Bitmap.createBitmap(
                                    image.width,
                                    image.height,
                                    Bitmap.Config.ARGB_8888
                                )
                            }
                            detectObjects(image)
                        }
                    }

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )

            }catch (e: Exception){
                Log.d(Constants.TAG, "startCamera Fail:", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun detectObjects(image: ImageProxy) {
        // Copy out RGB bits to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val imageRotation = image.imageInfo.rotationDegrees
        // Pass Bitmap and rotation to the object detector helper for processing and detection
        objectDetectorHelper.detect(bitmapBuffer, imageRotation, modelName)
    }

    private fun getWelcomeSpeech(): String {
        if(allPermissionGranted()){
            return Constants.HELP_MODE_1_FLASH_OFF
        }

        // camera not granted
        return Constants.NO_CAMERA_ACCESS
    }

    override fun onError(error: String) {
        // show error on toast
        // TODO: consider to tell error via tts
        textMessage(error, this)
    }

    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {

        // Pass necessary information to OverlayView for drawing on the canvas
        binding.overlay.setResults(
            results ?: LinkedList<Detection>(),
            imageHeight,
            imageWidth
        )

        if (results != null) {
            for (result in results) {
                // objek yang terdeteksi masuk queue untuk di-output sebagai speech
                // output setelah obrolan lainnya selesai dilakukan
                textToSpeechObjectDetected(result.categories[0].label)
            }
        }

        // Force a redraw
        binding.overlay.invalidate()

    }

    // TODO: ganti pake API beneran (Api, PostResponse, RetrofitClient)
    private fun getPostsDummy() {
        // retrofit
        RetrofitClient.instance.getPosts().enqueue(object: Callback<ArrayList<PostResponse>>{
            override fun onResponse(
                call: Call<ArrayList<PostResponse>>,
                response: Response<ArrayList<PostResponse>>
            ) {
                val responseCode = response.code().toString()
                Log.d(Constants.TAG, "response code$responseCode")

                response.body()?.let { list.addAll(it)}
                Log.d(Constants.TAG, list.toString())
            }

            override fun onFailure(call: Call<ArrayList<PostResponse>>, t: Throwable) {
                Log.e("Failed", t.message.toString())
            }
        })
    }

    // TODO: ganti pake API beneran (Api, CreatePostResponse, RetrofitClient)
    private fun ceratePostsDummy() {
        RetrofitClient.instance.createPosts(
            10,
            "judul apa hayo",
            "apa coba isinya apa coba apa coba"
        ).enqueue(object: Callback<CreatePostResponse>{
            override fun onResponse(
                call: Call<CreatePostResponse>,
                response: Response<CreatePostResponse>
            ) {
                val responseCode = response.code().toString()
                Log.d(Constants.TAG, "response code$responseCode")

                val response = "response $response"
                Log.d(Constants.TAG, response)
            }

            override fun onFailure(call: Call<CreatePostResponse>, t: Throwable) {
                Log.e("Failed", t.message.toString())
            }

        })
    }

    companion object {
        const val MODEL_MOBILENETV1 = 0
        const val MODEL_EFFICIENTDETV0 = 1
        const val MODEL_EFFICIENTDETV1 = 2
        const val MODEL_EFFICIENTDETV2 = 3
    }
}