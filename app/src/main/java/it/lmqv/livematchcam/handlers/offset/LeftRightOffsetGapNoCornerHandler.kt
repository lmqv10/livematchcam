package it.lmqv.livematchcam.handlers.offset

import android.content.Context

class LeftRightOffsetGapNoCornerHandler(context: Context) :
    BaseOffsetDegreeHandler(context),
    IOffsetDegreeHandler {

    override fun initialize() { }
    override fun destroy() { }
    override fun manualZoomLevel(zoomLevel: ManualZoomLevel) { }

    override fun getOffsetByDegrees(degrees: IntArray) : Float {
        var degree = degrees[0]
        if (this.offset > 0 && degree > this.leftDegree && degree < this.rightDegree) {
            this.degreeOffset = 0f
        } else if (this.degreeOffset > 0 && (degree < this.leftDegree * 2 || degree > this.rightDegree * 2)) {
            this.degreeOffset = 0f
        }else if (degree < this.leftDegree || degree > this.rightDegree) {
            this.degreeOffset = this.offset
        }
        return this.degreeOffset
    }
}