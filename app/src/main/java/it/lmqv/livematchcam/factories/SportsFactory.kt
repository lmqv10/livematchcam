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
    private var _Sport : Sports = Sports.SOCCER

    override fun set(sport: Sports) {
        _Sport = sport
    }

    override fun get() : ISportsComponentsFactory {
        return when (_Sport) {
            Sports.SOCCER -> SoccerFragmentsFactory.newInstance()
            Sports.VOLLEY -> VolleyFragmentsFactory.newInstance()
        }
    }
}