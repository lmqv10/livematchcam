package it.lmqv.livematchcam.fragments.soccer

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentSoccerControlBarBinding
import it.lmqv.livematchcam.extensions.Logd
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

        matchViewModel.homeTeam.observe(viewLifecycleOwner) { team ->
            binding.homeTeam.text = team
        }
        matchViewModel.guestTeam.observe(viewLifecycleOwner) { team ->
            binding.awayTeam.text = team
        }

        matchViewModel.homeScore.observe(viewLifecycleOwner) { score ->
            binding.homeScore.text = score.toString()
        }
        matchViewModel.guestScore.observe(viewLifecycleOwner) { score ->
            binding.awayScore.text = score.toString()
        }

        matchViewModel.homeColorHex.observe(viewLifecycleOwner) { homeColorHex ->
            binding.homeColor.setShirtByColor(Color.parseColor(homeColorHex))
        }
        matchViewModel.guestColorHex.observe(viewLifecycleOwner) { guestColorHex ->
            binding.awayColor.setShirtByColor(Color.parseColor(guestColorHex))
        }

        //Logd("SoccerControlBarFragment::matchViewModelID: ${matchViewModel.instanceId}")
        binding.homeScoreMinus.setOnClickListener {
            matchViewModel.incrementHomeScore(-1)
        }

        binding.homeScoreAdd.setOnClickListener {
            matchViewModel.incrementHomeScore()
        }

        binding.awayScoreMinus.setOnClickListener {
            matchViewModel.incrementGuestScore(-1)
        }

        binding.awayScoreAdd.setOnClickListener {
            matchViewModel.incrementGuestScore()
        }

        binding.homeColor.setOnClickListener {
            requireContext().showColorPickerDialog { color ->
                matchViewModel.setHomeColorHex(color)
            }
        }

        binding.awayColor.setOnClickListener {
            requireContext().showColorPickerDialog { color ->
                matchViewModel.setGuestColorHex(color)
            }
        }

        binding.homeTeam.setOnClickListener {
            var teamName = binding.homeTeam.text.toString()
            requireContext().showEditStringDialog(R.string.team_name, teamName) { updatedTeamName ->
                matchViewModel.setHomeTeam(updatedTeamName)
                requireActivity().hideSystemUI()
            }
        }

        binding.awayTeam.setOnClickListener {
            var teamName = binding.awayTeam.text.toString()
            requireContext().showEditStringDialog(R.string.team_name, teamName) { updatedTeamName ->
                matchViewModel.setGuestTeam(updatedTeamName)
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