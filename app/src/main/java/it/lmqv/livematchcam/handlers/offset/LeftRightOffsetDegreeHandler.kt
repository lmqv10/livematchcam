package it.lmqv.livematchcam.handlers.offset

import android.content.Context

class LeftRightOffsetDegreeHandler(context: Context) :
    BaseOffsetDegreeHandler(context),
    IOffsetDegreeHandler {

    override fun initialize() { }
    override fun destroy() { }
    override fun manualZoomLevel(zoomLevel: ManualZoomLevel) { }

    override fun getOffsetByDegrees(degrees: IntArray) : Float {
        var offset = 0.0f
        var degree = degrees[0]

        if (degree < this.leftDegree || degree > this.rightDegree) {
            offset += this.offset
        }
        this.degreeOffset = offset
        return this.degreeOffset
    }
}