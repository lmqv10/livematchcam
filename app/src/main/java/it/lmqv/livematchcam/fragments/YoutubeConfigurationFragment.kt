package it.lmqv.livematchcam.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import it.lmqv.livematchcam.INavigateDrawerActivity
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.viewmodels.FloatingActionsViewModel
import it.lmqv.livematchcam.utils.SyncStrategy
import it.lmqv.livematchcam.databinding.FragmentYoutubeConfigurationBinding
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.repositories.MatchSyncStrategyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.getValue

class YoutubeConfigurationFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = YoutubeConfigurationFragment()
    }

    private var _binding: FragmentYoutubeConfigurationBinding? = null
    private val binding get() = _binding!!

    private val youtubeFragment = YoutubeFragment.newInstance()
    private val matchInfoFragment = MatchInfoFragment.newInstance()

    private val syncJob = SupervisorJob()
    private val syncScope = CoroutineScope(syncJob + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        childFragmentManager
            .beginTransaction()
            .replace(R.id.youtubeContainer, youtubeFragment)
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
        _binding = FragmentYoutubeConfigurationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        syncScope.launch {
            try {
                val args: YoutubeConfigurationFragmentArgs by navArgs()
                val syncStrategy: SyncStrategy = args.syncStrategy
                MatchSyncStrategyRepository.initialize(requireActivity(), syncStrategy)
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        syncJob.cancel()
    }
}