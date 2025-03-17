package it.lmqv.livematchcam.fragments.volley

import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import coil.load
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentVolleyControlBarBinding
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

        /*scoreViewModel.matchLeague.observe(viewLifecycleOwner) { currentDescription ->
            this.currentDescription = currentDescription
        }

        scoreViewModel.currentScore.observe(viewLifecycleOwner) { score ->
            binding.homeScore.text = score.homePoints.toString()
            binding.awayScore.text = score.awayPoints.toString()
        }

        scoreViewModel.currentSet.observe(viewLifecycleOwner) { currentSet ->
            when (currentSet) {
                Set.SET1 -> binding.radioSet1.isChecked = true
                Set.SET2 -> binding.radioSet2.isChecked = true
                Set.SET3 -> binding.radioSet3.isChecked = true
                Set.SET4 -> binding.radioSet4.isChecked = true
                Set.SET5 -> binding.radioSet5.isChecked = true
            }
        }*/

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

        binding.matchLeague.setOnClickListener {
            requireContext().showEditStringDialog(R.string.match_league, this.currentDescription, filters = arrayOf<InputFilter>()) { updatedMatchLeague ->
                this.currentDescription = updatedMatchLeague
                volleyScoreViewModel.setMatchLeague(this.currentDescription)
                requireActivity().hideSystemUI()
            }
        }


        /*binding.setsGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.radioSet1.id -> {
                    scoreViewModel.setCurrentSet(Set.SET1)
                }
                binding.radioSet2.id -> {
                    scoreViewModel.setCurrentSet(Set.SET2)
                }
                binding.radioSet3.id -> {
                    scoreViewModel.setCurrentSet(Set.SET3)
                }
                binding.radioSet4.id -> {
                    scoreViewModel.setCurrentSet(Set.SET4)
                }
                binding.radioSet5.id -> {
                    scoreViewModel.setCurrentSet(Set.SET5)
                }
            }
        }*/
    }
}