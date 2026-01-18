package it.lmqv.livematchcam.factories.sports

import it.lmqv.livematchcam.factories.ISportsComponentsFactory
import it.lmqv.livematchcam.fragments.sports.IBannersControlFragment
import it.lmqv.livematchcam.fragments.sports.banners.BannersControlFragment
import it.lmqv.livematchcam.fragments.sports.basket.BasketControlBarFragment
import it.lmqv.livematchcam.fragments.sports.basket.BasketRemoteControlFragment
import it.lmqv.livematchcam.fragments.sports.basket.BasketScoreBoardFragment

class BasketFragmentsFactory : ISportsComponentsFactory {

    companion object {
        fun newInstance() = BasketFragmentsFactory()
    }

    private val _controlBar: BasketControlBarFragment by lazy {
        BasketControlBarFragment.newInstance()
    }

    override fun getControlBar() : BasketControlBarFragment {
        return _controlBar
    }

    private val _scoreBoard: BasketScoreBoardFragment by lazy {
        BasketScoreBoardFragment.newInstance()
    }

    override fun getScoreBoard(): BasketScoreBoardFragment {
        return _scoreBoard
    }

    private val _remoteScoreControl: BasketRemoteControlFragment by lazy {
        BasketRemoteControlFragment.newInstance()
    }

    override fun getRemoteControl() : BasketRemoteControlFragment {
        return _remoteScoreControl
    }

    private val _bannersControl: IBannersControlFragment by lazy {
        BannersControlFragment()
    }

    override fun getBannersControl(): IBannersControlFragment {
        return _bannersControl
    }

}