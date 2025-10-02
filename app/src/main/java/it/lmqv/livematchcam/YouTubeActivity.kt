@file:Suppress("DEPRECATION")

package it.lmqv.livematchcam

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
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
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.repositories.accountDataStore
import it.lmqv.livematchcam.utils.KeyDescription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

object OAuthConfig {
    //const val CLIENT_ID = "54641307189-6181k175ei3m80jnvot27qkfhfvmteqt.apps.googleusercontent.com"
    //const val REDIRECT_URI = "it.lmqv.livematchcam:/oauth2redirect"
    //const val AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
    //const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
    //const val SCOPE = "https://www.googleapis.com/auth/youtube"
    const val AUTHORIZATION_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
    const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
    const val REDIRECT_URI = "it.lmqv.livematchcam:/oauth2redirect"
    const val CLIENT_ID = "54641307189-6181k175ei3m80jnvot27qkfhfvmteqt.apps.googleusercontent.com"
    const val YOUTUBE_SCOPE = "https://www.googleapis.com/auth/youtube"
}

class YouTubeActivity : AppCompatActivity() {

    private val SERVICE_ID: String = "cam.livematch.nearby"
    //private val TAG: String = "LMCamNearbyConnections"
    private val STRATEGY: Strategy = Strategy.P2P_STAR
    private lateinit var connectionsClient: ConnectionsClient

    companion object {
        //private const val REQUEST_CODE_LOCATION_PERMISSION = 1
        //private const val REQUEST_CODE_BLUETOOTH_PERMISSION = 2
        private const val REQUEST_CODE_PERMISSION = 3
    }
    private var connectedEndpointId: String = ""

    private lateinit var binding: ActivityYouTubeBinding
    private val RC_SIGN_IN = 9991

    private var accessToken : String = ""
    private var accountName : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityYouTubeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fab: FloatingActionButton = binding.fab
        fab.setOnClickListener { view ->
            //toast("oAuthHandler")
            oAuthHandler()
        }

        binding.fab2.setOnClickListener { view ->
            //toast("getUserChannelInfo")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    accessToken = GoogleAuthUtil.getToken(
                        this@YouTubeActivity,
                        "calcio.lecco.2010@gmail.com",
                        //"paolodito@gmail.com",
                        "oauth2:https://www.googleapis.com/auth/youtube"
                    )

                    CoroutineScope(Dispatchers.Main).launch {
                        toast("accessToken $accessToken")
                    }

                    getUserChannelInfo(accessToken)

                } catch (e: Exception) {
                    e.printStackTrace()

                    CoroutineScope(Dispatchers.Main).launch {
                        toast("exception ${e.message}")
                    }

                }
            }
        }

        binding.fab3.setOnClickListener { view ->
            CoroutineScope(Dispatchers.IO).launch {
                //scheduleYouTube()
                streamsListYouTube()
                broadcastListYouTube()
            }
        }

        //connectionsClient = Nearby.getConnectionsClient(this)

        // Set OnClickListeners for buttons
//        binding.buttonDiscovery.setOnClickListener {
//            startDiscovery()
//        }
//
//        binding.buttonAdvertise.setOnClickListener {
//            startAdvertising()
//        }
//
//        binding.buttonSend.setOnClickListener {
//            // Example: sending a message to a connected device (replace endpointId with actual ID)
//            //val endpointId = "YourEndpointIdHere"
//            sendMessage("Hello from me!")
//        }

        //checkPermissions()
    }

    private fun checkPermissions() {
        /*ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN),
            REQUEST_CODE_BLUETOOTH_PERMISSION)
        */
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSION)
        /*} else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN),
                REQUEST_CODE_LOCATION_PERMISSION
            )
        }*/

        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Verifica i permessi di Posizione
            /*if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                // Richiedi il permesso di Posizione
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    REQUEST_CODE_LOCATION_PERMISSION)
            }*/

            // Verifica i permessi per il Bluetooth
            /*if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
                // Richiedi i permessi di Bluetooth
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN),
                    REQUEST_CODE_BLUETOOTH_PERMISSION)
            }*/
        //}
    }

    // Gestisci la risposta dell'utente quando si richiedono i permessi
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permessi concessi", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permessi negati", Toast.LENGTH_SHORT).show()
                }
            }

            /*REQUEST_CODE_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permesso di Posizione concesso
                    Toast.makeText(this, "Posizione concessa", Toast.LENGTH_SHORT).show()
                } else {
                    // Permesso di Posizione negato
                    Toast.makeText(this, "Permesso di posizione negato", Toast.LENGTH_SHORT).show()
                }
            }

            REQUEST_CODE_BLUETOOTH_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permessi di Bluetooth concessi
                    Toast.makeText(this, "Bluetooth concessa", Toast.LENGTH_SHORT).show()
                } else {
                    // Permessi di Bluetooth negati
                    Toast.makeText(this, "Permessi di Bluetooth negati", Toast.LENGTH_SHORT).show()
                }
            }*/
        }
    }

    override fun onStart() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            accountName = account.account?.name ?: ""

            CoroutineScope(Dispatchers.IO).launch {
                accessToken = getAccessToken(account, this@YouTubeActivity).toString()

                CoroutineScope(Dispatchers.Main).launch {
                    toast("accessToken $accessToken")
                }
            }
            toast("User is signed in $accountName")
        } else {
            toast("User is not signed in")
        }
        super.onStart()
    }
    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {

//        val account = GoogleSignIn.getLastSignedInAccount(this)
//        if (account != null) {
//            // User is already signed in
//            //val email = account.email
//            //val displayName = account.displayName
//            accountName = account.account?.name ?: ""
//            //val idToken = account.serverAuthCode
////            CoroutineScope(Dispatchers.IO).launch {
////                accessToken = getAccessToken(account, this@YouTubeActivity).toString()
////                streamsListYouTube()
////                broadcastListYouTube()
////            }
//            toast("User is signed in $accountName")
//        } else {
//            toast("User is not signed in")
//        }

        return super.onCreateView(name, context, attrs)
    }

    private suspend fun streamsListYouTube() {
        try {
            val transport = NetHttpTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()

            val credential = GoogleAccountCredential.usingOAuth2(
                applicationContext, listOf("https://www.googleapis.com/auth/youtube")
            )
            credential.selectedAccountName = accountName /* Set the authenticated user's account name */

            val youtubeService = YouTube.Builder(transport, jsonFactory, credential)
                .setApplicationName("LiveMatchCam")
                .build()

            val request = youtubeService.liveStreams().list("id,snippet,cdn,status")
            request.mine = true // Retrieve broadcasts from the authenticated user
            val response = request.execute()
            withContext(Dispatchers.Main) {
                var streams = response.items.map { x -> KeyDescription<String>(x.id, "LiveStream: ${x.snippet.title} ${x.id}") }

                val adapterServer =
                    ArrayAdapter(this@YouTubeActivity, android.R.layout.simple_spinner_item, streams)
                adapterServer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerLive.adapter = adapterServer
                binding.spinnerLive.setSelection(0)
            }
        }
        catch (e: Exception)
        {
            Loge("YoutubeActivity::tException:: ${e.message.toString()}")
        }
    }

    private suspend fun broadcastListYouTube() {
        try {
            val transport = NetHttpTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()

            val credential = GoogleAccountCredential.usingOAuth2(
                applicationContext, listOf("https://www.googleapis.com/auth/youtube")
            )
            credential.selectedAccountName = accountName /* Set the authenticated user's account name */

            val youtubeService = YouTube.Builder(transport, jsonFactory, credential)
                .setApplicationName("LiveMatchCam")
                .build()

            val request = youtubeService.liveBroadcasts().list("id,snippet,contentDetails,status")
            request.mine = true // Retrieve broadcasts from the authenticated user
            val response = request.execute()
            withContext(Dispatchers.Main) {
                var broadcasts = response.items.map { x -> KeyDescription<String>(x.id, "LiveBroadcast: ${x.snippet.title} - ${x.id}") }

                // "Os8jYda6AD0"
                val adapterServer = ArrayAdapter(this@YouTubeActivity, android.R.layout.simple_spinner_item, broadcasts)
                adapterServer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerBroadcast.adapter = adapterServer
                binding.spinnerBroadcast.setSelection(0)
            }
        }
        catch (e: Exception)
        {
            Loge("YoutubeActivity::tException:: ${e.message.toString()}")
        }
    }

    private fun scheduleYouTube() {
        try {
            val transport = NetHttpTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()

            val credential = GoogleAccountCredential.usingOAuth2(
                applicationContext, listOf("https://www.googleapis.com/auth/youtube")
            )
            credential.selectedAccountName = accountName /* Set the authenticated user's account name */

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

            toast("streamKey:: $streamKey")
            toast("ingestionUrl:: $ingestionUrl")

    // Bind the broadcast to the stream:
            val bindRequest = youtubeService.liveBroadcasts()
                .bind(broadcastId, "id,contentDetails")
            bindRequest.streamId = streamId
            bindRequest.execute()
        }
        catch (e: Exception) {
            Loge("YoutubeActivity::tException:: ${e.message.toString()}")
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
                accessToken = getAccessToken(account, this@YouTubeActivity).toString()
                CoroutineScope(Dispatchers.Main).launch {
                    toast("accessToken::" + accessToken)
                }
                //Logd("TOKEN::" + accessToken)
            }
            //_AccessToken = account.idToken!!
            //val tes = account.email
            //var scopes = account.grantedScopes

            // Exchange the serverAuthCode for an access token using your backend or library.
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                // Handle sign-in failure.
                toast(task.exception?.message.toString())
            }
        }
    }

    private fun getUserChannelInfo(_accessToken: String) {
        try {
            // Define the API endpoint
            val url = "https://www.googleapis.com/youtube/v3/channels?part=snippet,contentDetails,statistics&mine=true"

            // https://accounts.google.com/o/oauth2/v2/auth?
            // client_id=.apps.googleusercontent.com
            // &redirect_uri=com.rankedin.video%3A%2Fsportcam.app%2Foauth2redirect
            // &response_type=code
            // &access_type=offline
            // &scope=https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fyoutube
            // &display=touch
            // &prompt=select_account

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
                val json = JSONObject(responseData!!)
                val items = json.getJSONArray("items")

                if (items.length() > 0) {
                    val channel = items.getJSONObject(0)
                    val snippet = channel.getJSONObject("snippet")
                    val statistics = channel.getJSONObject("statistics")

                    // Extract channel details
                    val title = snippet.getString("title")
                    val description = snippet.getString("description")
                    val subscriberCount = statistics.getString("subscriberCount")
                    CoroutineScope(Dispatchers.Main).launch {
                        toast("Channel Title: $title" + " -Description: $description" + " -Subscribers: $subscriberCount")
                    }
                } else {
                    //Logd("No channel information found.")
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    toast("Error: ${response.code} - ${response.message}")
                }
                Logd("Error: ${response.code} - ${response.message}")
            }
        } catch (e : Exception) {
            CoroutineScope(Dispatchers.Main).launch {
                toast("Error: ${e.message}")
            }
            Loge("Error: ${e.message}")
        }

    }

    private fun getAccessToken(account: GoogleSignInAccount, context: Context): String? {
        return try {
            account.account?.let {
                GoogleAuthUtil.getToken(
                    context,
                    it.name,
                    "oauth2:https://www.googleapis.com/auth/youtube"
                )
            }
        } catch (e: UserRecoverableAuthException) {
            // Handle by prompting the user to grant permissions
            Loge("User action required: ${e.intent}")
            CoroutineScope(Dispatchers.Main).launch {
                toast("User action required: ${e.intent}")
            }
            null
        } catch (e: Exception) {
            Loge("Failed to get token: ${e.message}")
            CoroutineScope(Dispatchers.Main).launch {
                toast("Failed to get token: ${e.message}")
            }
            null
        }
    }

    /* NEARBY */
    private fun startAdvertising() {
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()

        toast("Advertising starting")
        connectionsClient.startAdvertising(
            "DeviceName",
            SERVICE_ID,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            toast("Advertising started")
        }.addOnFailureListener { e ->
            toast("Failed to start advertising ${e.message}")
        }
    }

    private fun startDiscovery() {
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()

        toast( "Discovery starting")
        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            toast("Discovery started")
        }.addOnFailureListener { e ->
            toast("Failed to start discovery ${e.message}")
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            toast("Connection initiated with ${connectionInfo.endpointName}")
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            if (resolution.status.isSuccess) {
                toast("Connected to $endpointId")
            } else {
                toast( "Connection failed")
            }
        }

        override fun onDisconnected(endpointId: String) {
            toast( "Disconnected from $endpointId")
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            toast("Endpoint found: $endpointId")
            connectedEndpointId = endpointId
            connectionsClient.requestConnection("DeviceName", endpointId, connectionLifecycleCallback)
        }

        override fun onEndpointLost(endpointId: String) {
            toast("Endpoint lost: $endpointId")
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val byteArray = payload.asBytes()
                val receivedMessage = byteArray?.let {
                    String(it)
                } ?: "Received message is null"

                toast("Message received: $receivedMessage from $endpointId")
                //Toast.makeText(this@YouTubeActivity, "Received: $receivedMessage", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Optional: Handle progress updates for large file transfers.
        }
    }

    fun sendMessage(message: String) {
        val payload = Payload.fromBytes(message.toByteArray())
        connectionsClient.sendPayload(connectedEndpointId, payload)
    }
    /* NEARBY */
}