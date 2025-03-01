package it.lmqv.livematchcam

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import it.lmqv.livematchcam.databinding.ActivityAccountBinding
import it.lmqv.livematchcam.databinding.ActivityRemoteScoreBinding
import it.lmqv.livematchcam.factories.SportsFactory
import it.lmqv.livematchcam.fragments.CameraFragment
import it.lmqv.livematchcam.fragments.IControlBarFragment
import it.lmqv.livematchcam.fragments.IRemoteControlFragment
import it.lmqv.livematchcam.handlers.offset.ManualZoomLevel

class RemoteScoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRemoteScoreBinding
    private lateinit var remoteControlFragment: IRemoteControlFragment
    private var sportsFactory = SportsFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRemoteScoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.arrow_left) // Opzionale: icona personalizzata

    }

    override fun onStart() {
        super.onStart()

        val sportFragmentFactory = sportsFactory.get()
        this.remoteControlFragment = sportFragmentFactory.getRemoteControl()

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, this.remoteControlFragment as Fragment)
            .commit()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}