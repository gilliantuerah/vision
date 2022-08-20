package com.example.visionapp

import android.content.Context
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.widget.Toast

class Utility(
    private val context: Context,
    private var tts : TextToSpeech,
    private var mapTTSid: HashMap<String, String>,
) {
    fun textMessage(s: String, c: Context) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show()
    }

    /** Check if this device has a camera */
    fun checkCameraHardware(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }

    fun textToSpeech(s: String, id:String) {
        mapTTSid[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = id;
        tts.speak(s, TextToSpeech.QUEUE_FLUSH, mapTTSid)
    }

    fun textToSpeechObjectDetected(s: String, id:String) {
        mapTTSid[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = id;
        tts.speak(s, TextToSpeech.QUEUE_ADD, mapTTSid)
    }

    fun stopSpeaking(id:String) {
        textToSpeech("", id)
    }
}