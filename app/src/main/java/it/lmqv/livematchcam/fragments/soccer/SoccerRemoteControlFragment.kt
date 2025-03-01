package it.lmqv.livematchcam.fragments.soccer

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentSoccerControlBarBinding
import it.lmqv.livematchcam.databinding.FragmentSoccerRemoteControlBinding
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.formatTime
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.extensions.setShirtByColor
import it.lmqv.livematchcam.extensions.showColorPickerDialog
import it.lmqv.livematchcam.extensions.showEditStringDialog
import it.lmqv.livematchcam.firebase.SoccerScore
import it.lmqv.livematchcam.fragments.BaseControlBarFragment
import it.lmqv.livematchcam.fragments.BaseRemoteControlFragment
import it.lmqv.livematchcam.utils.TimerHandler
import it.lmqv.livematchcam.viewmodels.Command
import it.lmqv.livematchcam.viewmodels.SoccerScoreViewModel

class SoccerRemoteControlFragment : BaseRemoteControlFragment() {
    companion object {
        fun newInstance() = SoccerRemoteControlFragment()
    }
    private val soccerScoreViewModel: SoccerScoreViewModel by activityViewModels()

    private var _binding: FragmentSoccerRemoteControlBinding? = null
    private val binding get() = _binding!!

    private val timerHandler: TimerHandler = TimerHandler.newInstance { timeElapsedInSeconds ->
        binding.matchTime.text = formatTime(timeElapsedInSeconds)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSoccerRemoteControlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Logd("SoccerControlBarFragment::matchViewModelID: ${matchViewModel.instanceId}")
        matchViewModel.homeTeam.observe(viewLifecycleOwner) { team ->
            binding.homeTeam.text = team
        }
        matchViewModel.guestTeam.observe(viewLifecycleOwner) { team ->
            binding.awayTeam.text = team
        }

        matchViewModel.score.observe(viewLifecycleOwner) { scoreInstance ->
            val score = scoreInstance as? SoccerScore ?: SoccerScore()
            soccerScoreViewModel.initScore(score)
            binding.homeScore.text = score.home.toString()
            binding.awayScore.text = score.away.toString()
            binding.currentPeriod.text = score.period

            val command = score.command
            if (command == Command.START_TIME.toString()) {
                timerHandler.startTime()
                binding.startTime.visibility = View.GONE
                binding.stopTime.visibility = View.VISIBLE
                binding.resetTime.isEnabled = false
            }
            if (command == Command.PAUSE.toString()) {
                timerHandler.pauseTime()
                binding.startTime.visibility = View.VISIBLE
                binding.stopTime.visibility = View.GONE
                binding.resetTime.isEnabled = true
            }
            if (command == Command.RESET_TIME.toString()) {
                timerHandler.resetTime()
            }
        }

        matchViewModel.homeColorHex.observe(viewLifecycleOwner) { homeColorHex ->
            binding.homeColor.setShirtByColor(Color.parseColor(homeColorHex))
        }
        matchViewModel.guestColorHex.observe(viewLifecycleOwner) { guestColorHex ->
            binding.awayColor.setShirtByColor(Color.parseColor(guestColorHex))
        }

        soccerScoreViewModel.liveScore.observe(viewLifecycleOwner) { liveScore ->
            if (liveScore != null) {
                matchViewModel.setScore(liveScore)
            }
        }

        binding.homeScoreMinus.setOnClickListener {
            soccerScoreViewModel.incrementHomeScore(-1)
        }

        binding.homeScoreAdd.setOnClickListener {
            soccerScoreViewModel.incrementHomeScore()
        }

        binding.awayScoreMinus.setOnClickListener {
            soccerScoreViewModel.incrementGuestScore(-1)
        }

        binding.awayScoreAdd.setOnClickListener {
            soccerScoreViewModel.incrementGuestScore()
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
            soccerScoreViewModel.nextPeriod()
        }

        binding.startTime.setOnClickListener {
            soccerScoreViewModel.setCommand(Command.START_TIME)
        }

        binding.stopTime.setOnClickListener {
            soccerScoreViewModel.setCommand(Command.PAUSE)
        }

        binding.resetTime.setOnClickListener {
            soccerScoreViewModel.setCommand(Command.RESET_TIME)
        }

        binding.zoomIn.setOnClickListener {
            binding.currentZoom.text = "In"
            soccerScoreViewModel.setCommand(Command.ZOOM_IN)
        }
        binding.zoomDefault.setOnClickListener {
            binding.currentZoom.text = "None"
            soccerScoreViewModel.setCommand(Command.ZOOM_DEFAULT)
        }
        binding.zoomOut.setOnClickListener {
            binding.currentZoom.text = "Out"
            soccerScoreViewModel.setCommand(Command.ZOOM_OUT)
        }
    }
}