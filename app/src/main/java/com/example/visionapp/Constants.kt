package com.example.visionapp

object Constants {

    const val TAG = "cameraX"
    const val REQUEST_CODE_PERMISSIONS = 123
    val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)

    //script text-to-speech
    const val HELP_MODE_1_FLASH_OFF = "Selamat datang di aplikasi V-sion. Arahkan handphone ke arah barang yang ingin Anda deteksi. Barang akan dideteksi secara terurut dari bagian kiri atas ke kanan bawah. Anda menggunakan Mode 1. Tekan tombol di kiri bawah untuk bantuan. Tekan tombol di kanan bawah untuk mengubah mode. Tekan tombol di tengah untuk menyalakan lampu flash."
    const val HELP_MODE_1_FLASH_ON = "Selamat datang di aplikasi V-sion. Arahkan handphone ke arah barang yang ingin Anda deteksi. Tahan posisi handphone selama 5 detik dan barang akan dideteksi secara terurut dari bagian kiri atas ke kanan bawah. Anda menggunakan Mode 1. Tekan tombol di kiri bawah untuk bantuan. Tekan tombol di kanan bawah untuk mengubah mode. Tekan tombol di tengah untuk mematikan lampu flash."
    const val HELP_MODE_2_FLASH_OFF = "Selamat datang di aplikasi V-sion. Arahkan handphone ke arah barang yang ingin Anda deteksi. Tahan posisi handphone selama 5 detik dan barang akan dideteksi secara terurut dari bagian kiri atas ke kanan bawah. Anda menggunakan Mode 2. Tekan tombol di kiri bawah untuk bantuan. Tekan tombol di kanan bawah untuk mengubah mode. Tekan tombol di tengah untuk menyalakan lampu flash."
    const val HELP_MODE_2_FLASH_ON = "Selamat datang di aplikasi V-sion. Arahkan handphone ke arah barang yang ingin Anda deteksi. Tahan posisi handphone selama 5 detik dan barang akan dideteksi secara terurut dari bagian kiri atas ke kanan bawah. Anda menggunakan Mode 2. Tekan tombol di kiri bawah untuk bantuan. Tekan tombol di kanan bawah untuk mengubah mode. Tekan tombol di tengah untuk mematikan lampu flash."
    const val FLASH_ON = "Anda menyalakan lampu flash. Tekan tombol di tengah untuk mematikan lampu flash."
    const val FLASH_OFF = "Anda mematikan lampu flash. Tekan tombol di tengah untuk menyalakan lampu flash."
    const val SWITCH_TO_MODE_1 = "Anda mengaktifkan mode satu"
    const val SWITCH_TO_MODE_2 = "Anda mengaktifkan mode dua"
    const val NO_CAMERA_ACCESS = "Selamat datang di aplikasi V-sion. Silakan beri akses kamera untuk menggunakan aplikasi ini."

}