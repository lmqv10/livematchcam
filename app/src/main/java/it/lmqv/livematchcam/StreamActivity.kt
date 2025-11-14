package it.lmqv.livematchcam

import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.fragments.CameraFragment

class StreamActivity : AppCompatActivity() {

    private val cameraFragment = CameraFragment.getInstance()

//    private val displayListener = object : DisplayManager.DisplayListener {
//        override fun onDisplayAdded(displayId: Int) {}
//        override fun onDisplayRemoved(displayId: Int) {}
//
//        override fun onDisplayChanged(displayId: Int) {
//            //val rotation = this@StreamActivity.display.rotation
//            val rotation = windowManager.defaultDisplay.rotation
//            cameraFragment.setRotation(rotation)
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //toast("StreamActivity::onCreate")
        setContentView(R.layout.activity_live_stream)
        //requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        supportActionBar?.hide()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
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

        //val dm = getSystemService(DisplayManager::class.java)
        //dm.registerDisplayListener(displayListener, null)
    }

    override fun onStart() {
        super.onStart()
        //toast("StreamActivity::onStart")

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, cameraFragment)
            .commit()
    }

    override fun onPause() {
        super.onPause()
        //toast("StreamActivity::OnPause")
    }

    override fun onResume() {
        super.onResume()
        //toast("StreamActivity::onResume")
    }

    override fun onDestroy() {
        super.onDestroy()
        //toast("StreamActivity::onDestroy")
        //val dm = getSystemService(DisplayManager::class.java)
        //dm.unregisterDisplayListener(displayListener)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        /*when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                cameraFragment.updateZoom(ManualZoomLevel.None)
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                cameraFragment.updateZoom(ManualZoomLevel.None)
                return true
            }
        }*/
        return super.onKeyUp(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        /*when (keyCode) {
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
        }*/
        return super.onKeyDown(keyCode, event)
    }

    /*override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ -> finish() }
            .setNegativeButton("No", null)
            .show()
    }*/
}