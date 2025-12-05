package it.lmqv.livematchcam.fragments

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.encoder.utils.gl.TranslateTo
import com.pedro.library.util.FpsListener
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentCameraBinding
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.formatHourTime
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.extensions.launchOnResumed
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.factories.SportsFactory
import it.lmqv.livematchcam.handlers.offset.IOffsetDegreeHandler
import it.lmqv.livematchcam.handlers.offset.ManualZoomLevelHandler
import it.lmqv.livematchcam.handlers.zoom.IZoomLevelHandler
import it.lmqv.livematchcam.handlers.zoom.NoDebounceExtraSmoothZoomLevelHandler
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.services.stream.IVideoSourceZoomHandler
import it.lmqv.livematchcam.services.stream.StreamServiceConnector
import it.lmqv.livematchcam.services.stream.filters.BitmapRotatorFilterRender
import it.lmqv.livematchcam.services.stream.filters.FilterDescriptor
import it.lmqv.livematchcam.services.stream.filters.RotatorDescriptor
import it.lmqv.livematchcam.services.stream.filters.ScoreBoardFilterRender
import it.lmqv.livematchcam.services.youtube.YouTubeClientProvider
import it.lmqv.livematchcam.utils.KeyDescription
import it.lmqv.livematchcam.viewmodels.StatusViewModel
import it.lmqv.livematchcam.viewmodels.StreamConfigurationViewModel
import it.lmqv.livematchcam.viewmodels.YoutubeViewModel
import it.lmqv.livematchcam.viewmodels.YoutubeViewModelFactory
import it.lmqv.livematchcam.views.SwipeSurfaceView
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class CameraFragment: Fragment(),
    ConnectChecker,
    FpsListener.Callback,
    StatusFragment.OnZoomButtonClickListener,
    SwipeSurfaceView.OnSwipeGesture {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun getInstance(): CameraFragment = CameraFragment()
    }

    private lateinit var streamServiceConnector : StreamServiceConnector
    private lateinit var zoomLevelHandler: IZoomLevelHandler

    private lateinit var offsetDegreeHandler: IOffsetDegreeHandler

    private lateinit var sportCollectJob : Job
    private lateinit var callback: OnBackPressedCallback

    private val statusFragment = StatusFragment.newInstance()
    private val streamConfigurationViewModel: StreamConfigurationViewModel by activityViewModels()
    private val statusViewModel: StatusViewModel by activityViewModels()

    private val youtubeViewModel: YoutubeViewModel by activityViewModels {
        YoutubeViewModelFactory(requireActivity().application, YouTubeClientProvider.get())
    }

    private lateinit var scoreBoardFilter: ScoreBoardFilterRender
    private lateinit var spotBannerFilter: BitmapRotatorFilterRender
    private lateinit var mainBannerFilter: BitmapRotatorFilterRender

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)

        childFragmentManager.beginTransaction()
            .replace(R.id.status_container, statusFragment)
            .commit()

        offsetDegreeHandler = ManualZoomLevelHandler(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Logd("CameraFragment::onViewCreated")

        this.streamServiceConnector = StreamServiceConnector(requireActivity())
        this.sportCollectJob = viewLifecycleOwner.lifecycleScope.launch {
            MatchRepository.sport.collectLatest { sport ->
                //Logd("CameraFragment::MatchRepository.sport $sport")
                var sportFragmentFactory = SportsFactory.get(sport)
                var controlBarFragment = sportFragmentFactory.getControlBar()

                val scoreBoardFragment = sportFragmentFactory.getScoreBoard()

                childFragmentManager.beginTransaction()
                    .replace(
                        R.id.control_bar_container,
                        controlBarFragment as Fragment,
                        "ControlBarFragmentTag"
                    ).commit()

                childFragmentManager.beginTransaction()
                    .replace(
                        R.id.score_board_placeholder,
                        scoreBoardFragment as Fragment,
                        "ScoreBoardFragmentTag"
                    ).commit()

                scoreBoardFilter = ScoreBoardFilterRender(scoreBoardFragment,
                    filterDescriptor = FilterDescriptor(maxFactor = 30f, translateTo = TranslateTo.TOP_LEFT))

                spotBannerFilter = BitmapRotatorFilterRender(requireContext(),
                    filterDescriptor = FilterDescriptor(maxFactor = 25f, translateTo = TranslateTo.TOP_RIGHT),
                    rotatorDescriptor = RotatorDescriptor())

                mainBannerFilter = BitmapRotatorFilterRender(requireContext(),
                    filterDescriptor = FilterDescriptor(maxFactor = 70f, translateTo = TranslateTo.CENTER),
                    rotatorDescriptor = RotatorDescriptor(targetWidthDp = 300))
            }
        }

        binding.bStartStop.setOnClickListener {
            streamServiceConnector.toggleStreaming({
                    binding.bStartStop.setImageResource(R.drawable.stream_stop_icon)
                },
                { shouldEnd ->
                    if (shouldEnd) { youtubeViewModel.completeLive() }
                    binding.bStartStop.setImageResource(R.drawable.stream_icon)
                })
        }

        binding.microphone.setOnClickListener {
            if (streamServiceConnector.toggleMicrophoneAudio() == true) {
                binding.microphone.setImageResource(R.drawable.microphone_off)
            } else {
                binding.microphone.setImageResource(R.drawable.microphone_on)
            }
        }

        binding.changeResolutionStrategy.setOnClickListener {
            this.changeVideoSettingsDialog()
        }

        binding.mainBannerSwitch.setOnCheckedChangeListener { _, isChecked ->
            MatchRepository.setMainBannerVisible(isChecked)
        }

        lifecycleScope.launch {
            MatchRepository.mainBannerVisible.collect { isVisible ->
                binding.mainBannerSwitch.isChecked = isVisible
                if (isVisible) {
                    binding.mainBannerTip.text = resources.getString(R.string.show_banner)
                } else {
                    binding.mainBannerTip.text = resources.getString(R.string.hide_banner)
                }
            }
        }

        lifecycleScope.launch {
            MatchRepository.mainBannerURL.collect { spotBannerURL ->
                var isEnabled = spotBannerURL.isNotEmpty()
                binding.mainBannerSwitch.isEnabled = isEnabled
            }
        }

        lifecycleScope.launch {
            combine(
                MatchRepository.spotBannerURL,
                MatchRepository.spotBannerVisible
            ) { url, visible -> Pair(url, visible)
            }.collect { (url, visible) ->
                launchOnStarted {
                    spotBannerFilter.setUrls(listOf(url))
//                    var urls = listOf<String>(
//                        "https://avisbiella.it/wp-content/uploads/2022/01/Logo_AVIS.png",
//                        "https://www.nowpadova.com/images/2020/01/23/AVIST_large.jpg")
//                    spotBannerFilter.setUrls(urls)
                    spotBannerFilter.setIsVisible(visible)
                }
            }
        }

        lifecycleScope.launch {
            combine(
                MatchRepository.mainBannerURL,
                MatchRepository.mainBannerVisible
            ) { url, visible ->
                Pair(url, visible)
            }.collect { (url, visible) ->
                launchOnStarted {
                    mainBannerFilter.setUrls(listOf(url))
//                    var urls = listOf<String>(
//                        "https://avisbiella.it/wp-content/uploads/2022/01/Logo_AVIS.png",
//                        "https://www.nowpadova.com/images/2020/01/23/AVIST_large.jpg")
//                    mainBannerFilter.setUrls(urls)
                    mainBannerFilter.setIsVisible(visible)
                }
            }
        }

        launchOnResumed {
            combine(
                streamConfigurationViewModel.fps,
                streamConfigurationViewModel.resolution
            ) { fps, resolution -> Pair(fps, resolution) }
            .distinctUntilChanged()
            .collect { (fps, resolutionHeight) ->
                if (fps != null && resolutionHeight != null) {
                    statusViewModel.setSourceResolution(resolutionHeight)
                    statusViewModel.setSourceFps(fps)
                }
            }
        }

        val audioManager = requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.isMicrophoneMute) {
            binding.microphone.setImageResource(R.drawable.microphone_off)
        } else {
            binding.microphone.setImageResource(R.drawable.microphone_on)
        }

        streamServiceConnector.setOnServiceConnected { streamService ->
            streamService.setConnectCheckerCallback(this)
            streamService.setFpsListenerCallback(this)

            if (streamService.isStreaming() == true) {
                binding.bStartStop.setImageResource(R.drawable.stream_stop_icon)
            } else {
                binding.bStartStop.setImageResource(R.drawable.stream_icon)

                streamServiceConnector.changeVideoSource(Camera2Source(requireContext()))
                //streamServiceConnector.changeVideoSource(UvcSonyCameraSource())
                streamServiceConnector.preparePreview(binding.surfaceView, listOf(
                    scoreBoardFilter,
                    spotBannerFilter,
                    mainBannerFilter)
                )

                zoomLevelHandler = NoDebounceExtraSmoothZoomLevelHandler(
                    requireContext(),
                    streamService as IVideoSourceZoomHandler
                )
            }

            launchOnResumed {
                streamService.streamingElapsedTime.collect { timeElapsedInSeconds ->
                    binding.streamingTime.text = formatHourTime(timeElapsedInSeconds)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Logd("CameraFragment :: onCreate")
    }

    override fun onStart() {
        super.onStart()
        Logd("CameraFragment :: onStart")

        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                streamServiceConnector.stopStreamWithConfirm {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }

        // Add callback to dispatcher
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)

        launchOnStarted {
            MatchRepository.RTMPServerURI.collect { configuredServerURI ->
                configuredServerURI?.let {
                    binding.bStartStop.isClickable = true
                }
                streamServiceConnector.setEndpoint(configuredServerURI)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        //Logd("CameraFragment :: onPause")
        streamServiceConnector.unbindService()
    }

    override fun onResume() {
        super.onResume()
        //Logd("CameraFragment :: onResume")
        streamServiceConnector.bindService()
    }

    override fun onDestroy() {
        super.onDestroy()
        //Logd("CameraFragment :: onDestroy")
        sportCollectJob.cancel()
        callback.remove()
        spotBannerFilter.stop()
        mainBannerFilter.stop()
        streamServiceConnector.stopService()
    }

    override fun onDestroyView() {
        //Logd("CameraFragment :: onDestroyView")
        super.onDestroyView()
        _binding = null
    }

    override fun onFps(fps: Int) {
        statusViewModel.setFPS(fps)
    }

    override fun onConnectionStarted(url: String) {
        toast("Streaming Started on $url")
    }

    override fun onConnectionSuccess() {
        toast("Connected on ${streamServiceConnector.getEndpoint()}")
    }

    override fun onConnectionFailed(reason: String) {
    }

    override fun onNewBitrate(bitrate: Long) {
        statusViewModel.setBitrate(bitrate / 1000_000f)
    }

    override fun onDisconnect() {
        statusViewModel.setBitrate(0f)
        binding.bStartStop.setImageResource(R.drawable.stream_icon)
        toast("Disconnected")
    }

    override fun onAuthError() {
        //binding.bStartStop.setImageResource(R.drawable.stream_icon)
        toast("Auth error")
    }

    override fun onAuthSuccess() {
        toast("Auth success")
    }

    override fun swipeUp() {
        //zoomLevelHandler.increase()
    }

    override fun swipeDown() {
        //zoomLevelHandler.decrease()
    }

    override fun swipeLeft() {
        //zoomLevelHandler.lower()
    }

    override fun swipeRight() {
        //zoomLevelHandler.upper()
    }

    override fun onZoomIn() {
        zoomLevelHandler.increase()
    }

    override fun onZoomOut() {
        zoomLevelHandler.decrease()
    }

    private fun changeVideoSettingsDialog() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_change_video_settings, null)

        val spinnerVideoResolutions = dialogView.findViewById<Spinner>(R.id.video_resolutions)
        val optionsVideoResolutions = listOf(
            KeyDescription(1080, "1920x1080p"),
            KeyDescription(720, "1280x720p")
        )

        val adapterVideoResolutions = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, optionsVideoResolutions)
        adapterVideoResolutions.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVideoResolutions.isEnabled = !streamServiceConnector.isStreaming()
        spinnerVideoResolutions.adapter = adapterVideoResolutions
        @Suppress("UNCHECKED_CAST")
        spinnerVideoResolutions.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position) as KeyDescription<Int>
                var selectedItemValue = selectedItem.key
                val height = streamConfigurationViewModel.resolution.value
                if (height != selectedItemValue) {
                    streamConfigurationViewModel.setResolution(selectedItemValue)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }
        val defaultResolution = optionsVideoResolutions.indexOfFirst {
            it.key == streamConfigurationViewModel.resolution.value
        }
        spinnerVideoResolutions.setSelection(defaultResolution)

        val spinnerVideoFps = dialogView.findViewById<Spinner>(R.id.video_fps)
        val optionsVideoFps = listOf(
            KeyDescription(20, "20fps"),
            KeyDescription(25, "25fps"),
            KeyDescription(30, "30fps"),
            //KeyDescription(60, "60fps")
        )

        val adapterVideoFps = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, optionsVideoFps)
        adapterVideoFps.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVideoFps.isEnabled = !streamServiceConnector.isStreaming()
        spinnerVideoFps.adapter = adapterVideoFps
        @Suppress("UNCHECKED_CAST")
        spinnerVideoFps.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position) as KeyDescription<Int>
                var selectedItemValue = selectedItem.key
                var fps = streamConfigurationViewModel.fps.value
                if (fps != selectedItemValue) {
                    streamConfigurationViewModel.setFps(selectedItemValue)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }
        val defaultVideoFps = optionsVideoFps.indexOfFirst { it.key == streamConfigurationViewModel.fps.value }
        spinnerVideoFps.setSelection(defaultVideoFps)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                requireActivity().hideSystemUI()
            }
            .create()

        dialog.setOnShowListener {
            requireActivity().hideSystemUI()
        }

        dialog.show()
    }
}