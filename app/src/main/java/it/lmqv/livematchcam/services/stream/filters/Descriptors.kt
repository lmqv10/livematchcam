package it.lmqv.livematchcam.services.stream.filters

import com.pedro.encoder.utils.gl.TranslateTo
import kotlinx.coroutines.flow.StateFlow

data class AnimationDescriptor(
    val targetAlpha: Float = 0.75f,
    val duration: Long = 250L
)

data class DimensionDescriptor(
    val targetWidthDp: Int = 250
)

data class RotatorDescriptor(
    val targetWidthDp: Int = 50,
    val intervalMillis: Long = 4000L,
)

data class FilterDescriptor(
    val defaultSize: Float = 18f,
    val preferencesSizeKey: String = "",
    val translateTo : TranslateTo = TranslateTo.TOP_LEFT
)

data class SourceDescriptor(
    val url : StateFlow<String>,
    val isVisible : StateFlow<Boolean>
)