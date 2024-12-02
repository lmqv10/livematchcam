package it.lmqv.livematchcam.fragments.soccer

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentSoccerControlBarBinding
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.extensions.setShirtByColor
import it.lmqv.livematchcam.extensions.showColorPickerDialog
import it.lmqv.livematchcam.extensions.showEditStringDialog
import it.lmqv.livematchcam.fragments.BaseControlBarFragment

class SoccerControlBarFragment(private val scoreBoardFragment: SoccerScoreBoardFragment) : BaseControlBarFragment() {
    companion object {
        fun newInstance(scoreBoardFragment: SoccerScoreBoardFragment) = SoccerControlBarFragment(scoreBoardFragment)
    }

    private var _binding: FragmentSoccerControlBarBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSoccerControlBarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeTeamViewModel.name.observe(viewLifecycleOwner) { team ->
            binding.homeTeam.text = Editable.Factory.getInstance().newEditable(team);
        }
        homeTeamViewModel.score.observe(viewLifecycleOwner) { score ->
            binding.homeScore.text = score.toString()
        }

        awayTeamViewModel.name.observe(viewLifecycleOwner) { team ->
            binding.awayTeam.text = team
        }

        awayTeamViewModel.score.observe(viewLifecycleOwner) { score ->
            binding.awayScore.text = score.toString()
        }

        homeTeamViewModel.logo.observe(viewLifecycleOwner) { color ->
            binding.homeColor.setShirtByColor(color)
        }
        awayTeamViewModel.logo.observe(viewLifecycleOwner) { color ->
            binding.awayColor.setShirtByColor(color)
        }

        binding.homeScoreMinus.setOnClickListener {
            homeTeamViewModel.incrementScore(-1)
        }

        binding.homeScoreAdd.setOnClickListener {
            homeTeamViewModel.incrementScore()
        }

        binding.awayScoreMinus.setOnClickListener {
            awayTeamViewModel.incrementScore(-1)
        }

        binding.awayScoreAdd.setOnClickListener {
            awayTeamViewModel.incrementScore()
        }

        binding.homeColor.setOnClickListener {
            requireContext().showColorPickerDialog { color ->
                homeTeamViewModel.setLogo(color)
            }
        }

        binding.awayColor.setOnClickListener {
            requireContext().showColorPickerDialog { color ->
                awayTeamViewModel.setLogo(color)
            }
        }

        binding.homeTeam.setOnClickListener {
            var teamName = binding.homeTeam.text.toString()
            requireContext().showEditStringDialog(R.string.team_name, teamName) { updatedTeamName ->
                homeTeamViewModel.setName(updatedTeamName)
                requireActivity().hideSystemUI()
            }
        }

        binding.awayTeam.setOnClickListener {
            var teamName = binding.awayTeam.text.toString()
            requireContext().showEditStringDialog(R.string.team_name, teamName) { updatedTeamName ->
                awayTeamViewModel.setName(updatedTeamName)
                requireActivity().hideSystemUI()
            }
        }

        binding.changePeriod.setOnClickListener {
            scoreBoardFragment.togglePeriod()
        }

        binding.startTime.setOnClickListener {
            if (scoreBoardFragment.isStarted()) {
                scoreBoardFragment.pauseTime()
                binding.startTime.setImageResource(R.drawable.time_start)
            } else {
                scoreBoardFragment.startTime()
                binding.startTime.setImageResource(R.drawable.time_pause)
            }
            binding.resetTime.isEnabled = !scoreBoardFragment.isStarted()
        }

        binding.resetTime.setOnClickListener {
            scoreBoardFragment.resetTime()
        }
    }
}