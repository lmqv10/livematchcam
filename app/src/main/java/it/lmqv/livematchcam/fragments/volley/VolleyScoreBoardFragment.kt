package it.lmqv.livematchcam.fragments.volley

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import coil.load
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentVolleyScoreBoardBinding
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.extensions.setShirtByColor
import it.lmqv.livematchcam.firebase.SetScore
import it.lmqv.livematchcam.firebase.VolleyScore
import it.lmqv.livematchcam.fragments.BaseScoreBoardFragment
import kotlinx.coroutines.flow.combine

class VolleyScoreBoardFragment : BaseScoreBoardFragment() {

    companion object {
        fun newInstance() = VolleyScoreBoardFragment()
    }

    private var setsControls: List<Pair<TextView, TextView>> = mutableListOf();

    private var _binding: FragmentVolleyScoreBoardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVolleyScoreBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        this.setsControls = mutableListOf(
            Pair(binding.homeScore1set, binding.awayScore1set),
            Pair(binding.homeScore2set, binding.awayScore2set),
            Pair(binding.homeScore3set, binding.awayScore3set),
            Pair(binding.homeScore4set, binding.awayScore4set),
            Pair(binding.homeScore5set, binding.awayScore5set),
        )

        matchViewModel.homeTeam.observe(viewLifecycleOwner) { team ->
            binding.homeTeam.text = team
            onUpdateCallback?.refresh()
        }

        matchViewModel.guestTeam.observe(viewLifecycleOwner) { team ->
            binding.awayTeam.text = team
            onUpdateCallback?.refresh()
        }

        /*matchViewModel.homeColorHex.observe(viewLifecycleOwner) { homeColorHex ->
            binding.homeLogo.setShirtByColor(Color.parseColor(homeColorHex))
            onUpdateCallback?.refresh()
        }
        matchViewModel.guestColorHex.observe(viewLifecycleOwner) { guestColorHex ->
            binding.awayLogo.setShirtByColor(Color.parseColor(guestColorHex))
            onUpdateCallback?.refresh()
        }*/

        launchOnStarted {
            combine(
                matchViewModel.homeColorHex,
                matchViewModel.homeLogo) {
                    color, logo -> Pair(color, logo)
            }.collect { (colorHex, logoURL) ->
                binding.homeColorBar.setBackgroundColor(Color.parseColor(colorHex))

                if (!logoURL.isNullOrEmpty()) {
                    binding.homeLogo.visibility = View.VISIBLE
                    binding.homeShirt.visibility = View.INVISIBLE
                    binding.homeLogo.load(logoURL) {
                        placeholder(R.drawable.shirt_white)
                        error(R.drawable.shirt_white)
                        allowHardware(false)
                        listener(
                            onSuccess = { _, _ ->
                                onUpdateCallback?.refresh()
                            }
                        )
                    }
                } else {
                    binding.homeLogo.visibility = View.GONE
                    binding.homeShirt.visibility = View.VISIBLE
                    binding.homeShirt.setShirtByColor(Color.parseColor(colorHex))
                    onUpdateCallback?.refresh()
                }
            }
        }

        launchOnStarted {
            combine(
                matchViewModel.guestColorHex,
                matchViewModel.guestLogo) {
                    color, logo -> Pair(color, logo)
            }.collect { (colorHex, logoURL) ->
                binding.awayColorBar.setBackgroundColor(Color.parseColor(colorHex))

                if (!logoURL.isNullOrEmpty()) {
                    binding.awayLogo.visibility = View.VISIBLE
                    binding.awayShirt.visibility = View.INVISIBLE
                    binding.awayLogo.load(logoURL) {
                        placeholder(R.drawable.shirt_white)
                        error(R.drawable.shirt_white)
                        allowHardware(false)
                        listener(
                            onSuccess = { _, _ ->
                                onUpdateCallback?.refresh()
                            }
                        )
                    }
                } else {
                    binding.awayLogo.visibility = View.GONE
                    binding.awayShirt.visibility = View.VISIBLE
                    binding.awayShirt.setShirtByColor(Color.parseColor(colorHex))
                    onUpdateCallback?.refresh()
                }
            }
        }

        matchViewModel.score.observe(viewLifecycleOwner) { scoreInstance ->
            try {
                //Logd("VolleyScoreBoard::score.observe")
                val score = scoreInstance as VolleyScore
                //Logd("VolleyScoreBoard::score $score")
                updateScore(score.sets)
                //updateSetsScore(score.sets)
                updateLeagueDescription(score.league)
            } catch (e: Exception) {
                //Logd("ScoreBoard::Exception ${e.message}")
            }
            onUpdateCallback?.refresh()
        }

        /*scoreViewModel.currentSet.observe(viewLifecycleOwner) { currentSet ->
            this.currentSet = currentSet
            onUpdateCallback?.refresh()
        }*/

        /*scoreViewModel.matchLeague.observe(viewLifecycleOwner) { currentDescription ->
            if (currentDescription == "") {
                binding.matchDescription.visibility = View.GONE
            } else {
                binding.matchDescription.visibility = View.VISIBLE
            }

            binding.matchDescription.text = currentDescription
            onUpdateCallback?.refresh()
        }*/

        /*scoreViewModel.score.observe(viewLifecycleOwner) { score ->
            binding.homeScore1set.text = score[Set.SET1]?.homePoints.toString()
            binding.awayScore1set.text = score[Set.SET1]?.awayPoints.toString()

            binding.homeScore2set.text = score[Set.SET2]?.homePoints.toString()
            binding.awayScore2set.text = score[Set.SET2]?.awayPoints.toString()

            binding.homeScore3set.text = score[Set.SET3]?.homePoints.toString()
            binding.awayScore3set.text = score[Set.SET3]?.awayPoints.toString()

            binding.homeScore4set.text = score[Set.SET4]?.homePoints.toString()
            binding.awayScore4set.text = score[Set.SET4]?.awayPoints.toString()

            binding.homeScore5set.text = score[Set.SET5]?.homePoints.toString()
            binding.awayScore5set.text = score[Set.SET5]?.awayPoints.toString()

            val sets = score.values
                .map { x ->
                    Pair((x.homePoints >= x.targetPoints && x.homePoints - x.awayPoints >= 2).toInt(),
                        (x.awayPoints >= x.targetPoints && x.awayPoints - x.homePoints >= 2).toInt())
                }
                .reduce { sets, result ->
                    Pair(sets.first + result.first, sets.second + result.second)  }

            binding.homeScoreSets.text = sets.first.toString()
            binding.awayScoreSets.text = sets.second.toString()
            onUpdateCallback?.refresh()
        }*/

    }

    private fun updateScore(sets: List<SetScore>) {
        val setsSize = sets.size

        setsControls.forEachIndexed { index, controls ->
            if (index < setsSize) {
                var score = sets[index]
                controls.first.text = score.home.toString()
                controls.second.text = score.guest.toString()

                controls.first.visibility = View.VISIBLE
                controls.second.visibility = View.VISIBLE

                if (index < setsSize - 1) {
                    val winColor = ContextCompat.getColor(requireContext(), R.color.secondary_dark)
                    if (score.home > score.guest) {
                        controls.first.setTextColor(winColor)
                    } else {
                        controls.second.setTextColor(winColor)
                    }
                }
            } else {
                controls.first.visibility = View.GONE
                controls.second.visibility = View.GONE
            }

        }
    }

    /*private fun updateSetsScore(setsScore: List<SetScore>) {
        val sets = setsScore
            .map { x ->
                Pair((x.home > x.guest).toInt(), (x.guest < x.home).toInt())
            }
            .reduce { sets, result ->
                Pair(sets.first + result.first, sets.second + result.second)  }

        binding.homeScoreSets.text = sets.first.toString()
        binding.awayScoreSets.text = sets.second.toString()
    }*/

    private fun updateLeagueDescription(description: String) {
        if (description == "") {
            binding.matchDescription.visibility = View.GONE
        } else {
            binding.matchDescription.visibility = View.VISIBLE
        }
        binding.matchDescription.text = description
    }
}