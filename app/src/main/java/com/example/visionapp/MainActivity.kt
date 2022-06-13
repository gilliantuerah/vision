package com.example.visionapp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.visionapp.databinding.ActivityMainBinding
import java.lang.Exception
import java.util.*

// TODO: delete later
private const val TAG = "MyActivity"

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var cameraM: CameraManager

    private var isFlashOn = false

    lateinit var tts : TextToSpeech

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraM = getSystemService(Context.CAMERA_SERVICE) as CameraManager


        if(allPermissionGranted()){
            startCamera()
        }else {
            ActivityCompat.requestPermissions(
                this,
                Constants.REQUIRED_PERMISSIONS,
                Constants.REQUEST_CODE_PERMISSIONS
            )
        }


        // TODO: on click button mode1
        binding.btnMode1.setOnClickListener{
            tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener { status ->
                tts.speak("halo nama aku gill", TextToSpeech.QUEUE_FLUSH, null)
                if (status!=TextToSpeech.ERROR) {
//                    tts.setLanguage(Locale("id", "ID"))
                    tts.language = Locale.US
                    tts.setSpeechRate(1.0f)
                    Log.i(TAG, "masuk pak ekok")
                    tts.speak("halo nama aku gill", TextToSpeech.QUEUE_FLUSH, null)
                } else {
                    Log.i(TAG, "helo aku tidak masuk")
                }
            })
            Log.i(TAG, "helo aku mode 1")
//            tts.speak("halo nama aku gill", TextToSpeech.QUEUE_FLUSH, null)
        }
        // TODO: on click button mode2
        binding.btnMode2.setOnClickListener{
            // go to tts page testing
            Log.i(TAG, "helo aku mode 2")
            val ttsIntent = Intent(this, com.example.visionapp.TextToSpeech::class.java)
            startActivity(ttsIntent)
        }
        // TODO: on click button flashlight
        binding.imgBtnFlash.setOnClickListener{
//            flashlightOnClick(it)
            Log.i(TAG, "helo flash")
            val tts2Intent = Intent(this, ttsPageTesting::class.java)
            startActivity(tts2Intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun flashlightOnClick(v: View?) {
        if (!isFlashOn) {
            val cameraListId = cameraM.cameraIdList[0]
            cameraM.setTorchMode(cameraListId,true)
            // flashlight turned on
            isFlashOn = true
            // pop up message
            textMessage("Flash Light is On", this)
        } else {
            val cameraListId = cameraM.cameraIdList[0]
            cameraM.setTorchMode(cameraListId,false)
            // flashlight turned off
            isFlashOn = false
            // pop up message
            textMessage("Flash Light is Off", this)
        }
    }

    private fun textMessage(s: String, c: Context) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show()
    }

    private fun startCamera(){
        val cameraProviderFuture = ProcessCameraProvider
            .getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also { mPreview ->
                    mPreview.setSurfaceProvider(
                        binding.viewFinder.surfaceProvider
                    )
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview
                )

            }catch (e: Exception){
                Log.d(Constants.TAG, "startCamera Fail:", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS){
            if(allPermissionGranted()){
                startCamera()
            }else{
                textMessage("permission not granted by the user", this)

                finish()
            }
        }
    }

    private fun allPermissionGranted() =
        Constants.REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                baseContext, it
            ) == PackageManager.PERMISSION_GRANTED
        }
}