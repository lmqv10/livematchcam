package it.lmqv.livematchcam

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ScoreBoardFragment : Fragment() {

    interface OnUpdateCallback {
        fun refresh()
    }

    private var onUpdateCallback: OnUpdateCallback? = null

    fun setOnUpdate(callback: OnUpdateCallback) {
        this.onUpdateCallback = callback
    }

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var homeTeam : TextView? = null
    private var awayTeam : TextView? = null

    private var homeScore : TextView? = null
    private var awayScore : TextView? = null

    var currentHomeScore = 0
    var currentAwayScore = 0

    var currentPeriod = "1T"

    private var matchPeriod : TextView? = null
    private var matchTime : TextView? = null

    private var timeElapsedInSeconds = 0

    private var job: Job? = null

    var isInPause = false

    private var homeLogo : ImageView? = null
    private var awayLogo : ImageView? = null

    fun setHomeTeam(value: String) {
        this.homeTeam?.text = value
    }
    fun setAwayTeam(value: String) {
        this.awayTeam?.text = value
    }

    fun setHomeLogo(color: Int) {
        this.homeLogo?.setColorFilter(color)
    }

    fun setAwayLogo(color: Int) {
        this.awayLogo?.setColorFilter(color)
    }

    fun startTime() {
        isInPause = false
        if (job == null || job?.isActive == false) {
            job = CoroutineScope(Dispatchers.Main).launch {
                while (isActive) {
                    matchTime?.text = formatTime(timeElapsedInSeconds)
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
        matchTime?.text = formatTime(0)
    }

    fun togglePeriod() {
        if (currentPeriod == "1T") {
            currentPeriod = "2T"
        } else {
            currentPeriod = "1T"
        }
        this.matchPeriod?.text = currentPeriod
    }

    fun addHomeScore(step: Int = 1) {
        this.currentHomeScore += step
        this.currentHomeScore = 0.coerceAtLeast(this.currentHomeScore)
        this.homeScore?.text = "" + this.currentHomeScore
    }

    fun addAwayScore(step: Int = 1) {
        this.currentAwayScore += step
        this.currentAwayScore = 0.coerceAtLeast(this.currentAwayScore)
        this.awayScore?.text = "" + this.currentAwayScore
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_score_board, container, false)

        this.homeTeam = view.findViewById(R.id.home_team)
        this.homeTeam?.text = param1

        this.awayTeam = view.findViewById(R.id.away_team)
        this.awayTeam?.text = param2

        this.homeScore = view.findViewById(R.id.home_score)
        this.awayScore = view.findViewById(R.id.away_score)

        this.matchTime = view.findViewById(R.id.match_time)
        this.matchPeriod = view.findViewById(R.id.match_period)

        this.homeLogo = view.findViewById(R.id.home_logo)
        this.awayLogo = view.findViewById(R.id.away_logo)

        //this.scoreBoard = view.findViewById(R.id.score_board)
        return view
    }

    private fun formatTime(seconds: Int = 0): String {
        return String.format("%02d:%02d", seconds / 60, seconds % 60)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ScoreBoardFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ScoreBoardFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}