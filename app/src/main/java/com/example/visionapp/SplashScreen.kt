package com.example.visionapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.visionapp.databinding.ActivitySplashScreenBinding
import java.util.*

class SplashScreen : AppCompatActivity() {
    private lateinit var binding: ActivitySplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivLogo.alpha = 0f
        binding.ivLogo.animate().setDuration(1500).alpha(1f).withEndAction{
            val mainActivityIntent = Intent(this, MainActivity::class.java)
            startActivity(mainActivityIntent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }
}