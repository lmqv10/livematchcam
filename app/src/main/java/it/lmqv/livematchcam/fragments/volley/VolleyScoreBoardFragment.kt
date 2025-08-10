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
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.extensions.setShirtByColor
import it.lmqv.livematchcam.firebase.SetScore
import it.lmqv.livematchcam.fragments.BaseScoreBoardFragment
import it.lmqv.livematchcam.repositories.MatchRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.firebase.VolleyScore

class VolleyScoreBoardFragment : BaseScoreBoardFragment() {

    companion object {
        fun newInstance() = VolleyScoreBoardFragment()
    }

    private var setsControls: List<Pair<TextView, TextView>> = mutableListOf();

    private var _binding: FragmentVolleyScoreBoardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
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

        MatchRepository.homeTeam.observe(viewLifecycleOwner) { team ->
            binding.homeTeam.text = team
            onUpdateCallback?.refresh()
        }

        MatchRepository.guestTeam.observe(viewLifecycleOwner) { team ->
            binding.awayTeam.text = team
            onUpdateCallback?.refresh()
        }

        launchOnStarted {
            combine(
                MatchRepository.homePrimaryColorHex,
                MatchRepository.homeLogo) {
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
                    binding.homeShirt.setShirtByColor(colorHex.toColorInt())
                    onUpdateCallback?.refresh()
                }
            }
        }

        launchOnStarted {
            combine(
                MatchRepository.guestPrimaryColorHex,
                MatchRepository.guestLogo) {
                    color, logo -> Pair(color, logo)
            }.collect { (colorHex, logoURL) ->
                binding.awayColorBar.setBackgroundColor(colorHex.toColorInt())

                if (logoURL.isNotEmpty()) {
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

        viewLifecycleOwner.lifecycleScope.launch {
            MatchRepository.score.collectLatest { scoreInstance ->
                //Logd("VolleyScoreBoard::score.collectLatest:: $scoreInstance")
                try {
                    val score = scoreInstance as VolleyScore
                    updateScore(score.sets)
                    updateLeagueDescription(score.league)
                    onUpdateCallback?.refresh()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Loge("VolleyScoreBoard::Exception:: ${e.message.toString()}")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

                val winColor = ContextCompat.getColor(requireContext(), R.color.secondary_dark)
                val loseColor = ContextCompat.getColor(requireContext(), R.color.BLACK)

                if (index < setsSize - 1) {
                    if (score.home > score.guest) {
                        controls.first.setTextColor(winColor)
                        controls.second.setTextColor(loseColor)
                    } else {
                        controls.first.setTextColor(loseColor)
                        controls.second.setTextColor(winColor)
                    }
                } else {
                    controls.second.setTextColor(loseColor)
                    controls.second.setTextColor(loseColor)
                }
            } else {
                controls.first.visibility = View.GONE
                controls.second.visibility = View.GONE
            }

        }
    }

    private fun updateLeagueDescription(description: String) {
        if (description == "") {
            binding.matchDescription.visibility = View.GONE
        } else {
            binding.matchDescription.visibility = View.VISIBLE
        }
        binding.matchDescription.text = description
    }
}