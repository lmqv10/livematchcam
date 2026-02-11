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
import it.lmqv.livematchcam.R

object FiltersFactory {

    fun get(sport: Sports, applicationContext: Context) : List<BaseObjectFilterRender> {

        val defaultScoreBoardSize = applicationContext.resources.getInteger(R.integer.default_scoreboard_size).toFloat()
        val scoreboardKey = applicationContext.resources.getString(R.string.scoreboard_size_key)

        return when (sport) {
            Sports.SOCCER ->
                listOf(
                    SoccerScoreboardViewFilterRender(applicationContext,
                        filterDescriptor = FilterDescriptor(
                            defaultSize = 10f + defaultScoreBoardSize,
                            preferencesSizeKey = scoreboardKey,
                            translateTo = TranslateTo.TOP_LEFT))
                )
            Sports.BASKET ->
                listOf(
                    BasketScoreboardViewFilterRender(applicationContext,
                        filterDescriptor = FilterDescriptor(
                            defaultSize = 10f + defaultScoreBoardSize,
                            preferencesSizeKey = scoreboardKey,
                            translateTo = TranslateTo.BOTTOM)),
                    OverlayFilterRender(applicationContext,
                        sourceDescriptor = SourceDescriptor(
                            url = MatchRepository.spotBannerURL,
                            isVisible = MatchRepository.spotBannerVisible),
                        filterDescriptor = FilterDescriptor(
                            defaultSize = applicationContext.resources.getInteger(R.integer.default_spot_banner_size).toFloat(),
                            preferencesSizeKey = applicationContext.resources.getString(R.string.spot_banner_size_key),
                            translateTo = TranslateTo.TOP_RIGHT),
                        dimensionDescriptor = DimensionDescriptor()
                    ),
                    OverlayFilterRender(applicationContext,
                        sourceDescriptor = SourceDescriptor(
                            url = MatchRepository.mainBannerURL,
                            isVisible = MatchRepository.mainBannerVisible),
                        filterDescriptor = FilterDescriptor(
                            defaultSize = applicationContext.resources.getInteger(R.integer.default_main_banner_size).toFloat(),
                            preferencesSizeKey = applicationContext.resources.getString(R.string.main_banner_size_key),
                            translateTo = TranslateTo.CENTER),
                        dimensionDescriptor = DimensionDescriptor(500)
                        //rotatorDescriptor = RotatorDescriptor(targetWidthDp = 250)
                    )
                )
            Sports.VOLLEY ->
                listOf(
                    VolleyScoreboardViewFilterRender(applicationContext,
                        filterDescriptor = FilterDescriptor(
                            defaultSize = defaultScoreBoardSize,
                            preferencesSizeKey = scoreboardKey,
                            translateTo = TranslateTo.TOP_LEFT)
                    ),
                    OverlayFilterRender(applicationContext,
                        sourceDescriptor = SourceDescriptor(
                            url = MatchRepository.spotBannerURL,
                            isVisible = MatchRepository.spotBannerVisible),
                        filterDescriptor = FilterDescriptor(
                            defaultSize = applicationContext.resources.getInteger(R.integer.default_spot_banner_size).toFloat(),
                            preferencesSizeKey = applicationContext.resources.getString(R.string.spot_banner_size_key),
                            translateTo = TranslateTo.TOP_RIGHT),
                        dimensionDescriptor = DimensionDescriptor()
                    ),
                    OverlayFilterRender(applicationContext,
                        sourceDescriptor = SourceDescriptor(
                            url = MatchRepository.mainBannerURL,
                            isVisible = MatchRepository.mainBannerVisible),
                        filterDescriptor = FilterDescriptor(
                            defaultSize = applicationContext.resources.getInteger(R.integer.default_main_banner_size).toFloat(),
                            preferencesSizeKey = applicationContext.resources.getString(R.string.main_banner_size_key),
                            translateTo = TranslateTo.CENTER),
                        dimensionDescriptor = DimensionDescriptor(500)
                        //rotatorDescriptor = RotatorDescriptor(targetWidthDp = 250)
                    )
                )
        }
    }
}