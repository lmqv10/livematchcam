package it.lmqv.livematchcam.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import it.lmqv.livematchcam.viewmodels.MatchViewModel

interface IRemoteControlFragment {
}

abstract class BaseRemoteControlFragment : Fragment(), IRemoteControlFragment {

    protected val matchViewModel: MatchViewModel by activityViewModels()
}