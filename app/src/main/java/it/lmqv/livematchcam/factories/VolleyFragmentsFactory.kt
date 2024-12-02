package it.lmqv.livematchcam.factories

import it.lmqv.livematchcam.fragments.volley.VolleyControlBarFragment
import it.lmqv.livematchcam.fragments.volley.VolleyScoreBoardFragment

object VolleyFragmentsFactory : ISportsComponentsFactory {

    private val _ControlBar: VolleyControlBarFragment by lazy {
        VolleyControlBarFragment.newInstance(_ScoreBoard)
    }
    override fun getControlBar() : VolleyControlBarFragment {
        return _ControlBar
    }

    private val _ScoreBoard: VolleyScoreBoardFragment by lazy {
        VolleyScoreBoardFragment.newInstance()
    }
    override fun getScoreBoard(): VolleyScoreBoardFragment {
        return _ScoreBoard
    }
}