package com.example.visionapp

import android.annotation.SuppressLint
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
import android.speech.tts.UtteranceProgressListener
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
import com.example.visionapp.api.*
import com.example.visionapp.api.datatype.ResultAnnotation
import com.example.visionapp.databinding.ActivityMainBinding
import com.example.visionapp.databinding.DenyCameraDialogBinding
import com.example.visionapp.databinding.TncDialogBinding
import com.example.visionapp.detection.ObjectDetectorHelper
import com.example.visionapp.env.CheckNetworkConnection
import com.example.visionapp.env.Constants
import com.example.visionapp.env.Utility
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet_dialog.*
import org.tensorflow.lite.examples.detection.tflite.Classifier
import java.io.InputStream
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity(), ObjectDetectorHelper.DetectorListener {
    private lateinit var binding: ActivityMainBinding

    private lateinit var tts : TextToSpeech
    private var isTTSObjectFinished = true
    var mapTTSid = HashMap<String, String>()
    var ttsId = "app"
    var ttsObjectId = "object"
    var ttsFinishedId = "finish"

    private lateinit var cameraM: CameraManager
    private lateinit var camera: Camera
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraId: String
    private var isFlashOn = false

    // make it public, accessible to other file (ObjectDetectorHelper file)
    private var modelInUse: Int = 0 // default model offline
    var modelName: String = Constants.MODEL_1 // default model offline
    private val labelsYoloV5 = mutableListOf<String>()

    // default offline, using mode 1
    private var isOnline: Boolean = false
    private lateinit var checkNetworkConnection: CheckNetworkConnection

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var bitmapBuffer: Bitmap
    private lateinit var objectDetectorHelper: ObjectDetectorHelper

    private lateinit var util: Utility
    private var serviceApi: Service = Service()
    private var alertDialogDeny: Dialog? = null
    private var alertDialogTnc: Dialog? = null
    private var bottomSheetDialog: BottomSheetDialog? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // check network connection
        callNetworkConnection()

        // init tts bahasa
        tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
            if (it == TextToSpeech.SUCCESS) {
                ttsInitialized()
            }
        })

        // yolo labels
        val labelsInput: InputStream = assets.open(Constants.YOLO_LABELS)
        labelsInput.bufferedReader().forEachLine {
            labelsYoloV5.add(it)
        }

        // init object detector helper
        objectDetectorHelper = ObjectDetectorHelper(
            context = applicationContext,
            objectDetectorListener = this,
            labels = labelsYoloV5,
            inputSize = 640
        )

        // init util
        util = Utility(
            context = applicationContext,
            tts,
            mapTTSid
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
    }

    override fun onStop() {
        super.onStop()

        // stop text to speech on application close
        if(tts != null){
            tts.shutdown();
        }
    }

    private fun callNetworkConnection() {
        checkNetworkConnection = CheckNetworkConnection(application)
        checkNetworkConnection.observe(this) { isConnected ->
            if (isConnected) {
                // network connected
                isOnline = true
                if (serviceApi.modelFromServer == null){
                    // get model from server if device is online
                    // and not get model from server yet
                    serviceApi.getLastModel()
                }

                // set model offline to modelFromServer
                if (modelInUse == 0) {
                    modelName = if (serviceApi.modelFromServer != null) serviceApi.modelFromServer.toString() else Constants.MODEL_1
                }
            } else {
                // network disconnected
                isOnline = false

                // device goes offline when using mode online (mode 2)
                // switch to mode offline (mode 1)
                if (modelInUse == 1) {
                    modelName = Constants.MODEL_1
                    switchModel.isChecked = false
                }
            }
        }
    }

    private fun onObjectDetectionDistracted(){
        // finish object detection
        isTTSObjectFinished = true
    }

    private fun ttsInitialized(){
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String?) {
                if(utteranceId == ttsFinishedId) {
                    isTTSObjectFinished = true
                }
            }

            override fun onError(utteranceId: String?) {}
            override fun onStart(utteranceId: String?) {}
        })

        tts.language = Locale("id", "ID")
        tts.setSpeechRate(1.0f)
    }

    private fun showPopupDenyDialog() {
        // pop up dialog on access camera deny
        // inflate the dialog
        val denyCameraDialog = DenyCameraDialogBinding.inflate(layoutInflater)
        // alert dialog builder
        val alertDialogBuilder = AlertDialog.Builder(this)
            .setView(denyCameraDialog.root)
        // show dialog
        alertDialogDeny = alertDialogBuilder.show()
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

    private fun showTncDialog() {
        // save value when tnc accepted
        val sharedPreference =  getSharedPreferences("sharedPrefs",Context.MODE_PRIVATE)
        var editor = sharedPreference.edit()

        // pop up dialog tnc
        // inflate the dialog
        val tncDialog = TncDialogBinding.inflate(layoutInflater)
        // alert dialog builder
        val alertDialogBuilder = AlertDialog.Builder(this)
            .setView(tncDialog.root)
        // show dialog
        alertDialogTnc = alertDialogBuilder.show()
        // on click button setup
        tncDialog.btnSetuju.setOnClickListener{
            // close dialog
            alertDialogTnc?.dismiss()
            // set tncAccepted
            editor.apply {
                putBoolean("tncAccepted", true)
            }.apply()
            // open bottomsheet
            showBottomSheetDialog()
        }
    }

    private fun showBottomSheetDialog() {
        // stop tts
        util.stopSpeaking(ttsId)

        // bottom sheet dialog
        bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val bottomSheetView = LayoutInflater.from(applicationContext).inflate(
            R.layout.bottom_sheet_dialog,
            findViewById<LinearLayout>(R.id.bottomSheet)
        )

        // on click button tutup
        bottomSheetView.findViewById<View>(R.id.btnClose).setOnClickListener{
            bottomSheetDialog?.dismiss()
            util.stopSpeaking(ttsId)
        }

        // on click button speaker
        bottomSheetView.findViewById<View>(R.id.imgBtnSpeaker).setOnClickListener{
            util.textToSpeech(Constants.HELP_TEXT, ttsId)
        }

        bottomSheetDialog?.setContentView(bottomSheetView)
        bottomSheetDialog?.show()
    }

    private fun isBottomSheetShowing(): Boolean{
        if(bottomSheetDialog?.isShowing == true){
            return true
        }
        return false
    }

    private fun modelSwitchOnClick(isChecked: Boolean) {
        onObjectDetectionDistracted()
        if (isChecked) {
            if(isOnline) {
                // if mobile has internet connectivity
                // change to mode 1
                modelInUse = 1

                switchModel.contentDescription = Constants.SWITCH_TO_MODE_1
                // text to speech
                util.textToSpeech(Constants.SWITCH_TO_MODE_1, ttsId)
            } else {
                // if mobile offline
                // -> model ga diganti
                // -> switch tetep diem
                switchModel.isChecked = false;
                util.textToSpeech(Constants.FAIL_SWITCH_TO_MODE_1, ttsId)
            }

        } else {
            // change to mode 0
            modelInUse = 0
            // text to speech
            switchModel.contentDescription = Constants.SWITCH_TO_MODE_0
            util.textToSpeech(Constants.SWITCH_TO_MODE_0, ttsId)
        }
    }

    private  fun helpOnClick(v: View?) {
        onObjectDetectionDistracted()
        showBottomSheetDialog()
    }

    @SuppressLint("ResourceAsColor")
    @RequiresApi(Build.VERSION_CODES.M)
    private fun flashlightOnClick(v: View?) {
        onObjectDetectionDistracted()
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
                util.textToSpeech(Constants.FLASH_ON, ttsId)
            } else { // ACTION: TURN OFF FLASH
                camera.cameraControl.enableTorch(false)

                // flashlight turned off
                isFlashOn = false

                // change icon color
                imgBtnFlash?.setBackgroundResource(R.drawable.ic_flash_off)

                // text to speech
                util.textToSpeech(Constants.FLASH_OFF, ttsId)
            }
        }
    }

    private fun onAccessGranted(){
        // on access camera granted
        // close deny dialog (if still open)
        // open tnc dialog
        // then open bottom sheet dialog (on click button "setuju")
        if(alertDialogDeny?.isShowing == true) {
            alertDialogDeny?.dismiss()
        }
        showTncDialog()
    }

    private fun checkCameraAccess() {
        if(allPermissionGranted()){
            startCamera()
            // load shared preferences data
            val sharedPreference =  getSharedPreferences("sharedPrefs",Context.MODE_PRIVATE)
            val tncAccepted = sharedPreference.getBoolean("tncAccepted", false)
            // if bottom sheet never show
            if(!tncAccepted){
                onAccessGranted()
            }
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
                onAccessGranted()
                startCamera()
            }else{
                util.textToSpeech(Constants.NO_CAMERA_ACCESS, ttsId)
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

        // Pass Bitmap and rotation to the object detector helper for processing and detection
        objectDetectorHelper.detect(bitmapBuffer, modelInUse)
    }

    private fun isSpeakResultAllowed(): Boolean{
        // true if output suara untuk hasil deteksi diperbolehkan
        // false jika tidak (ada popup/bottom sheet)

        // load shared preferences data
        val sharedPreference =  getSharedPreferences("sharedPrefs",Context.MODE_PRIVATE)
        val tncAccepted = sharedPreference.getBoolean("tncAccepted", false)

        // if pembacaan sebelumnya sudah selesai
        // if bottom sheet not showing
        // if popup tnc not showing
        return isTTSObjectFinished && !isBottomSheetShowing() && tncAccepted
    }

    override fun onError(error: String) {
        // show error on toast
        // TODO: consider to tell error via tts
        util.textMessage(error, this)
    }

    override fun onResultsModeOffline(
        results: ArrayList<Classifier.Recognition>?,
        image: Bitmap
    ) {
        // Pass necessary information to OverlayView for drawing on the canvas
        if (results != null) {
            overlay?.setResultsOffline(
                results,
                modelInUse,
                image.height,
                image.width
            )
        }

        Log.d("hasilink", results.toString())

        if (results != null && results.isNotEmpty() && isSpeakResultAllowed()) {
            // start speak
            isTTSObjectFinished = false
            // opening
            util.textToSpeechObjectDetected(Constants.OPEN_DETECTION, ttsId)

            // array for post to server
            var arrayResult: ArrayList<ResultAnnotation> = ArrayList()
            for (result in results) {

                val label = result.title
                val box = result.getLocation()

                val boxArray = arrayListOf(box.left, box.top, box.right, box.bottom)

                arrayResult.add(ResultAnnotation(
                    boxArray,
                    label.toString()
                ))
                // objek yang terdeteksi masuk queue untuk di-output sebagai speech
                // output setelah obrolan lainnya selesai dilakukan
                util.textToSpeechObjectDetected(label.toString(), ttsObjectId)
            }

            if (isOnline) {
                // if device is online, post prediction result to server
                // TODO: convert image of rectf to pascal voc format
                serviceApi.postPredictionResult(image, arrayResult)
            }

            // closing
            util.textToSpeechObjectDetected(Constants.CLOSE_DETECTION, ttsFinishedId)
        }

        // Force a redraw
        overlay?.invalidate()

    }

    override fun onResultsModeOnline(
        results: ArrayList<ResultAnnotation>?,
        image: Bitmap
    ){
        // Pass necessary information to OverlayView for drawing on the canvas
        if (results != null) {
            overlay?.setResultsOnline(
                results,
                modelInUse,
                image.height,
                image.width
            )
        }
        Log.d("Hasil prediksi server", results.toString())

        if (results != null && results.isNotEmpty() && isSpeakResultAllowed()) {
            // start speak
            isTTSObjectFinished = false
            // opening
            util.textToSpeechObjectDetected(Constants.OPEN_DETECTION, ttsId)

            for (result in results) {
                val label = result.label

                // objek yang terdeteksi masuk queue untuk di-output sebagai speech
                // output setelah obrolan lainnya selesai dilakukan
                util.textToSpeechObjectDetected(label, ttsObjectId)
            }

            // closing
            util.textToSpeechObjectDetected(Constants.CLOSE_DETECTION, ttsFinishedId)
        }

        // Force a redraw
        overlay?.invalidate()
    }
}