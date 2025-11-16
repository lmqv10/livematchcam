package it.lmqv.livematchcam.fragments.volley

import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentVolleyControlBarBinding
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.extensions.setShirtByColor
import it.lmqv.livematchcam.extensions.showColorPickerDialog
import it.lmqv.livematchcam.extensions.showEditStringDialog
import it.lmqv.livematchcam.services.firebase.VolleyScore
import it.lmqv.livematchcam.fragments.BaseControlBarFragment
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.viewmodels.VolleyScoreViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt

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

//        MatchRepository.homeTeam.observe(viewLifecycleOwner) { homeTeam ->
//            binding.homeTeam.text = homeTeam
//        }
//        MatchRepository.guestTeam.observe(viewLifecycleOwner) { guestTeam ->
//            binding.awayTeam.text = guestTeam
//        }
        MatchRepository.homeTeam.observe(viewLifecycleOwner) { homeTeam ->
            binding.homeTeamControl.setTeamName(homeTeam)
        }
        MatchRepository.guestTeam.observe(viewLifecycleOwner) { guestTeam ->
            binding.guestTeamControl.setTeamName(guestTeam)
        }

//        launchOnStarted {
//            combine(
//                MatchRepository.homePrimaryColorHex,
//                MatchRepository.homeLogo) {
//                    color, logo -> Pair(color, logo)
//            }.collect { (colorHex, logoURL) ->
//                if (logoURL.isNotEmpty()) {
//                    binding.homeColor.load(logoURL) {
//                        placeholder(R.drawable.shirt_white)
//                        error(R.drawable.shirt_white)
//                        allowHardware(false)
//                    }
//                } else {
//                    binding.homeColor.setShirtByColor(colorHex.toColorInt())
//                }
//            }
//        }

//        launchOnStarted {
//            combine(
//                MatchRepository.guestPrimaryColorHex,
//                MatchRepository.guestLogo) {
//                    color, logo -> Pair(color, logo)
//            }.collect { (colorHex, logoURL) ->
//                //binding.awayColor.isClickable = logoURL.isNullOrEmpty()
//                if (logoURL.isNotEmpty()) {
//                    binding.awayColor.load(logoURL) {
//                        placeholder(R.drawable.shirt_white)
//                        error(R.drawable.shirt_white)
//                        allowHardware(false)
//                    }
//                } else {
//                    binding.awayColor.setShirtByColor(colorHex.toColorInt())
//                }
//            }
//        }


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
                //Logd("VolleyControlBar::score.collectLatest:: $scoreInstance")
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

//        binding.homeColor.setOnClickListener {
//            requireContext().showColorPickerDialog { color ->
//                MatchRepository.setHomePrimaryColorHex(color)
//            }
//        }
//
//        binding.awayColor.setOnClickListener {
//            requireContext().showColorPickerDialog { color ->
//                MatchRepository.setGuestPrimaryColorHex(color)
//            }
//        }
//
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

        binding.matchLeagueDescription.setOnClickListener {
            requireContext().showEditStringDialog(R.string.match_league, this.currentDescription, filters = arrayOf<InputFilter>()) { updatedMatchLeague ->
                this.currentDescription = updatedMatchLeague
                volleyScoreViewModel.setMatchLeague(this.currentDescription)
                requireActivity().hideSystemUI()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}