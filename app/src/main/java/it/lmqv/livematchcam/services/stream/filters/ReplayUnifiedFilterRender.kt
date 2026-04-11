package it.lmqv.livematchcam.services.stream.filters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import com.pedro.encoder.input.gl.render.filters.`object`.SurfaceFilterRender
import com.pedro.encoder.utils.gl.GlUtil
import it.lmqv.livematchcam.services.stream.IVideoStreamData
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Unified filter that renders both the replay video surface AND the "REPLAY" badge.
 *
 * KEY DESIGN: The badge is drawn by overriding [draw] instead of [drawFilter].
 *
 * BaseFilterRender.draw() works like this:
 *   1. Bind FBO
 *   2. drawFilter()          ← only sets up GL state (program, textures, uniforms)
 *   3. glDrawArrays(...)     ← ACTUAL video draw happens here
 *   4. disableResources()
 *   5. Unbind FBO
 *
 * If badge drawing happens inside drawFilter() (step 2), it corrupts the GL state
 * (program, vertex attribs, active texture) before the video draw in step 3.
 *
 * By overriding draw(), we let the video render completely first via super.draw(),
 * then re-bind the FBO to composite the badge on top.
 *
 * PERFORMANCE:
 *  - Badge bitmap is recycled immediately after GPU texture upload (~192KB heap saved)
 *  - Vertex data is shared via a static companion buffer (zero per-instance allocation)
 *  - MVP matrix is cached and only recomputed on resolution change
 *  - Badge GL resources are lazily initialized on first visible frame
 *  - Zero GL calls when badge is hidden (alpha ≤ 0)
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class ReplayUnifiedFilterRender(
    surfaceReadyCallback: SurfaceReadyCallback? = null
) : SurfaceFilterRender(surfaceReadyCallback), IOverlayObjectFilterRender {

    companion object {
        private const val BADGE_W = 256  // power-of-two for GPU alignment
        private const val BADGE_H = 128  // power-of-two for GPU alignment
        private const val STRIDE = 5 * 4 // 5 floats × 4 bytes

        // Shared immutable vertex data — single allocation for all instances
        private val BADGE_VERTICES = floatArrayOf(
            // X,    Y,    Z,   U,   V
            -1.0f, -1.0f, 0.0f, 0.0f, 1.0f, // BL
             1.0f, -1.0f, 0.0f, 1.0f, 1.0f, // BR
            -1.0f,  1.0f, 0.0f, 0.0f, 0.0f, // TL
             1.0f,  1.0f, 0.0f, 1.0f, 0.0f  // TR
        )
        private val SHARED_BUFFER: FloatBuffer = ByteBuffer
            .allocateDirect(BADGE_VERTICES.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                put(BADGE_VERTICES)
                position(0)
            }
    }

    private var bannerTextureId = -1
    private var streamWidth = 1280
    private var streamHeight = 720

    // Badge GL handles (independent from parent's program)
    private var badgeProgram = -1
    private var badgePositionHandle = -1
    private var badgeTexCoordHandle = -1
    private var badgeMVPHandle = -1
    private var badgeSamplerHandle = -1
    private var badgeAlphaHandle = -1

    private val mvpMatrix = FloatArray(16)
    @Volatile private var matrixDirty = true

    // Cached FBO id — avoids getRenderHandler().fboId allocation per frame
    private var cachedFboId = -1

    init {
        alpha = 0f
    }

    /**
     * Create the badge bitmap, upload to GPU texture, then immediately recycle.
     * Saves ~64KB of heap (256×64×4 bytes) for the entire filter lifetime.
     */
    private fun createAndUploadBadgeTexture() {
        val bitmap = createBitmap(BADGE_W, BADGE_H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE53935.toInt() }
        canvas.drawRoundRect(
            RectF(0f, 0f, BADGE_W.toFloat(), BADGE_H.toFloat()), 8f, 8f, bgPaint
        )

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val textY = BADGE_H / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText("REPLAY", BADGE_W / 2f, textY, textPaint)

        // Upload to GPU
        val texIds = IntArray(1)
        GLES20.glGenTextures(1, texIds, 0)
        bannerTextureId = texIds[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bannerTextureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        // Immediately free heap — GPU now owns the pixel data
        bitmap.recycle()
    }

    /** Lazy init of all badge GL resources — called on GL thread only, once. */
    private fun initBadgeGl() {
        if (badgeProgram != -1) return

        createAndUploadBadgeTexture()

        // Minimal shaders — no branching, no dependent texture reads
        val vShader =
            "uniform mat4 uMVP;" +
            "attribute vec4 aPos;" +
            "attribute vec2 aTex;" +
            "varying vec2 vTex;" +
            "void main(){gl_Position=uMVP*aPos;vTex=aTex;}"

        val fShader =
            "precision lowp float;" +  // lowp is sufficient for badge rendering
            "varying vec2 vTex;" +
            "uniform sampler2D uSamp;" +
            "uniform float uAlpha;" +
            "void main(){vec4 c=texture2D(uSamp,vTex);gl_FragColor=vec4(c.rgb,c.a*uAlpha);}"

        badgeProgram = GlUtil.createProgram(vShader, fShader)
        badgePositionHandle = GLES20.glGetAttribLocation(badgeProgram, "aPos")
        badgeTexCoordHandle = GLES20.glGetAttribLocation(badgeProgram, "aTex")
        badgeMVPHandle = GLES20.glGetUniformLocation(badgeProgram, "uMVP")
        badgeSamplerHandle = GLES20.glGetUniformLocation(badgeProgram, "uSamp")
        badgeAlphaHandle = GLES20.glGetUniformLocation(badgeProgram, "uAlpha")
    }

    /**
     * Override draw() to composite badge AFTER the video is fully rendered.
     * Zero overhead when badge is hidden (alpha ≤ 0).
     */
    override fun draw() {
        // 1. Render source video completely (FBO bind → drawFilter → glDrawArrays → unbind)
        super.draw()

        // 2. Fast path: skip entirely when invisible
        if (alpha <= 0.01f) return

        // 3. Lazy-init badge resources (only once, on first visible frame)
        if (badgeProgram == -1) initBadgeGl()
        if (badgeProgram == -1) return

        // 4. Cache FBO id (avoids object allocation from getRenderHandler() per frame)
        if (cachedFboId == -1) {
            cachedFboId = getRenderHandler().fboId[0]
        }

        // 5. Update MVP only on resolution change
        if (matrixDirty) {
            updateBadgeMatrix()
            matrixDirty = false
        }

        // 6. Re-bind FBO and draw badge on top
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, cachedFboId)
        GLES20.glViewport(0, 0, getWidth(), getHeight())

        GLES20.glUseProgram(badgeProgram)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        GLES20.glUniformMatrix4fv(badgeMVPHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniform1f(badgeAlphaHandle, alpha)

        SHARED_BUFFER.position(0)
        GLES20.glEnableVertexAttribArray(badgePositionHandle)
        GLES20.glVertexAttribPointer(badgePositionHandle, 3, GLES20.GL_FLOAT, false, STRIDE, SHARED_BUFFER)

        SHARED_BUFFER.position(3)
        GLES20.glEnableVertexAttribArray(badgeTexCoordHandle)
        GLES20.glVertexAttribPointer(badgeTexCoordHandle, 2, GLES20.GL_FLOAT, false, STRIDE, SHARED_BUFFER)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bannerTextureId)
        GLES20.glUniform1i(badgeSamplerHandle, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // Cleanup — minimal calls required
        GLES20.glDisableVertexAttribArray(badgePositionHandle)
        GLES20.glDisableVertexAttribArray(badgeTexCoordHandle)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glUseProgram(0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    private fun updateBadgeMatrix() {
        val marginNDC = 0.04f
        val bW = (BADGE_W.toFloat() / streamWidth.coerceAtLeast(1)) * 2f
        val bH = (BADGE_H.toFloat() / streamHeight.coerceAtLeast(1)) * 2f

        Matrix.setIdentityM(mvpMatrix, 0)
        Matrix.translateM(mvpMatrix, 0, -1f + marginNDC + bW / 2f, 1f - marginNDC - bH / 2f, 0f)
        Matrix.scaleM(mvpMatrix, 0, bW / 2f, bH / 2f, 1f)
    }

    override fun setVideoStreamData(videoStreamData: IVideoStreamData) {
        if (this.streamWidth != videoStreamData.width || this.streamHeight != videoStreamData.height) {
            this.streamWidth = videoStreamData.width
            this.streamHeight = videoStreamData.height
            this.matrixDirty = true
        }
    }

    override fun release() {
        super.release()
        if (bannerTextureId != -1) {
            GLES20.glDeleteTextures(1, intArrayOf(bannerTextureId), 0)
            bannerTextureId = -1
        }
        if (badgeProgram != -1) {
            GLES20.glDeleteProgram(badgeProgram)
            badgeProgram = -1
        }
        cachedFboId = -1
    }

    // --- IOverlayObjectFilterRender ---
    override fun getBitmap(): Bitmap? = null  // bitmap recycled after GPU upload
    override fun getOverflowRatio(): Float = 0f
    override fun hide() { alpha = 0f }
    override fun show() { alpha = 1f }

    fun setFullScreen() {
        setScale(100f, 100f)
        setPosition(0f, 0f)
    }

    fun getCurrentAlpha(): Float = alpha
    fun updateAlpha(value: Float) { alpha = value }
}
