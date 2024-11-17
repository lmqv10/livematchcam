package it.lmqv.livematchcam.utils

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

object ScreenUtils {
    fun getScreenWidth(context: Context): Int {
        val displayMetrics = DisplayMetrics()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+
            context.display?.getRealMetrics(displayMetrics)
        } else {
            // API < 30
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.defaultDisplay.getMetrics(displayMetrics)
        }

        return displayMetrics.widthPixels
    }

    fun getScreenHeight(context: Context): Int {
        val displayMetrics = DisplayMetrics()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+
            context.display?.getRealMetrics(displayMetrics)
        } else {
            // API < 30
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.defaultDisplay.getMetrics(displayMetrics)
        }

        return displayMetrics.heightPixels
    }
}