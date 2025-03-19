package it.lmqv.livematchcam.fragments.volley

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentVolleyRemoteControlBinding
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.extensions.setShirtByColor
import it.lmqv.livematchcam.extensions.showColorPickerDialog
import it.lmqv.livematchcam.extensions.showEditStringDialog
import it.lmqv.livematchcam.firebase.VolleyScore
import it.lmqv.livematchcam.fragments.BaseRemoteControlFragment
import it.lmqv.livematchcam.viewmodels.VolleyScoreViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch

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
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVolleyRemoteControlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        matchViewModel.homeTeam.observe(viewLifecycleOwner) { homeTeam ->
            binding.homeTeam.text = homeTeam
        }
        matchViewModel.guestTeam.observe(viewLifecycleOwner) { guestTeam ->
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
                matchViewModel.homeColorHex,
                matchViewModel.homeLogo) {
                    color, logo -> Pair(color, logo)
            }.collect { (colorHex, logoURL) ->
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
            }
        }

        launchOnStarted {
            combine(
                matchViewModel.guestColorHex,
                matchViewModel.guestLogo) {
                    color, logo -> Pair(color, logo)
            }.collect { (colorHex, logoURL) ->
                //binding.awayColor.isClickable = logoURL.isNullOrEmpty()
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

        matchViewModel.score.observe(viewLifecycleOwner) { scoreInstance ->
            try {
                //Logd("VolleyRemoteControl::score.observe")
                val score = scoreInstance as? VolleyScore ?: VolleyScore()
                volleyScoreViewModel.initScore(score)
                //Logd("VolleyRemoteControl::score $score")

                binding.homeScore.text = score.sets.last().home.toString()
                binding.awayScore.text = score.sets.last().guest.toString()

                binding.removeLastSet.isEnabled = score.sets.size > 1
                binding.addNewSet.isEnabled = score.sets.size < 5

                binding.currentSet.text = score.sets.size.toString()
            } catch (e: Exception) {
                //Logd("VolleyRemoteControl Exception  $e.message")
            }
        }

        lifecycleScope.launch {
            matchViewModel.spotBannerVisible.collect { isVisible ->
                binding.spotBannerSwitch.isChecked = isVisible
            }
        }

        lifecycleScope.launch {
            matchViewModel.spotBannerURL.collect { spotBannerURL ->
                if (!spotBannerURL.isNullOrEmpty()) {
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
            matchViewModel.setSpotBannerURL("")
        }

        binding.spotBannerSwitch.setOnCheckedChangeListener { _, isChecked ->
            matchViewModel.setSpotBannerVisible(isChecked)
        }

        lifecycleScope.launch {
            matchViewModel.mainBannerVisible.collect { isVisible ->
                binding.mainBannerSwitch.isChecked = isVisible
            }
        }

        lifecycleScope.launch {
            matchViewModel.mainBannerURL.collect { spotBannerURL ->
                if (!spotBannerURL.isNullOrEmpty()) {
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
            matchViewModel.setMainBannerURL("")
        }

        binding.mainBannerSwitch.setOnCheckedChangeListener { _, isChecked ->
            matchViewModel.setMainBannerVisible(isChecked)
        }

        binding.spotBannerPreview.setOnClickListener {
            lifecycleScope.launch {
                requireContext().showEditStringDialog(
                    R.string.spot_banner_placeholder,
                    matchViewModel.spotBannerURL.last(),
                    arrayOf()
                ) { updatedText ->
                    matchViewModel.setSpotBannerURL(updatedText)
                    requireActivity().hideSystemUI()
                }
            }
        }

        binding.mainBannerPreview.setOnClickListener {
            lifecycleScope.launch {
                requireContext().showEditStringDialog(
                    R.string.main_banner_placeholder,
                    matchViewModel.mainBannerURL.last(),
                    arrayOf()
                ) { updatedText ->
                    matchViewModel.setMainBannerURL(updatedText)
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

        volleyScoreViewModel.liveScore.observe(viewLifecycleOwner) { liveScore ->
            if (liveScore != null) {
                matchViewModel.setScore(liveScore)
            }
        }

        binding.addNewSet.setOnClickListener {
            volleyScoreViewModel.addNewSet()
        }

        binding.removeLastSet.setOnClickListener {
            volleyScoreViewModel.removeLastSet()
        }

        binding.resetMatch.setOnClickListener {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_start_stop_stream, null)
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

        binding.homeColor.setOnClickListener {
            requireContext().showColorPickerDialog { color, logoUrl ->
                matchViewModel.setHomeColorHex(color)
                matchViewModel.setHomeLogo(logoUrl)
            }
        }

        binding.awayColor.setOnClickListener {
            requireContext().showColorPickerDialog { color, logoUrl ->
                matchViewModel.setGuestColorHex(color)
                matchViewModel.setGuestLogo(logoUrl)
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

        binding.matchLeague.setOnClickListener {
            requireContext().showEditStringDialog(R.string.match_league, this.currentDescription, filters = arrayOf<InputFilter>()) { updatedMatchLeague ->
                this.currentDescription = updatedMatchLeague
                volleyScoreViewModel.setMatchLeague(this.currentDescription)
                requireActivity().hideSystemUI()
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = requireActivity().currentFocus
        if (view != null) {
            imm.hideSoftInputFromWindow(view.windowToken, 0)
            view.clearFocus() // Rimuove il focus
        }
    }
}