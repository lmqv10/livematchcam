package it.lmqv.livematchcam.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import it.lmqv.livematchcam.databinding.FragmentScoreBoardBinding
import it.lmqv.livematchcam.extensions.formatTime
import it.lmqv.livematchcam.extensions.setShirtByColor
import it.lmqv.livematchcam.viewmodels.AwayScoreBoardViewModel
import it.lmqv.livematchcam.viewmodels.HomeScoreBoardViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ScoreBoardFragment : Fragment() {

    companion object {
        fun newInstance() = ScoreBoardFragment()
    }

    interface OnUpdateCallback {
        fun refresh()
    }
    private var onUpdateCallback: OnUpdateCallback? = null
    fun setOnUpdate(callback: OnUpdateCallback) {
        this.onUpdateCallback = callback
    }

    private val homeTeamViewModel: HomeScoreBoardViewModel by activityViewModels()
    private val awayTeamViewModel: AwayScoreBoardViewModel by activityViewModels()

    private var _binding: FragmentScoreBoardBinding? = null
    private val binding get() = _binding!!

    private var currentPeriod = "1T"
    private var timeElapsedInSeconds = 0
    private var job: Job? = null
    private var isInPause = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScoreBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeTeamViewModel.name.observe(viewLifecycleOwner, Observer { team ->
            binding.homeTeam.text = team
        })
        homeTeamViewModel.score.observe(viewLifecycleOwner, Observer { score ->
            binding.homeScore.text = score.toString()
            onUpdateCallback?.refresh()
        })

        awayTeamViewModel.name.observe(viewLifecycleOwner, Observer { team ->
            binding.awayTeam.text = team
        })
        awayTeamViewModel.score.observe(viewLifecycleOwner, Observer { score ->
            binding.awayScore.text = score.toString()
            onUpdateCallback?.refresh()
        })

        homeTeamViewModel.logo.observe(viewLifecycleOwner, Observer { color ->
            binding.homeLogo.setShirtByColor(color)
        })
        awayTeamViewModel.logo.observe(viewLifecycleOwner, Observer { color ->
            binding.awayLogo.setShirtByColor(color)
        })
    }

    fun startTime() {
        this.isInPause = false
        if (job == null || job?.isActive == false) {
            job = CoroutineScope(Dispatchers.Main).launch {
                while (isActive) {
                    binding.matchTime.text = formatTime(timeElapsedInSeconds)
                    delay(1000)
                    if (!isInPause) {
                        timeElapsedInSeconds++
                        onUpdateCallback?.refresh()
                    }
                }
            }
        }
    }

    fun pauseTime() {
        isInPause = true
    }

    fun resetTime() {
        job?.cancel()
        job = null
        timeElapsedInSeconds = 0
        isInPause = false
        binding.matchTime.text = formatTime(0)
        onUpdateCallback?.refresh()
    }

    fun isInPause() : Boolean {
        return this.isInPause
    }

    fun togglePeriod() {
        if (currentPeriod == "1T") {
            currentPeriod = "2T"
        } else {
            currentPeriod = "1T"
        }
        binding.matchPeriod.text = currentPeriod
        onUpdateCallback?.refresh()
    }

}