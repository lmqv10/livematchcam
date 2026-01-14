package it.lmqv.livematchcam.services.stream.filters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentVolleyScoreBoardBinding
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.extensions.equalizeMaxWidthWith
import it.lmqv.livematchcam.extensions.loadDrawableOffscreen
import it.lmqv.livematchcam.extensions.setShirtByColor
import it.lmqv.livematchcam.extensions.wrapLayout
import it.lmqv.livematchcam.services.firebase.IScore
import it.lmqv.livematchcam.services.firebase.Match
import it.lmqv.livematchcam.services.firebase.SetScore
import it.lmqv.livematchcam.services.firebase.VolleyScore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

class VolleyScoreboardViewFilterRender(
    applicationContext: Context,
    filterDescriptor: FilterDescriptor = FilterDescriptor()
) : ScoreboardViewFilterRender<FragmentVolleyScoreBoardBinding>(applicationContext, filterDescriptor) {

    private var previousHomeLogo: String? = null
    private var previousAwayLogo: String? = null

    private var setsControls: List<Pair<TextView, TextView>> = listOf()
    private var _previousSetsSize = 0
    private var _previousDescription : String? = null

    init {
        val inflater = LayoutInflater.from(applicationContext)
        _binding = FragmentVolleyScoreBoardBinding.inflate(inflater)

        this.setsControls = listOf(
            Pair(binding.homeScore1set, binding.awayScore1set),
            Pair(binding.homeScore2set, binding.awayScore2set),
            Pair(binding.homeScore3set, binding.awayScore3set),
            Pair(binding.homeScore4set, binding.awayScore4set),
            Pair(binding.homeScore5set, binding.awayScore5set),
        )
    }

    @Synchronized
    override fun match(match: Match) {
        try {
            //Logd("VolleyScoreboardViewFilterRender::match $match")
            this.handleHomeTeam(match)
            this.handleAwayTeam(match)

            this.updateTeamsView()

            super.render()
        } catch (e: Exception) {
            e.printStackTrace()
            Loge("VolleyScoreboardViewFilterRender::match Exception:: ${e.message.toString()}")
        }
    }

    @Synchronized
    override fun score(score: IScore) {
        try {
            //Logd("VolleyScoreboardViewFilterRender::score $score")
            val score = score as VolleyScore
            updateScore(score.sets)
            updateLeagueDescription(score.league)
            super.render()
        } catch (e: Exception) {
            e.printStackTrace()
            Loge("VolleyScoreboardViewFilterRender::score Exception:: ${e.message.toString()}")
        }
    }

    private fun handleHomeTeam(match: Match) {
        binding.homeTeam.text = match.homeTeam

        var homeColorHex = match.homePrimaryColorHex.toColorInt()
        binding.homeColorBar.setBackgroundColor(homeColorHex)
        binding.homeShirt.setShirtByColor(homeColorHex)

        if (match.homeLogo != this.previousHomeLogo) {
            //Logd("SoccerScoreboardViewRenderer::match.homeLogo ${match.homeLogo}")
            this.previousHomeLogo = match.homeLogo

            if (match.homeLogo.isNotEmpty()) {
                binding.homeLogo.visibility = View.VISIBLE
                binding.homeColorBar.visibility = View.VISIBLE
                binding.homeShirt.visibility = View.INVISIBLE

                CoroutineScope(Dispatchers.Default).launch {
                    val drawable =
                        loadDrawableOffscreen(applicationContext, match.homeLogo, R.drawable.shield)
                    binding.homeLogo.setImageDrawable(drawable)
                    super.render()
                }
            } else {
                binding.homeLogo.visibility = View.GONE
                binding.homeColorBar.visibility = View.INVISIBLE
                binding.homeShirt.visibility = View.VISIBLE
                binding.homeShirt.setShirtByColor(homeColorHex)
            }
        }
    }

    private fun handleAwayTeam(match: Match) {
        binding.awayTeam.text = match.guestTeam

        var awayColorHex = match.guestPrimaryColorHex.toColorInt()
        binding.awayColorBar.setBackgroundColor(awayColorHex)
        binding.awayShirt.setShirtByColor(awayColorHex)

        if (match.guestLogo != this.previousAwayLogo) {
            //Logd("SoccerScoreboardViewRenderer::match.guestLogo ${match.guestLogo}")
            this.previousAwayLogo = match.guestLogo

            if (match.guestLogo.isNotEmpty()) {
                binding.awayLogo.visibility = View.VISIBLE
                binding.awayColorBar.visibility = View.VISIBLE
                binding.awayShirt.visibility = View.INVISIBLE

                CoroutineScope(Dispatchers.Default).launch {
                    val drawable = loadDrawableOffscreen(
                        applicationContext,
                        match.guestLogo,
                        R.drawable.shield
                    )
                    binding.awayLogo.setImageDrawable(drawable)
                    super.render()
                }
            } else {
                binding.awayLogo.visibility = View.GONE
                binding.awayColorBar.visibility = View.INVISIBLE
                binding.awayShirt.visibility = View.VISIBLE
                binding.awayShirt.setShirtByColor(awayColorHex)
            }
        }
    }

    private fun updateScore(sets: List<SetScore>) {
        val setsSize = sets.size

        setsControls.forEachIndexed { index, controls ->
            if (index < setsSize) {
                var score = sets[index]
                controls.first.text = score.home.toString()
                controls.second.text = score.guest.toString()

                controls.first.visibility = View.VISIBLE
                controls.second.visibility = View.VISIBLE

                var winColor = ContextCompat.getColor(applicationContext, R.color.secondary_dark)
                val baseColor = ContextCompat.getColor(applicationContext, R.color.BLACK)

                var setTarget = if (setsSize < 5) 25 else 15
                val isEndedSet = index + 1 != setsSize ||
                        ((score.home >= setTarget || score.guest >= setTarget) &&
                        abs(score.home - score.guest) >= 2)

                if (isEndedSet) {
                    if (score.home > score.guest) {
                        controls.first.setTextColor(winColor)
                        controls.second.setTextColor(baseColor)
                    } else if (score.home < score.guest) {
                        controls.first.setTextColor(baseColor)
                        controls.second.setTextColor(winColor)
                    } else {
                        controls.first.setTextColor(baseColor)
                        controls.second.setTextColor(baseColor)
                    }
                } else {
                    controls.first.setTextColor(baseColor)
                    controls.second.setTextColor(baseColor)
                }
            } else {
                controls.first.visibility = View.GONE
                controls.second.visibility = View.GONE
            }
        }

        if (this._previousSetsSize != setsSize) {
            this._previousSetsSize = setsSize
            updateView()
        }
    }

    private fun updateLeagueDescription(description: String) {
        if (this._previousDescription != description) {
            this._previousDescription = description

            if (description == "") {
                binding.matchDescription.visibility = View.GONE
            } else {
                binding.matchDescription.visibility = View.VISIBLE
            }
            binding.matchDescription.text = description

            updateView()
        }
    }

    private fun updateTeamsView() {
        //Logd("VolleyScoreboardViewFilterRender :: updateTeamsView")
        with(binding) {
            homeTeam.equalizeMaxWidthWith(awayTeam)
            homeTeamBar.equalizeMaxWidthWith(awayTeamBar)
            updateView()
        }
    }

    private fun updateView() {
        //Logd("VolleyScoreboardViewFilterRender :: updateView")
        binding.scoreBoard.wrapLayout()
    }
}