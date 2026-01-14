package it.lmqv.livematchcam

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
import it.lmqv.livematchcam.factories.SportsFactory
import it.lmqv.livematchcam.fragments.status.StatusContainerFragment
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.services.stream.IStreamService
import it.lmqv.livematchcam.services.stream.StreamServiceConnector
import it.lmqv.livematchcam.services.stream.VideoCaptureFormat
import it.lmqv.livematchcam.services.youtube.YouTubeClientProvider
import it.lmqv.livematchcam.utils.OptionItem
import it.lmqv.livematchcam.viewmodels.StreamConfigurationViewModel
import it.lmqv.livematchcam.viewmodels.VideoSourceKind
import it.lmqv.livematchcam.viewmodels.YoutubeViewModel
import it.lmqv.livematchcam.viewmodels.YoutubeViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlin.getValue

class StreamActivity : BaseActivity(),
    ConnectChecker,
    FpsListener.Callback {

    private lateinit var binding: ActivityStreamBinding

    private val statusContainerFragment = StatusContainerFragment.getInstance()

    private lateinit var streamServiceConnector : StreamServiceConnector
    private lateinit var streamService : IStreamService

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logd("StreamActivity::onCreate")

        binding = ActivityStreamBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        //val dm = getSystemService(DisplayManager::class.java)
        //dm.registerDisplayListener(displayListener, null)

        binding.bStartStop.setOnClickListener {
            streamService.toggleStreaming({
                binding.bStartStop.setImageResource(R.drawable.stream_stop_icon)
            },
                { shouldEnd ->
                    if (shouldEnd) { youtubeViewModel.completeLive() }
                    binding.bStartStop.setImageResource(R.drawable.stream_icon)
                })
        }

        binding.microphone.setOnClickListener {
            if (streamService.toggleMicrophoneAudio() == true) {
                binding.microphone.setImageResource(R.drawable.microphone_off)
            } else {
                binding.microphone.setImageResource(R.drawable.microphone_on)
            }
        }

        binding.changeResolutionStrategy.setOnClickListener {
            this.changeVideoSettingsDialog()
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

        launchOnCreated {
            MatchRepository.sport.collectLatest { sport ->
                Logd("StreamActivity :: MatchRepository.sport $sport")

                var sportFragmentFactory = SportsFactory.get(sport)
                var controlBarFragment = sportFragmentFactory.getControlBar()
                supportFragmentManager.beginTransaction()
                    .replace(
                        binding.controlBarContainer.id,
                        controlBarFragment as Fragment,
                        "ControlBarFragmentTag"
                    ).commit()

                this.streamServiceConnector = StreamServiceConnector(this)
                this.streamServiceConnector.setOnServiceConnected { streamService ->
                    Logd("StreamActivity :: setOnServiceConnected")
                    this.streamService = streamService

                    this.streamService.setConnectCheckerCallback(this)
                    this.streamService.setFpsListenerCallback(this)

                    if (this.streamService.isStreaming() == true) {
                        binding.bStartStop.setImageResource(R.drawable.stream_stop_icon)
                        //Logd("StreamActivity :: setOnServiceConnected :: isStreaming!")
                    } else {
                        binding.bStartStop.setImageResource(R.drawable.stream_icon)
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

                    //this.initCameraSettingsDialog()
                }
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(binding.statusContainer.id,
                this.statusContainerFragment)
            .commit()
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
        val optionsVideoFps = listOf(
            OptionItem(20, "20 fps"),
            OptionItem(25, "25 fps"),
            OptionItem(30, "30 fps"),
            OptionItem(60, "60 fps")
        )

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
}