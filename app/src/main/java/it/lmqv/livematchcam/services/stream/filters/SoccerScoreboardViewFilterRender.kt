package it.lmqv.livematchcam.services.stream.filters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.core.graphics.toColorInt
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentSoccerScoreBoardLightBinding
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.extensions.formatTime
import it.lmqv.livematchcam.extensions.loadDrawableOffscreen
import it.lmqv.livematchcam.extensions.setShirtByColor
import it.lmqv.livematchcam.services.firebase.IScore
import it.lmqv.livematchcam.services.firebase.Match
import it.lmqv.livematchcam.services.firebase.SoccerScore
import it.lmqv.livematchcam.services.counter.CounterServiceConnector
import it.lmqv.livematchcam.services.counter.ICounterListener
import it.lmqv.livematchcam.viewmodels.Command
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SoccerScoreboardViewFilterRender(applicationContext: Context)
    : ScoreboardViewFilterRender<FragmentSoccerScoreBoardLightBinding>(applicationContext), ICounterListener {

    private var previousHomeLogo: String? = null
    private var previousAwayLogo: String? = null

    private val serviceConnector: CounterServiceConnector

    init {
        val inflater = LayoutInflater.from(applicationContext)
        _binding = FragmentSoccerScoreBoardLightBinding.inflate(inflater)

        serviceConnector = CounterServiceConnector(applicationContext)
        serviceConnector.setCounterListener(this)
    }

    override fun onTick(timeElapsedInSeconds: Int) {
        binding.matchTime.text = formatTime(timeElapsedInSeconds)
        updateContentView()
    }

    override fun release() {
        super.release()
        //Logd("SoccerScoreboardViewRenderer::release")
//        serviceConnector.stopCounter()
//        serviceConnector.setCounter(0)
        serviceConnector.unbind(applicationContext)
        _binding = null
    }

    @Synchronized
    override fun match(match: Match) {
        try {
            //Logd("SoccerScoreboardViewRenderer::match $match")
            this.handleHomeTeam(match)
            this.handleAwayTeam(match)

            updateContentView()
        } catch (e: Exception) {
            e.printStackTrace()
            Loge("SoccerScoreboardViewRenderer::match Exception:: ${e.message.toString()}")
        }
    }

    @Synchronized
    override fun score(score: IScore) {
        try {
            //Logd("SoccerScoreboardViewRenderer::score $score")
            this.handleScore(score as SoccerScore)
            this.handleCommand(score.command)

            updateContentView()
        } catch (e: Exception) {
            e.printStackTrace()
            Loge("SoccerScoreboardViewRenderer::score Exception:: ${e.message.toString()}")
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
                    updateContentView()
                }
            } else {
                binding.homeLogo.visibility = View.GONE
                binding.homeColorBar.visibility = View.GONE
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
                    updateContentView()
                }
            } else {
                binding.awayLogo.visibility = View.GONE
                binding.awayColorBar.visibility = View.GONE
                binding.awayShirt.visibility = View.VISIBLE
                binding.awayShirt.setShirtByColor(awayColorHex)
            }
        }
    }

    private fun handleScore(score: SoccerScore) {
        binding.homeScore.text = score.home.toString()
        binding.awayScore.text = score.away.toString()
        binding.matchPeriod.text = score.period
    }

    private fun handleCommand(command: String) {
        if (command == Command.START_TIME.toString()) {
            if (!serviceConnector.isRunning()) {
                serviceConnector.startCounter()
            }
        }
        if (command == Command.PAUSE.toString()) {
            if (serviceConnector.isRunning()) {
                serviceConnector.pauseCounter()
            }
        }
        if (command == Command.RESET_TIME.toString()) {
            if (!serviceConnector.isRunning()) {
                serviceConnector.setCounter(0)
            }
        }
    }
}