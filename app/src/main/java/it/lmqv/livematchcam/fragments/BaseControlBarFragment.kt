package it.lmqv.livematchcam.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import it.lmqv.livematchcam.viewmodels.AwayScoreBoardViewModel
import it.lmqv.livematchcam.viewmodels.HomeScoreBoardViewModel

interface IControlBarFragment {
}

abstract class BaseControlBarFragment : Fragment(), IControlBarFragment {

    protected val homeTeamViewModel: HomeScoreBoardViewModel by activityViewModels()
    protected val awayTeamViewModel: AwayScoreBoardViewModel by activityViewModels()

}