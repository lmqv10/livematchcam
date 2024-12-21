package it.lmqv.livematchcam.handlers.offset

import android.content.Context
import it.lmqv.livematchcam.extensions.Logd
import kotlin.math.max

class ManualZoomLevelHandler(context: Context) :
    BaseOffsetDegreeHandler(context),
    IOffsetDegreeHandler {

    private var manualZoomOffset = 0f

    override fun initialize() { }
    override fun destroy() { }

    override fun manualZoomLevel(zoomLevel: ManualZoomLevel) {
        if (zoomLevel == ManualZoomLevel.Out){
            manualZoomOffset = -1f
        } else if (zoomLevel == ManualZoomLevel.In) {
            manualZoomOffset = 1f
        } else if (zoomLevel == ManualZoomLevel.None) {
            manualZoomOffset = 0f
        }
    }

    override fun getOffsetByDegrees(degrees: IntArray) : Float {
        var multiplier = 1f + manualZoomOffset

        this.degreeOffset = max(0f, multiplier * this.offset)
        //Logd("ManualZoomLevelHandler:: this.offset:: ${this.offset}")
        //Logd("ManualZoomLevelHandler:: offsetMultiplier:: ${multiplier} ==> degreeOffset:: ${this.degreeOffset}")
        return this.degreeOffset
    }
}