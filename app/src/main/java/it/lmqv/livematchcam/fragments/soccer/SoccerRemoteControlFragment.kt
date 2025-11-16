package it.lmqv.livematchcam.fragments.soccer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentSoccerRemoteControlBinding
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.services.firebase.SoccerScore
import it.lmqv.livematchcam.fragments.BaseRemoteControlFragment
import it.lmqv.livematchcam.viewmodels.Command
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.viewmodels.SoccerScoreViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt
import it.lmqv.livematchcam.dialogs.TimePickerDialog
import it.lmqv.livematchcam.extensions.formatTime
import it.lmqv.livematchcam.extensions.parseTimeToSeconds
import it.lmqv.livematchcam.services.CounterService
import it.lmqv.livematchcam.viewmodels.CounterViewModel

class SoccerRemoteControlFragment : BaseRemoteControlFragment() {
    companion object {
        fun newInstance() = SoccerRemoteControlFragment()
    }
    private val soccerScoreViewModel: SoccerScoreViewModel by activityViewModels()
    private val counterViewModel: CounterViewModel by activityViewModels()

    private var _binding: FragmentSoccerRemoteControlBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSoccerRemoteControlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Logd("SoccerControlBarFragment::matchViewModelID: ${matchViewModel.instanceId}")
//        MatchRepository.homeTeam.observe(viewLifecycleOwner) { team ->
//            binding.homeTeam.text = team
//        }
        MatchRepository.homeTeam.observe(viewLifecycleOwner) { homeTeam ->
            binding.homeTeamControl.setTeamName(homeTeam)
        }
//        MatchRepository.guestTeam.observe(viewLifecycleOwner) { team ->
//            binding.awayTeam.text = team
//        }
        MatchRepository.guestTeam.observe(viewLifecycleOwner) { guestTeam ->
            binding.guestTeamControl.setTeamName(guestTeam)
        }

        launchOnStarted {
            MatchRepository.homeLogo.collect { logoUrl ->
                binding.homeTeamControl.setLogoUrl(logoUrl)
            }
        }
        launchOnStarted {
            MatchRepository.guestLogo.collect { logoUrl ->
                binding.guestTeamControl.setLogoUrl(logoUrl)
            }
        }

        launchOnStarted {
            MatchRepository.homePrimaryColorHex.collect { primaryColor ->
                binding.homeTeamControl.setPrimaryColor(primaryColor.toColorInt())
            }
        }
        launchOnStarted {
            MatchRepository.guestPrimaryColorHex.collect { primaryColor ->
                binding.guestTeamControl.setPrimaryColor(primaryColor.toColorInt())
            }
        }

//        launchOnStarted {
//            combine(
//                MatchRepository.firebaseAccountData,
//                MatchRepository.homePrimaryColorHex,
//                MatchRepository.homeLogo) {
//                    firebaseAccountData, color, logo -> Triple(firebaseAccountData, color, logo)
//            }.collect { (firebaseAccountData, colorHex, logoURL) ->
//                //binding.homeColor.isClickable = logoURL.isNullOrEmpty()
//                if (logoURL.isNotEmpty()) {
//                    binding.homeColor.load(logoURL) {
//                        placeholder(R.drawable.shirt_white)
//                        error(R.drawable.shirt_white)
//                        allowHardware(false)
//                    }
//                } else {
//                    binding.homeColor.setShirtByColor(colorHex.toColorInt())
//                }
//
//                binding.homeColor.setOnClickListener {
//                    if (firebaseAccountData.remoteScoreAvailable) {
//                        requireContext().showEditStringDialog(R.string.choose_logo, logoURL, arrayOf<InputFilter>()) { updatedTeamLogo ->
//                            MatchRepository.setHomeLogo(updatedTeamLogo)
//                            requireActivity().hideSystemUI()
//                        }
//                    } else {
//                        requireContext().showColorPickerDialog { color ->
//                            MatchRepository.setHomePrimaryColorHex(color)
//                        }
//                    }
//                }
//            }
//        }

//        launchOnStarted {
//            combine(
//                MatchRepository.firebaseAccountData,
//                MatchRepository.guestPrimaryColorHex,
//                MatchRepository.guestLogo) {
//                    firebaseAccountData, color, logo -> Triple(firebaseAccountData, color, logo)
//            }.collect { (firebaseAccountData, colorHex, logoURL) ->
//                //binding.guestColor.isClickable = logoURL.isNullOrEmpty()
//                if (logoURL.isNotEmpty()) {
//                    binding.guestColor.load(logoURL) {
//                        placeholder(R.drawable.shirt_white)
//                        error(R.drawable.shirt_white)
//                        allowHardware(false)
//                    }
//                } else {
//                    binding.guestColor.setShirtByColor(colorHex.toColorInt())
//                }
//
//                binding.guestColor.setOnClickListener {
//                    if (firebaseAccountData.remoteScoreAvailable) {
//                        requireContext().showEditStringDialog(R.string.choose_logo, logoURL, arrayOf<InputFilter>()) { updatedTeamLogo ->
//                            MatchRepository.setGuestLogo(updatedTeamLogo)
//                            requireActivity().hideSystemUI()
//                        }
//                    } else {
//                        requireContext().showColorPickerDialog { color ->
//                            MatchRepository.setGuestPrimaryColorHex(color)
//                        }
//                    }
//                }
//            }
//        }

        /*matchViewModel.homeColorHex.observe(viewLifecycleOwner) { homeColorHex ->
            binding.homeColor.setShirtByColor(Color.parseColor(homeColorHex))
        }
        matchViewModel.guestColorHex.observe(viewLifecycleOwner) { guestColorHex ->
            binding.awayColor.setShirtByColor(Color.parseColor(guestColorHex))
        }*/

        viewLifecycleOwner.lifecycleScope.launch {
            MatchRepository.score.collectLatest { scoreInstance ->
                //Logd("SoccerRemoteControl::${scoreInstance}")
                try {
                    val score = scoreInstance as SoccerScore
                    soccerScoreViewModel.initScore(score)
                    binding.homeScore.text = score.home.toString()
                    binding.awayScore.text = score.away.toString()

                    val command = score.command
                    if (command == Command.START_TIME.toString()) {
                        binding.startTime.visibility = View.GONE
                        binding.stopTime.visibility = View.VISIBLE
                        //binding.resetTime.isEnabled = false
                    }
                    if (command == Command.PAUSE.toString()) {
                        binding.startTime.visibility = View.VISIBLE
                        binding.stopTime.visibility = View.GONE
                        //binding.resetTime.isEnabled = true
                    }
                    //if (command == Command.RESET_TIME.toString()) {
                    //}
                } catch (e: Exception) {
                    e.printStackTrace()
                    Loge("SoccerRemoteControl::Exception:: ${e.message.toString()}")
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            soccerScoreViewModel.liveScore.collectLatest { liveScore ->
                //Logd("SoccerRemoteControlBar $liveScore")
                MatchRepository.setScore(liveScore)
            }
        }

        binding.homeScoreMinus.setOnClickListener {
            //Logd("SoccerRemoteControlBar::homeScoreMinus")
            soccerScoreViewModel.incrementHomeScore(-1)
        }

        binding.homeScoreAdd.setOnClickListener {
            //Logd("SoccerRemoteControlBar::homeScoreAdd")
            soccerScoreViewModel.incrementHomeScore()
        }

        binding.awayScoreMinus.setOnClickListener {
            //Logd("SoccerRemoteControlBar::awayScoreMinus")
            soccerScoreViewModel.incrementGuestScore(-1)
        }

        binding.awayScoreAdd.setOnClickListener {
            //Logd("SoccerRemoteControlBar::awayScoreAdd")
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

        binding.homeTeamControl.onTeamNameChanged = { updatedTeamName ->
            MatchRepository.setHomeTeam(updatedTeamName)
        }
        binding.homeTeamControl.onLogoURLChanged = { updatedLogoUrl ->
            MatchRepository.setHomeLogo(updatedLogoUrl)
        }
        binding.homeTeamControl.onPrimaryColorsChanged = { updatedColor ->
            MatchRepository.setHomePrimaryColorHex(updatedColor)
        }

        binding.guestTeamControl.onTeamNameChanged = { updatedTeamName ->
            MatchRepository.setGuestTeam(updatedTeamName)
        }
        binding.guestTeamControl.onLogoURLChanged = { updatedLogoUrl ->
            MatchRepository.setGuestLogo(updatedLogoUrl)
        }
        binding.guestTeamControl.onPrimaryColorsChanged = { updatedColor ->
            MatchRepository.setGuestPrimaryColorHex(updatedColor)
        }

//        binding.homeTeam.setOnClickListener {
//            var teamName = binding.homeTeam.text.toString()
//            requireContext().showEditStringDialog(R.string.team_name, teamName) { updatedTeamName ->
//                MatchRepository.setHomeTeam(updatedTeamName)
//                requireActivity().hideSystemUI()
//            }
//        }
//
//        binding.awayTeam.setOnClickListener {
//            var teamName = binding.awayTeam.text.toString()
//            requireContext().showEditStringDialog(R.string.team_name, teamName) { updatedTeamName ->
//                MatchRepository.setGuestTeam(updatedTeamName)
//                requireActivity().hideSystemUI()
//            }
//        }

        binding.changePeriod.setOnClickListener {
            soccerScoreViewModel.nextPeriod()
        }

        binding.startTime.setOnClickListener {
            soccerScoreViewModel.setCommand(Command.START_TIME)
        }

        binding.stopTime.setOnClickListener {
            soccerScoreViewModel.setCommand(Command.PAUSE)
        }

        /*binding.resetTime.setOnClickListener {
            soccerScoreViewModel.setCommand(Command.RESET_TIME)
        }*/

        binding.matchTime.setOnClickListener {
            val currentSeconds = parseTimeToSeconds(binding.matchTime.text.toString())

            TimePickerDialog(
                context = requireContext(),
                seconds = currentSeconds,
                onConfirm = { seconds ->
                    binding.matchTime.text = formatTime(seconds)
                    counterViewModel.setCounter(seconds)
                },
                onCancel = {
                }
            ).show()
        }

        binding.matchTime.text = formatTime(0)
        launchOnStarted {
            counterViewModel.counterState.collect { state ->
                var seconds = 0
                when (state) {
                    is CounterService.CounterState.Running -> seconds = state.seconds
                    is CounterService.CounterState.Paused -> seconds = state.seconds
                    is CounterService.CounterState.Stopped -> seconds = 0
                }

                binding.matchTime.text = formatTime(seconds)
            }
        }

        binding.zoomIn.setOnClickListener {
            binding.currentZoom.text = getString(R.string.zoom_in)
            soccerScoreViewModel.setCommand(Command.ZOOM_IN)
        }
        binding.zoomDefault.setOnClickListener {
            binding.currentZoom.text = getString(R.string.zoom_none)
            soccerScoreViewModel.setCommand(Command.ZOOM_DEFAULT)
        }
        binding.zoomOut.setOnClickListener {
            binding.currentZoom.text = getString(R.string.zoom_out)
            soccerScoreViewModel.setCommand(Command.ZOOM_OUT)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}