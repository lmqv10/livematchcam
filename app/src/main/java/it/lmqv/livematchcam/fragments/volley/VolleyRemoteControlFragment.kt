package it.lmqv.livematchcam.fragments.volley

import android.os.Bundle
import android.text.InputFilter
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
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.extensions.setShirtByColor
import it.lmqv.livematchcam.extensions.showColorPickerDialog
import it.lmqv.livematchcam.extensions.showEditStringDialog
import it.lmqv.livematchcam.services.firebase.VolleyScore
import it.lmqv.livematchcam.fragments.BaseRemoteControlFragment
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.viewmodels.VolleyScoreViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt

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

        MatchRepository.homeTeam.observe(viewLifecycleOwner) { homeTeam ->
            binding.homeTeam.text = homeTeam
        }
        MatchRepository.guestTeam.observe(viewLifecycleOwner) { guestTeam ->
            binding.awayTeam.text = guestTeam
        }

        /*matchViewModel.homeColorHex.observe(viewLifecycleOwner) { homeColorHex ->
            binding.homeColor.setShirtByColor(Color.parseColor(homeColorHex))
        }
        matchViewModel.guestColorHex.observe(viewLifecycleOwner) { guestColorHex ->
            binding.awayColor.setShirtByColor(Color.parseColor(guestColorHex))
        }*/

        launchOnStarted {
            combine(
                MatchRepository.isRealtimeDatabaseAvailable,
                MatchRepository.homePrimaryColorHex,
                MatchRepository.homeLogo) {
                    available, color, logo -> Triple(available, color, logo)
            }.collect { (isAvailable, colorHex, logoURL) ->
                //binding.homeColor.isClickable = logoURL.isNullOrEmpty()
                if (logoURL.isNotEmpty()) {
                    binding.homeColor.load(logoURL) {
                        placeholder(R.drawable.shirt_white)
                        error(R.drawable.shirt_white)
                        allowHardware(false)
                    }
                } else {
                    binding.homeColor.setShirtByColor(colorHex.toColorInt())
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
                if (logoURL.isNotEmpty()) {
                    binding.guestColor.load(logoURL) {
                        placeholder(R.drawable.shirt_white)
                        error(R.drawable.shirt_white)
                        allowHardware(false)
                    }
                } else {
                    binding.guestColor.setShirtByColor(colorHex.toColorInt())
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
                requireContext().showEditStringDialog(
                    R.string.spot_banner_placeholder,
                    MatchRepository.spotBannerURL.first(),
                    arrayOf()
                ) { updatedText ->
                    MatchRepository.setSpotBannerURL(updatedText)
                    requireActivity().hideSystemUI()
                }
            }
        }

        binding.mainBannerPreview.setOnClickListener {
            lifecycleScope.launch {
                requireContext().showEditStringDialog(
                    R.string.main_banner_placeholder,
                    MatchRepository.mainBannerURL.first(),
                    arrayOf()
                ) { updatedText ->
                    MatchRepository.setMainBannerURL(updatedText)
                    requireActivity().hideSystemUI()
                }
            }
        }

        /*binding.spotBannerUrl.setOnEditorActionListener(object : TextView.OnEditorActionListener {
                override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                    return if (actionId == EditorInfo.IME_ACTION_DONE) {
                        val text = v?.text.toString()
                        toast("Spot: $text")
                        hideKeyboard()
                        true
                    } else {
                        false
                    }
                }
            })

        binding.mainBannerUrl.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                return if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val text = v?.text.toString()
                    toast("Main: $text")
                    hideKeyboard()
                    true
                } else {
                    false
                }
            }
        })*/

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

        binding.matchLeague.setOnClickListener {
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

//    private fun hideKeyboard() {
//        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        val view = requireActivity().currentFocus
//        if (view != null) {
//            imm.hideSoftInputFromWindow(view.windowToken, 0)
//            view.clearFocus() // Rimuove il focus
//        }
//    }
}