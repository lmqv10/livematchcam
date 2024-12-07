package it.lmqv.livematchcam.handlers.offset

import android.content.Context

class LeftRightOffsetGapDegreeHandler(context: Context) : BaseOffsetDegreeHandler(context), IOffsetDegreeHandler {

    override fun getOffsetByDegree(degree: Int) : Float {
        if (this.offset > 0 && degree > this.leftDegree && degree < this.rightDegree) {
            this.degreeOffset = 0f
        } else if (degree < this.leftDegree || degree > this.rightDegree) {
            this.degreeOffset = this.offset
        }
        return this.degreeOffset
    }
}