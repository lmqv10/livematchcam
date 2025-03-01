package it.lmqv.livematchcam.factories

import it.lmqv.livematchcam.extensions.Logd

enum class Sports {
    SOCCER,
    VOLLEY
}

interface ISportsFactory {
    fun get() : ISportsComponentsFactory
    fun set(sport: Sports)
}

object SportsFactory : ISportsFactory {
    private var _sport : Sports = Sports.SOCCER

    override fun set(sport: Sports) {
        //Logd("SportsFactory: set ${sport.name}")
        _sport = sport
    }

    override fun get() : ISportsComponentsFactory {
        //Logd("SportsFactory: get ${_sport.name}")
        return when (_sport) {
            Sports.SOCCER -> SoccerFragmentsFactory.newInstance()
            Sports.VOLLEY -> VolleyFragmentsFactory.newInstance()
        }
    }
}