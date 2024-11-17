package it.lmqv.livematchcam

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.pedro.encoder.input.sources.video.Camera1Source
import it.lmqv.livematchcam.utils.FilterMenu
import it.lmqv.livematchcam.utils.toast
import it.lmqv.livematchcam.utils.updateMenuColor

class LiveStreamActivity : AppCompatActivity(), OnTouchListener {

    private val cameraFragment = CameraFragment.getInstance()
    private val filterMenu: FilterMenu by lazy { FilterMenu(this) }
    private var currentVideoSource: MenuItem? = null
    private var currentAudioSource: MenuItem? = null
    private var currentOrientation: MenuItem? = null
    private var currentFilter: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.live_stream_activity)
        supportFragmentManager.beginTransaction().add(R.id.container, cameraFragment).commit()
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.livestream_menu, menu)
        val defaultVideoSource = menu.findItem(R.id.video_source_camera2)
        val defaultAudioSource = menu.findItem(R.id.audio_source_microphone)
        val defaultOrientation = menu.findItem(R.id.orientation_horizontal)
        val defaultFilter = menu.findItem(R.id.no_filter)
        currentVideoSource = defaultVideoSource.updateMenuColor(this, currentVideoSource)
        currentAudioSource = defaultAudioSource.updateMenuColor(this, currentAudioSource)
        currentOrientation = defaultOrientation.updateMenuColor(this, currentOrientation)
        currentFilter = defaultFilter.updateMenuColor(this, currentFilter)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        try {
            when (item.itemId) {
                R.id.video_source_camera1 -> {
                    currentVideoSource = item.updateMenuColor(this, currentVideoSource)
                    cameraFragment.genericStream.changeVideoSource(Camera1Source(applicationContext))
                }
                /*R.id.video_source_camera2 -> {
                    currentVideoSource = item.updateMenuColor(this, currentVideoSource)
                    cameraFragment.genericStream.changeVideoSource(Camera2Source(applicationContext))
                }
                R.id.video_source_camerax -> {
                    currentVideoSource = item.updateMenuColor(this, currentVideoSource)
                    cameraFragment.genericStream.changeVideoSource(CameraXSource(applicationContext))
                }
                R.id.video_source_camera_uvc -> {
                    currentVideoSource = item.updateMenuColor(this, currentVideoSource)
                    cameraFragment.genericStream.changeVideoSource(CameraUvcSource())
                }
                R.id.video_source_bitmap -> {
                    currentVideoSource = item.updateMenuColor(this, currentVideoSource)
                    val bitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
                    cameraFragment.genericStream.changeVideoSource(BitmapSource(bitmap))
                }
                R.id.audio_source_microphone -> {
                    currentAudioSource = item.updateMenuColor(this, currentAudioSource)
                    cameraFragment.genericStream.changeAudioSource(MicrophoneSource())
                }
                R.id.orientation_horizontal -> {
                    currentOrientation = item.updateMenuColor(this, currentOrientation)
                    cameraFragment.setOrientationMode(false)
                }
                R.id.orientation_vertical -> {
                    currentOrientation = item.updateMenuColor(this, currentOrientation)
                    cameraFragment.setOrientationMode(true)
                }
                else -> {
                    val result = filterMenu.onOptionsItemSelected(item, cameraFragment.genericStream.getGlInterface())
                    if (result) currentFilter = item.updateMenuColor(this, currentFilter)
                    return result
                }*/
            }
        } catch (e: IllegalArgumentException) {
            toast("Change source error: ${e.message}")
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        if (filterMenu.spriteGestureController.spriteTouched(view, motionEvent)) {
            filterMenu.spriteGestureController.moveSprite(view, motionEvent)
            filterMenu.spriteGestureController.scaleSprite(motionEvent)
            return true
        }
        return false
    }
}