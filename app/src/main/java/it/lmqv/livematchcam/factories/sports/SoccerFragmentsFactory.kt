package it.lmqv.livematchcam.factories.sports

import it.lmqv.livematchcam.fragments.sports.IFiltersControlFragment
import it.lmqv.livematchcam.fragments.sports.banners.FiltersControlFragment
import it.lmqv.livematchcam.fragments.sports.soccer.SoccerControlBarFragment
import it.lmqv.livematchcam.fragments.sports.soccer.SoccerRemoteControlFragment
import it.lmqv.livematchcam.fragments.sports.soccer.SoccerScoreBoardFragment

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

    private val _filtersControlFragment: IFiltersControlFragment by lazy {
        FiltersControlFragment()
    }

    override fun getFiltersControl(): IFiltersControlFragment {
        return _filtersControlFragment
    }

    override fun getFiltersSlimControl(): IFiltersControlFragment {
        return _filtersControlFragment
    }
}