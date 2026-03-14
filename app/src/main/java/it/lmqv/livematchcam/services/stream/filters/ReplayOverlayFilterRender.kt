package it.lmqv.livematchcam.services.stream.filters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import it.lmqv.livematchcam.factories.FilterPosition
import it.lmqv.livematchcam.services.stream.IVideoStreamData

class ReplayOverlayFilterRender(var context: Context) : OverlayObjectFilterRender() {

    private var streamWidth: Int = 100
    private var streamHeight: Int = 100

    init {
        // By default, it's invisible until replay starts
        alpha = 0f
    }

    override fun setVideoStreamData(videoStreamData: IVideoStreamData) {
        this.streamWidth = videoStreamData.width
        this.streamHeight = videoStreamData.height

        createBadgeBitmap()
    }

    private fun createBadgeBitmap() {
        val width = 400
        val height = 120
        val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw red background with rounded corners
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E53935") // Material Red 600
        }
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        // Draw white text "REPLAY"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 36f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        
        val textY = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText("REPLAY", width / 2f, textY, textPaint)

        setImage(bitmap)
        
        // Scale and position top left
        if (streamObject != null && streamWidth > 0 && streamHeight > 0) {
            val scaleX = (width * 100f) / streamWidth
            val scaleY = (height * 100f) / streamHeight
            setScale(scaleX, scaleY)
            
            // Top Left position with margin
            val margin = 2f
            setPosition(margin, margin)
        }
    }

    fun setVisible(visible: Boolean) {
        alpha = if (visible) 1f else 0f
    }

    override fun getBitmap(): Bitmap? {
        return if (streamObject.bitmaps.size > 0) { streamObject.bitmaps[0] } else { null }
    }

    override fun getOverflowRatio(): Float { return 0f }
}
