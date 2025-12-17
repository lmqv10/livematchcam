package it.lmqv.livematchcam.factories

import android.content.Context
import com.pedro.encoder.utils.gl.TranslateTo
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.services.stream.filters.BitmapObjectFilterRender
import it.lmqv.livematchcam.services.stream.filters.BitmapRotatorFilterRender
import it.lmqv.livematchcam.services.stream.filters.FilterDescriptor
import it.lmqv.livematchcam.services.stream.filters.RotatorDescriptor
import it.lmqv.livematchcam.services.stream.filters.SoccerScoreboardViewFilterRender
import it.lmqv.livematchcam.services.stream.filters.SourceDescriptor
import it.lmqv.livematchcam.services.stream.filters.VolleyScoreboardViewFilterRender

object FiltersFactory {
    fun get(sport: Sports, context: Context) : List<BitmapObjectFilterRender> {
        return when (sport) {
            Sports.SOCCER ->
                listOf(
                    SoccerScoreboardViewFilterRender(
                        context,
                        filterDescriptor = FilterDescriptor(maxFactor = 30f, translateTo = TranslateTo.TOP_LEFT))
                )
            Sports.VOLLEY ->
                listOf(
                    VolleyScoreboardViewFilterRender(
                        context,
                        filterDescriptor = FilterDescriptor(maxFactor = 30f, translateTo = TranslateTo.TOP_LEFT)
                    ),
                    BitmapRotatorFilterRender(
                        context,
                        sourceDescriptor = SourceDescriptor(MatchRepository.spotBannerURL, MatchRepository.spotBannerVisible),
                        filterDescriptor = FilterDescriptor(maxFactor = 25f, translateTo = TranslateTo.TOP_RIGHT),
                        rotatorDescriptor = RotatorDescriptor()
                    ),
                    BitmapRotatorFilterRender(
                        context,
                        sourceDescriptor = SourceDescriptor(MatchRepository.mainBannerURL, MatchRepository.mainBannerVisible),
                        filterDescriptor = FilterDescriptor(maxFactor = 70f, translateTo = TranslateTo.CENTER),
                        rotatorDescriptor = RotatorDescriptor(targetWidthDp = 300)
                    )
                )
            //VideoSourceKind.UVC_CAMERA -> listOf()
        }
    }
}