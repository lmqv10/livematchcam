package it.lmqv.livematchcam.factories

import android.text.InputFilter

object TeamNameInputFiltersFactory {
    private var shortLength = 3
    private var normalLength = 20

    fun get(sport: Sports) : Array<InputFilter> {
        return when (sport) {
            Sports.SOCCER -> arrayOf<InputFilter>(
                InputFilter.LengthFilter(getMaxLength(sport)),
                InputFilter.AllCaps()
            )
            Sports.VOLLEY -> arrayOf<InputFilter>(
                InputFilter.LengthFilter(getMaxLength(sport)),
            )
        }
    }

    fun getMaxLength(sport: Sports) : Int {
        return when (sport) {
            Sports.SOCCER -> shortLength
            Sports.VOLLEY -> normalLength
        }
    }
}