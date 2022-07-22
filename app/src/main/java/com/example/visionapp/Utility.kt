package com.example.visionapp

import android.content.Context
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.widget.Toast

class Utility(
    private val context: Context,
    private var tts : TextToSpeech
) {
    fun textMessage(s: String, c: Context) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show()
    }

    /** Check if this device has a camera */
    fun checkCameraHardware(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }

    fun textToSpeech(s: String) {
        tts.speak(s, TextToSpeech.QUEUE_FLUSH, null)
    }

    fun textToSpeechObjectDetected(s: String) {
        tts.speak(s, TextToSpeech.QUEUE_ADD, null)
    }
}