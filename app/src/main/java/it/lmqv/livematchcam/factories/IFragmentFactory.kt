package it.lmqv.livematchcam.factories

import it.lmqv.livematchcam.fragments.BaseScoreBoardFragment
import it.lmqv.livematchcam.fragments.IControlBarFragment
import it.lmqv.livematchcam.fragments.IRemoteControlFragment
import it.lmqv.livematchcam.fragments.IScoreBoardFragment

interface ISportsComponentsFactory {
    fun getControlBar() : IControlBarFragment
    fun getScoreBoard() : IScoreBoardFragment<BaseScoreBoardFragment>
    fun getRemoteControl() : IRemoteControlFragment
}