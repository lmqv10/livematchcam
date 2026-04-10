package it.lmqv.livematchcam

import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.pedro.common.ConnectChecker
import com.pedro.library.util.FpsListener
import it.lmqv.livematchcam.databinding.ActivityStreamBinding
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.formatHourTime
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.extensions.launchOnCreated
import it.lmqv.livematchcam.extensions.launchOnResumed
import it.lmqv.livematchcam.extensions.toOptionItems
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.factories.EncoderItemsFactory
import it.lmqv.livematchcam.factories.sports.SportsFactory
import it.lmqv.livematchcam.fragments.status.StatusContainerFragment
import it.lmqv.livematchcam.preferences.PerformancePreferencesManager
import it.lmqv.livematchcam.preferences.ReplayPreferencesManager
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.services.stream.IStreamService
import it.lmqv.livematchcam.services.stream.StreamServiceConnector
import it.lmqv.livematchcam.services.stream.VideoCaptureFormat
import it.lmqv.livematchcam.services.stream.audio.AudioDeviceManager
import it.lmqv.livematchcam.services.youtube.YouTubeClientProvider
import it.lmqv.livematchcam.utils.OptionItem
import it.lmqv.livematchcam.viewmodels.StreamConfigurationViewModel
import it.lmqv.livematchcam.viewmodels.VideoSourceKind
import it.lmqv.livematchcam.viewmodels.YoutubeViewModel
import it.lmqv.livematchcam.viewmodels.YoutubeViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlin.getValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class StreamActivity : BaseActivity(),
    ConnectChecker,
    FpsListener.Callback {

    private lateinit var binding: ActivityStreamBinding

    private val statusContainerFragment = StatusContainerFragment.getInstance()

    private lateinit var streamServiceConnector : StreamServiceConnector
    internal lateinit var streamService : IStreamService

    private val streamConfigurationViewModel: StreamConfigurationViewModel by viewModels()
    private val youtubeViewModel: YoutubeViewModel by viewModels {
        YoutubeViewModelFactory(application, YouTubeClientProvider.get())
    }

    private lateinit var callback: OnBackPressedCallback
    //private lateinit var cameraSettingsDialog: CameraSettingsDialog

//    private val displayListener = object : DisplayManager.DisplayListener {
//        override fun onDisplayAdded(displayId: Int) {}
//        override fun onDisplayRemoved(displayId: Int) {}
//
//        override fun onDisplayChanged(displayId: Int) {
//            //val rotation = this@StreamActivity.display.rotation
//            val rotation = windowManager.defaultDisplay.rotation
//            cameraFragment.setRotation(rotation)
//        }
//    }

    // Replay Preview
    private var replayDurationMs: Int = 0

    private lateinit var performancePrefs: PerformancePreferencesManager
    private lateinit var replayPrefs: ReplayPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logd("StreamActivity::onCreate")

        binding = ActivityStreamBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        performancePrefs = PerformancePreferencesManager(this)
        replayPrefs = ReplayPreferencesManager(this)

        //val dm = getSystemService(DisplayManager::class.java)
        //dm.registerDisplayListener(displayListener, null)

        binding.bStartStop.setOnClickListener {
            streamService.toggleStreaming({
                binding.bStartStop.setImageResource(R.drawable.stream_stop_icon)
                if (performancePrefs.isReplayEnabled()) {
                    binding.bReplay.visibility = View.VISIBLE
                    binding.bReplay.isClickable = true
                    binding.bQuickReplay.visibility = View.VISIBLE
                    binding.bQuickReplay.isClickable = true
                }
            },
                { shouldEnd ->
                    if (shouldEnd) { youtubeViewModel.completeLive() }
                    binding.bStartStop.setImageResource(R.drawable.stream_icon)
                    binding.bReplay.visibility = View.GONE
                    binding.bReplay.isClickable = false
                    binding.bQuickReplay.visibility = View.GONE
                    binding.bQuickReplay.isClickable = false
                })
        }

        binding.bReplay.setOnClickListener {
            lifecycleScope.launch {
                binding.bReplay.isEnabled = false
                binding.bQuickReplay.isEnabled = false
                toast("Preparing replay...")
                val success = streamService.prepareReplay()
                if (!success) {
                    toast("Replay not available")
                }
                binding.bReplay.isEnabled = true
                binding.bQuickReplay.isEnabled = true
            }
        }

        binding.bQuickReplay.setOnClickListener {
            lifecycleScope.launch {
                binding.bReplay.isEnabled = false
                binding.bQuickReplay.isEnabled = false
                toast("Starting quick replay...")
                if (streamService.prepareReplay()) {
                    val metadata = streamService.replayMetadata.value
                    if (metadata != null) {
                        val durationMs = metadata.durationMs
                        val quickReplayMs = replayPrefs.getQuickReplayDurationSeconds() * 1000L
                        val seekMs = if (durationMs > quickReplayMs) durationMs - quickReplayMs else 0L
                        val replayDuration = (durationMs - seekMs) / 1000L
                        toast("Quick Replay duration ${replayDuration}s")
                        streamService.startReplay(seekMs)
                    }
                } else {
                    toast("Replay not available")
                }
                binding.bReplay.isEnabled = true
                binding.bQuickReplay.isEnabled = true
            }
        }

        binding.microphone.setOnClickListener {
            if (streamService.toggleMicrophoneAudio() == true) {
                binding.microphone.setImageResource(R.drawable.microphone_off)
            } else {
                binding.microphone.setImageResource(R.drawable.microphone_on)
            }
        }

        // Audio Monitor toggle (mic -> headphones)
        binding.audioMonitorToggle.setOnClickListener {
            onAudioMonitorToggleClicked()
        }

        // USB Audio source selector (visible only with UVC)
        binding.usbAudioSource.setOnClickListener {
            onUsbAudioSourceClicked()
        }

//        binding.changeFiltersStrategy.setOnClickListener {
//            val dialog = PreferencesDialogFragment
//                .newInstance(R.xml.filters_preferences)//, getString(R.string.video_settings_screen_key))
//            dialog.show(supportFragmentManager, "preferences_dialog")
//        }

        binding.changeResolutionStrategy.setOnClickListener {
            this.changeVideoSettingsDialog()

            // CUSTOM DIALOG
//            this.cameraSettingsDialog
//                .setEnabled(!this.streamService.isStreaming())
//                .setVideoSource(streamConfigurationViewModel.videoSourceKind.value)
//                .setVideoCaptureFormat(streamConfigurationViewModel.videoCaptureFormat.value)
//                .setEncoderFps(streamConfigurationViewModel.fps.value)
//                .show()

        }
//
//        binding.mainBannerSwitch.setOnCheckedChangeListener { _, isChecked ->
//            MatchRepository.setMainBannerVisible(isChecked)
//        }
//
//        lifecycleScope.launch {
//            MatchRepository.mainBannerVisible.collect { isVisible ->
//                binding.mainBannerSwitch.isChecked = isVisible
//                if (isVisible) {
//                    binding.mainBannerTip.text = resources.getString(R.string.show_banner)
//                } else {
//                    binding.mainBannerTip.text = resources.getString(R.string.hide_banner)
//                }
//            }
//        }
//
//        lifecycleScope.launch {
//            MatchRepository.mainBannerURL.collect { spotBannerURL ->
//                var isEnabled = spotBannerURL.isNotEmpty()
//                binding.mainBannerSwitch.isEnabled = isEnabled
//            }
//        }

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        if (audioManager.isMicrophoneMute) {
            binding.microphone.setImageResource(R.drawable.microphone_off)
        } else {
            binding.microphone.setImageResource(R.drawable.microphone_on)
        }

        this.streamServiceConnector = StreamServiceConnector(this)

        launchOnCreated {
            MatchRepository.sport.collectLatest { sport ->
                Logd("StreamActivity :: MatchRepository.sport $sport")

                this.streamServiceConnector.setOnServiceConnected { streamService ->
                    Logd("StreamActivity :: setOnServiceConnected")
                    this.streamService = streamService

                    this.streamService.setConnectCheckerCallback(this)
                    this.streamService.setFpsListenerCallback(this)

                    if (this.streamService.isStreaming() == true) {
                        binding.bStartStop.setImageResource(R.drawable.stream_stop_icon)
                        if (performancePrefs.isReplayEnabled()) {
                            binding.bReplay.visibility = View.VISIBLE
                            binding.bReplay.isClickable = true
                            binding.bQuickReplay.visibility = View.VISIBLE
                            binding.bQuickReplay.isClickable = true
                        }
                        //Logd("StreamActivity :: setOnServiceConnected :: isStreaming!")
                    } else {
                        binding.bStartStop.setImageResource(R.drawable.stream_icon)
                        binding.bReplay.visibility = View.GONE
                        binding.bReplay.isClickable = false
                        binding.bQuickReplay.visibility = View.GONE
                        binding.bQuickReplay.isClickable = false
                    }

                    Logd("StreamActivity :: setOnServiceConnected :: initPreview?")
                    this.streamService.initPreview(binding.surfaceView, sport)

                    launchOnResumed {
                        this.streamService.videoSourceZoomHandler.collect { videoSourceZoomHandler ->
                            if (videoSourceZoomHandler != null) {
                                //Logd("StreamActivity :: videoSourceZoomHandler")
                                this.statusContainerFragment.setVideoSourceZoomHandler(videoSourceZoomHandler)
                            }
                        }
                    }

                    launchOnResumed {
                        this.streamService.streamingElapsedTime.collect { timeElapsedInSeconds ->
                            binding.streamingTime.text = formatHourTime(timeElapsedInSeconds)
                        }
                    }

                    launchOnResumed {
                        MatchRepository.RTMPServerURI.collect { configuredServerURI ->
                            configuredServerURI?.let {
                                binding.bStartStop.isClickable = true
                            }
                            //Logd("StreamActivity :: RTMPServerURI $configuredServerURI")
                            this.streamService.setEndpoint(configuredServerURI)
                        }
                    }

                    launchOnResumed {
                        this.streamService.replayState.collect { state ->
                            updateReplayUI(state)
                        }
                    }

                    launchOnResumed {
                        this.streamService.replayMetadata.collect { metadata ->
                            if (metadata != null) {
                                val durationMs = metadata.durationMs.toInt()
                                replayDurationMs = durationMs
                            }
                        }
                    }

                    // Observe audio monitor state for icon updates
                    launchOnResumed {
                        streamService.audioMonitorEnabled.collectLatest { enabled ->
                            if (enabled) {
                                binding.audioMonitorToggle.setImageResource(R.drawable.ic_headphones_on)
                            } else {
                                binding.audioMonitorToggle.setImageResource(R.drawable.ic_headphones_off)
                            }
                        }
                    }
                    //this.initCameraSettingsDialog()
                }

                // Observe VideoSourceKind to show/hide USB audio button
                launchOnResumed {
                    streamConfigurationViewModel.videoSourceKind.collectLatest { kind ->
                        if (kind == VideoSourceKind.UVC_SONY) {
                            binding.usbAudioSource.visibility = View.VISIBLE
                        } else {
                            binding.usbAudioSource.visibility = View.GONE
                            // Reset to default mic when switching away from UVC
                            if (::streamService.isInitialized) {
                                streamService.setAudioInputDevice(null)
                                binding.usbAudioSource.setImageResource(R.drawable.ic_usb_audio)
                            }
                        }
                    }
                }

                var sportFragmentFactory = SportsFactory.get(sport)
                
                val transaction = supportFragmentManager.beginTransaction()
                    .replace(binding.statusContainer.id, this.statusContainerFragment)

                if (performancePrefs.isScoreboardEnabled()) {
                    var controlBarFragment = sportFragmentFactory.getControlBar()
                    transaction.replace(binding.controlBarContainer.id, controlBarFragment as Fragment, "ControlBarFragmentTag")
                } else {
                    binding.controlBarContainer.visibility = View.GONE
                }

                if (performancePrefs.isFiltersEnabled()) {
                    var bannersContainerFragment = sportFragmentFactory.getFiltersSlimControl()
                    transaction.replace(binding.bannersContainer.id, bannersContainerFragment as Fragment, "bannersContainerFragmentTag")
                } else {
                    binding.bannersContainer.visibility = View.GONE
                }

                transaction.commitAllowingStateLoss()

            }
        }

    }

    override fun onStart() {
        super.onStart()
        Logd("StreamActivity::onStart")

        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                streamService.stopStreamWithConfirm {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        this.onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onPause() {
        super.onPause()
        Logd("StreamActivity::OnPause")
        this.streamServiceConnector.unbindService()
    }

    override fun onResume() {
        super.onResume()
        Logd("StreamActivity::onResume")
        this.streamServiceConnector.bindService()
    }

    private fun updateReplayUI(state: it.lmqv.livematchcam.services.replay.ReplayState) {
        if (!performancePrefs.isReplayEnabled()) {
            binding.replayFragmentContainer.visibility = View.GONE
            return
        }

        // Hide trigger buttons during replay/seeking
        val isReplayActive = state == it.lmqv.livematchcam.services.replay.ReplayState.SEEKING ||
                state == it.lmqv.livematchcam.services.replay.ReplayState.REPLAYING

        if (isReplayActive) {
            binding.bReplay.visibility = View.GONE
            binding.bQuickReplay.visibility = View.GONE
            binding.controlBar.visibility = View.GONE
            binding.controlBarContainer.visibility = View.GONE
            binding.bannersContainer.visibility = View.GONE
        } else {
            if (::streamService.isInitialized && streamService.isStreaming() == true) {
                binding.bReplay.visibility = View.VISIBLE
                binding.bQuickReplay.visibility = View.VISIBLE
            }
            binding.controlBar.visibility = View.VISIBLE
            // Restore scoreboard if enabled
            if (performancePrefs.isScoreboardEnabled()) {
                binding.controlBarContainer.visibility = View.VISIBLE
            }
            // Restore banners if enabled
            if (performancePrefs.isFiltersEnabled()) {
                binding.bannersContainer.visibility = View.VISIBLE
            }
        }

        val fragment = when (state) {
            it.lmqv.livematchcam.services.replay.ReplayState.SEEKING -> it.lmqv.livematchcam.fragments.replay.ReplaySeekingFragment()
            it.lmqv.livematchcam.services.replay.ReplayState.REPLAYING -> it.lmqv.livematchcam.fragments.replay.ReplayPlayingFragment()
            else -> null
        }

        if (fragment != null) {
            binding.replayFragmentContainer.visibility = View.VISIBLE
            supportFragmentManager.beginTransaction()
                .replace(R.id.replay_fragment_container, fragment)
                .commit()
        } else {
            binding.replayFragmentContainer.visibility = View.GONE
            val currentFragment = supportFragmentManager.findFragmentById(R.id.replay_fragment_container)
            if (currentFragment != null) {
                supportFragmentManager.beginTransaction()
                    .remove(currentFragment)
                    .commit()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Logd("StreamActivity::onDestroy")
        //val dm = getSystemService(DisplayManager::class.java)
        //dm.unregisterDisplayListener(displayListener)
        this.callback.remove()
        this.streamServiceConnector.stopService()
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        /*when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                cameraFragment.updateZoom(ManualZoomLevel.None)
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                cameraFragment.updateZoom(ManualZoomLevel.None)
                return true
            }
        }*/
        return super.onKeyUp(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        /*when (keyCode) {
            KeyEvent.KEYCODE_HEADSETHOOK -> {
                cameraFragment.updateZoom(ManualZoomLevel.None)
                return true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                cameraFragment.updateZoom(ManualZoomLevel.In)
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                cameraFragment.updateZoom(ManualZoomLevel.Out)
                return true
            }
        }*/
        return super.onKeyDown(keyCode, event)
    }

    override fun onFps(fps: Int) {
       //Logd("StreamActivity :: onFps: $fps")
        this.statusContainerFragment.setFps(fps)
    }

    override fun onConnectionStarted(url: String) {
        toast("Streaming Started on $url")
    }

    override fun onConnectionSuccess() {
        //toast("Connected on ${streamServiceConnector.getEndpoint()}")
        toast("Connected")
    }

    override fun onConnectionFailed(reason: String) {
        toast("Connection failed! $reason")
    }

    override fun onNewBitrate(bitrate: Long) {
        this.statusContainerFragment.setBitrate(bitrate)
    }

    override fun onDisconnect() {
        this.statusContainerFragment.setBitrate(0)
        //binding.bStartStop.setImageResource(R.drawable.stream_icon)
        toast("Disconnected")
    }

    override fun onAuthError() {
        //binding.bStartStop.setImageResource(R.drawable.stream_icon)
        toast("Auth error")
    }

    override fun onAuthSuccess() {
        toast("Auth success")
    }

//    private fun initCameraSettingsDialog() {
//
//        binding.changeResolutionStrategy.isClickable = true
//
//        this.cameraSettingsDialog = CameraSettingsDialog(
//            this,
//            this.streamService,
//            { videoSourceKind ->
//                val current = streamConfigurationViewModel.videoSourceKind.value
//                if (current != videoSourceKind) {
//                    streamConfigurationViewModel.setVideoSourceKind(videoSourceKind)
//                }
//                this.hideSystemUI()
//            },
//            { selectedVideoCaptureFormat ->
//                val videoCaptureFormat = streamConfigurationViewModel.videoCaptureFormat.value
//                if (videoCaptureFormat?.width != selectedVideoCaptureFormat.width && videoCaptureFormat?.height != selectedVideoCaptureFormat.height) {
//                    streamConfigurationViewModel.setVideoCaptureFormat(selectedVideoCaptureFormat)
//                }
//                this.hideSystemUI()
//            },
//            { selectedFps ->
//                var fps = streamConfigurationViewModel.fps.value
//                if (fps != selectedFps) {
//                    streamConfigurationViewModel.setFps(selectedFps)
//                }
//                this.hideSystemUI()
//            })
//        this.cameraSettingsDialog.setOnShowListener {
//            this.hideSystemUI()
//        }
//    }

    private fun changeVideoSettingsDialog() {
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_camera_settings, null)

        val spinnerVideoSource = dialogView.findViewById<Spinner>(R.id.video_source)
        val optionsVideoSource = VideoSourceKind.entries

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            optionsVideoSource
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        //spinnerVideoSource.isEnabled = !this.streamService.isStreaming()
        spinnerVideoSource.adapter = adapter
        spinnerVideoSource.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val selected = parent.getItemAtPosition(position) as VideoSourceKind

                    val current = streamConfigurationViewModel.videoSourceKind.value
                    if (current != selected) {
                        streamConfigurationViewModel.setVideoSourceKind(selected)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

        val defaultIndex = optionsVideoSource.indexOf(
            streamConfigurationViewModel.videoSourceKind.value
        )
        spinnerVideoSource.setSelection(defaultIndex)

        val optionsVideoResolutions = this.streamService.getVideoCaptureFormats().toOptionItems()
        val spinnerVideoResolutions = dialogView.findViewById<Spinner>(R.id.video_resolutions)
        val adapterVideoResolutions = ArrayAdapter(this, android.R.layout.simple_spinner_item, optionsVideoResolutions)
        adapterVideoResolutions.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVideoResolutions.isEnabled = !this.streamService.isStreaming()
        spinnerVideoResolutions.adapter = adapterVideoResolutions
        @Suppress("UNCHECKED_CAST")
        spinnerVideoResolutions.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position) as OptionItem<VideoCaptureFormat>
                var selectedItemValue = selectedItem.key
                val resolution = this@StreamActivity.streamConfigurationViewModel.videoCaptureFormat.value
                if (resolution?.width != selectedItemValue.width && resolution?.height != selectedItemValue.height) {
                    this@StreamActivity.streamConfigurationViewModel.setVideoCaptureFormat(selectedItemValue)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }
        val defaultResolution = optionsVideoResolutions.indexOfFirst {
            it.key.width == this@StreamActivity.streamConfigurationViewModel.videoCaptureFormat.value?.width &&
            it.key.height == this@StreamActivity.streamConfigurationViewModel.videoCaptureFormat.value?.height
        }
        spinnerVideoResolutions.setSelection(defaultResolution)

        val spinnerVideoFps = dialogView.findViewById<Spinner>(R.id.encoder_fps)
        val optionsVideoFps = EncoderItemsFactory.getFps()

        val adapterVideoFps = ArrayAdapter(this, android.R.layout.simple_spinner_item, optionsVideoFps)
        adapterVideoFps.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVideoFps.isEnabled = !this.streamService.isStreaming()
        spinnerVideoFps.adapter = adapterVideoFps
        @Suppress("UNCHECKED_CAST")
        spinnerVideoFps.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position) as OptionItem<Int>
                var selectedItemValue = selectedItem.key
                var fps = this@StreamActivity.streamConfigurationViewModel.fps.value
                if (fps != selectedItemValue) {
                    this@StreamActivity.streamConfigurationViewModel.setFps(selectedItemValue)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }
        val defaultVideoFps = optionsVideoFps.indexOfFirst { it.key == this@StreamActivity.streamConfigurationViewModel.fps.value }
        spinnerVideoFps.setSelection(defaultVideoFps)

        val spinnerVideoBitrate = dialogView.findViewById<Spinner>(R.id.encoder_bitrate)
        val optionsVideoBitrate = EncoderItemsFactory.getBitrate()

        val adapterVideoBitrate = ArrayAdapter(this, android.R.layout.simple_spinner_item, optionsVideoBitrate)
        adapterVideoBitrate.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVideoBitrate.isEnabled = !this.streamService.isStreaming()
        spinnerVideoBitrate.adapter = adapterVideoBitrate
        @Suppress("UNCHECKED_CAST")
        spinnerVideoBitrate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position) as OptionItem<Int>
                var selectedItemValue = selectedItem.key
                var bitrate = this@StreamActivity.streamConfigurationViewModel.bitrate.value
                if (bitrate != selectedItemValue) {
                    this@StreamActivity.streamConfigurationViewModel.setBitrate(selectedItemValue)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }
        val defaultVideoBitrate = optionsVideoBitrate.indexOfFirst { it.key == this@StreamActivity.streamConfigurationViewModel.bitrate.value }
        spinnerVideoBitrate.setSelection(defaultVideoBitrate)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                hideSystemUI()
            }
            .create()

        dialog.setOnShowListener {
            hideSystemUI()
        }

        dialog.show()
    }
    
    private fun formatTimeMmSs(totalSeconds: Int): String {
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    // --- AUDIO MONITOR ---

    private fun onAudioMonitorToggleClicked() {
        if (!::streamService.isInitialized) return

        // If already enabled, just toggle off
        if (streamService.audioMonitorEnabled.value) {
            streamService.toggleAudioMonitor()
            return
        }

        // Check available headphone devices
        val headphones = streamService.getAvailableOutputDevices()

        when {
            headphones.isEmpty() -> {
                toast("Nessuna cuffia connessa")
            }
            headphones.size == 1 -> {
                // Single device: auto-select and enable
                streamService.setMonitorOutputDevice(headphones[0])
                streamService.toggleAudioMonitor()
            }
            else -> {
                // Multiple devices: show selection dialog
                showHeadphoneSelectionDialog(headphones)
            }
        }
    }

    private fun showHeadphoneSelectionDialog(devices: List<AudioDeviceInfo>) {
        val names = devices.map { AudioDeviceManager.getDeviceDisplayName(it) }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Seleziona dispositivo audio")
            .setItems(names) { dialog, which ->
                streamService.setMonitorOutputDevice(devices[which])
                streamService.toggleAudioMonitor()
                dialog.dismiss()
                hideSystemUI()
            }
            .setNegativeButton("Annulla") { dialog, _ ->
                dialog.dismiss()
                hideSystemUI()
            }
            .create()
            .also { dlg ->
                dlg.setOnShowListener { hideSystemUI() }
                dlg.show()
            }
    }

    // --- USB AUDIO SOURCE ---

    private fun onUsbAudioSourceClicked() {
        if (!::streamService.isInitialized) return

        val currentDevice = streamService.getSelectedAudioInputDevice()

        // If already using a USB device, toggle it off (reset to default mic)
        if (currentDevice != null) {
            streamService.setAudioInputDevice(null)
            binding.usbAudioSource.setImageResource(R.drawable.ic_usb_audio)
            return
        }

        // Check available USB audio input devices
        val usbInputDevices = streamService.getAvailableInputDevices()

        when {
            usbInputDevices.isEmpty() -> {
                toast("Nessun dispositivo audio USB trovato")
            }
            usbInputDevices.size == 1 -> {
                streamService.setAudioInputDevice(usbInputDevices[0])
                binding.usbAudioSource.setImageResource(R.drawable.ic_usb_audio_active)
            }
            else -> {
                showUsbAudioSelectionDialog(usbInputDevices)
            }
        }
    }

    private fun showUsbAudioSelectionDialog(devices: List<AudioDeviceInfo>) {
        val names = devices.map { AudioDeviceManager.getDeviceDisplayName(it) }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Seleziona sorgente audio USB")
            .setItems(names) { dialog, which ->
                streamService.setAudioInputDevice(devices[which])
                binding.usbAudioSource.setImageResource(R.drawable.ic_usb_audio_active)
                dialog.dismiss()
                hideSystemUI()
            }
            .setNegativeButton("Annulla") { dialog, _ ->
                dialog.dismiss()
                hideSystemUI()
            }
            .create()
            .also { dlg ->
                dlg.setOnShowListener { hideSystemUI() }
                dlg.show()
            }
    }
}