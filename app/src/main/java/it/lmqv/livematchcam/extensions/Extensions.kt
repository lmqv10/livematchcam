package it.lmqv.livematchcam.extensions

import android.app.Service
import android.content.Context
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.google.api.client.util.DateTime
import it.lmqv.livematchcam.R
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

fun Service.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun MenuItem.setColor(context: Context, @ColorRes color: Int) {
    val spannableString = SpannableString(title.toString())
    spannableString.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, color)), 0, spannableString.length, 0)
    title = spannableString
}

fun MenuItem.updateMenuColor(context: Context, currentItem: MenuItem?): MenuItem {
    currentItem?.setColor(context, R.color.black)
    setColor(context, R.color.appColor)
    return this
}

fun ImageView.setShirtByColor(@ColorInt color: Int) {
    val layeredDrawable = ContextCompat.getDrawable(this.context, R.drawable.shirt_layers) as LayerDrawable
    val maskLayer = layeredDrawable.findDrawableByLayerId(R.id.mask_layer)
    maskLayer.setTint(color)
    this.setImageDrawable(layeredDrawable)
}

@Suppress("DEPRECATION")
fun Drawable.setColorFilter(@ColorInt color: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        colorFilter = BlendModeColorFilter(color, BlendMode.SRC_IN)
    } else {
        setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }
}

inline fun <reified T : Any> mapToClass(map: Map<String, Any?>?): T? {
    val constructor = T::class.primaryConstructor ?: return null
    val parameters = constructor.parameters.associateWith { map?.get(it.name) }
    return constructor.callBy(parameters)
}

inline fun <reified T : Any> T.toMap(): Map<String, Any?> {
    return T::class.memberProperties.associate { prop ->
        prop.name to prop.get(this)
    }
}


fun Int.toArgbHex(): String {
    return String.format("#%08X", this)
}


fun singleDecimalFormat(value:Float) : String {
    val formatter = DecimalFormat("#.0")
    return formatter.format(value)
}

fun degreeFormat(value:Int) : String {
    val formatter = String.format("${value}°")
    return formatter.format(value)
}

fun degreeFormat(prefix: String, value:Int) : String {
    val formatter = String.format("${prefix}:${value}°")
    return formatter.format(value)
}

fun bitrateFormat(value:Float) : String {
    val formatter = String.format(Locale.getDefault(), "%.1f mb/s", value)
    return formatter.format(value)
}

fun fpsFormat(value:Int) : String {
    val formatter = String.format(Locale.getDefault(), "%d fps", value)
    return formatter.format(value)
}

fun formatTime(seconds: Int = 0): String {
    var formattedTime : String
    if (seconds > 3600) {
        formattedTime = formatHourTime(seconds)
    }  else {
        formattedTime = String.format("%02d:%02d", seconds / 60, seconds % 60)
    }
    return formattedTime
}
fun formatHourTime(seconds: Int = 0): String {
    return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60)
}

fun formatDate(dateTime: DateTime?, format: String = "EEEE dd MMMM yyyy HH:mm"): String {
    var dateFormat = "-"
    if (dateTime != null) {
        val date = Date(dateTime.value)
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        dateFormat = sdf.format(date)
    }
    return dateFormat
}

fun formatDateFromString(dateString: String, format: String = "ddd dd/MM/yyyy HH:mm"): String {
    val sdf = SimpleDateFormat(format, Locale.getDefault())
    val dateTime = DateTime.parseRfc3339(dateString)
    return sdf.format(dateTime)
}