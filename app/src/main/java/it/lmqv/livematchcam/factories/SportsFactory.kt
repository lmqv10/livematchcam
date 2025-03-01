package it.lmqv.livematchcam.factories

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
        _sport = sport
    }

    override fun get() : ISportsComponentsFactory {
        return when (_sport) {
            Sports.SOCCER -> SoccerFragmentsFactory.newInstance()
            Sports.VOLLEY -> VolleyFragmentsFactory.newInstance()
        }
    }
}