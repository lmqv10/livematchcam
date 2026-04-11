package it.lmqv.livematchcam.services.stream.filters

import android.content.Context
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
 * A stable unified filter that renders both the replay video surface AND the "REPLAY" badge.
 * This implementation inherits from SurfaceFilterRender for maximum stability with the 
 * library's Surface management, but isolates the badge drawing to prevent video freezing.
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class ReplayUnifiedFilterRender(
    surfaceReadyCallback: SurfaceReadyCallback? = null
) : SurfaceFilterRender(surfaceReadyCallback), IOverlayObjectFilterRender {

    private var bannerBitmap: Bitmap? = null
    private var bannerTextureId = -1
    
    private var streamWidth = 1200
    private var streamHeight = 720
    
    private var badgeProgram = -1
    private var badgePositionHandle = -1
    private var badgeTextureCoordHandle = -1
    private var badgeMVPMatrixHandle = -1
    private var badgeSamplerHandle = -1
    private var badgeAlphaHandle = -1
    
    private val badgeVertices = floatArrayOf(
        -1.0f, -1.0f, 0.0f, 0.0f, 1.0f, // BL
         1.0f, -1.0f, 0.0f, 1.0f, 1.0f, // BR
        -1.0f,  1.0f, 0.0f, 0.0f, 0.0f, // TL
         1.0f,  1.0f, 0.0f, 1.0f, 0.0f  // TR
    )
    private var badgeBuffer: FloatBuffer = ByteBuffer.allocateDirect(badgeVertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer().apply {
            put(badgeVertices)
            position(0)
        }
        
    private val badgeMVPMatrix = FloatArray(16)
    private var isMatrixDirty = true

    init {
        // Start invisible
        alpha = 0f
        createBannerBitmap()
    }

    private fun createBannerBitmap() {
        val width = 400
        val height = 120
        val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E53935")
        }
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 36f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val textY = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText("REPLAY", width / 2f, textY, textPaint)
        
        this.bannerBitmap = bitmap
    }

    private fun initBadgeGl() {
        if (bannerTextureId != -1) return
        
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        bannerTextureId = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bannerTextureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        bannerBitmap?.let { android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, it, 0) }
        
        val vShader = """
            uniform mat4 uMVPMatrix;
            attribute vec4 aPosition;
            attribute vec2 aTextureCoord;
            varying vec2 vTextureCoord;
            void main() {
              gl_Position = uMVPMatrix * aPosition;
              vTextureCoord = aTextureCoord;
            }
        """.trimIndent()
        
        val fShader = """
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform sampler2D uSampler;
            uniform float uAlpha;
            void main() {
              vec4 color = texture2D(uSampler, vTextureCoord);
              gl_FragColor = vec4(color.rgb, color.a * uAlpha);
            }
        """.trimIndent()
        
        badgeProgram = GlUtil.createProgram(vShader, fShader)
        badgePositionHandle = GLES20.glGetAttribLocation(badgeProgram, "aPosition")
        badgeTextureCoordHandle = GLES20.glGetAttribLocation(badgeProgram, "aTextureCoord")
        badgeMVPMatrixHandle = GLES20.glGetUniformLocation(badgeProgram, "uMVPMatrix")
        badgeSamplerHandle = GLES20.glGetUniformLocation(badgeProgram, "uSampler")
        badgeAlphaHandle = GLES20.glGetUniformLocation(badgeProgram, "uAlpha")
    }

    override fun drawFilter() {
        // 1. Draw the Video Frame (Surface via library)
        super.drawFilter()
        
        // 2. Draw the Badge on top (with absolute state isolation)
        if (alpha > 0.01f) {
            if (bannerTextureId == -1) initBadgeGl()
            if (badgeProgram == -1) return
            
            if (isMatrixDirty) {
                updateBadgeMatrix()
                isMatrixDirty = false
            }

            // --- PROTECTED GL STATE BLOCK ---
            val isBlendEnabled = GLES20.glIsEnabled(GLES20.GL_BLEND)
            val prevProgram = IntArray(1)
            GLES20.glGetIntegerv(GLES20.GL_CURRENT_PROGRAM, prevProgram, 0)
            
            GLES20.glUseProgram(badgeProgram)
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            
            GLES20.glUniformMatrix4fv(badgeMVPMatrixHandle, 1, false, badgeMVPMatrix, 0)
            GLES20.glUniform1f(badgeAlphaHandle, alpha)
            
            GLES20.glEnableVertexAttribArray(badgePositionHandle)
            badgeBuffer.position(0)
            GLES20.glVertexAttribPointer(badgePositionHandle, 3, GLES20.GL_FLOAT, false, 5 * 4, badgeBuffer)
            
            GLES20.glEnableVertexAttribArray(badgeTextureCoordHandle)
            badgeBuffer.position(3)
            GLES20.glVertexAttribPointer(badgeTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 5 * 4, badgeBuffer)
            
            GLES20.glActiveTexture(GLES20.GL_TEXTURE7)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bannerTextureId)
            GLES20.glUniform1i(badgeSamplerHandle, 7)
            
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            
            // --- RESTORE STATE ---
            GLES20.glDisableVertexAttribArray(badgePositionHandle)
            GLES20.glDisableVertexAttribArray(badgeTextureCoordHandle)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            if (!isBlendEnabled) GLES20.glDisable(GLES20.GL_BLEND)
            
            // Restore previously active program to avoid freezing subsequent filters
            if (prevProgram[0] != 0 && prevProgram[0] != badgeProgram) {
                GLES20.glUseProgram(prevProgram[0])
            } else {
                GLES20.glUseProgram(0)
            }
        }
    }

    private fun updateBadgeMatrix() {
        val marginPercent = 0.02f
        val bWNDC = (400f / streamWidth.coerceAtLeast(1)) * 2f
        val bHNDC = (120f / streamHeight.coerceAtLeast(1)) * 2f
        
        Matrix.setIdentityM(badgeMVPMatrix, 0)
        Matrix.translateM(badgeMVPMatrix, 0, -1f + marginPercent + bWNDC/2f, 1f - marginPercent - bHNDC/2f, 0f)
        Matrix.scaleM(badgeMVPMatrix, 0, bWNDC/2f, bHNDC/2f, 1f)
    }

    override fun setVideoStreamData(videoStreamData: IVideoStreamData) {
        if (this.streamWidth != videoStreamData.width || this.streamHeight != videoStreamData.height) {
            this.streamWidth = videoStreamData.width
            this.streamHeight = videoStreamData.height
            this.isMatrixDirty = true
        }
    }

    override fun release() {
        super.release()
        bannerBitmap?.recycle()
        bannerBitmap = null
        if (bannerTextureId != -1) {
            GLES20.glDeleteTextures(1, intArrayOf(bannerTextureId), 0)
            bannerTextureId = -1
        }
        if (badgeProgram != -1) {
            GLES20.glDeleteProgram(badgeProgram)
            badgeProgram = -1
        }
    }

    // IOverlayObjectFilterRender implementation
    override fun getBitmap(): Bitmap? = bannerBitmap
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
