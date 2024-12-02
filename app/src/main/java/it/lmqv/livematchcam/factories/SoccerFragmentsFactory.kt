package it.lmqv.livematchcam.factories

import it.lmqv.livematchcam.fragments.soccer.SoccerControlBarFragment
import it.lmqv.livematchcam.fragments.soccer.SoccerScoreBoardFragment

object SoccerFragmentsFactory : ISportsComponentsFactory {

    private val _ControlBar: SoccerControlBarFragment by lazy {
        SoccerControlBarFragment.newInstance(_ScoreBoard)
    }
    override fun getControlBar() : SoccerControlBarFragment {
        return _ControlBar
    }

    private val _ScoreBoard: SoccerScoreBoardFragment by lazy {
        SoccerScoreBoardFragment.newInstance()
    }
    override fun getScoreBoard(): SoccerScoreBoardFragment {
        return _ScoreBoard
    }
}