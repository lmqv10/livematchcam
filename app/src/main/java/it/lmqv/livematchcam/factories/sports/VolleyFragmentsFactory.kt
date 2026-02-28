package it.lmqv.livematchcam.factories.sports

import it.lmqv.livematchcam.fragments.sports.IFiltersControlFragment
import it.lmqv.livematchcam.fragments.sports.banners.BannersControlFragment
import it.lmqv.livematchcam.fragments.sports.banners.FiltersControlFragment
import it.lmqv.livematchcam.fragments.sports.banners.FiltersControlSlimFragment
import it.lmqv.livematchcam.fragments.sports.volley.VolleyControlBarFragment
import it.lmqv.livematchcam.fragments.sports.volley.VolleyRemoteControlFragment
import it.lmqv.livematchcam.fragments.sports.volley.VolleyScoreBoardFragment

class VolleyFragmentsFactory : ISportsComponentsFactory {

    companion object {
        fun newInstance() = VolleyFragmentsFactory()
    }

    private val _controlBar: VolleyControlBarFragment by lazy {
        VolleyControlBarFragment.newInstance()
    }
    override fun getControlBar() : VolleyControlBarFragment {
        return _controlBar
    }

    private val _scoreBoard: VolleyScoreBoardFragment by lazy {
        VolleyScoreBoardFragment.newInstance()
    }
    override fun getScoreBoard(): VolleyScoreBoardFragment {
        return _scoreBoard
    }
    private val _remoteScoreControl: VolleyRemoteControlFragment by lazy {
        VolleyRemoteControlFragment.newInstance()
    }

    override fun getRemoteControl() : VolleyRemoteControlFragment {
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