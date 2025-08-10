package it.lmqv.livematchcam.fragments.soccer

import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentSoccerRemoteControlBinding
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.extensions.setShirtByColor
import it.lmqv.livematchcam.extensions.showColorPickerDialog
import it.lmqv.livematchcam.extensions.showEditStringDialog
import it.lmqv.livematchcam.firebase.SoccerScore
import it.lmqv.livematchcam.fragments.BaseRemoteControlFragment
import it.lmqv.livematchcam.viewmodels.Command
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.viewmodels.SoccerScoreViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SoccerRemoteControlFragment : BaseRemoteControlFragment() {
    companion object {
        fun newInstance() = SoccerRemoteControlFragment()
    }
    private val soccerScoreViewModel: SoccerScoreViewModel by activityViewModels()

    private var _binding: FragmentSoccerRemoteControlBinding? = null
    private val binding get() = _binding!!

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
        MatchRepository.homeTeam.observe(viewLifecycleOwner) { team ->
            binding.homeTeam.text = team
        }
        MatchRepository.guestTeam.observe(viewLifecycleOwner) { team ->
            binding.awayTeam.text = team
        }

        launchOnStarted {
            combine(
                MatchRepository.isRealtimeDatabaseAvailable,
                MatchRepository.homePrimaryColorHex,
                MatchRepository.homeLogo) {
                    available, color, logo -> Triple(available, color, logo)
            }.collect { (isAvailable, colorHex, logoURL) ->
                //binding.homeColor.isClickable = logoURL.isNullOrEmpty()
                if (!logoURL.isNullOrEmpty()) {
                    binding.homeColor.load(logoURL) {
                        placeholder(R.drawable.shirt_white)
                        error(R.drawable.shirt_white)
                        allowHardware(false)
                    }
                } else {
                    binding.homeColor.setShirtByColor(Color.parseColor(colorHex))
                }

                binding.homeColor.setOnClickListener {
                    if (isAvailable) {
                        requireContext().showEditStringDialog(R.string.choose_logo, logoURL, arrayOf<InputFilter>()) { updatedTeamLogo ->
                            MatchRepository.setHomeLogo(updatedTeamLogo)
                            requireActivity().hideSystemUI()
                        }
                    } else {
                        requireContext().showColorPickerDialog { color ->
                            MatchRepository.setHomePrimaryColorHex(color)
                        }
                    }
                }
            }
        }

        launchOnStarted {
            combine(
                MatchRepository.isRealtimeDatabaseAvailable,
                MatchRepository.guestPrimaryColorHex,
                MatchRepository.guestLogo) {
                    available, color, logo -> Triple(available, color, logo)
            }.collect { (isAvailable, colorHex, logoURL) ->
                //binding.guestColor.isClickable = logoURL.isNullOrEmpty()
                if (!logoURL.isNullOrEmpty()) {
                    binding.guestColor.load(logoURL) {
                        placeholder(R.drawable.shirt_white)
                        error(R.drawable.shirt_white)
                        allowHardware(false)
                    }
                } else {
                    binding.guestColor.setShirtByColor(Color.parseColor(colorHex))
                }

                binding.guestColor.setOnClickListener {
                    if (isAvailable) {
                        requireContext().showEditStringDialog(R.string.choose_logo, logoURL, arrayOf<InputFilter>()) { updatedTeamLogo ->
                            MatchRepository.setGuestLogo(updatedTeamLogo)
                            requireActivity().hideSystemUI()
                        }
                    } else {
                        requireContext().showColorPickerDialog { color ->
                            MatchRepository.setGuestPrimaryColorHex(color)
                        }
                    }
                }
            }
        }

        /*matchViewModel.homeColorHex.observe(viewLifecycleOwner) { homeColorHex ->
            binding.homeColor.setShirtByColor(Color.parseColor(homeColorHex))
        }
        matchViewModel.guestColorHex.observe(viewLifecycleOwner) { guestColorHex ->
            binding.awayColor.setShirtByColor(Color.parseColor(guestColorHex))
        }*/

        viewLifecycleOwner.lifecycleScope.launch {
            MatchRepository.score.collectLatest { scoreInstance ->
                Logd("SoccerRemoteControl::${scoreInstance}")
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
                    if (command == Command.RESET_TIME.toString()) {
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Loge("SoccerRemoteControl::Exception:: ${e.message.toString()}")
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            soccerScoreViewModel.liveScore.collectLatest { liveScore ->
                Logd("SoccerRemoteControlBar $liveScore")
                MatchRepository.setScore(liveScore)
            }
        }

        binding.homeScoreMinus.setOnClickListener {
            Logd("SoccerRemoteControlBar::homeScoreMinus")
            soccerScoreViewModel.incrementHomeScore(-1)
        }

        binding.homeScoreAdd.setOnClickListener {
            Logd("SoccerRemoteControlBar::homeScoreAdd")
            soccerScoreViewModel.incrementHomeScore()
        }

        binding.awayScoreMinus.setOnClickListener {
            Logd("SoccerRemoteControlBar::awayScoreMinus")
            soccerScoreViewModel.incrementGuestScore(-1)
        }

        binding.awayScoreAdd.setOnClickListener {
            Logd("SoccerRemoteControlBar::awayScoreAdd")
            soccerScoreViewModel.incrementGuestScore()
        }

        /*binding.homeColor.setOnClickListener {
            requireContext().showColorPickerDialog { color ->
                matchViewModel.setHomeColorHex(color)
            }
        }

        binding.awayColor.setOnClickListener {
            requireContext().showColorPickerDialog { color ->
                matchViewModel.setGuestColorHex(color)
            }
        }*/

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}