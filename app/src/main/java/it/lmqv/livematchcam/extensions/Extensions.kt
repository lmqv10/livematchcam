package it.lmqv.livematchcam.extensions

import android.animation.ValueAnimator
import android.app.Activity
import android.app.Service
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SpinnerAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import androidx.fragment.app.Fragment
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.api.client.util.DateTime
import com.pedro.encoder.input.gl.render.filters.`object`.BaseObjectFilterRender
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.repositories.KeyDescription
import it.lmqv.livematchcam.services.stream.VideoCaptureFormat
import it.lmqv.livematchcam.utils.OptionItem
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.reflect.full.memberProperties


fun BaseObjectFilterRender.animateAlpha(
    from: Float,
    to: Float,
    duration: Long,
    onEnd: (() -> Unit)? = null) {
    ValueAnimator.ofFloat(from, to).apply {
        this.duration = duration
        addUpdateListener {
            val alpha = it.animatedValue as Float
            this@animateAlpha.setAlpha(alpha)
        }
        if (onEnd != null) {
            doOnEnd { onEnd() }
        }
        start()
    }
}

fun Service.toast(message: String, duration: Int = Toast.LENGTH_SHORT, @DrawableRes iconResId: Int = R.drawable.ic_confirm) {
    customToast(this, message, duration, iconResId)
}

/*fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT, @DrawableRes iconResId: Int = R.drawable.ic_confirm) {
    customToast(this, message, duration, iconResId)
}*/

fun SpinnerAdapter.getItemPositionByKey(key: String): Int {
    val itemsList = List(this.count) { index ->
        this.getItem(index)!! as KeyDescription
    }
    return max(0, itemsList.indexOfFirst { it.key == key })
}

fun Activity.toast(message: String, duration: Int = Toast.LENGTH_SHORT, @DrawableRes iconResId: Int = R.drawable.ic_confirm) {
    customToast(this, message, duration, iconResId)
}

fun Fragment.toast(message: String, duration: Int = Toast.LENGTH_SHORT, @DrawableRes iconResId: Int = R.drawable.ic_confirm) {
    customToast(this.requireActivity(), message, duration, iconResId)
}

fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT, @DrawableRes iconResId: Int = R.drawable.ic_confirm) {
    customToast(this, message, duration, iconResId)
}

fun customToast(
    context: Context,
    message: String,
    duration: Int = Toast.LENGTH_SHORT,
    @DrawableRes iconResId: Int = R.drawable.ic_confirm
) {
    val inflater = LayoutInflater.from(context)
    val parent = if (context is Activity) {
        context.findViewById<ViewGroup>(android.R.id.content)
    } else {
        null
    }
    val layout: View = inflater.inflate(R.layout.context_toast, parent, false)

    val toastText = layout.findViewById<TextView>(R.id.toast_text)
    val toastIcon = layout.findViewById<ImageView>(R.id.toast_icon)

    toastText.text = message
    toastIcon.setImageResource(iconResId)

    with(Toast(context)) {
        this.duration = duration
        @Suppress("DEPRECATION")
        this.view = layout
        show()
    }
}

/*fun Context.onMainToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    CoroutineScope(Dispatchers.Main).launch {
        Toast.makeText(this@onMainToast, message, duration).show()
    }
}*/

fun View.setEnabledRecursively(enabled: Boolean) {
    this.isEnabled = enabled
    this.isClickable = enabled
    this.isFocusable = enabled
    this.alpha = if (enabled) 1.0f else 0.5f

    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            getChildAt(i).setEnabledRecursively(enabled)
        }
    }
}

fun List<VideoCaptureFormat>.toOptionItems(): List<OptionItem<VideoCaptureFormat>> {
    return this.map { x ->
        val label = buildString {
            append("${x.width}x${x.height}p")
            if (x.fps > 0) append(" ${x.fps}fps")
        }
        OptionItem(x, label)
    }
}

fun List<com.serenegiant.usb.Size>.toCameraSourceParametersWithFps() : List<VideoCaptureFormat> {
    return this.map { x -> VideoCaptureFormat(x.width, x.height, x.fps) }
}
fun List<Size>.toCameraSourceParameters() : List<VideoCaptureFormat> {
    return this.map { x -> VideoCaptureFormat(x.width, x.height) }
}

suspend fun loadDrawableOffscreen(
    context: Context,
    data: String?,
    @DrawableRes icon: Int = 0
): Drawable? {
    val request = ImageRequest.Builder(context)
        .data(data).allowHardware(false).build()

    return (context.imageLoader.execute(request) as? SuccessResult)?.drawable
        ?: AppCompatResources.getDrawable(context, icon)
}

suspend fun loadBitmapOffscreen(context: Context,
    data: String?,
targetWidth: Int) : Bitmap? {
    return loadDrawableOffscreen(context, data)?.toBitmap()?.let {
        return it.scale(targetWidth, targetWidth * it.height / it.width)
            .copy(Bitmap.Config.ARGB_8888, true)
    }
}

/*fun MenuItem.setColor(context: Context, @ColorRes color: Int) {
    val spannableString = SpannableString(title.toString())
    spannableString.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, color)), 0, spannableString.length, 0)
    title = spannableString
}*/

/*fun MenuItem.updateMenuColor(context: Context, currentItem: MenuItem?): MenuItem {
    currentItem?.setColor(context, R.color.black)
    setColor(context, R.color.appColorSecondary)
    return this
}*/

fun ImageView.setShirtByColor(@ColorInt color: Int) {
    val layeredDrawable = ContextCompat.getDrawable(this.context, R.drawable.shirt_layers) as LayerDrawable
    val maskLayer = layeredDrawable.findDrawableByLayerId(R.id.mask_layer)
    maskLayer.setTint(color)
    this.setImageDrawable(layeredDrawable)
}

/*@Suppress("DEPRECATION")
fun Drawable.setColorFilter(@ColorInt color: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        colorFilter = BlendModeColorFilter(color, BlendMode.SRC_IN)
    } else {
        setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }
}*/

/*inline fun <reified T : Any> mapToClass(map: Map<String, Any?>?): T? {
    val constructor = T::class.primaryConstructor ?: return null
    val parameters = constructor.parameters.associateWith { map?.get(it.name) }
    return constructor.callBy(parameters)
}*/

inline fun <reified T : Any> T.toMap(): Map<String, Any?> {
    return T::class.memberProperties.associate { prop ->
        prop.name to prop.get(this)
    }
}


fun Int.toArgbHex(): String {
    return String.format("#%08X", this)
}


fun singleDecimalFormat(value: Float, suffix: String = "") : String {
    val formatter = DecimalFormat("0.0")
    return formatter.format(value) + suffix
}

fun degreeFormat(value:Int) : String {
    val formatter = String.format("${value}°")
    return formatter.format(value)
}

/*fun degreeFormat(prefix: String, value:Int) : String {
    val formatter = String.format("${prefix}:${value}°")
    return formatter.format(value)
}*/

fun bitrateFormat(value:Float) : String {
    val formatter = String.format(Locale.getDefault(), "%.1f mb/s", value)
    return formatter //.format(value)
}

fun fpsFormat(value:Int) : String {
    val formatter = String.format(Locale.getDefault(), "%d fps", value)
    return formatter //.format(value)
}

fun sourceBitrateFormat(value:Int) : String {
    //Logd("sourceBitrateFormat :: ${value}")
    return String.format(Locale.getDefault(), "%.1f mb/s", value / 1000_000f)
}

fun resolutionFormat(value:Int) : String {
    val formatter = String.format(Locale.getDefault(), "%dp", value)
    return formatter.format(value)
}


fun formatTime(seconds: Int = 0): String {
    //var formattedTime : String
    //if (seconds > 3600) {
    //    formattedTime = formatHourTime(seconds)
    //}  else {
    // formattedTime = String.format("%02d:%02d", seconds / 60, seconds % 60)
    //}
    //return formattedTime

    return String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)
}

fun formatHourTime(seconds: Int = 0): String {
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60)
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

fun formatDate(dateTime: ZonedDateTime?, pattern: String = "EEEE dd MMMM yyyy HH:mm"): String {
    var dateFormat = "-"
    if (dateTime != null) {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        dateFormat = dateTime.format(formatter)
    }
    return dateFormat
}

fun Long.toFormattedDate(): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy - HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}

/*fun formatDate(calendar: Calendar): String {
    val dateFormat = SimpleDateFormat("EEEE - dd MMMM yyyy - HH:mm", Locale.getDefault())
    val dateTimeString = dateFormat.format(calendar.time).lowercase()
    return dateTimeString
}

fun formatDateFromString(dateString: String, format: String = "ddd dd/MM/yyyy HH:mm"): String {
    val sdf = SimpleDateFormat(format, Locale.getDefault())
    val dateTime = DateTime.parseRfc3339(dateString)
    return sdf.format(dateTime)
}*/

fun parseTimeToSeconds(time: String): Int = runCatching {
    val (min, sec) = time.split(":").map { it.toInt() }
    min * 60 + sec
}.getOrDefault(0)
