package it.lmqv.livematchcam.fragments

import android.accounts.Account
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import it.lmqv.livematchcam.adapters.BroadcastItem
import it.lmqv.livematchcam.adapters.BroadcastsAdapter
import it.lmqv.livematchcam.auth.AuthResult
import it.lmqv.livematchcam.databinding.FragmentYoutubeBinding
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.extensions.formatDate
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.extensions.showQRCode
import it.lmqv.livematchcam.viewmodels.AccountViewModel
import it.lmqv.livematchcam.viewmodels.YoutubeViewModel
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

    private val youtubeViewModel: YoutubeViewModel by activityViewModels()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launchOnStarted {
            youtubeViewModel.liveBroadcasts.collect { liveBroadcasts ->
                var liveBroadcastItems = liveBroadcasts.map { x ->
                    BroadcastItem(
                        x.snippet.thumbnails.standard.url,
                        "${x.snippet.title}", //"${x.snippet.title} - ${x.contentDetails.boundStreamId}",
                        formatDate(x.snippet.scheduledStartTime),
                        x.id,
                        x.contentDetails.boundStreamId,
                        x.status.lifeCycleStatus)
                }

                val adapterServer = BroadcastsAdapter(requireActivity(), liveBroadcastItems)
                binding.spinnerBroadcast.adapter = adapterServer
            }
        }

        launchOnStarted {
            accountViewModel.authState.collectLatest { state ->
                val account = when (state) {
                    is AuthResult.Authenticated -> state.account.account
                    is AuthResult.Unauthenticated, is AuthResult.Error -> null
                }
                //Logd("YouTubeFragment. Google Account Name :${account}");
                if (account != null) {
                    handleSignIn(account)
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
                    val itemsList = List(binding.spinnerBroadcast.adapter.count) { index ->
                        binding.spinnerBroadcast.adapter.getItem(index)!! as BroadcastItem
                    }
                    val selectedPosition = max(0, itemsList.indexOfFirst { it.id == currentBroadcast.id })
                    binding.spinnerBroadcast.setSelection(selectedPosition)
                    var boundStreamId = currentBroadcast.boundStreamId
                    youtubeViewModel.setCurrentBoundStreamId(boundStreamId)
                }
            }
        }

        launchOnStarted {
            youtubeViewModel.streamURL.collect { streamURL ->
                binding.serverUrl.text = streamURL
            }
        }

        binding.spinnerBroadcast.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                lifecycleScope.launch {
                    val selectedBroadcast = parent.getItemAtPosition(position) as BroadcastItem
                    youtubeViewModel.setCurrentBroadcast(selectedBroadcast)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
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
            accountViewModel.authState.value.let {
                val account = when (it) {
                    is AuthResult.Authenticated -> it.account.account
                    is AuthResult.Unauthenticated, is AuthResult.Error -> null
                }
                if (account != null) {
                    handleSignIn(account)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun handleSignIn(account: Account?) {
        CoroutineScope(Dispatchers.IO).launch {
            streamsListYouTube(account)
            broadcastListYouTube(account)
        }
    }

    private fun streamsListYouTube(account: Account?) {
        try {
            val youtubeService = getYouTubeService(account)
            val request = youtubeService.liveStreams().list("id,snippet,cdn,status")
            request.mine = true // Retrieve livestreams from the authenticated user
            val response = request.execute()
            youtubeViewModel.setLiveStreams(response.items)
        }
        catch (e: Exception) {
            Loge("Exception:: ${e.message.toString()}")
        }
    }

    private fun broadcastListYouTube(account: Account?) {
        try {
            val youtubeService = getYouTubeService(account)
            val request = youtubeService.liveBroadcasts().list("id,snippet,contentDetails,status")
            request.mine = true // Retrieve broadcasts from the authenticated user
            val response = request.execute()
            youtubeViewModel.setLiveBroadcasts(response.items)
        }
        catch (e: Exception) {
            Loge("Exception:: ${e.message.toString()}")
        }
    }

    private fun getYouTubeService(account: Account?) : YouTube {
        val transport = NetHttpTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()

        val credential = GoogleAccountCredential.usingOAuth2(
            requireContext(), listOf("https://www.googleapis.com/auth/youtube")
        )
        credential.selectedAccountName = account?.name ?: ""

        val youtubeService = YouTube.Builder(transport, jsonFactory, credential)
            .setApplicationName("LiveMatchCam")
            .build()

        return youtubeService;
    }
}