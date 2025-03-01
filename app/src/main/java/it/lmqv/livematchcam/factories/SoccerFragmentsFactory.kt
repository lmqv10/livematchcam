package it.lmqv.livematchcam.factories

import it.lmqv.livematchcam.fragments.soccer.SoccerControlBarFragment
import it.lmqv.livematchcam.fragments.soccer.SoccerRemoteControlFragment
import it.lmqv.livematchcam.fragments.soccer.SoccerScoreBoardFragment

class SoccerFragmentsFactory : ISportsComponentsFactory {

    companion object {
        fun newInstance() = SoccerFragmentsFactory()
    }

    private val _controlBar: SoccerControlBarFragment by lazy {
        SoccerControlBarFragment.newInstance()
    }

    override fun getControlBar() : SoccerControlBarFragment {
        return _controlBar
    }

    private val _scoreBoard: SoccerScoreBoardFragment by lazy {
        SoccerScoreBoardFragment.newInstance()
    }

    override fun getScoreBoard(): SoccerScoreBoardFragment {
        return _scoreBoard
    }

    private val _remoteScoreControl: SoccerRemoteControlFragment by lazy {
        SoccerRemoteControlFragment.newInstance()
    }

    override fun getRemoteControl() : SoccerRemoteControlFragment {
        return _remoteScoreControl
    }
}