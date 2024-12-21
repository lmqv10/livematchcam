package it.lmqv.livematchcam.handlers.offset

import android.content.Context
import kotlin.math.floor

class ProgressiveOffsetDegreeHandler(context: Context) :
    BaseOffsetDegreeHandler(context),
    IOffsetDegreeHandler {

    override fun initialize() { }
    override fun destroy() { }
    override fun manualZoomLevel(zoomLevel: ManualZoomLevel) { }

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

        this.degreeOffset = offset * this.offset
        return this.degreeOffset
    }
}