package it.lmqv.livematchcam.services.stream.filters

import android.graphics.Color
import android.opengl.GLES20
import android.os.Build
import androidx.annotation.RequiresApi
import com.pedro.encoder.input.gl.render.filters.`object`.SurfaceFilterRender
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.services.stream.IVideoStreamData
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * A specialized SurfaceFilterRender for displaying replay video (or any other video content)
 * as an overlay on top of the live stream.
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class ReplayVideoFilterRender(
    surfaceReadyCallback: SurfaceReadyCallback? = null
) : SurfaceFilterRender(surfaceReadyCallback) {

    init {
        // Start invisible
        alpha = 0f
    }

    /**
     * Clear the surface by drawing a black frame.
     */
    fun clearSurface() {
        //surface?.let { s ->
            try {
                GLES20.glClearColor(0f, 0f, 0f, 1f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                Loge("ReplayVideoFilterRender :: clear surface DONE")
//                val canvas = s.lockCanvas(null)
//                canvas.drawColor(Color.BLACK)
//                s.unlockCanvasAndPost(canvas)
            } catch (e: Exception) {
                e.printStackTrace()
                Loge("ReplayVideoFilterRender :: Failed to clear surface: ${e.message}")
            }
        //}

    }

    /**
     * Set visibility of the replay overlay.
     */
    fun hide() {
        alpha = 0f
    }

    fun getCurrentAlpha(): Float {
        return alpha
    }

    fun updateAlpha(value: Float) {
        alpha = value
    }

//    private fun animateReplayAlpha(target: Float, durationMs: Long) {
//        fadeJob?.cancel()
//        fadeJob = streamServiceScope.launch {
//            val startAlpha = replayVideoFilterRender?.alpha ?: 0f
//            val steps = 20
//            val stepDuration = durationMs / steps
//            val alphaDelta = (target - startAlpha) / steps
//
//            for (i in 1..steps) {
//                if (!isActive) break
//                val newAlpha = startAlpha + (alphaDelta * i)
//                replayVideoFilterRender?.alpha = newAlpha
//                delay(stepDuration)
//            }
//            replayVideoFilterRender?.alpha = target
//        }
//    }

    /**
     * Scale to full screen by default.
     */
    fun setFullScreen() {
        setScale(100f, 100f)
        setPosition(0f, 0f)
    }
}
