package it.lmqv.livematchcam.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import it.lmqv.livematchcam.databinding.FragmentVolleyScoreBoardBinding
import it.lmqv.livematchcam.extensions.setShirtByColor

class VolleyScoreBoardFragment : BaseScoreBoardFragment() {

    companion object {
        fun newInstance() = VolleyScoreBoardFragment()
    }

    private var _binding: FragmentVolleyScoreBoardBinding? = null
    private val binding get() = _binding!!

    private var currentPeriod = "1T"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVolleyScoreBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeTeamViewModel.name.observe(viewLifecycleOwner) { team ->
            binding.homeTeam.text = team
        }
        homeTeamViewModel.score.observe(viewLifecycleOwner) { score ->
            //binding.homeScore.text = score.toString()
            onUpdateCallback?.refresh()
        }

        awayTeamViewModel.name.observe(viewLifecycleOwner) { team ->
            binding.awayTeam.text = team
        }
        awayTeamViewModel.score.observe(viewLifecycleOwner) { score ->
            //binding.awayScore.text = score.toString()
            onUpdateCallback?.refresh()
        }

        homeTeamViewModel.logo.observe(viewLifecycleOwner) { color ->
            binding.homeLogo.setShirtByColor(color)
        }
        awayTeamViewModel.logo.observe(viewLifecycleOwner) { color ->
            binding.awayLogo.setShirtByColor(color)
        }
    }

    override fun togglePeriod() {
        currentPeriod = if (currentPeriod == "1T") "2T" else "1T"
        //binding.matchPeriod.text = currentPeriod
        super.togglePeriod()
    }
}