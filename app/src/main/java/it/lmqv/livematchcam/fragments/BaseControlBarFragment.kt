package it.lmqv.livematchcam.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import it.lmqv.livematchcam.viewmodels.MatchViewModel

interface IControlBarFragment {
}

abstract class BaseControlBarFragment : Fragment(), IControlBarFragment {

    protected val matchViewModel: MatchViewModel by activityViewModels()
    //protected val homeTeamViewModel: HomeScoreBoardViewModel by activityViewModels()
    //protected val awayTeamViewModel: AwayScoreBoardViewModel by activityViewModels()

}