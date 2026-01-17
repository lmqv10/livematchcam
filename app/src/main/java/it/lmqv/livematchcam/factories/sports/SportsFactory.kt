package it.lmqv.livematchcam.factories.sports

import android.os.Parcelable
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.adapters.CardItem
import it.lmqv.livematchcam.factories.ISportsComponentsFactory
import kotlinx.parcelize.Parcelize

@Parcelize
enum class Sports : Parcelable {
    SOCCER,
    BASKET,
    VOLLEY
}

object SportsFactory {
    fun get(sport: Sports) : ISportsComponentsFactory {
        return when (sport) {
            Sports.SOCCER -> SoccerFragmentsFactory.newInstance()
            Sports.BASKET -> BasketFragmentsFactory.newInstance()
            Sports.VOLLEY -> VolleyFragmentsFactory.newInstance()
        }
    }

    fun getSports() : List<CardItem> {
        return listOf(
            CardItem(
                sport = Sports.SOCCER,
                description = R.string.sport_soccer,
                icon = R.drawable.sport_soccer
            ),
//            CardItem(
//                sport = Sports.BASKET,
//                description = R.string.sport_basket,
//                icon = R.drawable.sport_basket
//            ),
            CardItem(
                sport = Sports.VOLLEY,
                description = R.string.sport_volley,
                icon = R.drawable.sport_volley
            )
        )
    }
}