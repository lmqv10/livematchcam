package it.lmqv.livematchcam.fragments.soccer

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentSoccerControlBarBinding
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.extensions.setShirtByColor
import it.lmqv.livematchcam.extensions.showColorPickerDialog
import it.lmqv.livematchcam.extensions.showEditStringDialog
import it.lmqv.livematchcam.firebase.SoccerScore
import it.lmqv.livematchcam.fragments.BaseControlBarFragment
import it.lmqv.livematchcam.viewmodels.Command
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.viewmodels.SoccerScoreViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SoccerControlBarFragment() : BaseControlBarFragment() {
    companion object {
        fun newInstance() = SoccerControlBarFragment()
    }
    private val soccerScoreViewModel: SoccerScoreViewModel by activityViewModels()

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

        //Logd("SoccerControlBarFragment::matchViewModelID: ${matchViewModel.instanceId}")
        MatchRepository.homeTeam.observe(viewLifecycleOwner) { team ->
            binding.homeTeam.text = team
        }
        MatchRepository.guestTeam.observe(viewLifecycleOwner) { team ->
            binding.awayTeam.text = team
        }

        launchOnStarted {
            combine(
                MatchRepository.homePrimaryColorHex,
                MatchRepository.homeLogo) {
                    color, logo -> Pair(color, logo)
            }.collect { (colorHex, logoURL) ->
                if (!logoURL.isNullOrEmpty()) {
                    binding.homeColor.load(logoURL) {
                        placeholder(R.drawable.shirt_white)
                        error(R.drawable.shirt_white)
                        allowHardware(false)
                    }
                } else {
                    binding.homeColor.setShirtByColor(Color.parseColor(colorHex))
                }
            }
        }

        launchOnStarted {
            combine(
                MatchRepository.guestPrimaryColorHex,
                MatchRepository.guestLogo) {
                    color, logo -> Pair(color, logo)
            }.collect { (colorHex, logoURL) ->
                if (!logoURL.isNullOrEmpty()) {
                    binding.awayColor.load(logoURL) {
                        placeholder(R.drawable.shirt_white)
                        error(R.drawable.shirt_white)
                        allowHardware(false)
                    }
                } else {
                    binding.awayColor.setShirtByColor(Color.parseColor(colorHex))
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            MatchRepository.score.collectLatest { scoreInstance ->
                //Logd("SoccerControlBar::score.collectLatest::${scoreInstance}")
                try {
                    val score = scoreInstance as SoccerScore
                    soccerScoreViewModel.initScore(score)
                    binding.homeScore.text = score.home.toString()
                    binding.awayScore.text = score.away.toString()

                    val command = score.command
                    if (command == Command.START_TIME.toString()) {
                        binding.startTime.visibility = View.GONE
                        binding.stopTime.visibility = View.VISIBLE
                        binding.resetTime.isEnabled = false
                    }
                    if (command == Command.PAUSE.toString()) {
                        binding.startTime.visibility = View.VISIBLE
                        binding.stopTime.visibility = View.GONE
                        binding.resetTime.isEnabled = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Logd("SoccerControlBar::Exception:: ${e.message.toString()}")
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            soccerScoreViewModel.liveScore.collectLatest { liveScore ->
                //Logd("SoccerControlBar::liveScore.collectLatest::$liveScore")
                MatchRepository.setScore(liveScore)
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
            launchOnStarted {
                requireContext().showColorPickerDialog { color ->
                    MatchRepository.setHomePrimaryColorHex(color)
                }
            }
        }

        binding.awayColor.setOnClickListener {
            requireContext().showColorPickerDialog { color ->
                MatchRepository.setGuestPrimaryColorHex(color)
            }
        }

        binding.homeTeam.setOnClickListener {
            var teamName = binding.homeTeam.text.toString()
            requireContext().showEditStringDialog(R.string.team_name, teamName) { updatedTeamName ->
                MatchRepository.setHomeTeam(updatedTeamName)
                requireActivity().hideSystemUI()
            }
        }

        binding.awayTeam.setOnClickListener {
            var teamName = binding.awayTeam.text.toString()
            requireContext().showEditStringDialog(R.string.team_name, teamName) { updatedTeamName ->
                MatchRepository.setGuestTeam(updatedTeamName)
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}