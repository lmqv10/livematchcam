package it.lmqv.livematchcam.fragments

import android.accounts.Account
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.LiveBroadcast
import com.google.api.services.youtube.model.LiveBroadcastContentDetails
import com.google.api.services.youtube.model.LiveBroadcastSnippet
import com.google.api.services.youtube.model.LiveBroadcastStatus
import it.lmqv.livematchcam.dialogs.DateTimePickerDialog
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.adapters.LiveStreamAdapter
import it.lmqv.livematchcam.adapters.LiveStreamItem
import it.lmqv.livematchcam.auth.AuthResult
import it.lmqv.livematchcam.databinding.FragmentYoutubeStreamBinding
import it.lmqv.livematchcam.dialogs.LogosRecentsDialog
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.extensions.formatDate
import it.lmqv.livematchcam.extensions.hideKeyboard
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.extensions.setEnabledRecursively
import it.lmqv.livematchcam.extensions.showEditStringDialog
import it.lmqv.livematchcam.preferences.LiveStreamPreferences
import it.lmqv.livematchcam.viewmodels.AccountViewModel
import it.lmqv.livematchcam.viewmodels.FloatingActionsViewModel
import it.lmqv.livematchcam.viewmodels.YoutubeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

data class ThumbnailAssets (
    var background: Bitmap? = null,
    var logoHome: Bitmap? = null,
    var logoGuest: Bitmap? = null,
    var calendar: Calendar = Calendar.getInstance()
)

class YoutubeStreamFragment : Fragment() {

    private val PICK_IMAGE = 1

    companion object {
        @JvmStatic
        fun newInstance() = YoutubeStreamFragment()
    }

    private lateinit var liveStreamPreferences: LiveStreamPreferences

    private val youtubeViewModel: YoutubeViewModel by activityViewModels()
    private val accountViewModel: AccountViewModel by activityViewModels()
    private val actionsViewModel: FloatingActionsViewModel by activityViewModels()

    private val thumbnailAssets: ThumbnailAssets = ThumbnailAssets()

    private var _binding: FragmentYoutubeStreamBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        liveStreamPreferences = LiveStreamPreferences(requireContext())

        _binding = FragmentYoutubeStreamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivPreviewBackground.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE)
        }

        binding.textDescription.setOnClickListener {
            val sourceDescription = binding.textDescription.text.toString()

            requireContext().showEditStringDialog(R.string.label_event_description, sourceDescription, arrayOf()) { updatedDescription ->
                binding.textDescription.text = updatedDescription
                liveStreamPreferences.setDescription(updatedDescription)
                this.hideKeyboard()
            }
        }

        binding.ivDateTime.setOnClickListener {
            DateTimePickerDialog(
                context = requireContext(),
                calendar = thumbnailAssets.calendar,
                onConfirm = { calendar ->
                    thumbnailAssets.calendar = calendar
                    liveStreamPreferences.setDate(calendar)
                    updatePreviewThumbnail()
                },
                onCancel = {
                }
            ).show()
        }

        binding.ivLogoHome.setOnClickListener {
            var dialog = LogosRecentsDialog(requireContext(), "") { selectedLogoUrl ->
                lifecycleScope.launch {
                    liveStreamPreferences.setLogoHome(selectedLogoUrl)
                    thumbnailAssets.logoHome = loadBitmapFromUrl(selectedLogoUrl)
                    updatePreviewThumbnail()
                }
            }
            dialog.show()
        }

        binding.ivLogoGuest.setOnClickListener {
            var dialog = LogosRecentsDialog(requireContext(), "") { selectedLogoUrl ->
                lifecycleScope.launch {
                    liveStreamPreferences.setLogoGuest(selectedLogoUrl)
                    thumbnailAssets.logoGuest = loadBitmapFromUrl(selectedLogoUrl)
                    updatePreviewThumbnail()
                }
            }
            dialog.show()
        }

        binding.ivTitleClear.setOnClickListener {
            binding.textDescription.text = ""
            liveStreamPreferences.setDescription("")
        }

        thumbnailAssets.calendar = liveStreamPreferences.getDate()

        launchOnStarted {
            youtubeViewModel.liveStreams.collect { liveStreams ->
                var liveStreamsItems = liveStreams.map { x ->
                    LiveStreamItem(
                        x.snippet.title,
                        x.id,
                        x.cdn.ingestionInfo.ingestionAddress,
                        x.cdn.ingestionInfo.streamName)
                }

                var id = liveStreamPreferences.getId()
                var selectedIndex = liveStreamsItems.indexOfFirst { it.id == id }

                val adapterStreams = LiveStreamAdapter(requireActivity(), liveStreamsItems)
                binding.spinnerLivestream.adapter = adapterStreams
                binding.spinnerLivestream.setSelection(selectedIndex)

                binding.spinnerLivestream.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        lifecycleScope.launch {
                            val selectedLiveStream = parent.getItemAtPosition(position) as LiveStreamItem
                            liveStreamPreferences.setId(selectedLiveStream.id)
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) { }
                }
            }
        }

        launchOnStarted {
            accountViewModel.authState.collectLatest { state ->
                val account = when (state) {
                    is AuthResult.Authenticated -> state.account.account
                    is AuthResult.Unauthenticated, is AuthResult.Error -> null
                }
                if (account != null) {
                    handleSignIn(account)
                }
            }
        }

        binding.container.setEnabledRecursively(false)
        liveStreamPreferences.getBackground()?.let {
            val backgroundBitmap = BitmapFactory.decodeFile(it)
            thumbnailAssets.background = backgroundBitmap
            binding.container.setEnabledRecursively(true)
            updatePreviewThumbnail()
        }

        launchOnStarted {
            val logoHome = liveStreamPreferences.getLogoHome()
            if (logoHome.isNotEmpty()) {
                thumbnailAssets.logoHome = loadBitmapFromUrl(logoHome)
                updatePreviewThumbnail()
            }
        }
        launchOnStarted {
            val logoGuest = liveStreamPreferences.getLogoGuest()
            if (logoGuest.isNotEmpty()) {
                thumbnailAssets.logoGuest = loadBitmapFromUrl(logoGuest)
                updatePreviewThumbnail()
            }
        }

        binding.textDescription.text = liveStreamPreferences.getDescription()

        binding.ivCreateLiveStream.setOnClickListener {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_warning, null)
            dialogView.findViewById<TextView>(R.id.dialog_message).text = getString(R.string.confirm_create_live_stream)
            AlertDialog.Builder(requireContext()).setView(dialogView)
                .setPositiveButton("OK") { dialog, _ ->
                    createLiveStream(accountViewModel.accountName())
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .create()
                .show()
        }

        actionsViewModel.setEmptyActions()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri: Uri = data.data!!
            thumbnailAssets.background = MediaStore.Images.Media
                .getBitmap(requireActivity().contentResolver, selectedImageUri)

            thumbnailAssets.background?.let {
                var filePath = saveBitmapToFile(it, "background.jpg")
                liveStreamPreferences.setBackground(filePath.absolutePath)
                binding.container.setEnabledRecursively(true)
                updatePreviewThumbnail()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun handleSignIn(account: Account?) {
        CoroutineScope(Dispatchers.IO).launch {
            streamsListYouTube(account?.name)
            //broadcastListYouTube(account)
        }
    }

    suspend fun loadBitmapFromUrl(urlString: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            null
        }
    }

    private fun streamsListYouTube(accountName: String?) {
        try {
            val youtubeService = getYouTubeService(accountName)
            val request = youtubeService.liveStreams().list("id,snippet,cdn,status")
            request.mine = true // Retrieve livestreams from the authenticated user
            val response = request.execute()
            youtubeViewModel.setLiveStreams(response.items)
        }
        catch (e: Exception) {
            Loge("Exception:: ${e.message.toString()}")
        }
    }

    private fun updatePreviewThumbnail() : Bitmap? {
        //CoroutineScope(Dispatchers.IO).launch {
            var resultBitmap: Bitmap? = null

            if (thumbnailAssets.background != null) {
                val background = thumbnailAssets.background!!
                val width = background.width
                val height = background.height

                resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(resultBitmap)

                canvas.drawBitmap(background, 0f, 0f, null)

                val logoHeight = height / 3
                val centerY = height / 2 - logoHeight / 2

                thumbnailAssets.logoHome?.let {
                    val logoWidth = it.width * logoHeight / it.height
                    val scaledLeft = Bitmap.createScaledBitmap(it, logoWidth, logoHeight, true)
                    canvas.drawBitmap(
                        scaledLeft,
                        (width / 4 - logoWidth / 2).toFloat(),
                        centerY.toFloat(),
                        null
                    )
                }

                thumbnailAssets.logoGuest?.let {
                    val logoWidth = it.width * logoHeight / it.height
                    val scaledRight = Bitmap.createScaledBitmap(it, logoWidth, logoHeight, true)
                    canvas.drawBitmap(
                        scaledRight,
                        (3 * width / 4 - logoWidth / 2).toFloat(),
                        centerY.toFloat(),
                        null
                    )
                }

                val typeface =
                    ResourcesCompat.getFont(requireContext(), R.font.roboto_condensed_medium_italic)
                //val typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)

                thumbnailAssets.calendar.let {
                    val paint = Paint()
                    paint.color = Color.WHITE
                    paint.textSize = height / 12f
                    paint.isAntiAlias = true
                    paint.textAlign = Paint.Align.CENTER
                    paint.setShadowLayer(10f, 2f, 2f, Color.BLACK)
                    paint.typeface = typeface

                    var title = formatDate(it)
                    canvas.drawText(title, width / 2f, paint.textSize * 1.5f, paint)
                }

                binding.imagePreviewThumbnail.setImageBitmap(resultBitmap);

            } else {
                binding.imagePreviewThumbnail.setImageResource(R.drawable.preview_missing)
            }

        return resultBitmap
    }

    private fun saveBitmapToFile(bitmap: Bitmap, fileName: String): File {
        val file = File(requireActivity().cacheDir, fileName)
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.flush()
        outputStream.close()
        return file
    }

    private fun createLiveStream(accountName: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Start create LiveStream", Toast.LENGTH_SHORT).show()
                }

                val youtubeService = getYouTubeService(accountName)

                var calendar = thumbnailAssets.calendar
                val liveBroadcast = LiveBroadcast().apply {
                    snippet = LiveBroadcastSnippet().apply {
                        title = binding.textDescription.text.toString()
                        scheduledStartTime = DateTime(calendar.time, calendar.timeZone)
                    }
                    status = LiveBroadcastStatus().apply {
                        privacyStatus = "unlisted"
                    }
                    kind = "youtube#liveBroadcast"
                    contentDetails = LiveBroadcastContentDetails().apply {
                        enableClosedCaptions = false
                        enableEmbed = false
                        enableAutoStart = true
                    }
                }
                val broadcastResponse = youtubeService
                    .liveBroadcasts()
                    .insert("snippet,status,contentDetails", liveBroadcast)
                    .execute()

                val broadcastId = broadcastResponse.id

                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Stream Broadcast Created ${broadcastId}", Toast.LENGTH_SHORT).show()
                }

                val bindRequest = youtubeService
                    .liveBroadcasts()
                    .bind(broadcastId, "id,contentDetails")

                var liveStreamId = liveStreamPreferences.getId()
                bindRequest.streamId = liveStreamId
                bindRequest.execute()

                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Bind LiveStream ${liveStreamId}", Toast.LENGTH_SHORT).show()
                }

                updatePreviewThumbnail()?.let {
                    var filePath = saveBitmapToFile(it, "thumbnail.jpg")
                    val thumbnailFile = File(filePath.absolutePath)

                    val mediaContent = FileContent("image/jpeg", thumbnailFile)

                    val thumbnailSet = youtubeService.thumbnails()
                        .set(broadcastId, mediaContent)

                    thumbnailSet.execute()

                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "Splashscreen Uploaded", Toast.LENGTH_SHORT).show()
                    }
                }

                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "LiveStream Created!", Toast.LENGTH_SHORT).show()
                }
            }
            catch (e: Exception) {
                e.printStackTrace()
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Exception:\n${e.message.toString()}", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    private fun updateBroadcastThumbnail(broadcastId: String, accountName: String?)
    {
        val youtubeService = getYouTubeService(accountName)

        updatePreviewThumbnail()?.let {
            var filePath = saveBitmapToFile(it, "thumbnail.jpg")
            val thumbnailFile = File(filePath.absolutePath)

            val mediaContent = FileContent("image/jpeg", thumbnailFile)

            val thumbnailSet = youtubeService.thumbnails()
                .set(broadcastId, mediaContent)

            thumbnailSet.execute()

            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Splashscreen Uploaded", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getYouTubeService(accountName: String?) : YouTube {
        val transport = NetHttpTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()

        val credential = GoogleAccountCredential.usingOAuth2(
            requireContext(), listOf("https://www.googleapis.com/auth/youtube")
        )
        credential.selectedAccountName = accountName

        val youtubeService = YouTube.Builder(transport, jsonFactory, credential)
            .setApplicationName("LiveMatchCam")
            .build()

        return youtubeService;
    }
}