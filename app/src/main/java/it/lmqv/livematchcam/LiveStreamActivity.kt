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
import it.lmqv.livematchcam.fragments.CameraFragment
import it.lmqv.livematchcam.handlers.offset.ManualZoomLevel

class LiveStreamActivity : AppCompatActivity() {

    private val cameraFragment = CameraFragment.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_stream)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        supportFragmentManager
            .beginTransaction()
            .add(R.id.container, cameraFragment)
            .commit()

        supportActionBar?.hide()

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

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                cameraFragment.updateZoom(ManualZoomLevel.None)
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                cameraFragment.updateZoom(ManualZoomLevel.None)
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_HEADSETHOOK -> {
                cameraFragment.updateZoom(ManualZoomLevel.None)
                return true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                cameraFragment.updateZoom(ManualZoomLevel.In)
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                cameraFragment.updateZoom(ManualZoomLevel.Out)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}