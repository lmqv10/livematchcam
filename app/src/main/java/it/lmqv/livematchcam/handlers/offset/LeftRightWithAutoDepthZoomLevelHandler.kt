package it.lmqv.livematchcam.handlers.offset

import android.content.Context
import it.lmqv.livematchcam.extensions.Logd
import kotlin.math.max
import kotlin.math.min

class LeftRightWithAutoDepthZoomLevelHandler(context: Context) :
    BaseOffsetDegreeHandler(context),
    IOffsetDegreeHandler {

    private var zBoundDegree = 1

    override fun initialize() { }
    override fun destroy() { }
    override fun manualZoomLevel(zoomLevel: ManualZoomLevel) { }

    override fun getOffsetByDegrees(degrees: IntArray) : Float {
        var multiplier = 1f
        var degreeX = degrees[0]

        if (this.offset > 0 && degreeX > this.leftDegree && degreeX < this.rightDegree) {
            multiplier = 1f
        } else if (degreeX < this.leftDegree || degreeX > this.rightDegree) {
            multiplier = 2f
        }

        var degreeZ = degrees[2]
        if (this.offset > 0 && degreeZ > zBoundDegree){
            multiplier -= 1f
        } else if (degreeZ < -zBoundDegree) {
            multiplier += 1f
        }

        this.degreeOffset = min(2f, max(0f, multiplier * this.offset))

        //Logd("LeftRightWithAutoDepthZoomLevelHandler:: offsetMultiplier:: ${multiplier} ==> degreeOffset:: ${this.degreeOffset}")

        //Logd("degree:: X: ${degreeX} Z: ${degreeZ} >>> offsetMultiplier:: ${multiplier} ==> degreeOffset:: ${this.degreeOffset}")
        /*Logd("degree:: X: ${degreeX} | Z: ${degreeZ} \n"+
            "this.offset:: ${this.offset}" +
            "zoomLevel: ${manualZoomLevel} \n"+
            "multiplier:: ${multiplier} \n"+
            "degreeOffset:: ${this.degreeOffset}")*/

        return this.degreeOffset
    }
}