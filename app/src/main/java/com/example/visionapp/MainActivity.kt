package com.example.visionapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.example.visionapp.databinding.ActivityMainBinding
import java.lang.Exception

// TODO: delete later
private const val TAG = "MyActivity"

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var cameraM: CameraManager

    private lateinit var cameraId: String

    private var isFlashOn = false

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraM = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        checkCameraAccess()

        // TODO: on click button help
        binding.imgBtnHelp.setOnClickListener{
            Log.i(TAG, "helo aku help")
            // TODO: tts help script
        }
        // TODO: on click button flashlight
        binding.imgBtnFlash.setOnClickListener{
            flashlightOnClick(it)
            Log.i(TAG, "helo flash")
        }
        // TODO: handle switch button
    }

    @SuppressLint("ResourceAsColor")
    @RequiresApi(Build.VERSION_CODES.M)
    private fun flashlightOnClick(v: View?) {
        val isFlashAvailable = applicationContext.packageManager
            .hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)

        if (!isFlashAvailable) {
            Log.d(TAG, "gapunya flash")
        } else {
            Log.d(TAG, "flash masuk")
            try {
                val cameraIdList = cameraM.getCameraIdList()

                Log.d(TAG, cameraIdList.toString())

                cameraId = cameraM.cameraIdList[0]

                Log.d(TAG, cameraId)

            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }

            if (!isFlashOn) {
                cameraM.setTorchMode(cameraId, true)

                // flashlight turned on
                isFlashOn = true

                // change icon color
                // TODO: align color with design
                ImageViewCompat.setImageTintList(
                    binding.imgBtnFlash,
                    ColorStateList.valueOf(R.color.blue)
                )

                // pop up message
                // textMessage("Flash Light is On", this)
            } else {
                cameraM.setTorchMode(cameraId, false)

                // flashlight turned off
                isFlashOn = false

                // change icon color
                // TODO: align color with design
                ImageViewCompat.setImageTintList(
                    binding.imgBtnFlash,
                    ColorStateList.valueOf(R.color.black)
                )

                // pop up message
                // textMessage("Flash Light is Off", this)
            }
        }
    }

    private fun textMessage(s: String, c: Context) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show()
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.REQUEST_CODE_PERMISSIONS){
            if(allPermissionGranted()){
                startCamera()
            }else{
                textMessage("permission not granted by the user", this)

                // TODO: tts user should grant camera access to use app
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