package it.lmqv.livematchcam.factories

import android.content.Context

interface IDrawerMenuFactory {
    //fun get() : ISportsComponentsFactory
    //fun set(sport: Sports)
}

class DrawerMenuFactory(context: Context) : IDrawerMenuFactory {
/*
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
*/
}