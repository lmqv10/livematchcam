package it.lmqv.livematchcam.handlers.offset

import android.content.Context
import kotlin.math.floor
import kotlin.math.min

class ProgressiveOffsetDegreeWithCapHandler(context: Context) :
    BaseOffsetDegreeHandler(context),
    IOffsetDegreeHandler {

    override fun initialize() { }
    override fun destroy() { }
    override fun manualZoomLevel(zoomLevel: ManualZoomLevel) { }

    val cap: Float = 3.0f

    override fun getOffsetByDegrees(degrees: IntArray) : Float {
        var offset : Float
        var degree = degrees[0]

        if (degree < this.leftDegree) {
            offset = floor(degree.toDouble() / this.leftDegree).toFloat()
        } else if (degree > this.rightDegree) {
            offset = floor(degree.toDouble() / this.rightDegree).toFloat()
        } else {
            offset = 0.0f
        }

        this.degreeOffset = min(cap, offset) * this.offset
        return this.degreeOffset
    }
}