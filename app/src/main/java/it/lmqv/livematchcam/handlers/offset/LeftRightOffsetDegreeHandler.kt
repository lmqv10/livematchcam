package it.lmqv.livematchcam.handlers.offset

import android.content.Context

class LeftRightOffsetDegreeHandler(context: Context) : BaseOffsetDegreeHandler(context), IOffsetDegreeHandler {

    override fun getOffsetByDegree(degree: Int) : Float {
        var offset = 0.0f

        if (degree < this.leftDegree || degree > this.rightDegree) {
            offset += this.offset
        }
        this.degreeOffset = offset
        return this.degreeOffset
    }
}