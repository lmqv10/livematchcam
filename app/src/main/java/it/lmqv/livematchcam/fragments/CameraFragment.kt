package it.lmqv.livematchcam.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import coil.Coil
import coil.request.ImageRequest
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import com.pedro.encoder.input.sources.audio.MicrophoneSource
import com.pedro.encoder.input.sources.video.Camera1Source
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.library.generic.GenericStream
import com.pedro.library.util.BitrateAdapter
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.views.SwipeSurfaceView
import it.lmqv.livematchcam.databinding.FragmentCameraBinding
import it.lmqv.livematchcam.dialogs.StartStreamingDialog
import it.lmqv.livematchcam.dialogs.StopStreamingDialog
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.extensions.animateAlpha
import it.lmqv.livematchcam.extensions.formatHourTime
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.extensions.launchOnResumed
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.viewmodels.StatusViewModel
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.factories.SportsFactory
import it.lmqv.livematchcam.handlers.offset.IOffsetDegreeHandler
import it.lmqv.livematchcam.handlers.offset.ManualZoomLevelHandler
import it.lmqv.livematchcam.handlers.zoom.IZoomLevelHandler
import it.lmqv.livematchcam.handlers.zoom.NoDebounceExtraSmoothZoomLevelHandler
import it.lmqv.livematchcam.utils.KeyDescription
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.services.youtube.YouTubeClientProvider
import it.lmqv.livematchcam.utils.BannerBitmapRotator
import it.lmqv.livematchcam.viewmodels.StreamConfigurationViewModel
import it.lmqv.livematchcam.viewmodels.YoutubeViewModel
import it.lmqv.livematchcam.viewmodels.YoutubeViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

open class CameraFragment: Fragment(), ConnectChecker,
    IScoreBoardFragment.OnUpdateCallback,
    StatusFragment.OnZoomButtonClickListener,
    SwipeSurfaceView.OnSwipeGesture,
    BannerBitmapRotator.BitmapRotationListener{

    companion object {
        fun getInstance(): CameraFragment = CameraFragment()
    }

    private lateinit var zoomLevelHandler: IZoomLevelHandler
    private lateinit var offsetDegreeHandler: IOffsetDegreeHandler

    private val statusFragment = StatusFragment.newInstance()
    private val streamConfigurationViewModel: StreamConfigurationViewModel by activityViewModels()
    private val statusViewModel: StatusViewModel by activityViewModels()
    private val youtubeViewModel: YoutubeViewModel by activityViewModels {
        YoutubeViewModelFactory(requireActivity().application, YouTubeClientProvider.get())
    }

    private lateinit var controlBarFragment: IControlBarFragment
    private lateinit var scoreBoardFragment: IScoreBoardFragment
    private var scoreBoardFilter: ImageObjectFilterRender = ImageObjectFilterRender()
    private var spotBannerFilter: ImageObjectFilterRender = ImageObjectFilterRender()
    private var mainBannerFilter: ImageObjectFilterRender = ImageObjectFilterRender()
    private lateinit var sportCollectJob : Job
    private lateinit var bannerBitmapRotator: BannerBitmapRotator

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    val genericStream: GenericStream by lazy {
        GenericStream(requireContext(), this).apply {
            getGlInterface().autoHandleOrientation = true
            getStreamClient().setBitrateExponentialFactor(0.5f)
        }
    }
    private lateinit var callback: OnBackPressedCallback

    private var isMute : Boolean = false

    private var width = 1920
    private var height = 1080
    private val vBitrate = 6000 * 1000
    private var fps = 30

    private var rotation = 0
    private val sampleRate = 32000
    private val isStereo = true
    private val aBitrate = 128 * 1000
    //private var recordPath = ""

    private var timeElapsedInSeconds = 0
    private var job: Job? = null

    private var serverURI: String? = null

    //Bitrate adapter used to change the bitrate on fly depend of the bandwidth.
    private val bitrateAdapter = BitrateAdapter {
        genericStream.setVideoBitrateOnFly(it)
    }.apply {
        setMaxBitrate(vBitrate + aBitrate)
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)

        childFragmentManager.beginTransaction()
            .add(R.id.status_container, statusFragment).commit()

        this.sportCollectJob = viewLifecycleOwner.lifecycleScope.launch {
            MatchRepository.sport.collectLatest { sport ->
                var sportFragmentFactory = SportsFactory.get(sport)
                controlBarFragment = sportFragmentFactory.getControlBar()
                scoreBoardFragment = sportFragmentFactory.getScoreBoard()

                childFragmentManager.beginTransaction()
                    .replace(
                        R.id.control_bar_container,
                        controlBarFragment as Fragment,
                        "ControlBarFragmentTag"
                    )
                    .commit()

                childFragmentManager.beginTransaction()
                    .replace(
                        R.id.score_board_placeholder,
                        scoreBoardFragment as Fragment,
                        "ScoreBoardFragmentTag"
                    )
                    .commit()
            }
        }

        when (genericStream.videoSource) {
            is Camera1Source -> {
                (genericStream.videoSource as Camera1Source).enableVideoStabilization()
            }
            is Camera2Source -> {
                with (genericStream.videoSource as Camera2Source) {
                    enableVideoStabilization()
                    enableOpticalVideoStabilization()
                    enableAutoFocus()
                    enableAutoExposure()
                    disableFaceDetection()
                }
            }
        }

        this.zoomLevelHandler = NoDebounceExtraSmoothZoomLevelHandler(requireContext(), genericStream.videoSource)
        this.offsetDegreeHandler = ManualZoomLevelHandler(requireContext())
        this.bannerBitmapRotator = BannerBitmapRotator(requireContext(), lifecycleScope)
        this.bannerBitmapRotator.setBitmapRotationListener(this@CameraFragment)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launchOnStarted {
            MatchRepository.RTMPServerURI.collect { configuredServerURI ->
                configuredServerURI?.let {
                    binding.bStartStop.isClickable = true
                }
                this.serverURI = configuredServerURI
            }
        }

//        launchOnStarted {
//            MatchRepository.isRealtimeDatabaseAvailable.collect { isAvailable ->
//                /*if (isAvailable) {
//                    binding.mainBannerContainer.visibility = View.VISIBLE
//                } else {
//                    binding.mainBannerContainer.visibility = View.GONE
//                }*/
//                binding.mainBannerContainer.visibility = View.GONE
//            }
//        }

        statusViewModel.angleDegrees.observe(viewLifecycleOwner) { degrees ->
            val offset = this.offsetDegreeHandler.getOffsetByDegrees(degrees)
            onChangeZoom(offset)
        }

        val audioManager = requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        this.isMute = audioManager.isMicrophoneMute

        if (this.isMute) {
            binding.microphone.setImageResource(R.drawable.microphone_off)
        } else {
            binding.microphone.setImageResource(R.drawable.microphone_on)
        }
        //this.genericStream.getStreamClient().setOnlyVideo(true)

        this.genericStream.setFpsListener { fps ->
            statusViewModel.setFPS(fps)
        }

        binding.surfaceView.setCallbackListener(this)
        binding.surfaceView.holder.addCallback(object: SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                if (!genericStream.isOnPreview) genericStream.startPreview(binding.surfaceView)
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                genericStream.getGlInterface().setPreviewResolution(width, height)
            }
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                if (genericStream.isOnPreview) genericStream.stopPreview()
            }
        })

        binding.bStartStop.setOnClickListener {
            if (this.serverURI != null) {
                if (genericStream.isStreaming) {
                    var dialog = StopStreamingDialog(requireContext(),
                        { shouldEnd ->
                            genericStream.stopStream()
                            if (shouldEnd) { youtubeViewModel.completeLive() }
                            binding.bStartStop.setImageResource(R.drawable.stream_icon)
                            requireActivity().hideSystemUI()
                        }, { requireActivity().hideSystemUI() })
                    dialog.setOnShowListener {
                        requireActivity().hideSystemUI()
                    }
                    dialog.show()
                } else {
                    toast(this.serverURI!!)
                    var dialog = StartStreamingDialog(requireContext(),
                        {
                            genericStream.startStream(this.serverURI!!)
                            binding.bStartStop.setImageResource(R.drawable.stream_stop_icon)
                            requireActivity().hideSystemUI()
                        }, { requireActivity().hideSystemUI() })
                    dialog.setOnShowListener {
                        requireActivity().hideSystemUI()
                    }
                    dialog.show()
                }
            } else {
                toast("no RTMP server configured")
            }
        }

        /*bRecord.setOnClickListener {
            if (!genericStream.isRecording) {
                val folder = PathUtils.getRecordPath()
                if (!folder.exists()) folder.mkdir()
                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                recordPath = "${folder.absolutePath}/${sdf.format(Date())}.mp4"
                genericStream.startRecord(recordPath) { status ->
                    if (status == RecordController.Status.RECORDING) {
                        bRecord.setImageResource(R.drawable.stop_icon)
                    }
                }
                bRecord.setImageResource(R.drawable.pause_icon)
            } else {
                genericStream.stopRecord()
                bRecord.setImageResource(R.drawable.record_icon)
                PathUtils.updateGallery(requireContext(), recordPath)
            }
        }*/

        binding.microphone.setOnClickListener {
            val microphoneSource = MicrophoneSource()
            this.isMute = !this.isMute
            if (this.isMute) {
                microphoneSource.mute()
                binding.microphone.setImageResource(R.drawable.microphone_off)
            } else {
                microphoneSource.unMute()
                binding.microphone.setImageResource(R.drawable.microphone_on)
            }
            genericStream.changeAudioSource(microphoneSource)
            //genericStream.getStreamClient().setOnlyVideo(true)
            //genericStream.getStreamClient().setOnlyAudio(false)
        }

        binding.changeResolutionStrategy.setOnClickListener {
            //this.changeZoomStrategyDialog()
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
                    bannerBitmapRotator.stop()
                    spotBannerFilter.setAlpha(0.0f)

                    if (visible) {
                        bannerBitmapRotator.setUrls(listOf(url))
                        bannerBitmapRotator.start()
                    }
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
                    //Logd("mainBannerURL : $mainBannerURL")
                    if (url.isNotEmpty() && visible) {
                        val bitmap = Coil.imageLoader(requireContext()).execute(
                            ImageRequest.Builder(requireContext())
                                .data(url)
                                .build()
                        ).drawable?.toBitmap()?.copy(Bitmap.Config.ARGB_8888, true)

                        bitmap.apply {
                            val maxFactor = 80f
                            val defaultScaleX = ((bitmap?.width?.times(100) ?: 0) / width).toFloat()
                            val defaultScaleY = ((bitmap?.height?.times(100) ?: 0) / height).toFloat()

                            val factorX = maxFactor / defaultScaleX
                            val scaleX = factorX * defaultScaleX
                            val scaleY = factorX * defaultScaleY

                            mainBannerFilter.animateAlpha(0.75f, 0f, 500L) {
                                mainBannerFilter.apply {
                                    setImage(bitmap)
                                    setScale(scaleX, scaleY)
                                    setPosition((100f - scaleX) / 2, (100f - scaleY) / 2)
                                    animateAlpha(0f, 0.75f, 500L)
                                }
                            }
                        }
                    } else {
                        mainBannerFilter.setAlpha(0.0f)
                    }
                }
            }
        }

        launchOnResumed {
            combine(
                streamConfigurationViewModel.fps,
                streamConfigurationViewModel.resolution
            ) { fps, resolution ->
                Pair(fps, resolution)
            }.collect { (fps, resolution) ->
                if (fps != null && resolution != null) {
                    this.fps = fps
                    this.height = resolution
                    this.width = if (resolution == 1080) { 1920 } else { 1280 }

                    if (genericStream.isOnPreview) {
                        toast("Set ${this.width}x${this.height}p@${this.fps}fps")
                        recreate()
                    }
                }
            }
        }

        /*matchViewModel.score.observe(viewLifecycleOwner) { iScore ->
            val command = iScore?.command
            if (command == Command.ZOOM_IN.toString()) {
                this.offsetDegreeHandler.manualZoomLevel(ManualZoomLevel.In)
            }
            if (command == Command.ZOOM_DEFAULT.toString()) {
                this.offsetDegreeHandler.manualZoomLevel(ManualZoomLevel.None)
            }
            if (command == Command.ZOOM_OUT.toString()) {
                this.offsetDegreeHandler.manualZoomLevel(ManualZoomLevel.Out)
            }
        }*/
    }

    override fun onBitmapAvailable(bitmap: Bitmap) {
        val maxFactor = 20f
        var padding = 2f
        val defaultScaleX = (bitmap.width.times(100) / width).toFloat()
        val defaultScaleY = (bitmap.height.times(100) / height).toFloat()

        val factorX = maxFactor / defaultScaleX

        val scaleX = factorX * defaultScaleX
        val scaleY = factorX * defaultScaleY

        val targetAlpha = 0.75f
        val duration = 250L
        spotBannerFilter.animateAlpha(targetAlpha, 0f, duration) {
            spotBannerFilter.apply {
                setScale(scaleX, scaleY)
                setImage(bitmap)
                setPosition(100f - padding - scaleX, 0f + padding)
                animateAlpha(0f, targetAlpha, duration)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //toast("CameraFragment::onCreate")

        genericStream.getStreamClient().setReTries(10)

        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (genericStream.isStreaming) {
                    var dialog = StopStreamingDialog(requireContext(),
                        { shouldEnd ->
                            genericStream.stopStream()
                            if (shouldEnd) { youtubeViewModel.completeLive() }

                            isEnabled = false
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }, { })
                    dialog.setOnShowListener {
                        requireActivity().hideSystemUI()
                    }
                    dialog.show()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }

        // Add callback to dispatcher
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onStart() {
        super.onStart()

        prepare()
//
        this.scoreBoardFragment.setOnUpdate(this)

//        genericStream.getGlInterface().clearFilters()
//        genericStream.getGlInterface().addFilter(0, this.scoreBoardFilter)
//        genericStream.getGlInterface().addFilter(1, this.spotBannerFilter)
//        genericStream.getGlInterface().addFilter(2, this.mainBannerFilter)

        refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        //toast("CameraFragment::OnPause")
    }

    override fun onResume() {
        super.onResume()
        //toast("CameraFragment::onResume")
    }

    override fun onDestroy() {
        super.onDestroy()
        //toast("CameraFragment::onDestroy")
        this.sportCollectJob.cancel()
        genericStream.release()
        callback.remove()
        bannerBitmapRotator.stop()
    }

//    fun setRotation(updatedRotation: Int) {
//        val rotate = when (updatedRotation) {
//            Surface.ROTATION_90 -> { rotation = 0; true }
//            Surface.ROTATION_270 -> { rotation = 180; true }
//            else -> { false }
//        }
//
//        if (rotate) {
//            recreate()
//        }
//    }

    private fun prepare() {
        val prepared = try {
            val screenWidth = width
            val screenHeight = height

            statusViewModel.setSourceResolution(this.height)
            statusViewModel.setSourceFps(this.fps)

            genericStream.prepareVideo(screenWidth, screenHeight, vBitrate, rotation = rotation, fps = this.fps) &&
                    genericStream.prepareAudio(sampleRate, isStereo, aBitrate)
            true
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            false
        }

        if (!prepared) {
            toast("Audio or Video configuration failed")
            activity?.finish()
        }
    }

    private fun recreate() {
        if (genericStream.isOnPreview) {
            //Logd("stopPreview")
            genericStream.stopPreview()
            //uvcCameraSource.updatePreviewSize(width, height, fps)
            prepare()
            //Logd("setPreviewResolution")
            genericStream.getGlInterface().clearFilters()
            genericStream.getGlInterface().addFilter(0, this.scoreBoardFilter)
            genericStream.getGlInterface().addFilter(1, this.spotBannerFilter)
            genericStream.getGlInterface().addFilter(2, this.mainBannerFilter)
            genericStream.getGlInterface().setPreviewResolution(width, height)
            //Logd("startPreview")
            genericStream.startPreview(binding.surfaceView)
            //Logd("refresh")
            refresh()
        }
    }

    override fun onConnectionStarted(url: String) {
        toast("Streaming Started on $url")
        this.startStreamingTimer()
    }

    override fun onConnectionSuccess() {
        toast("Connected on ${serverURI}")
    }

    override fun onConnectionFailed(reason: String) {
        if (genericStream.getStreamClient().reTry(5000, reason, null)) {
            toast("Retry: $reason")
        } else {
            genericStream.stopStream()
            binding.bStartStop.setImageResource(R.drawable.stream_icon)
            toast("Failed: $reason")
        }
    }

    override fun onNewBitrate(bitrate: Long) {
        bitrateAdapter.adaptBitrate(bitrate, genericStream.getStreamClient().hasCongestion())
        statusViewModel.setBitrate(bitrate / 1000_000f)
    }

    override fun onDisconnect() {
        statusViewModel.setBitrate(0f)
        this.stopStreamingTimer()
        toast("Disconnected")
    }

    override fun onAuthError() {
        genericStream.stopStream()
        binding.bStartStop.setImageResource(R.drawable.stream_icon)
        toast("Auth error")
    }

    override fun onAuthSuccess() {
        toast("Auth success")
    }

    override fun refresh() {
        try {
            this.scoreBoardFragment.getBitmapView { scoreBoardBitmap ->
                val maxFactor = 25f
                val defaultScaleX = (scoreBoardBitmap.width * 100 / width).toFloat()
                val defaultScaleY = (scoreBoardBitmap.height * 100 / height).toFloat()

                val factorX = maxFactor / defaultScaleX
                val scaleX = factorX * defaultScaleX
                val scaleY = factorX * defaultScaleY

                this.scoreBoardFilter.apply {
                    setImage(scoreBoardBitmap)
                    setScale(scaleX, scaleY)
                    setPosition(0.15f, 0.15f)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Loge("CameraFragment::Exception:: ${e.message.toString()}")
        }
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

    /*fun updateZoom(zoomLevel: ManualZoomLevel) {
        this.offsetDegreeHandler.manualZoomLevel(zoomLevel)
        statusViewModel.angleDegrees.value?.let {
            val offset = this.offsetDegreeHandler.getOffsetByDegrees(it)
            onChangeZoom(offset)
        }
    }*/

    private fun onChangeZoom(offset: Float) {
        zoomLevelHandler.withOffset(offset) { zoomLevel ->
            statusViewModel.setZoomLevel(zoomLevel)
        }
    }

    private fun startStreamingTimer() {
        if (job == null || job?.isActive == false) {
            job = CoroutineScope(Dispatchers.Main).launch {
                while (isActive) {
                    binding.streamingTime.text = formatHourTime(timeElapsedInSeconds)
                    delay(1000)
                    timeElapsedInSeconds++
                }
            }
        }
    }

    private fun stopStreamingTimer() {
        job?.cancel()
        job = null
        timeElapsedInSeconds = 0
    }

    /*
    private fun changeZoomStrategyDialog() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_change_settings, null)

        val spinnerZoomStrategies = dialogView.findViewById<Spinner>(R.id.zoom_strategies)
        val optionsZoomStrategies = listOf(
            KeyDescription<KClass<*>>(NoDebounceExtraSmoothZoomLevelHandler::class, "1. smooth progressive zoom No Debounce"),
            KeyDescription<KClass<*>>(NoDebounceSmoothZoomLevelHandler::class, "2. progressive zoom No Debounce"),
            KeyDescription<KClass<*>>(SmoothZoomLevelHandler::class, "3. progressive zoom with Debounce"),
            KeyDescription<KClass<*>>(NoDebounceZoomLevelHandler::class, "4. No Debounce"),
            KeyDescription<KClass<*>>(SingleZoomLevelHandler::class, "5. Debounce"),
        )

        val adapterZoomServer = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, optionsZoomStrategies)
        adapterZoomServer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerZoomStrategies.adapter = adapterZoomServer
        spinnerZoomStrategies.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position) as KeyDescription<KClass<*>>
                val selectedZoomHandler = selectedItem.key.constructors.first().call(requireContext(), genericStream.videoSource)
                this@CameraFragment.zoomLevelHandler = selectedZoomHandler as IZoomLevelHandler
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }
        val defaultPositionStrategy = optionsZoomStrategies.indexOfFirst { it.key == zoomLevelHandler::class }
        spinnerZoomStrategies.setSelection(defaultPositionStrategy)

        val spinnerOffsetStrategies = dialogView.findViewById<Spinner>(R.id.offset_strategies)
        val optionsOffsetStrategies = listOf(
            KeyDescription<KClass<*>>(LeftRightOffsetGapDegreeHandler::class, "1. Fixed Left/Right with Gap Degree"),
            KeyDescription<KClass<*>>(ManualZoomLevelHandler::class, "2. Manual Zoom In/Out (vol +/-)"),
            KeyDescription<KClass<*>>(LeftRightWithManualZoomLevelHandler::class, "3. Field Zone Left/Right Manual Depth In/Out (vol +/-)"),
            KeyDescription<KClass<*>>(LeftRightWithAutoDepthZoomLevelHandler::class, "4. Field Zone Left/Right Auto Up Down"),
            KeyDescription<KClass<*>>(LeftRightOffsetGapNoCornerHandler::class, "5. Fixed Left/Right with Gap Degree No Corner (2x angle)"),
            KeyDescription<KClass<*>>(ProgressiveOffsetDegreeHandler::class, "6. Progressive Left/Right Degree multiplier"),
            KeyDescription<KClass<*>>(ProgressiveOffsetDegreeWithCapHandler::class, "7. Progressive Left/Right Degree multiplier with cap (3x)"),
            KeyDescription<KClass<*>>(LeftRightOffsetDegreeHandler::class, "8. Fixed Left/Right Degree")
        )

        val adapterOffsetServer = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, optionsOffsetStrategies)
        adapterOffsetServer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerOffsetStrategies.adapter = adapterOffsetServer
        spinnerOffsetStrategies.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position) as KeyDescription<KClass<*>>
                this@CameraFragment.offsetDegreeHandler.destroy()
                val selectedOffsetHandler = selectedItem.key.constructors.first().call(requireContext())
                this@CameraFragment.offsetDegreeHandler = selectedOffsetHandler as IOffsetDegreeHandler
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }
        val defaultPositionOffset = optionsOffsetStrategies.indexOfFirst { it.key == offsetDegreeHandler::class }
        spinnerOffsetStrategies.setSelection(defaultPositionOffset)


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
    }*/

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
        spinnerVideoResolutions.isEnabled = !genericStream.isStreaming
        spinnerVideoResolutions.adapter = adapterVideoResolutions
        @Suppress("UNCHECKED_CAST")
        spinnerVideoResolutions.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position) as KeyDescription<Int>
                var selectedItemValue = selectedItem.key
                if (this@CameraFragment.height != selectedItemValue) {
                    //height = selectedItemValue
                    //width = if (height == 1080) { 1920 } else { 1280 }

                    streamConfigurationViewModel.setResolution(selectedItemValue)
                    //Logd("change Resolutions:${width}x${height}")
                    //recreate()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }
        val defaultResolution = optionsVideoResolutions.indexOfFirst { it.key == this.height }
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
        spinnerVideoFps.isEnabled = !genericStream.isStreaming
        spinnerVideoFps.adapter = adapterVideoFps
        @Suppress("UNCHECKED_CAST")
        spinnerVideoFps.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position) as KeyDescription<Int>
                var selectedItemValue = selectedItem.key
                if (this@CameraFragment.fps != selectedItemValue) {
                    //this.fps = selectedItemValue
                    //Logd("change Fps:$fps")
                    streamConfigurationViewModel.setFps(selectedItemValue)
                    //recreate()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }
        val defaultVideoFps = optionsVideoFps.indexOfFirst { it.key == this.fps }
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