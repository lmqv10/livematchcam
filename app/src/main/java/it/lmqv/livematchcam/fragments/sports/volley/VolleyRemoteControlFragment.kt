package it.lmqv.livematchcam.fragments.sports.volley

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentVolleyRemoteControlBinding
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.services.firebase.VolleyScore
import it.lmqv.livematchcam.fragments.sports.BaseRemoteControlFragment
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.viewmodels.VolleyScoreViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt
import it.lmqv.livematchcam.handlers.DialogContext
import it.lmqv.livematchcam.handlers.DialogHandler
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class VolleyRemoteControlFragment() : BaseRemoteControlFragment() {
    companion object {
        fun newInstance() = VolleyRemoteControlFragment()
    }

    private val volleyScoreViewModel: VolleyScoreViewModel by activityViewModels()

    private var _binding: FragmentVolleyRemoteControlBinding? = null
    private val binding get() = _binding!!

    private var currentDescription = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentVolleyRemoteControlBinding.inflate(inflater, container, false)
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
                //Logd("VolleyRemoteControl::score.collectLatest:: $scoreInstance")
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
                    Loge("VolleyRemoteControl::tException:: ${e.message.toString()}")
                }
            }
        }

        lifecycleScope.launch {
            MatchRepository.spotBannerVisible.collect { isVisible ->
                binding.spotBannerSwitch.isChecked = isVisible
            }
        }

        lifecycleScope.launch {
            MatchRepository.spotBannerURL.collect { spotBannerURL ->
                if (spotBannerURL.isNotEmpty()) {
                    binding.spotBannerPreview.load(spotBannerURL) {
                        placeholder(R.drawable.preview_missing)
                        error(R.drawable.preview_missing)
                        allowHardware(false)
                        listener(
                            onError = { _, error ->
                                binding.spotBannerSwitch.isEnabled = false
                                binding.spotBannerTrash.visibility = View.GONE
                            },
                            onSuccess = { _, result ->
                                binding.spotBannerSwitch.isEnabled = true
                                binding.spotBannerTrash.visibility = View.VISIBLE
                            }
                        )
                    }
                } else {
                    binding.spotBannerSwitch.isEnabled = false
                    binding.spotBannerTrash.visibility = View.GONE
                    val drawable =
                        ContextCompat.getDrawable(requireContext(), R.drawable.preview_missing)
                    binding.spotBannerPreview.setImageDrawable(drawable)
                }
            }
        }

        binding.spotBannerTrash.setOnClickListener {
            MatchRepository.setSpotBannerURL("")
        }

        binding.spotBannerSwitch.setOnCheckedChangeListener { _, isChecked ->
            MatchRepository.setSpotBannerVisible(isChecked)
        }

        lifecycleScope.launch {
            MatchRepository.mainBannerVisible.collect { isVisible ->
                binding.mainBannerSwitch.isChecked = isVisible
            }
        }

        lifecycleScope.launch {
            MatchRepository.mainBannerURL.collect { spotBannerURL ->
                if (spotBannerURL.isNotEmpty()) {
                    binding.mainBannerPreview.load(spotBannerURL) {
                        placeholder(R.drawable.preview_missing)
                        error(R.drawable.preview_missing)
                        allowHardware(false)
                        listener(
                            onError = { _, error ->
                                binding.mainBannerSwitch.isEnabled = false
                                binding.mainBannerTrash.visibility = View.GONE
                            },
                            onSuccess = { _, result ->
                                binding.mainBannerSwitch.isEnabled = true
                                binding.mainBannerTrash.visibility = View.VISIBLE
                            }
                        )
                    }
                } else {
                    binding.mainBannerSwitch.isEnabled = false
                    binding.mainBannerTrash.visibility = View.GONE
                    val drawable =
                        ContextCompat.getDrawable(requireContext(), R.drawable.preview_missing)
                    binding.mainBannerPreview.setImageDrawable(drawable)
                }
            }
        }

        binding.mainBannerTrash.setOnClickListener {
            MatchRepository.setMainBannerURL("")
        }

        binding.mainBannerSwitch.setOnCheckedChangeListener { _, isChecked ->
            MatchRepository.setMainBannerVisible(isChecked)
        }

        binding.spotBannerPreview.setOnClickListener {
            lifecycleScope.launch {
                var banner = MatchRepository.spotBannerURL.first()
                DialogHandler.editText(DialogContext(this@VolleyRemoteControlFragment, binding.spotBannerPreview, R.string.spot_banner_placeholder, banner)) {
                    MatchRepository.setSpotBannerURL(it)
                }
            }
        }

        binding.mainBannerPreview.setOnClickListener {
            lifecycleScope.launch {
                var mainBannerURL = MatchRepository.mainBannerURL.first()
                DialogHandler.editText(DialogContext(this@VolleyRemoteControlFragment, binding.mainBannerPreview, R.string.main_banner_placeholder, mainBannerURL)) {
                    MatchRepository.setMainBannerURL(it)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            volleyScoreViewModel.liveScore.collectLatest { liveScore ->
                //Logd("VolleyControlBar::liveScore.collectLatest::$liveScore")
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

        binding.matchLeague.setOnClickListener {
            DialogHandler.editText(DialogContext(this@VolleyRemoteControlFragment, binding.matchLeague, R.string.match_league, this.currentDescription)) {
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