package com.example.visionapp

object Constants {

    const val TAG = "DEBUG"
    const val REQUEST_CODE_PERMISSIONS = 123
    val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)

    // model file name
    const val MODEL_1 = "mobilenetv1.tflite"
    const val MODEL_2 = "mobilenetv1.tflite"

    // script text-to-speech
    const val HELP_TEXT = "Arahkan handphone ke arah barang yang ingin Anda deteksi. Pembacaan hasil deteksi akan dilakukan setiap 5 detik. Barang akan dideteksi secara terurut dari bagian kiri atas ke kanan bawah. Tekan tombol di kiri bawah untuk bantuan. Tekan tombol di kanan bawah untuk mengubah mode. Tekan tombol di tengah untuk mematikan lampu flash."
    const val FLASH_ON = "Anda menyalakan lampu flash. Tekan tombol di tengah untuk mematikan lampu flash."
    const val FLASH_OFF = "Anda mematikan lampu flash. Tekan tombol di tengah untuk menyalakan lampu flash."
    const val SWITCH_TO_MODE_0 = "Anda mengaktifkan mode satu"
    const val SWITCH_TO_MODE_1 = "Anda mengaktifkan mode dua"
    const val NO_CAMERA_ACCESS = "Selamat datang di aplikasi V-sion. Silakan beri akses kamera untuk menggunakan aplikasi ini."

}