package it.lmqv.livematchcam.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import it.lmqv.livematchcam.INavigateDrawerActivity
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.viewmodels.FloatingActionsViewModel
import it.lmqv.livematchcam.utils.SyncStrategy
import it.lmqv.livematchcam.databinding.FragmentServerConfigurationBinding
import it.lmqv.livematchcam.repositories.MatchSyncStrategyRepository
import kotlin.getValue

class ServerConfigurationFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = ServerConfigurationFragment()
    }

    private val actionsViewModel: FloatingActionsViewModel by activityViewModels()

    private var _binding: FragmentServerConfigurationBinding? = null
    private val binding get() = _binding!!

    private val serverFragment = ServerFragment.newInstance()
    private val matchInfoFragment = MatchInfoFragment.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        childFragmentManager
            .beginTransaction()
            .replace(R.id.serverContainer, serverFragment)
            .commit()

        childFragmentManager
            .beginTransaction()
            .replace(R.id.matchInfoContainer, matchInfoFragment)
            .commit()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentServerConfigurationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args: ServerConfigurationFragmentArgs by navArgs()
        val syncStrategy: SyncStrategy = args.syncStrategy
        MatchSyncStrategyRepository.initialize(requireActivity(), syncStrategy)

        actionsViewModel.setOnlyStreamActions((activity as? INavigateDrawerActivity))
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}