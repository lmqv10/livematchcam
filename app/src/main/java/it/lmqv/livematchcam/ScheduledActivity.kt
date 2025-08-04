package it.lmqv.livematchcam

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import it.lmqv.livematchcam.databinding.ActivityScheduledBinding
import it.lmqv.livematchcam.fragments.IServersFragment
import it.lmqv.livematchcam.fragments.YoutubeFragment

class ScheduledActivity : AppCompatActivity() {

    private val youtubeFragment : IServersFragment = YoutubeFragment.newInstance()

    private lateinit var binding: ActivityScheduledBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduledBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            )
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayShowTitleEnabled(false) // Hide default title if needed
        }

        binding.activityLive.setOnClickListener {
            startActivity(Intent(this, StreamActivity::class.java))
        }

        binding.activityUsb.setOnClickListener {
            startActivity(Intent(this, UVCStreamActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()

        /*supportFragmentManager.beginTransaction()
            .replace(R.id.sportsContainer, MatchInfoFragment.newInstance()).commit()

        supportFragmentManager.beginTransaction()
            .replace(R.id.serversContainer, youtubeFragment as Fragment).commit()*/

    }
}
