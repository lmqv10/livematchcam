package it.lmqv.livematchcam.fragments.youtube

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.adapters.LiveBroadcastItem
import it.lmqv.livematchcam.adapters.LiveStreamAdapter
import it.lmqv.livematchcam.adapters.LiveStreamItem
import it.lmqv.livematchcam.databinding.FragmentYoutubeStreamBinding
import it.lmqv.livematchcam.dialogs.DateTimePickerDialog
import it.lmqv.livematchcam.dialogs.RecentsDialog
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.services.auth.AuthResult
import it.lmqv.livematchcam.services.youtube.YouTubeClientProvider
import it.lmqv.livematchcam.viewmodels.GoogleAccountViewModel
import it.lmqv.livematchcam.viewmodels.YoutubeViewModel
import it.lmqv.livematchcam.viewmodels.YoutubeViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import it.lmqv.livematchcam.adapters.BroadcastsAdapter
import it.lmqv.livematchcam.extensions.convertBitmapToFIle
import it.lmqv.livematchcam.extensions.createThumbnail
import it.lmqv.livematchcam.extensions.launchOnResumed
import it.lmqv.livematchcam.extensions.saveBitmapToFile
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.handlers.DialogContext
import it.lmqv.livematchcam.handlers.DialogHandler
import it.lmqv.livematchcam.preferences.RecentsLogosPreferences
import it.lmqv.livematchcam.preferences.SchedulesPreferences
import it.lmqv.livematchcam.preferences.toThumbnailAsset
import it.lmqv.livematchcam.services.youtube.LiveStreamContentData
import java.time.ZonedDateTime

class YoutubeStreamFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = YoutubeStreamFragment()
    }

    private val youtubeViewModel: YoutubeViewModel by activityViewModels {
        YoutubeViewModelFactory(requireActivity().application, YouTubeClientProvider.get())
    }

    private val googleAccountViewModel: GoogleAccountViewModel by activityViewModels()

    private lateinit var schedulesPrefs: SchedulesPreferences
    private lateinit var dateTimePickerDialog: DateTimePickerDialog

    private var _binding: FragmentYoutubeStreamBinding? = null
    private val binding get() = _binding!!

    private val launcher = registerForActivityResult (
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val selectedImageUri: Uri? = data?.data

            @Suppress("DEPRECATION")
            var backgroundBitmap = MediaStore.Images.Media
                .getBitmap(requireActivity().contentResolver, selectedImageUri)

            var backgroundFile = requireContext().saveBitmapToFile(backgroundBitmap, "background.jpg")
            schedulesPrefs.set(backgroundFilePath = backgroundFile.absolutePath)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        this.schedulesPrefs = SchedulesPreferences(requireContext())

        this.dateTimePickerDialog = DateTimePickerDialog(
            context = requireContext(),
            onConfirm = { calendar ->
                schedulesPrefs.set(scheduledStartTime = calendar)
            },
            onCancel = { }
        )

        _binding = FragmentYoutubeStreamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launchOnStarted {
            googleAccountViewModel.authState.collectLatest { state ->
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

        launchOnResumed {
            schedulesPrefs.currentKeyScheduleData.collect { currentSchedule ->
                try {
                    val schedule = currentSchedule.value
                    //dateTimePickerDialog.setDate(schedule.scheduleStartTime)
                    dateTimePickerDialog.setDate(minOf(ZonedDateTime.now(schedule.scheduleStartTime.zone), schedule.scheduleStartTime))

                    var thumbnailAssets = schedule.toThumbnailAsset(requireContext())
                    val thumbnail = requireContext().createThumbnail(thumbnailAssets)

                    val liveStreamContentData : LiveStreamContentData?

                    if (thumbnail != null && schedule.liveStreamId != null) {
                        binding.ivThumbnail.setImageBitmap(thumbnail)

                        binding.ivRemoveBackground.visibility = View.VISIBLE
                        binding.ivLogoHome.visibility = View.VISIBLE
                        binding.ivLogoGuest.visibility = View.VISIBLE
                        binding.ivDateTime.visibility = View.VISIBLE

                        var broadcastId: String?
                        if (schedulesPrefs.isEditing()) {
                            binding.ivDeleteLiveStream.visibility = View.VISIBLE
                            binding.ivCreateLiveStream.setImageResource(R.drawable.ic_save)
                            broadcastId = currentSchedule.key
                        } else {
                            binding.ivDeleteLiveStream.visibility = View.GONE
                            binding.ivCreateLiveStream.setImageResource(R.drawable.ic_add)
                            broadcastId = null
                        }

                        liveStreamContentData = LiveStreamContentData(
                            requireContext().convertBitmapToFIle(thumbnail),
                            schedule.title,
                            schedule.scheduleStartTime,
                            schedule.liveStreamId!!,
                            broadcastId
                        )

                        binding.ivCreateLiveStream.visibility = View.VISIBLE
                    } else {
                        binding.ivThumbnail.setImageResource(R.drawable.preview_missing)

                        binding.ivRemoveBackground.visibility = View.GONE
                        binding.ivLogoHome.visibility = View.GONE
                        binding.ivLogoGuest.visibility = View.GONE
                        binding.ivDateTime.visibility = View.GONE

                        liveStreamContentData = null

                        binding.ivCreateLiveStream.visibility = View.INVISIBLE
                    }

                    handleCreateListener(liveStreamContentData)
                    handleDeleteListener(currentSchedule.key)

                    binding.textTitle.text = schedule.title

                    val adapter = binding.spinnerLivestream.adapter as BaseAdapter
                    val liveStreamsItems = (0 until adapter.count).map { adapter.getItem(it) as? LiveStreamItem }
                    val selectedIndex = liveStreamsItems.indexOfFirst { it?.id == schedule.liveStreamId }
                    if (selectedIndex >= 0) {
                        binding.spinnerLivestream.setSelection(selectedIndex)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    toast(e.message.toString())
                }
            }
        }

        launchOnResumed {
            youtubeViewModel.liveStreams.collect { liveStreams ->
                var liveStreamsItems = liveStreams
                    .map { x ->
                        LiveStreamItem(
                            x.snippet.title,
                            x.id,
                            x.cdn.ingestionInfo.ingestionAddress,
                            x.cdn.ingestionInfo.streamName)
                }

                val adapterStreams = LiveStreamAdapter(requireActivity(), liveStreamsItems)
                binding.spinnerLivestream.adapter = adapterStreams

                binding.spinnerLivestream.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        val selectedItem = parent.getItemAtPosition(position) as LiveStreamItem
                        schedulesPrefs.set(liveStreamId = selectedItem.id)
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) { }
                }
            }
        }

        launchOnResumed {
            youtubeViewModel.liveBroadcasts.collect { liveBroadcasts ->
                var broadcastItems = liveBroadcasts
                    .sortedBy { x -> x.snippet.scheduledStartTime.value }
                    .filter { x -> x.status.lifeCycleStatus == "ready" }
                    .map { x -> LiveBroadcastItem.EditBroadcast(x, "dd MMMM yyyy HH:mm") }
                    .plus(LiveBroadcastItem.AddBroadcast())

                this.schedulesPrefs.cleanupMatches(broadcastItems)

                val broadcastsAdapter = BroadcastsAdapter(requireActivity(), broadcastItems, R.dimen.image_size_width_small)
                binding.spinnerBroadcast.adapter = broadcastsAdapter

                binding.spinnerBroadcast.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        val broadcastItem = parent.getItemAtPosition(position) as LiveBroadcastItem

                        when (broadcastItem) {
                            is LiveBroadcastItem.EditBroadcast -> {
                                schedulesPrefs.load(broadcastItem)
                            }
                            is LiveBroadcastItem.AddBroadcast -> {
                                schedulesPrefs.load()
                            }
                        }
                        binding.ivCreateLiveStream.visibility = View.VISIBLE
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) { }
                }
            }
        }

        binding.ivThumbnail.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            launcher.launch(intent)
        }

        binding.textTitle.setOnClickListener {
            val sourceTitle = binding.textTitle.text.toString()
            DialogHandler.editText(DialogContext(this, binding.textTitle, R.string.label_event_title, sourceTitle)) {
                schedulesPrefs.set(title = it)
            }
        }

        binding.ivDateTime.setOnClickListener {
            dateTimePickerDialog.show()
        }

        binding.ivLogoHome.setOnClickListener {
            var dialog = RecentsDialog(requireContext(), "", RecentsLogosPreferences(requireContext())) { selectedLogoUrl ->
                lifecycleScope.launch {
                    schedulesPrefs.set(logoHome = selectedLogoUrl)
                }
            }
            dialog.show()
        }

        binding.ivLogoGuest.setOnClickListener {
            var dialog = RecentsDialog(requireContext(), "", RecentsLogosPreferences(requireContext())) { selectedLogoUrl ->
                lifecycleScope.launch {
                    schedulesPrefs.set(logoGuest = selectedLogoUrl)
                }
            }
            dialog.show()
        }

        binding.ivTitleClear.setOnClickListener {
            schedulesPrefs.set(title = "")
        }

        binding.ivDeleteLiveStream.visibility = View.GONE
        binding.ivCreateLiveStream.visibility = View.INVISIBLE
        binding.ivRemoveBackground.visibility = View.GONE
        binding.ivLogoHome.visibility = View.GONE
        binding.ivLogoGuest.visibility = View.GONE
        binding.ivDateTime.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun handleCreateListener(liveStreamContentData: LiveStreamContentData?)
    {
        if (liveStreamContentData != null) {
            binding.ivCreateLiveStream.setOnClickListener {
                var eventScheduledStartTime = liveStreamContentData.scheduledStartTime
                val now = ZonedDateTime.now()
                val minimumStartTime = now.minusMinutes((now.minute % 5).toLong())
                if (minimumStartTime.isAfter(eventScheduledStartTime)) {
                    toast(getString(R.string.warning_schedule_start_time), Toast.LENGTH_LONG, R.drawable.ic_warning)
                } else {
                    val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_warning, null)
                    dialogView.findViewById<TextView>(R.id.dialog_message).text = getString(R.string.confirm_create_live_stream)

                    AlertDialog.Builder(requireContext())
                        .setView(dialogView)
                        .setPositiveButton("OK") { dialog, _ ->
                            youtubeViewModel.addOrUpdateBroadcastEvent(liveStreamContentData) { broadcastId ->
                                schedulesPrefs.add(broadcastId)
                                youtubeViewModel.loadLiveBroadcast()
                                CoroutineScope(Dispatchers.Main).launch {
                                    (binding.spinnerBroadcast.adapter as BaseAdapter).notifyDataSetChanged()
                                }
                            }
                            dialog.dismiss()
                        }
                        .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                        .create()
                        .show()
                }
            }
        } else {
            binding.ivCreateLiveStream.setOnClickListener { }
        }
    }

    private fun handleDeleteListener(broadcastId: String) {
        if (schedulesPrefs.isEditing()) {
            binding.ivDeleteLiveStream.setOnClickListener {
                val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_warning, null)
                dialogView.findViewById<TextView>(R.id.dialog_message).text =
                    getString(R.string.confirm_delete_live_stream)

                AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .setPositiveButton("OK") { dialog, _ ->
                        youtubeViewModel.deleteLive(broadcastId) {
                            youtubeViewModel.loadLiveBroadcast()
                            CoroutineScope(Dispatchers.Main).launch {
                                (binding.spinnerBroadcast.adapter as BaseAdapter).notifyDataSetChanged()
                            }
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                    .create()
                    .show()
            }
        } else {
            binding.ivDeleteLiveStream.setOnClickListener { }
        }
    }
}