package it.lmqv.livematchcam.factories

enum class Sports {
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