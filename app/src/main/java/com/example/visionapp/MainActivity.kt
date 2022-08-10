package com.example.visionapp

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
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
import com.example.visionapp.databinding.BottomSheetDialogBinding
import com.example.visionapp.databinding.DenyCameraDialogBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet_dialog.*
import org.tensorflow.lite.task.vision.detector.Detection
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


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

    private lateinit var util: Utility
    private var alertDialog: Dialog? = null

    // TODO: data dummy for testing retrofit
    private val list = ArrayList<PostResponse>()

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // init tts bahasa
        tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
            if (it == TextToSpeech.SUCCESS) {
                tts.language = Locale("id", "ID")
                tts.setSpeechRate(1.0f)
            }
        })

        // init object detector helper
        objectDetectorHelper = ObjectDetectorHelper(
            context = applicationContext,
            objectDetectorListener = this)

        // init util
        util = Utility(
            context = applicationContext,
            tts = tts
        )

        // before do the initialization for camera, check if device has camera first
        if(util.checkCameraHardware()) {
            // init camera executor
            cameraExecutor = Executors.newSingleThreadExecutor()
            // init camera manager
            cameraM = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            // check camera permission
            checkCameraAccess()
        } else {
            // TODO: consider to tell error via tts
            util.textMessage("Your device has no camera", this)
        }

        imgBtnHelp?.setOnClickListener{
            helpOnClick(it)
        }

        imgBtnFlash?.setOnClickListener{
            flashlightOnClick(it)
        }

        switchModel?.setOnCheckedChangeListener{ _, isChecked ->
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
        createPostsDummy()
    }

    override fun onStop() {
        super.onStop()

        // stop text to speech on application close
        if(tts != null){
            tts.shutdown();
        }
    }

    private fun showPopupDenyDialog() {
        // pop up dialog on access camera deny
        // inflate the dialog
        val denyCameraDialog = DenyCameraDialogBinding.inflate(layoutInflater)
        // alert dialog builder
        val alertDialogBuilder = AlertDialog.Builder(this)
            .setView(denyCameraDialog.root)
        // show dialog
        alertDialog = alertDialogBuilder.show()
        // on click button setup
        denyCameraDialog.btnAccess.setOnClickListener{
            // go to setting
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val uri: Uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
            finish()
        }
        // on click button exit
        denyCameraDialog.btnExit.setOnClickListener{
            // keluar aplikasi
            finish()
        }
    }

    private fun modelSwitchOnClick(isChecked: Boolean) {
        if (isChecked) {
            // change to mode 1
            modelInUse.value = 1
            // text to speech
            util.textToSpeech(Constants.SWITCH_TO_MODE_1)

        } else {
            // change to mode 0
            modelInUse.value = 0
            // text to speech
            util.textToSpeech(Constants.SWITCH_TO_MODE_0)
        }
    }

    private  fun helpOnClick(v: View?) {
        // bottom sheet dialog
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val bottomSheetView = LayoutInflater.from(applicationContext).inflate(
            R.layout.bottom_sheet_dialog,
            findViewById<LinearLayout>(R.id.bottomSheet)
        )

        // on click button tutup
        bottomSheetView.findViewById<View>(R.id.btnClose).setOnClickListener{
            bottomSheetDialog.dismiss()
        }

        // on click button speaker
        bottomSheetView.findViewById<View>(R.id.imgBtnSpeaker).setOnClickListener{
            util.textToSpeech(Constants.HELP_TEXT)
        }

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }

    @SuppressLint("ResourceAsColor")
    @RequiresApi(Build.VERSION_CODES.M)
    private fun flashlightOnClick(v: View?) {
        val isFlashAvailable = applicationContext.packageManager
            .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

        if (!isFlashAvailable) {
            // TODO: ganti pake toast yg bagus y
            Log.d(Constants.TAG, "gapunya flash")
        } else {
            // TODO: KEKNYA INI DIAPUS GASIIII
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
            // TODO: TUTUP TODO

            if (!isFlashOn) { // ACTION: TURN ON FLASH
                camera.cameraControl.enableTorch(true)

                // flashlight turned on
                isFlashOn = true

                // change icon color
                imgBtnFlash?.setBackgroundResource(R.drawable.ic_flash_on)

                // text to speech
                util.textToSpeech(Constants.FLASH_ON)
            } else { // ACTION: TURN OFF FLASH
                camera.cameraControl.enableTorch(false)

                // flashlight turned off
                isFlashOn = false

                // change icon color
                imgBtnFlash?.setBackgroundResource(R.drawable.ic_flash_off)

                // text to speech
                util.textToSpeech(Constants.FLASH_OFF)
            }
        }
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
                if(alertDialog?.isShowing == true) {
                    alertDialog?.dismiss()
                }
                startCamera()
            }else{
                util.textToSpeech(Constants.NO_CAMERA_ACCESS)
                showPopupDenyDialog()
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
                        viewFinder?.surfaceProvider
                    )
                }

            // use back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // ImageAnalysis. Using RGBA 8888 to match how our models work
            imageAnalyzer =
                ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                    .setTargetRotation(viewFinder?.display.rotation)
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
                            //TODO: set delay on call detect function
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

    override fun onError(error: String) {
        // show error on toast
        // TODO: consider to tell error via tts
        util.textMessage(error, this)
    }

    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {

        // Pass necessary information to OverlayView for drawing on the canvas
        overlay?.setResults(
            results ?: LinkedList<Detection>(),
            imageHeight,
            imageWidth
        )

        if (results != null) {
            for (result in results) {
                // objek yang terdeteksi masuk queue untuk di-output sebagai speech
                // output setelah obrolan lainnya selesai dilakukan
                util.textToSpeechObjectDetected(result.categories[0].label)
            }
        }

        // Force a redraw
        overlay?.invalidate()

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
    private fun createPostsDummy() {
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