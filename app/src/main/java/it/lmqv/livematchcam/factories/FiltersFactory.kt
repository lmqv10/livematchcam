package it.lmqv.livematchcam.factories

import android.content.Context
import com.pedro.encoder.input.gl.render.filters.`object`.BaseObjectFilterRender
import com.pedro.encoder.utils.gl.TranslateTo
import it.lmqv.livematchcam.factories.sports.Sports
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.services.stream.filters.BasketScoreboardViewFilterRender
import it.lmqv.livematchcam.services.stream.filters.DimensionDescriptor
import it.lmqv.livematchcam.services.stream.filters.FilterDescriptor
import it.lmqv.livematchcam.services.stream.filters.OverlayFilterRender
import it.lmqv.livematchcam.services.stream.filters.SoccerScoreboardViewFilterRender
import it.lmqv.livematchcam.services.stream.filters.SourceDescriptor
import it.lmqv.livematchcam.services.stream.filters.VolleyScoreboardViewFilterRender

object FiltersFactory {
    fun get(sport: Sports, applicationContext: Context) : List<BaseObjectFilterRender> {
        return when (sport) {
            Sports.SOCCER ->
                listOf(
                    SoccerScoreboardViewFilterRender(applicationContext,
                        filterDescriptor = FilterDescriptor(maxFactor = 30f, translateTo = TranslateTo.TOP_LEFT))
                )
            Sports.BASKET ->
                listOf(
                    BasketScoreboardViewFilterRender(applicationContext,
                        filterDescriptor = FilterDescriptor(maxFactor = 25f, translateTo = TranslateTo.BOTTOM)),
                    OverlayFilterRender(applicationContext,
                        sourceDescriptor = SourceDescriptor(MatchRepository.spotBannerURL, MatchRepository.spotBannerVisible),
                        filterDescriptor = FilterDescriptor(maxFactor = 20f, translateTo = TranslateTo.TOP_RIGHT),
                        dimensionDescriptor = DimensionDescriptor()
                    ),
                )
            Sports.VOLLEY ->
                listOf(
                    VolleyScoreboardViewFilterRender(applicationContext,
                        filterDescriptor = FilterDescriptor(maxFactor = 20f, translateTo = TranslateTo.TOP_LEFT)
                    ),
                    OverlayFilterRender(applicationContext,
                        sourceDescriptor = SourceDescriptor(MatchRepository.spotBannerURL, MatchRepository.spotBannerVisible),
                        filterDescriptor = FilterDescriptor(maxFactor = 20f, translateTo = TranslateTo.TOP_RIGHT),
                        dimensionDescriptor = DimensionDescriptor()
                    ),
                    OverlayFilterRender(applicationContext,
                        sourceDescriptor = SourceDescriptor(MatchRepository.mainBannerURL, MatchRepository.mainBannerVisible),
                        filterDescriptor = FilterDescriptor(maxFactor = 65f, translateTo = TranslateTo.BOTTOM),
                        dimensionDescriptor = DimensionDescriptor(500)
                        //rotatorDescriptor = RotatorDescriptor(targetWidthDp = 250)
                    )
                )
        }
    }
}