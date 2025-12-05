package it.lmqv.livematchcam.services.stream.filters

import com.pedro.encoder.utils.gl.TranslateTo

data class AnimationDescriptor(
    val targetAlpha: Float = 0.75f,
    val duration: Long = 250L
)

data class RotatorDescriptor(
    val targetWidthDp: Int = 50,
    var intervalMillis: Long = 4000L,
)

data class FilterDescriptor(
    val maxFactor: Float = 25f,
    val translateTo : TranslateTo = TranslateTo.TOP_LEFT
)
