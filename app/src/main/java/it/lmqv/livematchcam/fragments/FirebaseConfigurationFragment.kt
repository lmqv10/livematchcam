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
import it.lmqv.livematchcam.databinding.FragmentFirebaseConfigurationBinding
import it.lmqv.livematchcam.extensions.launchOnCreated
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.viewmodels.FloatingActionsViewModel
import it.lmqv.livematchcam.utils.SyncStrategy
import it.lmqv.livematchcam.repositories.MatchSyncStrategyRepository
import kotlin.getValue

class FirebaseConfigurationFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = FirebaseConfigurationFragment()
    }

    private val floatingActionsViewModel: FloatingActionsViewModel by activityViewModels()

    private var _binding: FragmentFirebaseConfigurationBinding? = null
    private val binding get() = _binding!!

    private val firebaseFragment = FirebaseFragment.newInstance()
    private val matchInfoFragment = MatchInfoFragment.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        childFragmentManager
            .beginTransaction()
            .replace(R.id.firebaseContainer, firebaseFragment)
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
        _binding = FragmentFirebaseConfigurationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args: FirebaseConfigurationFragmentArgs by navArgs()
        val syncStrategy: SyncStrategy = args.syncStrategy
        MatchSyncStrategyRepository.initialize(requireActivity(), syncStrategy)

        launchOnCreated {
            MatchRepository.firebaseAccountData.collect { firebaseAccountData ->
                var streams = firebaseAccountData.streams
                if (streams.isEmpty()) {
                    floatingActionsViewModel.setNoActions()
                } else {
                    //floatingActionsViewModel.setWithRemoteScoreActions((activity as? INavigateDrawerActivity))
                    floatingActionsViewModel.setFirebaseAccountData(firebaseAccountData, (activity as? INavigateDrawerActivity))
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}