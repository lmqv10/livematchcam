package it.lmqv.livematchcam.fragments.soccer

import android.graphics.Color
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
import it.lmqv.livematchcam.firebase.SoccerScore
import it.lmqv.livematchcam.fragments.BaseScoreBoardFragment
import it.lmqv.livematchcam.viewmodels.Command
import kotlinx.coroutines.flow.combine

class SoccerScoreBoardFragment : BaseScoreBoardFragment() {

    companion object {
        fun newInstance() = SoccerScoreBoardFragment()
    }

    private var _binding: FragmentSoccerScoreBoardLightBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSoccerScoreBoardLightBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        matchViewModel.homeTeam.observe(viewLifecycleOwner) { team ->
            binding.homeTeam.text = team
            onUpdateCallback?.refresh()
        }

        matchViewModel.guestTeam.observe(viewLifecycleOwner) { team ->
            binding.awayTeam.text = team
            onUpdateCallback?.refresh()
        }

        matchViewModel.score.observe(viewLifecycleOwner) { scoreInstance ->
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
            } catch (e: Exception) { }
            onUpdateCallback?.refresh()
        }

        launchOnStarted {
            combine(
                matchViewModel.homeColorHex,
                matchViewModel.homeLogo) {
                    color, logo -> Pair(color, logo)
            }.collect { (colorHex, logoURL) ->
                binding.homeColorBar.setBackgroundColor(Color.parseColor(colorHex))

                if (!logoURL.isNullOrEmpty()) {
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
                    binding.homeShirt.setShirtByColor(Color.parseColor(colorHex))
                    onUpdateCallback?.refresh()
                }
            }
        }

        launchOnStarted {
            combine(
                matchViewModel.guestColorHex,
                matchViewModel.guestLogo) {
                    color, logo -> Pair(color, logo)
            }.collect { (colorHex, logoURL) ->
                binding.awayColorBar.setBackgroundColor(Color.parseColor(colorHex))

                if (!logoURL.isNullOrEmpty()) {
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
                    binding.awayShirt.setShirtByColor(Color.parseColor(colorHex))
                    onUpdateCallback?.refresh()
                }
            }
        }
    }

    override fun onTickTimer(timeElapsedInSeconds: Int) {
        binding.matchTime.text = formatTime(timeElapsedInSeconds)
    }
}