package it.lmqv.livematchcam

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.CdnSettings
import com.google.api.services.youtube.model.LiveBroadcast
import com.google.api.services.youtube.model.LiveBroadcastSnippet
import com.google.api.services.youtube.model.LiveBroadcastStatus
import com.google.api.services.youtube.model.LiveStream
import com.google.api.services.youtube.model.LiveStreamSnippet
import it.lmqv.livematchcam.databinding.ActivityYouTubeBinding
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.utils.KeyValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class YouTubeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityYouTubeBinding
    private val RC_SIGN_IN = 9991

    private var _AccessToken : String = ""
    private var _AccountName : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityYouTubeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fab: FloatingActionButton = binding.fab
        fab.setOnClickListener { view ->
            oAuthHandler()
        }

        binding.fab2.setOnClickListener { view ->
            CoroutineScope(Dispatchers.IO).launch {
                getUserChannelInfo(_AccessToken)
            }
        }
        binding.fab3.setOnClickListener { view ->
            CoroutineScope(Dispatchers.IO).launch {
                scheduleYouTube()
                //streamsListYouTube()
                //broadcastListYouTube()
            }
        }
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {

        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            // User is already signed in
            val email = account.email
            val displayName = account.displayName
            _AccountName = account.account?.name ?: ""
            //val idToken = account.serverAuthCode
            CoroutineScope(Dispatchers.IO).launch {
                _AccessToken = getAccessToken(account, this@YouTubeActivity).toString()
                streamsListYouTube()
                broadcastListYouTube()
            }
        } else {
            toast("User is not signed in")
        }

        return super.onCreateView(name, context, attrs)
    }

    private suspend fun streamsListYouTube() {
        try {
            val transport = NetHttpTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()

            val credential = GoogleAccountCredential.usingOAuth2(
                applicationContext, listOf("https://www.googleapis.com/auth/youtube")
            )
            credential.selectedAccountName = _AccountName /* Set the authenticated user's account name */

            val youtubeService = YouTube.Builder(transport, jsonFactory, credential)
                .setApplicationName("LiveMatchCam")
                .build()

            val request = youtubeService.liveStreams().list("id,snippet,cdn,status")
            request.mine = true // Retrieve broadcasts from the authenticated user
            val response = request.execute()
            withContext(Dispatchers.Main) {
                var streams = response.items.map { x -> KeyValue<String>(x.id, "LiveStream: ${x.snippet.title} ${x.id}") }

                val adapterServer =
                    ArrayAdapter(this@YouTubeActivity, android.R.layout.simple_spinner_item, streams)
                adapterServer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerLive.adapter = adapterServer
                binding.spinnerLive.setSelection(0)
            }
        }
        catch (e: Exception)
        {

        }
    }

    private suspend fun broadcastListYouTube() {
        try {
            val transport = NetHttpTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()

            val credential = GoogleAccountCredential.usingOAuth2(
                applicationContext, listOf("https://www.googleapis.com/auth/youtube")
            )
            credential.selectedAccountName = _AccountName /* Set the authenticated user's account name */

            val youtubeService = YouTube.Builder(transport, jsonFactory, credential)
                .setApplicationName("LiveMatchCam")
                .build()

            val request = youtubeService.liveBroadcasts().list("id,snippet,contentDetails,status")
            request.mine = true // Retrieve broadcasts from the authenticated user
            val response = request.execute()
            withContext(Dispatchers.Main) {
                var broadcasts = response.items.map { x -> KeyValue<String>(x.id, "LiveBroadcast: ${x.snippet.title} - ${x.id}") }

                // "Os8jYda6AD0"
                val adapterServer = ArrayAdapter(this@YouTubeActivity, android.R.layout.simple_spinner_item, broadcasts)
                adapterServer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerBroadcast.adapter = adapterServer
                binding.spinnerBroadcast.setSelection(0)
            }
        }
        catch (e: Exception)
        {
            var m = e.message
        }
    }

    private fun scheduleYouTube() {
        try {
            val transport = NetHttpTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()

            val credential = GoogleAccountCredential.usingOAuth2(
                applicationContext, listOf("https://www.googleapis.com/auth/youtube")
            )
            credential.selectedAccountName = _AccountName /* Set the authenticated user's account name */

            val youtubeService = YouTube.Builder(transport, jsonFactory, credential)
                .setApplicationName("LiveMatchCam")
                .build()

            val liveBroadcast = LiveBroadcast()
            val snippet = LiveBroadcastSnippet()
            snippet.title = "Your Live Stream Title"
            snippet.scheduledStartTime = DateTime.parseRfc3339("2024-12-10T10:00:00Z")
            //snippet.scheduledEndTime = DateTime.parseRfc3339("2024-12-07T11:00:00Z")
            //snippet.thumbnails =

            val status = LiveBroadcastStatus()
            status.privacyStatus = "unlisted"  // "private", "public", or "unlisted"

            liveBroadcast.snippet = snippet
            liveBroadcast.status = status
            liveBroadcast.kind = "youtube#liveBroadcast"

            val broadcastInsert = youtubeService.liveBroadcasts()
                .insert("snippet,status", liveBroadcast)

            val broadcastResponse = broadcastInsert.execute()
            val broadcastId = broadcastResponse.id

            val liveStream = LiveStream()
            val streamSnippet = LiveStreamSnippet()
            streamSnippet.title = "Your Stream Title"

            val cdnSettings = CdnSettings()
            cdnSettings.format = "720p"
            cdnSettings.ingestionType = "rtmp"
            cdnSettings.resolution = "720p"
            cdnSettings.frameRate = "30fps"

            liveStream.snippet = streamSnippet
            liveStream.cdn = cdnSettings
            liveStream.kind = "youtube#liveStream"

            val streamInsert = youtubeService.liveStreams()
                .insert("snippet,cdn", liveStream)

            val streamResponse = streamInsert.execute()
            val streamId = streamResponse.id

            // Retrieve the ingestion details:
            val ingestionInfo = streamResponse.cdn.ingestionInfo
            val streamKey = ingestionInfo.streamName  // This is your RTMP stream key
            val ingestionUrl = ingestionInfo.ingestionAddress  // RTMP server URL

            Logd("streamKey:: $streamKey")
            Logd("ingestionUrl:: $ingestionUrl")

    // Bind the broadcast to the stream:
            val bindRequest = youtubeService.liveBroadcasts()
                .bind(broadcastId, "id,contentDetails")
            bindRequest.streamId = streamId
            bindRequest.execute()
        }
        catch (e: Exception) {
            Log.d("EXCEPTION", e.message.toString())
            //var exx = e.message
        }

    }

    private fun oAuthHandler() {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/youtube"))
            //.requestIdToken("54641307189-6181k175ei3m80jnvot27qkfhfvmteqt.apps.googleusercontent.com")
            .requestServerAuthCode("54641307189-6181k175ei3m80jnvot27qkfhfvmteqt.apps.googleusercontent.com", true)
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        googleSignInClient.signOut().addOnCompleteListener {
            toast("User signed out")
        }
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful) {
            val account = task.result
            CoroutineScope(Dispatchers.IO).launch {
                _AccessToken = getAccessToken(account, this@YouTubeActivity).toString()
                toast("accessToken::" + _AccessToken)
                Logd("TOKEN::" + _AccessToken)
            }
            //_AccessToken = account.idToken!!
            val tes = account.email
            var scopes = account.grantedScopes

            // Exchange the serverAuthCode for an access token using your backend or library.
        } else {
            // Handle sign-in failure.
            toast(task.exception?.message.toString())
        }
    }

    private fun getUserChannelInfo(accessToken: String) {
        try {
            // Define the API endpoint
            val url = "https://www.googleapis.com/youtube/v3/channels?part=snippet,contentDetails,statistics&mine=true"

            // Create an HTTP client
            val client = OkHttpClient()

            // Build the request
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken") // Pass the access token
                .build()

            // Execute the request
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                // Parse the JSON response
                val responseData = response.body?.string()
                val json = JSONObject(responseData)
                val items = json.getJSONArray("items")

                if (items.length() > 0) {
                    val channel = items.getJSONObject(0)
                    val snippet = channel.getJSONObject("snippet")
                    val statistics = channel.getJSONObject("statistics")

                    // Extract channel details
                    val title = snippet.getString("title")
                    val description = snippet.getString("description")
                    val subscriberCount = statistics.getString("subscriberCount")

                    Logd("Channel Title: $title" + " -Description: $description" + " -Subscribers: $subscriberCount")
                } else {
                    Logd("No channel information found.")
                }
            } else {
                Logd("Error: ${response.code} - ${response.message}")
            }
        } catch (e : Exception) {
            Logd("Error: ${e.message}")
        }

    }

    fun getAccessToken(account: GoogleSignInAccount, context: Context): String? {
        return try {
            account.account?.let {
                GoogleAuthUtil.getToken(
                    context,
                    it,
                    "oauth2:https://www.googleapis.com/auth/youtube"
                )
            }
        } catch (e: UserRecoverableAuthException) {
            // Handle by prompting the user to grant permissions
            Logd("User action required: ${e.intent}")
            null
        } catch (e: Exception) {
            Logd("Failed to get token: ${e.message}")
            null
        }
    }
}