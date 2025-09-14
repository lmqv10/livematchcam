package it.lmqv.livematchcam.fragments

import android.annotation.SuppressLint
import android.content.Context
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import com.pedro.encoder.input.sources.audio.MicrophoneSource
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.library.generic.GenericStream
import com.pedro.library.util.BitrateAdapter
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentUvcCameraBinding
import it.lmqv.livematchcam.dialogs.StartStreamingDialog
import it.lmqv.livematchcam.dialogs.StopStreamingDialog
import it.lmqv.livematchcam.extensions.Loge
import it.lmqv.livematchcam.extensions.formatHourTime
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.factories.SportsFactory
import it.lmqv.livematchcam.repositories.MatchRepository
import it.lmqv.livematchcam.services.youtube.YouTubeClientProvider
import it.lmqv.livematchcam.sources.UvcSonyCameraSource
import it.lmqv.livematchcam.utils.KeyDescription
import it.lmqv.livematchcam.viewmodels.UVCStatusViewModel
import it.lmqv.livematchcam.viewmodels.YoutubeViewModel
import it.lmqv.livematchcam.viewmodels.YoutubeViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class UVCCameraFragment: Fragment(), ConnectChecker,
    IScoreBoardFragment.OnUpdateCallback {

    companion object {
        fun getInstance(): UVCCameraFragment = UVCCameraFragment()
    }

    private val statusFragment = UVCStatusFragment.newInstance()
    private val statusViewModel: UVCStatusViewModel by activityViewModels()
    private val youtubeViewModel: YoutubeViewModel by activityViewModels {
        YoutubeViewModelFactory(requireActivity().application, YouTubeClientProvider.get())
    }

    private lateinit var controlBarFragment: IControlBarFragment
    private lateinit var scoreBoardFragment: IScoreBoardFragment
    private var uvcSonyCameraSource: UvcSonyCameraSource = UvcSonyCameraSource()
    private var scoreBoardFilter: ImageObjectFilterRender = ImageObjectFilterRender()
    private lateinit var sportCollectJob : Job

    private var _binding: FragmentUvcCameraBinding? = null
    private val binding get() = _binding!!

    val genericStream: GenericStream by lazy {
        GenericStream(requireContext(), this,
            this.uvcSonyCameraSource,
            MicrophoneSource()).apply {
            //getGlInterface().autoHandleOrientation = true
            getStreamClient().setBitrateExponentialFactor(0.5f)
        }
    }
    private lateinit var callback: OnBackPressedCallback

    private var isMute : Boolean = false

    private var width = 1280
    private var height = 720
    private val vBitrate = 6000 * 1000
    private var fps = 30

    private var rotation = 0
    private val sampleRate = 32000
    private val isStereo = true
    private val aBitrate = 128 * 1000

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
        _binding = FragmentUvcCameraBinding.inflate(inflater, container, false)

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

        binding.surfaceView.holder.addCallback(object: SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                //Logd("surfaceCreated::startPreview")
                if (!genericStream.isOnPreview) genericStream.startPreview(binding.surfaceView)
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                //Logd("surfaceChanged::setPreviewResolution")
                genericStream.getGlInterface().setPreviewResolution(width, height)
            }
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                //Logd("surfaceDestroyed::stopPreview")
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

        binding.changeVideoSettings.setOnClickListener {
            this.changeVideoSettingsDialog()
        }

        binding.changeVideoSource.setOnClickListener {
            when (genericStream.videoSource)
            {
                is UvcSonyCameraSource -> {
                    genericStream.changeVideoSource(Camera2Source(requireContext()))
                }
                is Camera2Source -> {
                    genericStream.changeVideoSource(this.uvcSonyCameraSource)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        genericStream.getStreamClient().setReTries(10)

        // Create the callback
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

        this.scoreBoardFragment.setOnUpdate(this)

        genericStream.getGlInterface().clearFilters()
        genericStream.getGlInterface().addFilter(0, this.scoreBoardFilter)

        refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        this.sportCollectJob.cancel()
        genericStream.release()
        callback.remove()
    }

    private fun prepare() {
        val prepared = try {
            val screenWidth = width
            val screenHeight = height

            //Logd("prepareVideo && prepareAudio")

            statusViewModel.setSourceResolution(height)
            statusViewModel.setSourceFps(fps)

            genericStream.prepareVideo(screenWidth, screenHeight, vBitrate, rotation = rotation, fps = fps) &&
                    genericStream.prepareAudio(sampleRate, isStereo, aBitrate)
            true
        } catch (e: IllegalArgumentException) {
            Loge("IllegalArgumentException:: ${e.message.toString()}")
            false
        } catch (e: Exception) {
            Loge("Exception:: ${e.message.toString()}")
            false
        }

        if (!prepared) {
            toast("Audio or Video configuration failed")
            //activity?.finish()
        }
    }

    private fun recreate()
    {
        if (genericStream.isOnPreview) {
            //Logd("stopPreview")
            genericStream.stopPreview()
            //uvcCameraSource.updatePreviewSize(width, height, fps)
            prepare()
            //Logd("setPreviewResolution")
            genericStream.getGlInterface().clearFilters()
            genericStream.getGlInterface().addFilter(0, this.scoreBoardFilter)
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
            toast("Retry $reason")
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

    private fun changeVideoSettingsDialog() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_change_video_settings, null)

        val spinnerVideoResolutions = dialogView.findViewById<Spinner>(R.id.video_resolutions)
        val optionsVideoResolutions = listOf(
            KeyDescription(1080, "1080p"),
            KeyDescription(720, "720p")
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
                if (height != selectedItemValue) {
                    height = selectedItemValue
                    width = if (height == 1080) { 1920 } else { 1280 }
                    //Logd("change Resolutions:${width}x${height}")
                    recreate()
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
            KeyDescription(60, "60fps")
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
                if (fps != selectedItemValue) {
                    fps = selectedItemValue
                    //Logd("change Fps:$fps")
                    recreate()
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