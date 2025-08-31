package it.lmqv.livematchcam.fragments.soccer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.load
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentSoccerScoreBoardLightBinding
import it.lmqv.livematchcam.extensions.formatTime
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.extensions.setShirtByColor
import it.lmqv.livematchcam.fragments.BaseScoreBoardFragment
import kotlinx.coroutines.flow.combine
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.services.firebase.SoccerScore
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.viewmodels.Command
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SoccerScoreBoardFragment : BaseScoreBoardFragment() {

    companion object {
        fun newInstance() = SoccerScoreBoardFragment()
    }

    private var _binding: FragmentSoccerScoreBoardLightBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSoccerScoreBoardLightBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MatchRepository.homeTeam.observe(viewLifecycleOwner) { team ->
            binding.homeTeam.text = team
            onUpdateCallback?.refresh()
        }

        MatchRepository.guestTeam.observe(viewLifecycleOwner) { team ->
            binding.awayTeam.text = team
            onUpdateCallback?.refresh()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            MatchRepository.score.collectLatest { scoreInstance ->
                //Logd("SoccerScoreBoard::${scoreInstance}")
                try {
                    val score = scoreInstance as SoccerScore
                    binding.homeScore.text = score.home.toString()
                    binding.awayScore.text = score.away.toString()
                    binding.matchPeriod.text = score.period

                    val command = score.command
                    if (command == Command.START_TIME.toString()) {
                        if (!isStarted()) {
                            startTime()
                        }
                    }
                    if (command == Command.PAUSE.toString()) {
                        if (isStarted()) {
                            pauseTime()
                        }
                    }
                    if (command == Command.RESET_TIME.toString()) {
                        if (!isStarted()) {
                            resetTime()
                        }
                    }
                    onUpdateCallback?.refresh()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Loge("SoccerScoreBoard::Exception:: ${e.message.toString()}")
                }
            }
        }

        launchOnStarted {
            combine(
                MatchRepository.homePrimaryColorHex,
                MatchRepository.homeLogo) {
                    color, logo -> Pair(color, logo)
            }.collect { (colorHex, logoURL) ->
                binding.homeColorBar.setBackgroundColor(colorHex.toColorInt())

                if (logoURL.isNotEmpty()) {
                    binding.homeLogo.visibility = View.VISIBLE
                    binding.homeColorBar.visibility = View.VISIBLE
                    binding.homeShirt.visibility = View.INVISIBLE
                    binding.homeLogo.load(logoURL) {
                        placeholder(R.drawable.shirt_white)
                        error(R.drawable.shirt_white)
                        allowHardware(false)
                        listener(
                            onSuccess = { _, _ ->
                                onUpdateCallback?.refresh()
                            }
                        )
                    }
                } else {
                    binding.homeLogo.visibility = View.GONE
                    binding.homeColorBar.visibility = View.GONE
                    binding.homeShirt.visibility = View.VISIBLE
                    binding.homeShirt.setShirtByColor(colorHex.toColorInt())
                    onUpdateCallback?.refresh()
                }
            }
        }

        launchOnStarted {
            combine(
                MatchRepository.guestPrimaryColorHex,
                MatchRepository.guestLogo) {
                    color, logo -> Pair(color, logo)
            }.collect { (colorHex, logoURL) ->
                binding.awayColorBar.setBackgroundColor(colorHex.toColorInt())

                if (logoURL.isNotEmpty()) {
                    binding.awayLogo.visibility = View.VISIBLE
                    binding.awayColorBar.visibility = View.VISIBLE
                    binding.awayShirt.visibility = View.INVISIBLE
                    binding.awayLogo.load(logoURL) {
                        placeholder(R.drawable.shirt_white)
                        error(R.drawable.shirt_white)
                        allowHardware(false)
                        listener(
                            onSuccess = { _, _ ->
                                onUpdateCallback?.refresh()
                            }
                        )
                    }
                } else {
                    binding.awayLogo.visibility = View.GONE
                    binding.awayColorBar.visibility = View.GONE
                    binding.awayShirt.visibility = View.VISIBLE
                    binding.awayShirt.setShirtByColor(colorHex.toColorInt())
                    onUpdateCallback?.refresh()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onTickTimer(timeElapsedInSeconds: Int) {
        binding.matchTime.text = formatTime(timeElapsedInSeconds)
    }
}