package it.lmqv.livematchcam.handlers.offset

import android.content.Context

class ProgressiveOffsetDegreeHandler(context: Context) :
    BaseOffsetDegreeHandler(context),
    IOffsetDegreeHandler {

    override fun getOffsetByDegree(degree: Int) : Float {
        var offset : Float

        if (degree < this.leftDegree) {
            offset = Math.round(degree.toDouble() / this.leftDegree).toFloat()
        } else if (degree > this.rightDegree) {
            offset = Math.round(degree.toDouble() / this.rightDegree).toFloat()
        } else {
            offset = 0.0f
        }
        this.degreeOffset = offset * this.offset
        return this.degreeOffset
    }
}