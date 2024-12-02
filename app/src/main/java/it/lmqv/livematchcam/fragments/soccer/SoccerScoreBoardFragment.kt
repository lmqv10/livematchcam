package it.lmqv.livematchcam.fragments.soccer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import it.lmqv.livematchcam.databinding.FragmentSoccerScoreBoardBinding
import it.lmqv.livematchcam.extensions.formatTime
import it.lmqv.livematchcam.extensions.setShirtByColor
import it.lmqv.livematchcam.fragments.BaseScoreBoardFragment

class SoccerScoreBoardFragment : BaseScoreBoardFragment() {

    companion object {
        fun newInstance() = SoccerScoreBoardFragment()
    }

    private var _binding: FragmentSoccerScoreBoardBinding? = null
    private val binding get() = _binding!!

    private var currentPeriod = "1T"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSoccerScoreBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeTeamViewModel.name.observe(viewLifecycleOwner) { team ->
            binding.homeTeam.text = team
            onUpdateCallback?.refresh()
        }
        homeTeamViewModel.score.observe(viewLifecycleOwner) { score ->
            binding.homeScore.text = score.toString()
            onUpdateCallback?.refresh()
        }

        awayTeamViewModel.name.observe(viewLifecycleOwner) { team ->
            binding.awayTeam.text = team
            onUpdateCallback?.refresh()
        }
        awayTeamViewModel.score.observe(viewLifecycleOwner) { score ->
            binding.awayScore.text = score.toString()
            onUpdateCallback?.refresh()
        }

        homeTeamViewModel.logo.observe(viewLifecycleOwner) { color ->
            binding.homeLogo.setShirtByColor(color)
            onUpdateCallback?.refresh()
        }
        awayTeamViewModel.logo.observe(viewLifecycleOwner) { color ->
            binding.awayLogo.setShirtByColor(color)
            onUpdateCallback?.refresh()
        }
    }

    override fun onTickTimer(timeElapsedInSeconds: Int) {
        binding.matchTime.text = formatTime(timeElapsedInSeconds)
    }

    override fun togglePeriod() {
        currentPeriod = if (currentPeriod == "1T") "2T" else "1T"
        binding.matchPeriod.text = currentPeriod
        onUpdateCallback?.refresh()
    }
}