package it.lmqv.livematchcam.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import it.lmqv.livematchcam.adapters.BroadcastsAdapter
import it.lmqv.livematchcam.adapters.LiveBroadcastItem
import it.lmqv.livematchcam.services.auth.AuthResult
import it.lmqv.livematchcam.databinding.FragmentYoutubeBinding
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.extensions.showQRCode
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.services.youtube.YouTubeClientProvider
import it.lmqv.livematchcam.viewmodels.AccountViewModel
import it.lmqv.livematchcam.viewmodels.YoutubeViewModel
import it.lmqv.livematchcam.viewmodels.YoutubeViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.max

class YoutubeFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = YoutubeFragment()
    }

    private val youtubeViewModel: YoutubeViewModel by activityViewModels {
        YoutubeViewModelFactory(requireActivity().application, YouTubeClientProvider.get())
    }
    private val accountViewModel: AccountViewModel by activityViewModels()

    private var _binding: FragmentYoutubeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentYoutubeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launchOnStarted {
            youtubeViewModel.liveBroadcasts.collect { liveBroadcasts ->
                var liveBroadcastItems = liveBroadcasts
                    .map { x -> LiveBroadcastItem.EditBroadcast(x) }

                val adapterServer = BroadcastsAdapter(requireActivity(), liveBroadcastItems)
                binding.spinnerBroadcast.adapter = adapterServer
                binding.spinnerBroadcast.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        lifecycleScope.launch {
                            val selectedBroadcast =
                                parent.getItemAtPosition(position) as LiveBroadcastItem.EditBroadcast
                            youtubeViewModel.setCurrentBroadcast(selectedBroadcast)
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) { }
                }
            }
        }

        launchOnStarted {
            accountViewModel.authState.collectLatest { state ->
                when (state) {
                    is AuthResult.Authenticated -> {
                        CoroutineScope(Dispatchers.IO).launch {
                            youtubeViewModel.loadLiveStreams()
                            youtubeViewModel.loadLiveBroadcast()
                        }
                    }
                    is AuthResult.Error -> { }
                    AuthResult.Unauthenticated -> { }
                }
            }
        }

        launchOnStarted {
            youtubeViewModel.liveURL.collect { liveURL ->
                binding.liveUrl.text = liveURL
            }
        }

        launchOnStarted {
            youtubeViewModel.currentBroadcast.collect { currentBroadcast ->
                if (currentBroadcast != null) {
                    //Logd("youtubeFragment:currentBroadcast.collect::${currentBroadcast.broadcastId}")
                    val itemsList = List(binding.spinnerBroadcast.adapter.count) { index ->
                        binding.spinnerBroadcast.adapter.getItem(index)!! as LiveBroadcastItem.EditBroadcast
                    }
                    val selectedPosition = max(0, itemsList.indexOfFirst { it.broadcastId == currentBroadcast.broadcastId })
                    binding.spinnerBroadcast.setSelection(selectedPosition)
                    var boundStreamId = currentBroadcast.boundStreamId
                    youtubeViewModel.setCurrentBoundStreamId(boundStreamId)
                }
            }
        }

        launchOnStarted {
            MatchRepository.RTMPServerURI.collect { streamURL ->
                binding.serverUrl.text = streamURL
            }
        }

        binding.share.setOnClickListener {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, binding.liveUrl.text.toString())
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "Share Link Via"))
        }

        binding.qrcode.setOnClickListener {
            requireContext().showQRCode(binding.liveUrl.text.toString())
        }

        binding.refresh.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                youtubeViewModel.loadLiveBroadcast()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}