package com.example.visionapp

import android.Manifest
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
import com.example.visionapp.databinding.ActivityMainBinding
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// TODO: delete later
private const val TAG = "MyActivity"

class MainActivity : AppCompatActivity(), ObjectDetectorHelper.DetectorListener {
    private lateinit var binding: ActivityMainBinding

    private lateinit var tts : TextToSpeech
    private lateinit var ttsDetection: TextToSpeech

    private lateinit var cameraM: CameraManager
    private lateinit var camera: Camera
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraId: String
    private var isFlashOn = false
    private var modelInUse = 1

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var bitmapBuffer: Bitmap
    private lateinit var objectDetectorHelper: ObjectDetectorHelper

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
                if(modelInUse == 1){
                    textToSpeech(Constants.HELP_MODE_1_FLASH_ON)
                } else {
                    // model 2
                    textToSpeech(Constants.HELP_MODE_2_FLASH_ON)
                }
            } else {
                if(modelInUse == 1){
                    textToSpeech(Constants.HELP_MODE_1_FLASH_OFF)
                } else {
                    // model 2
                    textToSpeech(Constants.HELP_MODE_2_FLASH_OFF)
                }
            }
        }
        // TODO: on click button flashlight
        binding.imgBtnFlash.setOnClickListener{
            flashlightOnClick(it)
            Log.d(TAG, "helo flash")
        }
        // TODO: handle switch button
        binding.switchModel.setOnCheckedChangeListener{ _, isChecked ->
            if (isChecked) {
                // mode 2
                modelInUse = 2
                // text to speech
                textToSpeech(Constants.SWITCH_TO_MODE_2)

            } else {
                // mode 1
                modelInUse = 1
                // text to speech
                textToSpeech(Constants.SWITCH_TO_MODE_1)

            }
        }
    }

    @SuppressLint("ResourceAsColor")
    @RequiresApi(Build.VERSION_CODES.M)
    private fun flashlightOnClick(v: View?) {
        val isFlashAvailable = applicationContext.packageManager
            .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

        if (!isFlashAvailable) {
            Log.d(TAG, "gapunya flash")
        } else {
            Log.d(TAG, "flash masuk")
            try {
                val cameraIdList = cameraM.cameraIdList

                Log.d(TAG, cameraIdList.toString())

                cameraId = cameraM.cameraIdList[0]

                Log.d(TAG, cameraId)

            } catch (e: CameraAccessException) {
                Log.d(TAG, "error ni ges si flash ges$e")
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
                    .setTargetRotation(binding.viewFinder.display.rotation)
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
        objectDetectorHelper.detect(bitmapBuffer, imageRotation)
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

}