package it.lmqv.livematchcam.factories

import android.text.InputFilter
import it.lmqv.livematchcam.factories.sports.Sports

object TeamNameInputFiltersFactory {
    private var shortLength = 3
    private var midLength = 5
    private var normalLength = 10
    private var longLength = 20

    fun get(sport: Sports) : Array<InputFilter> {
        return when (sport) {
            Sports.SOCCER -> arrayOf<InputFilter>(
                InputFilter.LengthFilter(getMaxLength(sport)),
                InputFilter.AllCaps()
            )
            Sports.BASKET -> arrayOf<InputFilter>(
                InputFilter.LengthFilter(getMaxLength(sport))
            )
            Sports.VOLLEY -> arrayOf<InputFilter>(
                InputFilter.LengthFilter(getMaxLength(sport)),
            )
        }
    }

    fun getMaxLength(sport: Sports) : Int {
        return when (sport) {
            Sports.SOCCER -> shortLength
            Sports.BASKET -> midLength
            Sports.VOLLEY -> longLength
        }
    }
}