package it.lmqv.livematchcam.fragments.volley

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.pedro.srt.utils.toInt
import it.lmqv.livematchcam.databinding.FragmentVolleyScoreBoardBinding
import it.lmqv.livematchcam.extensions.setShirtByColor
import it.lmqv.livematchcam.fragments.BaseScoreBoardFragment
import it.lmqv.livematchcam.viewmodels.Set
import it.lmqv.livematchcam.viewmodels.VolleyScoreViewModel

class VolleyScoreBoardFragment : BaseScoreBoardFragment() {

    companion object {
        fun newInstance() = VolleyScoreBoardFragment()
    }

    private val scoreViewModel: VolleyScoreViewModel by activityViewModels()

    private var _binding: FragmentVolleyScoreBoardBinding? = null
    private val binding get() = _binding!!

    //private var currentSet = Set.SET1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVolleyScoreBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        matchViewModel.homeTeam.observe(viewLifecycleOwner) { team ->
            binding.homeTeam.text = team
            onUpdateCallback?.refresh()
        }

        matchViewModel.guestTeam.observe(viewLifecycleOwner) { team ->
            binding.awayTeam.text = team
            onUpdateCallback?.refresh()
        }

        /*scoreViewModel.currentSet.observe(viewLifecycleOwner) { currentSet ->
            this.currentSet = currentSet
            onUpdateCallback?.refresh()
        }*/

        scoreViewModel.matchLeague.observe(viewLifecycleOwner) { currentDescription ->
            if (currentDescription == "") {
                binding.matchDescription.visibility = View.GONE
            } else {
                binding.matchDescription.visibility = View.VISIBLE
            }

            binding.matchDescription.text = currentDescription
            onUpdateCallback?.refresh()
        }

        scoreViewModel.score.observe(viewLifecycleOwner) { score ->
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
        }

        matchViewModel.homeColorHex.observe(viewLifecycleOwner) { homeColorHex ->
            binding.homeLogo.setShirtByColor(Color.parseColor(homeColorHex))
            onUpdateCallback?.refresh()
        }
        matchViewModel.guestColorHex.observe(viewLifecycleOwner) { guestColorHex ->
            binding.awayLogo.setShirtByColor(Color.parseColor(guestColorHex))
            onUpdateCallback?.refresh()
        }
    }
}