package it.lmqv.livematchcam.drawable

import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.drawable.Drawable
import androidx.core.graphics.toColorInt
import androidx.core.graphics.createBitmap

class CheckerboardDrawable(
    squareSize: Int = 30, // Dimensione in pixel del singolo quadrato
    color1: Int = "#FFFFFF".toColorInt(), // Bianco
    color2: Int = "#CCCCCC".toColorInt(),  // Grigio
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        // Creiamo un piccolo pattern 2x2
        val bitmap = createBitmap(squareSize * 2, squareSize * 2)
        val canvas = Canvas(bitmap)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // Sfondo tutto color1 (Bianco)
        p.color = color1
        canvas.drawRect(0f, 0f, (squareSize * 2).toFloat(), (squareSize * 2).toFloat(), p)

        // Disegniamo i due quadrati color2 (Grigi) sfalsati
        p.color = color2
        canvas.drawRect(0f, 0f, squareSize.toFloat(), squareSize.toFloat(), p) // In alto a sinistra
        canvas.drawRect(squareSize.toFloat(), squareSize.toFloat(), (squareSize * 2).toFloat(), (squareSize * 2).toFloat(), p) // In basso a destra

        // Creiamo uno shader che ripete il pattern all'infinito sia in X che in Y
        val shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        paint.shader = shader
    }

    override fun draw(canvas: Canvas) {
        // Riempiamo l'intera area disponibile con il pattern ripetuto
        canvas.drawRect(bounds, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}
