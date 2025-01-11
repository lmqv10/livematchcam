package it.lmqv.livematchcam.fragments

import android.accounts.Account
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.adapters.BroadcastItem
import it.lmqv.livematchcam.adapters.BroadcastsAdapter
import it.lmqv.livematchcam.databinding.FragmentYoutubeBinding
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.formatDate
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.extensions.showQRCode
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.firebase.FirebaseDataManager
import it.lmqv.livematchcam.viewmodels.GoogleViewModel
import it.lmqv.livematchcam.viewmodels.YoutubeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.max

class YoutubeFragment : Fragment(), IServersFragment {

    companion object {
        fun newInstance() = YoutubeFragment()
    }

    //private val GOOGLE_APIS_AUTH_YOUTUBE : String = "https://www.googleapis.com/auth/youtube"
    //private val CLIENT_ID : String = "54641307189-6181k175ei3m80jnvot27qkfhfvmteqt.apps.googleusercontent.com"

    private val youtubeViewModel: YoutubeViewModel by activityViewModels()
    private val googleViewModel: GoogleViewModel by activityViewModels()

    private var _binding: FragmentYoutubeBinding? = null
    private val binding get() = _binding!!

    /*private val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(GOOGLE_APIS_AUTH_YOUTUBE))
        .requestServerAuthCode(CLIENT_ID, true)
        .build()

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if (task.isSuccessful) {
                val account = task.result
                handleSignIn(account)
                toast("account:${account.account?.name}")
            } else {
                // Handle sign-in failure.
                toast(task.exception?.message.toString())
            }
        } else {
            toast("googleSignInLauncher::failed::${result.resultCode}")
        }
    }*/

    /*fun getServerURI() : String {
        return youtubeViewModel.streamURL.value ?: ""
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*val account = GoogleSignIn.getLastSignedInAccount(requireActivity())
        if (account != null) {
            handleSignIn(account)
        }*/
    }


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
            /*combine(
                googleViewModel.account,
                youtubeViewModel.liveBroadcasts,
                googleViewModel.firebaseAccountKey)
            { account, liveBroadcasts, firebaseAccountKey -> Triple(account, liveBroadcasts, firebaseAccountKey) }
            .distinctUntilChanged()
            .collect { (account, liveBroadcasts, firebaseAccountKey) ->
            */

            youtubeViewModel.liveBroadcasts
            .collect { liveBroadcasts ->
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

                /*val accountName = account?.name

                if (!accountName.isNullOrEmpty() &&
                    !firebaseAccountKey.isNullOrEmpty() &&
                    !liveBroadcastItems.isNullOrEmpty()) {
                    //Logd("YouTubeFragment: googleViewModel.firebaseAccount.connect!")

                    FirebaseDataManager.getInstance()
                        .authenticateAccount(accountName, firebaseAccountKey, { account ->
                            //Logd("RealtimeDB Account Name: ${account.name}")
                            //Logd("RealtimeDB Admin: ${account.admin}")
                            toast("Connected as ${account.name}")
                            var validMatches = account.matches.filter {
                                liveBroadcastItems.any { item -> item.id == it.key }
                            }
                        },{
                            //toast("Failed to fetch account.")
                        })
                }*/
            }
        }

        launchOnStarted {
            googleViewModel.account.collectLatest { account ->
                //Logd("YouTubeFragment. Google Account Name :${account}");
                if (account != null) {
                    handleSignIn(account)
                }
            }
        }

        /*launchOnStarted {
            youtubeViewModel.liveBroadcasts.collect { liveBroadcasts ->
                Logd("YouTubeFragment: youtubeViewModel.liveBroadcasts.collect")

                var liveBroadcastItems = liveBroadcasts.map { x ->
                    BroadcastItem(
                        x.snippet.thumbnails.standard.url,
                        "${x.snippet.title}", //"${x.snippet.title} - ${x.contentDetails.boundStreamId}",
                        formatDate(x.snippet.scheduledStartTime),
                        x.id,
                        x.contentDetails.boundStreamId)
                }
                val adapterServer = BroadcastsAdapter(requireActivity(), liveBroadcastItems)
                binding.spinnerBroadcast.adapter = adapterServer
            }
        }*/

        launchOnStarted {
            youtubeViewModel.liveURL.collect { liveURL ->
                binding.liveUrl.text = liveURL
            }
        }

        /*launchOnStarted {
            googleViewModel.firebaseAccount.collect { firebaseAccount ->
                val accountName = firebaseAccount.accountName
                val accountKey = firebaseAccount.accountKey

                if (!accountName.isNullOrEmpty() && !accountKey.isNullOrEmpty()) {
                    Logd("YouTubeFragment: googleViewModel.firebaseAccount.collect")

                    FirebaseDataManager.getInstance()
                        .authenticateAccount(accountName, accountKey, { account ->
                            Logd("Account Name: ${account.name}")
                            Logd("Admin: ${account.admin}")
                        },{
                            Log.e("Firebase", "Failed to fetch account.")
                        })
                }
            }
        }*/
        /*lifecycleScope.launch {
            youtubeViewModel.liveStreams.collect { liveStreams ->
                var liveStreamItems = liveStreams.map { x -> KeyValue<String>(x.id, "L:${x.snippet.title} - ${x.cdn.ingestionInfo.streamName}") }
                val adapterServer = ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_item, liveStreamItems)
                adapterServer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerStreams.adapter = adapterServer
            }
        }*/

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

        /*binding.login.setOnClickListener {
            var googleSignInClient = GoogleSignIn.getClient(requireActivity(), googleSignInOptions)
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        binding.logout.setOnClickListener {
            var googleSignInClient = GoogleSignIn.getClient(requireActivity(), googleSignInOptions)
            googleSignInClient.signOut().addOnCompleteListener {
                toast("User signed out")
                googleViewModel.isLoggedIn(false)
            }
        }*/
    }

    private fun handleSignIn(account: Account?) {
        CoroutineScope(Dispatchers.IO).launch {
            //googleViewModel.isLoggedIn(true)
            //googleViewModel.setAccountName(account.account?.name ?: "")
            //_AccessToken = getAccessToken(account).toString()
            streamsListYouTube(account)
            broadcastListYouTube(account)
        }
    }

    /*private fun getAccessToken(account: GoogleSignInAccount): String? {
        return try {
            account.account?.let {
                GoogleAuthUtil.getToken(
                    requireActivity(),
                    it,
                    "oauth2:https://www.googleapis.com/auth/youtube"
                )
            }
        } catch (e: UserRecoverableAuthException) {
            // Handle by prompting the user to grant permissions
            toast("User action required: ${e.intent}")
            null
        } catch (e: Exception) {
            toast("Failed to get token: ${e.message}")
            null
        }
    }*/

    private fun streamsListYouTube(account: Account?) {
        try {
            val youtubeService = getYouTubeService(account)
            val request = youtubeService.liveStreams().list("id,snippet,cdn,status")
            request.mine = true // Retrieve livestreams from the authenticated user
            val response = request.execute()
            youtubeViewModel.setLiveStreams(response.items)
        }
        catch (e: Exception)
        {
            toast("streamsListYouTube::${e.message.toString()}")
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
        catch (e: Exception)
        {
            toast("broadcastListYouTube::${e.message.toString()}")
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