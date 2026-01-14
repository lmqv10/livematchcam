package it.lmqv.livematchcam.factories

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class Sports : Parcelable {
    SOCCER,
    VOLLEY
}

object SportsFactory {
    fun get(sport: Sports) : ISportsComponentsFactory {
        return when (sport) {
            Sports.SOCCER -> SoccerFragmentsFactory.newInstance()
            Sports.VOLLEY -> VolleyFragmentsFactory.newInstance()
        }
    }
}