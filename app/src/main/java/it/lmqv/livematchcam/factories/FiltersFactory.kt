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

    /*
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
                            translateTo = TranslateTo.TOP_RIGHT)
                    ),
                    OverlayFilterRender(applicationContext,
                        sourceDescriptor = SourceDescriptor(
                            url = MatchRepository.mainBannerURL,
                            isVisible = MatchRepository.mainBannerVisible),
                        filterDescriptor = FilterDescriptor(
                            defaultSize = applicationContext.resources.getInteger(R.integer.default_main_banner_size).toFloat(),
                            preferencesSizeKey = applicationContext.resources.getString(R.string.main_banner_size_key),
                            translateTo = TranslateTo.CENTER)
                        //rotatorDescriptor = RotatorDescriptor(targetWidthDp = 250)
                    )
                )
            Sports.VOLLEY ->
                listOf(
                    OverlayFilterRender(applicationContext,
                        sourceDescriptor = SourceDescriptor(
                            url = MatchRepository.spotBannerURL,
                            isVisible = MatchRepository.spotBannerVisible),
                        filterDescriptor = FilterDescriptor(
                            defaultSize = applicationContext.resources.getInteger(R.integer.default_spot_banner_size).toFloat(),
                            preferencesSizeKey = applicationContext.resources.getString(R.string.spot_banner_size_key),
                            translateTo = TranslateTo.TOP_RIGHT)
                    ),
                    OverlayFilterRender(applicationContext,
                        sourceDescriptor = SourceDescriptor(
                            url = MatchRepository.mainBannerURL,
                            isVisible = MatchRepository.mainBannerVisible),
                        filterDescriptor = FilterDescriptor(
                            defaultSize = applicationContext.resources.getInteger(R.integer.default_main_banner_size).toFloat(),
                            preferencesSizeKey = applicationContext.resources.getString(R.string.main_banner_size_key),
                            translateTo = TranslateTo.CENTER)
                        //rotatorDescriptor = RotatorDescriptor(targetWidthDp = 250)
                    ),
                    VolleyScoreboardViewFilterRender(applicationContext,
                        filterDescriptor = FilterDescriptor(
                            defaultSize = defaultScoreBoardSize,
                            preferencesSizeKey = scoreboardKey,
                            translateTo = TranslateTo.TOP_LEFT)
                    )
                )
        }
    }
    */
}