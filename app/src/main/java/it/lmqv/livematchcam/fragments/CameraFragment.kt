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
import android.widget.TextView
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
import com.pedro.encoder.input.sources.video.VideoFileSource
import com.pedro.encoder.input.sources.video.VideoSource
import com.pedro.extrasources.CameraUvcSource
import com.pedro.library.generic.GenericStream
import com.pedro.library.rtmp.RtmpCamera1
import com.pedro.library.util.BitrateAdapter
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.views.SwipeSurfaceView
import it.lmqv.livematchcam.databinding.FragmentCameraBinding
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.extensions.formatHourTime
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.viewmodels.StatusViewModel
import it.lmqv.livematchcam.repositories.SettingsRepository
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.factories.SportsFactory
import it.lmqv.livematchcam.handlers.offset.LeftRightWithManualZoomLevelHandler
import it.lmqv.livematchcam.handlers.offset.IOffsetDegreeHandler
import it.lmqv.livematchcam.handlers.offset.LeftRightOffsetDegreeHandler
import it.lmqv.livematchcam.handlers.offset.LeftRightOffsetGapDegreeHandler
import it.lmqv.livematchcam.handlers.offset.LeftRightOffsetGapNoCornerHandler
import it.lmqv.livematchcam.handlers.offset.LeftRightWithAutoDepthZoomLevelHandler
import it.lmqv.livematchcam.handlers.offset.ManualZoomLevel
import it.lmqv.livematchcam.handlers.offset.ManualZoomLevelHandler
import it.lmqv.livematchcam.handlers.offset.ProgressiveOffsetDegreeHandler
import it.lmqv.livematchcam.handlers.offset.ProgressiveOffsetDegreeWithCapHandler
import it.lmqv.livematchcam.handlers.zoom.IZoomLevelHandler
import it.lmqv.livematchcam.handlers.zoom.NoDebounceExtraSmoothZoomLevelHandler
import it.lmqv.livematchcam.handlers.zoom.NoDebounceSmoothZoomLevelHandler
import it.lmqv.livematchcam.handlers.zoom.NoDebounceZoomLevelHandler
import it.lmqv.livematchcam.handlers.zoom.SingleZoomLevelHandler
import it.lmqv.livematchcam.handlers.zoom.SmoothZoomLevelHandler
import it.lmqv.livematchcam.utils.KeyValue
import it.lmqv.livematchcam.viewmodels.Command
import it.lmqv.livematchcam.viewmodels.MatchViewModel
import it.lmqv.livematchcam.viewmodels.StreamersViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

open class CameraFragment: Fragment(), ConnectChecker,
    IScoreBoardFragment.OnUpdateCallback,
    SwipeSurfaceView.OnSwipeGesture {

    companion object {
        fun getInstance(): CameraFragment = CameraFragment()
    }

    private val streamersViewModel: StreamersViewModel by activityViewModels()
    protected val matchViewModel: MatchViewModel by activityViewModels()

    private lateinit var settingsRepository: SettingsRepository

    private lateinit var zoomLevelHandler: IZoomLevelHandler
    private lateinit var offsetDegreeHandler: IOffsetDegreeHandler

    private val statusFragment = StatusFragment.newInstance()
    private val statusViewModel: StatusViewModel by activityViewModels()

    private lateinit var controlBarFragment: IControlBarFragment
    private lateinit var scoreBoardFragment: IScoreBoardFragment
    private var scoreBoardFilter: ImageObjectFilterRender = ImageObjectFilterRender()
    private var spotBannerFilter: ImageObjectFilterRender = ImageObjectFilterRender()
    private var mainBannerFilter: ImageObjectFilterRender = ImageObjectFilterRender()
    private var sportsFactory = SportsFactory

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    val genericStream: GenericStream by lazy {
        GenericStream(requireContext(), this).apply {
            //getGlInterface().autoHandleOrientation = true
            getStreamClient().setBitrateExponentialFactor(0.5f)
        }
    }
    private lateinit var callback: OnBackPressedCallback

    private var isMute : Boolean = false

    private val width = 1920
    private val height = 1080
    private val vBitrate = 7500 * 1000
    private var fps = 25

    private var rotation = 0
    private val sampleRate = 32000
    private val isStereo = true
    private val aBitrate = 128 * 1000
    //private var recordPath = ""

    private var timeElapsedInSeconds = 0
    private var job: Job? = null

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

        settingsRepository = SettingsRepository(requireContext())

        childFragmentManager.beginTransaction()
            .add(R.id.status_container, statusFragment).commit()

        var sportFragmentFactory = sportsFactory.get()
        this.controlBarFragment = sportFragmentFactory.getControlBar()
        this.scoreBoardFragment = sportFragmentFactory.getScoreBoard()

        childFragmentManager.beginTransaction()
            .replace(R.id.control_bar_container, this.controlBarFragment as Fragment, "ControlBarFragmentTag")
            .commit()

        childFragmentManager.beginTransaction()
            .replace(R.id.score_board_placeholder, scoreBoardFragment as Fragment, "ScoreBoardFragmentTag")
            .commit()

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
                }
            }
        }

        this.zoomLevelHandler = NoDebounceExtraSmoothZoomLevelHandler(requireContext(), genericStream.videoSource)
        this.offsetDegreeHandler = ManualZoomLevelHandler(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            toast(streamersViewModel.getServerURI())
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_start_stop_stream, null)
            val title = dialogView.findViewById<TextView>(R.id.dialog_message)
            if (genericStream.isStreaming) {
                title.text = getString(R.string.confirm_stop_message)
            } else {
                title.text = getString(R.string.confirm_start_message)
            }

            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("OK") { dialog, _ ->
                    val serverUri = streamersViewModel.getServerURI()
                    if (!genericStream.isStreaming) {
                        genericStream.startStream(serverUri)
                        binding.bStartStop.setImageResource(R.drawable.stream_stop_icon)
                    } else {
                        genericStream.stopStream()
                        binding.bStartStop.setImageResource(R.drawable.stream_icon)
                    }
                    dialog.dismiss()
                    requireActivity().hideSystemUI()
                    toast(serverUri)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    requireActivity().hideSystemUI()
                }
                .create()
            dialog.setOnShowListener {
                requireActivity().hideSystemUI()
            }
            dialog.show()
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

        binding.changeRotationStrategy.setOnClickListener {
            this.changeZoomStrategyDialog()
        }

        lifecycleScope.launch {
            streamersViewModel.currentKey.collect { _ ->
                binding.bStartStop.isClickable = true
            }
        }

        lifecycleScope.launch {
            combine(
                matchViewModel.spotBannerURL,
                matchViewModel.spotBannerVisible
            ) { url, visible -> Pair(url, visible)
            }.collect { (url, visible) ->
                launchOnStarted {
                    //Logd("spotBannerURL : $spotBannerURL")
                    if (url.isNotEmpty() && visible)
                    {
                        val bitmap = Coil.imageLoader(requireContext()).execute(
                            ImageRequest.Builder(requireContext())
                                .data(url)
                                .build()
                        ).drawable?.toBitmap()?.copy(Bitmap.Config.ARGB_8888, true)

                        bitmap.apply {
                            val maxFactor = 20f
                            val defaultScaleX = ((bitmap?.width?.times(100) ?: 0) / width).toFloat()
                            val defaultScaleY = ((bitmap?.height?.times(100) ?: 0) / height).toFloat()

                            val factorX = maxFactor / defaultScaleX
                            val scaleX = factorX * defaultScaleX
                            val scaleY = factorX * defaultScaleY

                            spotBannerFilter.apply {
                                setImage(bitmap)
                                setScale(scaleX, scaleY)
                                setAlpha(0.75f)
                                setPosition(100f - scaleX, 0f)
                            }
                        }
                    } else {
                        spotBannerFilter.setAlpha(0.0f)
                    }
                }
            }
        }

        lifecycleScope.launch {
            combine(
                matchViewModel.mainBannerURL,
                matchViewModel.mainBannerVisible
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

                            mainBannerFilter.apply {
                                setImage(bitmap)
                                setScale(scaleX, scaleY)
                                setAlpha(0.75f)
                                setPosition((100f - scaleX) / 2, (100f - scaleY) / 2)
                            }
                        }
                    } else {
                        mainBannerFilter.setAlpha(0.0f)
                    }
                }
            }
        }

        matchViewModel.score.observe(viewLifecycleOwner) { iScore ->
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
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepare()
        genericStream.getStreamClient().setReTries(10)

        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (genericStream.isStreaming) {
                    val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_start_stop_stream, null)
                    val title = dialogView.findViewById<TextView>(R.id.dialog_message)
                    title.text = getString(R.string.confirm_stop_message)

                    val dialog = AlertDialog.Builder(requireContext())
                        .setView(dialogView)
                        .setPositiveButton("OK") { dialog, _ ->
                            genericStream.stopStream()
                            binding.bStartStop.setImageResource(R.drawable.stream_icon)
                            isEnabled = false
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                        .setNegativeButton("Cancel") { _, _ -> }
                        .create()
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

        this.scoreBoardFragment.setOnUpdate(this)

        genericStream.getGlInterface().clearFilters()
        genericStream.getGlInterface().addFilter(0, this.scoreBoardFilter)
        genericStream.getGlInterface().addFilter(1, this.spotBannerFilter)
        genericStream.getGlInterface().addFilter(2, this.mainBannerFilter)

        refresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        genericStream.release()
        callback.remove()
    }

    private fun prepare() {
        val prepared = try {
            val screenWidth = width
            val screenHeight = height

            genericStream.prepareVideo(screenWidth, screenHeight, vBitrate, rotation = rotation, fps = fps) &&
                    genericStream.prepareAudio(sampleRate, isStereo, aBitrate)
            true
        } catch (e: IllegalArgumentException) {
            false
        }

        if (!prepared) {
            toast("Audio or Video configuration failed")
            activity?.finish()
        }
    }

    override fun onConnectionStarted(url: String) {
        toast("Streaming Started")
        this.startStreamingTimer()
    }

    override fun onConnectionSuccess() {
        toast("Connected on ${streamersViewModel.getServerURI()}")
    }

    override fun onConnectionFailed(reason: String) {
        if (genericStream.getStreamClient().reTry(5000, reason, null)) {
            toast("Retry")
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
    }

    override fun swipeUp() {
        zoomLevelHandler.increase()
    }

    override fun swipeDown() {
        zoomLevelHandler.decrease()
    }

    override fun swipeLeft() {
        zoomLevelHandler.lower()
    }

    override fun swipeRight() {
        zoomLevelHandler.upper()
    }

    fun updateZoom(zoomLevel: ManualZoomLevel) {
        this.offsetDegreeHandler.manualZoomLevel(zoomLevel)
        statusViewModel.angleDegrees.value?.let {
            val offset = this.offsetDegreeHandler.getOffsetByDegrees(it)
            onChangeZoom(offset)
        }
    }

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

    private fun changeZoomStrategyDialog() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_change_settings, null)

        val spinnerZoomStrategies = dialogView.findViewById<Spinner>(R.id.zoom_strategies)
        val optionsZoomStrategies = listOf(
            KeyValue<KClass<*>>(NoDebounceExtraSmoothZoomLevelHandler::class, "1. smooth progressive zoom No Debounce"),
            KeyValue<KClass<*>>(NoDebounceSmoothZoomLevelHandler::class, "2. progressive zoom No Debounce"),
            KeyValue<KClass<*>>(SmoothZoomLevelHandler::class, "3. progressive zoom with Debounce"),
            KeyValue<KClass<*>>(NoDebounceZoomLevelHandler::class, "4. No Debounce"),
            KeyValue<KClass<*>>(SingleZoomLevelHandler::class, "5. Debounce"),
        )

        val adapterZoomServer = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, optionsZoomStrategies)
        adapterZoomServer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerZoomStrategies.adapter = adapterZoomServer
        spinnerZoomStrategies.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position) as KeyValue<KClass<*>>
                val selectedZoomHandler = selectedItem.key.constructors.first().call(requireContext(), genericStream.videoSource)
                this@CameraFragment.zoomLevelHandler = selectedZoomHandler as IZoomLevelHandler
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }
        val defaultPositionStrategy = optionsZoomStrategies.indexOfFirst { it.key == zoomLevelHandler::class }
        spinnerZoomStrategies.setSelection(defaultPositionStrategy)

        val spinnerOffsetStrategies = dialogView.findViewById<Spinner>(R.id.offset_strategies)
        val optionsOffsetStrategies = listOf(
            KeyValue<KClass<*>>(LeftRightOffsetGapDegreeHandler::class, "1. Fixed Left/Right with Gap Degree"),
            KeyValue<KClass<*>>(ManualZoomLevelHandler::class, "2. Manual Zoom In/Out (vol +/-)"),
            KeyValue<KClass<*>>(LeftRightWithManualZoomLevelHandler::class, "3. Field Zone Left/Right Manual Depth In/Out (vol +/-)"),
            KeyValue<KClass<*>>(LeftRightWithAutoDepthZoomLevelHandler::class, "4. Field Zone Left/Right Auto Up Down"),
            KeyValue<KClass<*>>(LeftRightOffsetGapNoCornerHandler::class, "5. Fixed Left/Right with Gap Degree No Corner (2x angle)"),
            KeyValue<KClass<*>>(ProgressiveOffsetDegreeHandler::class, "6. Progressive Left/Right Degree multiplier"),
            KeyValue<KClass<*>>(ProgressiveOffsetDegreeWithCapHandler::class, "7. Progressive Left/Right Degree multiplier with cap (3x)"),
            KeyValue<KClass<*>>(LeftRightOffsetDegreeHandler::class, "8. Fixed Left/Right Degree")
        )

        val adapterOffsetServer = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, optionsOffsetStrategies)
        adapterOffsetServer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerOffsetStrategies.adapter = adapterOffsetServer
        spinnerOffsetStrategies.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position) as KeyValue<KClass<*>>
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
    }

}