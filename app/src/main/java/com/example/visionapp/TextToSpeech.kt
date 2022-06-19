package com.example.visionapp

import androidx.appcompat.app.AppCompatActivity
import android.speech.tts.TextToSpeech
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import java.util.*

private const val TAG = "MyActivity"

class TextToSpeech : AppCompatActivity() {

    private lateinit var tts : TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_to_speech)

        var btnSpeak = findViewById<Button>(R.id.btnSpeak)
        var etText = findViewById<EditText>(R.id.etText)



        btnSpeak.setOnClickListener {
            tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
                if (it==TextToSpeech.SUCCESS) {
                    Log.d("tts","masuk pak ekok")
                    tts.language = Locale.US
                    tts.setSpeechRate(1.0f)
                    tts.speak(etText.text.toString(), TextToSpeech.QUEUE_ADD, null)
                } else {
                    Log.d("tts", "hahahaa")
                    Log.d("tts", TextToSpeech.ERROR.toString())
                }
            })
        }
    }

}