package com.example.visionapp.env

object Constants {

    const val TAG = "DEBUG"
    const val REQUEST_CODE_PERMISSIONS = 123
    val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)

    // model file name
    const val MODEL_1 = "mobilenetv1.tflite"
    const val MODEL_2 = "mobilenetv1.tflite"

    // script text-to-speech
    const val NO_CAMERA_ACCESS = "Selamat datang di aplikasi V-sion. Silakan beri akses kamera untuk menggunakan aplikasi ini."

    // help
    const val HELP_TEXT = "Arahkan handphone ke arah barang yang ingin Anda deteksi. Barang akan dideteksi secara terurut dari bagian kiri atas ke kanan bawah. Tekan tombol di kiri bawah untuk bantuan. Tekan tombol di tengah bawah untuk mematikan lampu flash. Tekan tombol di kanan bawah untuk mengubah mode. Mode online hanya dapat dilakukan jika perangkat tersambung ke internet."

    // flash
    const val FLASH_ON = "Anda menyalakan lampu flash. Tekan tombol di tengah untuk mematikan lampu flash."
    const val FLASH_OFF = "Anda mematikan lampu flash. Tekan tombol di tengah untuk menyalakan lampu flash."

    // mode
    const val SWITCH_TO_MODE_0 = "Anda mengaktifkan mode offline"
    const val FAIL_SWITCH_TO_MODE_1 = "Anda tidak bisa mengaktifkan mode online. Nyalakan koneksi internet untuk menggunakan mode ini."
    const val SWITCH_TO_MODE_1 = "Anda mengaktifkan mode online. Mode ini menggunakan koneksi internet"

    // object detection
    const val OPEN_DETECTION = "Objek terdeteksi. Ada barang"
    const val CLOSE_DETECTION = "selesai"
}