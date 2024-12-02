package it.lmqv.livematchcam.fragments.volley

import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentVolleyControlBarBinding
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.extensions.setShirtByColor
import it.lmqv.livematchcam.extensions.showColorPickerDialog
import it.lmqv.livematchcam.extensions.showEditStringDialog
import it.lmqv.livematchcam.fragments.BaseControlBarFragment
import it.lmqv.livematchcam.viewmodels.Set
import it.lmqv.livematchcam.viewmodels.VolleyScoreViewModel

class VolleyControlBarFragment(scoreBoardFragment: VolleyScoreBoardFragment) : BaseControlBarFragment() {
    companion object {
        fun newInstance(scoreBoardFragment: VolleyScoreBoardFragment) = VolleyControlBarFragment(scoreBoardFragment)
    }

    private val scoreViewModel: VolleyScoreViewModel by activityViewModels()

    private var _binding: FragmentVolleyControlBarBinding? = null
    private val binding get() = _binding!!

    private var currentDescription = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVolleyControlBarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeTeamViewModel.name.observe(viewLifecycleOwner) { team ->
            binding.homeTeam.text = team
        }
        awayTeamViewModel.name.observe(viewLifecycleOwner) { team ->
            binding.awayTeam.text = team
        }

        scoreViewModel.matchLeague.observe(viewLifecycleOwner) { currentDescription ->
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
        }
        homeTeamViewModel.logo.observe(viewLifecycleOwner) { color ->
            binding.homeColor.setShirtByColor(color)
        }
        awayTeamViewModel.logo.observe(viewLifecycleOwner) { color ->
            binding.awayColor.setShirtByColor(color)
        }

        binding.resetMatch.setOnClickListener {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_start_stop_stream, null)
            val title = dialogView.findViewById<TextView>(R.id.dialog_message)
            title.text = getString(R.string.reset_match_data_message)

            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("OK") { dialog, _ ->
                    scoreViewModel.resetMatch()
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
            scoreViewModel.decrementHomeScore()
        }
        binding.homeScoreAdd.setOnClickListener {
            scoreViewModel.incrementHomeScore()
        }

        binding.awayScoreMinus.setOnClickListener {
            scoreViewModel.decrementAwayScore()
        }
        binding.awayScoreAdd.setOnClickListener {
            scoreViewModel.incrementAwayScore()
        }

        binding.homeColor.setOnClickListener {
            requireContext().showColorPickerDialog { color ->
                homeTeamViewModel.setLogo(color)
            }
        }
        binding.awayColor.setOnClickListener {
            requireContext().showColorPickerDialog { color ->
                awayTeamViewModel.setLogo(color)
            }
        }

        binding.homeTeam.setOnClickListener {
            val teamName = binding.homeTeam.text.toString()
            requireContext().showEditStringDialog(R.string.team_name, teamName) { updatedTeamName ->
                homeTeamViewModel.setName(updatedTeamName)
                requireActivity().hideSystemUI()
            }
        }

        binding.awayTeam.setOnClickListener {
            val teamName = binding.awayTeam.text.toString()
            requireContext().showEditStringDialog(R.string.team_name, teamName) { updatedTeamName ->
                awayTeamViewModel.setName(updatedTeamName)
                requireActivity().hideSystemUI()
            }
        }

        binding.matchLeague.setOnClickListener {
            requireContext().showEditStringDialog(R.string.match_league, this.currentDescription, filters = arrayOf<InputFilter>()) { updatedMatchLeague ->
                this.currentDescription = updatedMatchLeague
                scoreViewModel.setMatchLeague(this.currentDescription)
                requireActivity().hideSystemUI()
            }
        }


        binding.setsGroup.setOnCheckedChangeListener { _, checkedId ->
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
        }
    }
}