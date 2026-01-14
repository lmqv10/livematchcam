package it.lmqv.livematchcam.utils

import android.content.Context
import android.content.res.Configuration

object FontScaleHelper {
    private const val MAX_FONT_SCALE = 1.1f
    private const val MIN_FONT_SCALE = 0.9f

    fun applyLimit(context: Context): Context {
        val configuration = Configuration(context.resources.configuration)
        //configuration.fontScale = 1.0f
        configuration.fontScale = configuration
            .fontScale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
        return context.createConfigurationContext(configuration)
    }
}