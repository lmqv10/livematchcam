package it.lmqv.livematchcam.fragments.soccer

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import it.lmqv.livematchcam.databinding.FragmentSoccerScoreBoardLightBinding
import it.lmqv.livematchcam.extensions.formatTime
import it.lmqv.livematchcam.extensions.setShirtByColor
import it.lmqv.livematchcam.firebase.SoccerScore
import it.lmqv.livematchcam.fragments.BaseScoreBoardFragment
import it.lmqv.livematchcam.viewmodels.Command

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

        /*matchViewModel.score.observe(viewLifecycleOwner) { score ->
            binding.homeScore.text = score.toString()
            binding.awayScore.text = score.toString()
            onUpdateCallback?.refresh()
        }*/

        /*matchViewModel.homeScore.observe(viewLifecycleOwner) { score ->
            binding.homeScore.text = score.toString()
            onUpdateCallback?.refresh()
        }
        matchViewModel.guestScore.observe(viewLifecycleOwner) { score ->
            binding.awayScore.text = score.toString()
            onUpdateCallback?.refresh()
        }*/

        matchViewModel.homeColorHex.observe(viewLifecycleOwner) { homeColorHex ->
            binding.homeLogo.setShirtByColor(Color.parseColor(homeColorHex))
            onUpdateCallback?.refresh()
        }
        matchViewModel.guestColorHex.observe(viewLifecycleOwner) { guestColorHex ->
            binding.awayLogo.setShirtByColor(Color.parseColor(guestColorHex))
            onUpdateCallback?.refresh()
        }
        //Logd("SoccerScoreBoardFragment::matchViewModelID: ${matchViewModel.instanceId}")
    }

    override fun onTickTimer(timeElapsedInSeconds: Int) {
        binding.matchTime.text = formatTime(timeElapsedInSeconds)
    }
}