package it.lmqv.livematchcam.extensions

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.ImageView

fun ImageView.setFillAndBorder(fillColor: Int, borderColor: Int = Color.BLACK, borderWidthDp: Float = 1f, cornerRadiusDp: Float = 5f) {
    val drawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fillColor) // Sfondo trasparente
        setStroke(
            borderWidthDp.toPx(context),
            borderColor
        )
        cornerRadius = cornerRadiusDp.toPxF(context)
    }
    background = drawable
}

// Estensione per convertire dp in px
fun Float.toPx(context: Context): Int =
    (this * context.resources.displayMetrics.density).toInt()

fun Float.toPxF(context: Context): Float =
    (this * context.resources.displayMetrics.density)

fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()