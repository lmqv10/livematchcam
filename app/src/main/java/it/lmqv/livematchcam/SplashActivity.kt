package it.lmqv.livematchcam

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import it.lmqv.livematchcam.repositories.MatchRepository

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MatchActivity::class.java))
            finish()
        }, 500)
    }
}