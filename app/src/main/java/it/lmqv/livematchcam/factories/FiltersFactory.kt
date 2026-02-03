package it.lmqv.livematchcam.factories

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
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

        var sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        val defaultScoreBoardSize = applicationContext.resources.getInteger(R.integer.default_scoreboard_size)
        val scoreboardKey = applicationContext.resources.getString(R.string.scoreboard_size_key)
        val scoreboardSize = sharedPreferences.getString(scoreboardKey, defaultScoreBoardSize.toString())?.toFloatOrNull() ?: defaultScoreBoardSize.toFloat()

        val defaultSpotBannerValue = applicationContext.resources.getInteger(R.integer.default_spot_banner_size)
        val spotBannerSizeKey = applicationContext.resources.getString(R.string.spot_banner_size_key)
        val spotBannerSize = sharedPreferences.getString(spotBannerSizeKey, defaultSpotBannerValue.toString())?.toFloatOrNull() ?: defaultSpotBannerValue.toFloat()

        val defaultMainBannerValue = applicationContext.resources.getInteger(R.integer.default_main_banner_size)
        val mainBannerSizeKey = applicationContext.resources.getString(R.string.main_banner_size_key)
        val mainBannerSize = sharedPreferences.getString(mainBannerSizeKey, defaultMainBannerValue.toString())?.toFloatOrNull() ?: defaultMainBannerValue.toFloat()

        return when (sport) {
            Sports.SOCCER ->
                listOf(
                    SoccerScoreboardViewFilterRender(applicationContext,
                        filterDescriptor = FilterDescriptor(maxFactor = scoreboardSize + 10f, translateTo = TranslateTo.TOP_LEFT))
                )
            Sports.BASKET ->
                listOf(
                    BasketScoreboardViewFilterRender(applicationContext,
                        filterDescriptor = FilterDescriptor(maxFactor = scoreboardSize + 5f, translateTo = TranslateTo.BOTTOM)),
                    OverlayFilterRender(applicationContext,
                        sourceDescriptor = SourceDescriptor(MatchRepository.spotBannerURL, MatchRepository.spotBannerVisible),
                        filterDescriptor = FilterDescriptor(maxFactor = spotBannerSize, translateTo = TranslateTo.TOP_RIGHT),
                        dimensionDescriptor = DimensionDescriptor()
                    ),
                    OverlayFilterRender(applicationContext,
                        sourceDescriptor = SourceDescriptor(MatchRepository.mainBannerURL, MatchRepository.mainBannerVisible),
                        filterDescriptor = FilterDescriptor(maxFactor = mainBannerSize, translateTo = TranslateTo.CENTER),
                        dimensionDescriptor = DimensionDescriptor(500)
                        //rotatorDescriptor = RotatorDescriptor(targetWidthDp = 250)
                    )
                )
            Sports.VOLLEY ->
                listOf(
                    VolleyScoreboardViewFilterRender(applicationContext,
                        filterDescriptor = FilterDescriptor(maxFactor = scoreboardSize, translateTo = TranslateTo.TOP_LEFT)
                    ),
                    OverlayFilterRender(applicationContext,
                        sourceDescriptor = SourceDescriptor(MatchRepository.spotBannerURL, MatchRepository.spotBannerVisible),
                        filterDescriptor = FilterDescriptor(maxFactor = spotBannerSize, translateTo = TranslateTo.TOP_RIGHT),
                        dimensionDescriptor = DimensionDescriptor()
                    ),
                    OverlayFilterRender(applicationContext,
                        sourceDescriptor = SourceDescriptor(MatchRepository.mainBannerURL, MatchRepository.mainBannerVisible),
                        filterDescriptor = FilterDescriptor(maxFactor = mainBannerSize, translateTo = TranslateTo.CENTER),
                        dimensionDescriptor = DimensionDescriptor(500)
                        //rotatorDescriptor = RotatorDescriptor(targetWidthDp = 250)
                    )
                )
        }
    }
}