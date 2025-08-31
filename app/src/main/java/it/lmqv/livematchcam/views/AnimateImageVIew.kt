package it.lmqv.livematchcam.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.widget.ImageView
import androidx.core.graphics.drawable.toDrawable

@SuppressLint("ClickableViewAccessibility", "AppCompatCustomView")
class AnimateImageVIew @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ImageView(context, attrs, defStyleAttr, defStyleRes) {
    init {
        isClickable = true
        isFocusable = true

        if (background == null) {
            background = createRippleDrawable()
        }

        setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).start()
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                    -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(250).start()
                }
            }
            false
        }
    }

    private fun createRippleDrawable(): RippleDrawable {
        val outValue = TypedValue()
        context.theme.resolveAttribute(
            android.R.attr.colorControlHighlight, outValue, true
        )
        val rippleColor = ColorStateList.valueOf(outValue.data)

        val mask = Color.RED.toDrawable()
        val content = Color.TRANSPARENT.toDrawable()

        return RippleDrawable(rippleColor, content, mask)
    }
}