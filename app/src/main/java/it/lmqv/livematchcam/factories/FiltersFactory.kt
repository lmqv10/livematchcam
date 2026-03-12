package it.lmqv.livematchcam.factories

import android.content.Context
import com.pedro.encoder.input.gl.render.filters.`object`.BaseObjectFilterRender
import it.lmqv.livematchcam.factories.sports.Sports
import it.lmqv.livematchcam.services.stream.filters.BasketScoreboardViewFilterRender
import it.lmqv.livematchcam.services.stream.filters.OverlayFilterRender
import it.lmqv.livematchcam.services.stream.filters.SoccerScoreboardViewFilterRender
import it.lmqv.livematchcam.services.stream.filters.VolleyScoreboardViewFilterRender
import it.lmqv.livematchcam.services.stream.filters.OverlayObjectFilterRender

enum class FilterPosition {
    TOP_LEFT, TOP, TOP_RIGHT,
    CENTER,
    BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT
}

object FiltersFactory {

    fun getScoreBoard(sport: Sports, applicationContext: Context) : OverlayObjectFilterRender {
        return when (sport) {
            Sports.SOCCER -> SoccerScoreboardViewFilterRender(applicationContext)
            Sports.BASKET -> BasketScoreboardViewFilterRender(applicationContext)
            Sports.VOLLEY -> VolleyScoreboardViewFilterRender(applicationContext)
        }
    }

    fun getFilters(applicationContext: Context) : List<BaseObjectFilterRender> {
        return listOf(
            OverlayFilterRender(applicationContext, FilterPosition.TOP_LEFT),
            OverlayFilterRender(applicationContext, FilterPosition.TOP),
            OverlayFilterRender(applicationContext, FilterPosition.TOP_RIGHT),
//            OverlayFilterRender(applicationContext, FilterPosition.LEFT),
            OverlayFilterRender(applicationContext, FilterPosition.CENTER),
//            OverlayFilterRender(applicationContext, FilterPosition.RIGHT),
            OverlayFilterRender(applicationContext, FilterPosition.BOTTOM_LEFT),
            OverlayFilterRender(applicationContext, FilterPosition.BOTTOM),
            OverlayFilterRender(applicationContext, FilterPosition.BOTTOM_RIGHT)
        )
    }
}