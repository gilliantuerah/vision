//package com.example.visionapp
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.content.pm.PackageManager
//import android.graphics.Bitmap
//import android.hardware.camera2.CameraManager
//import android.os.Bundle
//import android.util.Log
//import androidx.fragment.app.Fragment
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.camera.core.Camera
//import androidx.camera.core.CameraSelector
//import androidx.camera.core.ImageAnalysis
//import androidx.camera.core.Preview
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import androidx.core.content.ContextCompat.getSystemService
//import com.example.visionapp.databinding.FragmentCameraBinding
//import java.lang.Exception
//import java.util.concurrent.ExecutorService
//import java.util.concurrent.Executors
//
//class CameraFragment : Fragment(R.layout.fragment_camera) {
//    private val TAG = "ObjectDetection"
//
//    private var binding: FragmentCameraBinding? = null
//
//    private val fragmentCameraBinding
//        get() = binding!!
//\
//    private lateinit var bitmapBuffer: Bitmap
//    private var preview: Preview? = null
//    private var imageAnalyzer: ImageAnalysis? = null
//    private var camera: Camera? = null
//    private var cameraProvider: ProcessCameraProvider? = null
//
//    /** Blocking camera operations are performed using this executor */
//    private lateinit var cameraExecutor: ExecutorService
//
//    override fun onResume() {
//        super.onResume()
//        // Make sure that all permissions are still present, since the
//        // user could have removed them while the app was in paused state.
//        if (!PermissionsFragment.hasPermissions(requireContext())) {
//            Navigation.findNavController(requireActivity(), R.id.fragment_container)
//                .navigate(CameraFragmentDirections.actionCameraToPermissions())
//        }
//    }
//
//    override fun onDestroyView() {
//        binding = null
//        super.onDestroyView()
//
//        // Shut down our background executor
//        cameraExecutor.shutdown()
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        binding = FragmentCameraBinding.inflate(inflater, container, false)
//
//        return fragmentCameraBinding.root
//    }
//
//    @SuppressLint("MissingPermission")
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        // Initialize our background executor
//        cameraExecutor = Executors.newSingleThreadExecutor()
//
//        // Wait for the views to be properly laid out
//        fragmentCameraBinding.viewFinder.post {
//            // Set up the camera and its use cases
//            setUpCamera()
//        }
//    }
//
//    // Initialize CameraX, and prepare to bind the camera use cases
//    private fun setUpCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
//        cameraProviderFuture.addListener(
//            {
//                // CameraProvider
//                cameraProvider = cameraProviderFuture.get()
//            },
//            ContextCompat.getMainExecutor(requireContext())
//        )
//    }
//}