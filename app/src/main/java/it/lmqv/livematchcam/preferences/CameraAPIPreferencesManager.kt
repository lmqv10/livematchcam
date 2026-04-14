package it.lmqv.livematchcam.preferences

import android.content.Context
import android.content.SharedPreferences
import android.view.MotionEvent
import androidx.preference.PreferenceManager
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.encoder.input.sources.video.VideoSource
import com.pedro.encoder.input.video.CameraCallbacks
import com.pedro.encoder.input.video.CameraHelper
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.extensions.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.os.SystemClock;
import android.view.SurfaceView

class CameraAPIPreferencesManager(
    private val context : Context)
    : CameraCallbacks {

    private val KEY_HAS_CAMERA_STATUS_NOTIFICATIONS = context.getString(R.string.camera_status_notifications_key)
    //private val KEY_AUTO_FOCUS = context.getString(R.string.camera_auto_focus_key)
    private val KEY_VIDEO_STABILIZATION = context.getString(R.string.camera_video_stabilization_key)
    private val KEY_OPTICAL_VIDEO_STABILIZATION = context.getString(R.string.camera_optical_video_stabilization_key)
    private val KEY_LOCK_AUTO_FOCUS = context.getString(R.string.camera_lock_af_key)

    private var hasNotifications: Boolean = false

    private var videoSource: VideoSource? = null
    private var surfaceView: SurfaceView? = null
    private var sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            key?.let {
                handlePreferenceKey(it)
            }
        }

    init {
        this.sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        this.hasNotifications = sharedPreferences.getBoolean(KEY_HAS_CAMERA_STATUS_NOTIFICATIONS, false)
    }

    fun setVideoSource(videoSource: VideoSource) {
        this.videoSource = videoSource
        this.videoSource.let {
            when (it) {
                is Camera2Source -> {
                    it.setCameraCallback(this@CameraAPIPreferencesManager)
                }
            }
        }
    }

    fun setSurfaceView(surfaceView: SurfaceView) {
        this.surfaceView = surfaceView
    }

    private fun handlePreferenceKey(key: String) {
        videoSource?.let {
            var keyTitle = ""
            var status: Boolean? = null

            when (it) {
                is Camera2Source -> when (key) {
                    KEY_HAS_CAMERA_STATUS_NOTIFICATIONS -> {
                        var isEnabled = sharedPreferences.getBoolean(key, false)
                        this.hasNotifications = isEnabled
                    }

//                    KEY_AUTO_FOCUS -> {
//                        keyTitle = context.getString(R.string.camera_auto_focus_title)
//                        var isEnabled = sharedPreferences.getBoolean(key, false)
//                        if (isEnabled) {
//                            it.enableAutoFocus()
//                            status = it.isAutoFocusEnabled()
//                        } else {
//                            //it.disableAutoFocus()
//                            val motionEvent = MotionEvent.obtain(
//                                SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
//                                MotionEvent.ACTION_DOWN, it.width / 2f, it.height / 2f, 0)
//                            it.tapToFocus(motionEvent)
//                            motionEvent.recycle()
//                            status = false
//                        }
//                    }

                    KEY_VIDEO_STABILIZATION -> {
                        keyTitle = context.getString(R.string.camera_video_stabilization_title)
                        var isEnabled = sharedPreferences.getBoolean(key, false)
                        if (isEnabled) {
                            it.enableVideoStabilization()
                        } else {
                            it.disableVideoStabilization()
                        }
                        status = it.isVideoStabilizationEnabled()
                    }

                    KEY_OPTICAL_VIDEO_STABILIZATION -> {
                        keyTitle = context.getString(R.string.camera_optical_video_stabilization_title)
                        var isEnabled = sharedPreferences.getBoolean(key, false)
                        if (isEnabled) {
                            it.enableOpticalVideoStabilization()
                        } else {
                            it.disableOpticalVideoStabilization()
                        }
                        status = it.isOpticalVideoStabilizationEnabled()
                    }

                    KEY_LOCK_AUTO_FOCUS -> {
                        keyTitle = context.getString(R.string.camera_lock_af_title)
                        var isEnabled = sharedPreferences.getBoolean(key, false)

                        if (isEnabled && this.surfaceView != null) {
                            val motionEvent = MotionEvent.obtain(
                                SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                                MotionEvent.ACTION_DOWN, it.width / 2f, it.height / 2f, 0)
                            it.tapToFocus(this.surfaceView!!, motionEvent)
                            motionEvent.recycle()
                        }
                        status = isEnabled
                    }
                }
            }

            status?.let {
                var statusMessage = if (it) {
                    context.getString(R.string.enabled)
                } else {
                    context.getString(R.string.disabled)
                }
                showNotification("$keyTitle: $statusMessage")
            }
        }
    }

    fun cancel() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onCameraChanged(facing: CameraHelper.Facing) {
        showNotification("camera changed")
        //handlePreferenceKey(KEY_AUTO_FOCUS)
        handlePreferenceKey(KEY_VIDEO_STABILIZATION)
        handlePreferenceKey(KEY_OPTICAL_VIDEO_STABILIZATION)
    }

    override fun onCameraError(error: String) {
        showNotification("camera error: $error")
    }

    override fun onCameraOpened() {
        showNotification("camera opened")
    }

    override fun onCameraDisconnected() {
        showNotification("camera disconnected")
    }

    fun onConnectionStarted() {
        handlePreferenceKey(KEY_LOCK_AUTO_FOCUS)
    }

    fun onDisconnect() {
        //handlePreferenceKey(KEY_AUTO_FOCUS)
        this.videoSource.let {
            when (it) {
                is Camera2Source -> {
                    it.enableAutoFocus()
                }
            }
        }
    }

    private fun showNotification(message: String) {
        if (this.hasNotifications) {
            CoroutineScope(Dispatchers.Main).launch {
                context.toast(message)
            }
        }
    }
}