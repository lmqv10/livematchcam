package it.lmqv.livematchcam.fragments.sports.volley

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentVolleyControlBarBinding
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.services.firebase.VolleyScore
import it.lmqv.livematchcam.fragments.sports.BaseControlBarFragment
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.viewmodels.VolleyScoreViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.handlers.DialogContext
import it.lmqv.livematchcam.handlers.DialogHandler
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class VolleyControlBarFragment() : BaseControlBarFragment() {
    companion object {
        fun newInstance() = VolleyControlBarFragment()
    }

    private val volleyScoreViewModel: VolleyScoreViewModel by activityViewModels()

    private var _binding: FragmentVolleyControlBarBinding? = null
    private val binding get() = _binding!!

    private var currentDescription = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentVolleyControlBarBinding.inflate(inflater, container, false)
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
                Logd("VolleyControlBar::score.collectLatest:: $scoreInstance")
                try {
                    val score = scoreInstance as VolleyScore
                    volleyScoreViewModel.initScore(score)
                    binding.homeScore.text = score.sets.last().home.toString()
                    binding.awayScore.text = score.sets.last().guest.toString()

                    binding.removeLastSet.isEnabled = score.sets.size > 1
                    binding.addNewSet.isEnabled = score.sets.size < 5

                    binding.currentSet.text = score.sets.size.toString()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Loge("VolleyControlBar::Exception:: ${e.message.toString()}")
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            volleyScoreViewModel.liveScore.collectLatest { liveScore ->
                Logd("VolleyControlBar::liveScore.collectLatest::$liveScore")
                MatchRepository.setScore(liveScore)
            }
        }

        binding.addNewSet.setOnClickListener {
            volleyScoreViewModel.addNewSet()
        }

        binding.removeLastSet.setOnClickListener {
            volleyScoreViewModel.removeLastSet()
        }

        binding.resetMatch.setOnClickListener {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_warning, null)
            val title = dialogView.findViewById<TextView>(R.id.dialog_message)
            title.text = getString(R.string.reset_match_data_message)

            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("OK") { dialog, _ ->
                    volleyScoreViewModel.resetMatch()
                    dialog.dismiss()
                    requireActivity().hideSystemUI()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    requireActivity().hideSystemUI()
                }
                .create()
            dialog.setOnShowListener {
                requireActivity().hideSystemUI()
            }
            dialog.show()
        }

        binding.homeScoreMinus.setOnClickListener {
            volleyScoreViewModel.decrementHomeScore()
        }
        binding.homeScoreAdd.setOnClickListener {
            volleyScoreViewModel.incrementHomeScore()
        }

        binding.awayScoreMinus.setOnClickListener {
            volleyScoreViewModel.decrementAwayScore()
        }
        binding.awayScoreAdd.setOnClickListener {
            volleyScoreViewModel.incrementAwayScore()
        }

        binding.homeTeamControl.onEditTeamName = { teamName, sport ->
            DialogHandler.editText(DialogContext(this, binding.homeTeamControl, R.string.team_name, teamName, sport)) {
                MatchRepository.setHomeTeam(it)
            }
        }

//        binding.homeTeamControl.onTeamNameChanged = { updatedTeamName ->
//            MatchRepository.setHomeTeam(updatedTeamName)
//        }
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

//        binding.guestTeamControl.onTeamNameChanged = { updatedTeamName ->
//            MatchRepository.setGuestTeam(updatedTeamName)
//        }
        binding.guestTeamControl.onLogoURLChanged = { updatedLogoUrl ->
            MatchRepository.setGuestLogo(updatedLogoUrl)
        }
        binding.guestTeamControl.onPrimaryColorsChanged = { updatedColor ->
            MatchRepository.setGuestPrimaryColorHex(updatedColor)
        }

        binding.matchLeagueDescription.setOnClickListener {
            DialogHandler.editText(DialogContext(this, binding.matchLeagueDescription,  R.string.match_league, currentDescription)) {
                this.currentDescription = it
                volleyScoreViewModel.setMatchLeague(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}