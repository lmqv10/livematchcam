package it.lmqv.livematchcam.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class ToggleTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    override fun setEnabled(enabled: Boolean) {
        this.text = if (!enabled) "OFF" else "..."
        super.setEnabled(enabled)
    }
}
