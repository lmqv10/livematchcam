package it.lmqv.livematchcam.fragments.sports.soccer

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
import it.lmqv.livematchcam.fragments.sports.BaseRemoteControlFragment
import it.lmqv.livematchcam.viewmodels.Command
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.viewmodels.SoccerScoreViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt
import it.lmqv.livematchcam.dialogs.TimePickerDialog
import it.lmqv.livematchcam.extensions.formatTime
import it.lmqv.livematchcam.extensions.parseTimeToSeconds
import it.lmqv.livematchcam.handlers.DialogContext
import it.lmqv.livematchcam.handlers.DialogHandler
import it.lmqv.livematchcam.services.counter.CounterService
import it.lmqv.livematchcam.viewmodels.CounterViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

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

        launchOnStarted {
            combine(
                MatchRepository.sport,
                MatchRepository.homeTeam,
                MatchRepository.guestTeam)
            { sport, homeTeam, guestTeam -> Triple(sport, homeTeam, guestTeam)
            }.distinctUntilChanged()
                .collect { (sport, homeTeam, guestTeam) ->
                binding.homeTeamControl.setTeamName(homeTeam, sport)
                binding.guestTeamControl.setTeamName(guestTeam, sport)
            }
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

        binding.homeTeamControl.onEditTeamName = { teamName, sport ->
            DialogHandler.editText(DialogContext(this, binding.homeTeamControl, R.string.team_name, teamName, sport)) {
                MatchRepository.setHomeTeam(it)
            }
        }

        binding.homeTeamControl.onLogoURLChanged = { updatedLogoUrl ->
            MatchRepository.setHomeLogo(updatedLogoUrl)
        }
        binding.homeTeamControl.onPrimaryColorsChanged = { updatedColor ->
            MatchRepository.setHomePrimaryColorHex(updatedColor)
        }

        binding.guestTeamControl.onEditTeamName = { teamName, sport ->
            DialogHandler.editText(DialogContext(this, binding.guestTeamControl, R.string.team_name, teamName, sport)) {
                MatchRepository.setGuestTeam(it)
            }
        }

        binding.guestTeamControl.onLogoURLChanged = { updatedLogoUrl ->
            MatchRepository.setGuestLogo(updatedLogoUrl)
        }
        binding.guestTeamControl.onPrimaryColorsChanged = { updatedColor ->
            MatchRepository.setGuestPrimaryColorHex(updatedColor)
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