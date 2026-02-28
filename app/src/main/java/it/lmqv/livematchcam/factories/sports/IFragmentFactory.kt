package it.lmqv.livematchcam.factories.sports

import it.lmqv.livematchcam.fragments.sports.BaseScoreBoardFragment
import it.lmqv.livematchcam.fragments.sports.IFiltersControlFragment
import it.lmqv.livematchcam.fragments.sports.IControlBarFragment
import it.lmqv.livematchcam.fragments.sports.IRemoteControlFragment
import it.lmqv.livematchcam.fragments.sports.IScoreBoardFragment

interface ISportsComponentsFactory {
    fun getControlBar() : IControlBarFragment
    fun getScoreBoard() : IScoreBoardFragment<BaseScoreBoardFragment>
    fun getRemoteControl() : IRemoteControlFragment
    fun getFiltersControl() : IFiltersControlFragment
    fun getFiltersSlimControl() : IFiltersControlFragment
}