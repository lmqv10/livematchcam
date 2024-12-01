package it.lmqv.livematchcam

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
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import com.pedro.encoder.input.sources.audio.MicrophoneSource
import com.pedro.encoder.input.sources.video.VideoSource
import com.pedro.library.generic.GenericStream
import com.pedro.library.util.BitrateAdapter
import it.lmqv.livematchcam.extensions.formatHourTime
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.fragments.StatusFragment
import it.lmqv.livematchcam.viewmodels.StatusViewModel
import it.lmqv.livematchcam.settings.SettingsRepository
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.fragments.IScoreBoardFragment
import it.lmqv.livematchcam.fragments.SoccerScoreBoardFragment
import it.lmqv.livematchcam.handlers.offset.IOffsetDegreeHandler
import it.lmqv.livematchcam.handlers.offset.LeftRightOffsetDegreeHandler
import it.lmqv.livematchcam.handlers.offset.ProgressiveOffsetDegreeHandler
import it.lmqv.livematchcam.handlers.zoom.IZoomLevelHandler
import it.lmqv.livematchcam.handlers.zoom.NoDebounceSmoothZoomLevelHandler
import it.lmqv.livematchcam.handlers.zoom.NoDebounceZoomLevelHandler
import it.lmqv.livematchcam.handlers.zoom.SingleZoomLevelHandler
import it.lmqv.livematchcam.handlers.zoom.SmoothZoomLevelHandler
import it.lmqv.livematchcam.utils.KeyValue
import it.lmqv.livematchcam.viewmodels.AwayScoreBoardViewModel
import it.lmqv.livematchcam.viewmodels.HomeScoreBoardViewModel
import it.lmqv.livematchcam.viewmodels.StreamersViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class CameraFragment: Fragment(), ConnectChecker,
    IScoreBoardFragment.OnUpdateCallback,
    SwipeSurfaceView.OnSwipeGesture {

    companion object {
        fun getInstance(): CameraFragment = CameraFragment()
    }

    private val streamersViewModel: StreamersViewModel by viewModels()
    private lateinit var settingsRepository: SettingsRepository

    private lateinit var zoomLevelHandler: IZoomLevelHandler
    private lateinit var offsetDegreeHandler: IOffsetDegreeHandler

    private val statusFragment = StatusFragment.newInstance()
    private val statusViewModel: StatusViewModel by activityViewModels()

    private var scoreBoardFragment: IScoreBoardFragment = SoccerScoreBoardFragment.newInstance()
    //private var scoreBoardFragment: IScoreBoardFragment = VolleyScoreBoardFragment.newInstance()

    private val homeTeamViewModel: HomeScoreBoardViewModel by activityViewModels()
    private val awayTeamViewModel: AwayScoreBoardViewModel by activityViewModels()

    val genericStream: GenericStream by lazy {
        GenericStream(requireContext(), this).apply {
            //getGlInterface().autoHandleOrientation = true
            getStreamClient().setBitrateExponentialFactor(0.5f)
        }
    }
    private lateinit var videoSource: VideoSource

    private var isMute : Boolean = false

    private lateinit var homeTeam : TextView
    private lateinit var homeScore : TextView
    private lateinit var awayTeam : TextView
    private lateinit var awayScore : TextView
    private lateinit var txStreamingTime : TextView

    private lateinit var surfaceView: SwipeSurfaceView
    private lateinit var bStartStop: ImageView

    private val width = 1280
    private val height = 720
    private val vBitrate = 5000 * 1000
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
    ): View? {
        settingsRepository = SettingsRepository(requireContext())

        val view = inflater.inflate(R.layout.fragment_camera, container, false)

        childFragmentManager.beginTransaction()
            .add(R.id.status_container, statusFragment).commit()

        videoSource = genericStream.videoSource

        this.zoomLevelHandler = NoDebounceZoomLevelHandler(requireContext(), videoSource)
        this.offsetDegreeHandler = ProgressiveOffsetDegreeHandler(requireContext())

        statusViewModel.angleDegree.observe(viewLifecycleOwner) { degree ->
            val offset = this.offsetDegreeHandler.getOffsetByDegree(degree)
            zoomLevelHandler.withOffset(offset) { zoomLevel ->
                statusViewModel.setZoomLevel(zoomLevel)
            }
        }

        val audioManager = requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        this.isMute = audioManager.isMicrophoneMute
        val bSwitchMicrophone = view.findViewById<ImageView>(R.id.microphone)
        if (this.isMute) {
            bSwitchMicrophone.setImageResource(R.drawable.microphone_off)
        } else {
            bSwitchMicrophone.setImageResource(R.drawable.microphone_on)
        }
        this.bStartStop = view.findViewById(R.id.b_start_stop)

        this.txStreamingTime = view.findViewById(R.id.streaming_time)

        surfaceView = view.findViewById(R.id.surfaceView)
        surfaceView.setCallbackListener(this)

        (activity as? LiveStreamActivity)?.let {
            surfaceView.setOnTouchListener(it)
        }
        surfaceView.holder.addCallback(object: SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                if (!genericStream.isOnPreview) genericStream.startPreview(surfaceView)
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                genericStream.getGlInterface().setPreviewResolution(width, height)
            }
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                if (genericStream.isOnPreview) genericStream.stopPreview()
            }
        })

        bStartStop.setOnClickListener {
            val dialogView = inflater.inflate(R.layout.dialog_start_stop_stream, null)
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
                        bStartStop.setImageResource(R.drawable.stream_stop_icon)
                    } else {
                        genericStream.stopStream()
                        bStartStop.setImageResource(R.drawable.stream_icon)
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

        homeTeam = view.findViewById(R.id.home_team)
        homeTeamViewModel.name.observe(viewLifecycleOwner) { team ->
            homeTeam.text = team
        }
        homeScore = view.findViewById(R.id.home_score)
        homeTeamViewModel.score.observe(viewLifecycleOwner) { score ->
            homeScore.text = score.toString()
        }

        awayTeam = view.findViewById(R.id.away_team)
        awayTeamViewModel.name.observe(viewLifecycleOwner) { team ->
            awayTeam.text = team
        }

        awayScore = view.findViewById(R.id.away_score)
        awayTeamViewModel.score.observe(viewLifecycleOwner) { score ->
            awayScore.text = score.toString()
        }

        childFragmentManager.beginTransaction()
            .replace(R.id.score_board_placeholder, scoreBoardFragment as Fragment, "ScoreBoardFragmentTag")
            .commit()

        bSwitchMicrophone.setOnClickListener {
            val microphoneSource = MicrophoneSource()
            this.isMute = !this.isMute
            if (this.isMute) {
                microphoneSource.mute()
                bSwitchMicrophone.setImageResource(R.drawable.microphone_off)
            } else {
                microphoneSource.unMute()
                bSwitchMicrophone.setImageResource(R.drawable.microphone_on)
            }
            genericStream.changeAudioSource(microphoneSource)
            //genericStream.getStreamClient().setOnlyVideo(true)
            //genericStream.getStreamClient().setOnlyAudio(false)

            toast(streamersViewModel.getServerURI())
        }

        val bHomeScoreMinus = view.findViewById<ImageView>(R.id.home_score_minus)
        bHomeScoreMinus.setOnClickListener {
            homeTeamViewModel.incrementScore(-1)
        }

        val bHomeScoreAdd = view.findViewById<ImageView>(R.id.home_score_add)
        bHomeScoreAdd.setOnClickListener {
            homeTeamViewModel.incrementScore()
        }

        val bAwayScoreMinus = view.findViewById<ImageView>(R.id.away_score_minus)
        bAwayScoreMinus.setOnClickListener {
            awayTeamViewModel.incrementScore(-1)
        }

        val bAwayScoreAdd = view.findViewById<ImageView>(R.id.away_score_add)
        bAwayScoreAdd.setOnClickListener {
            awayTeamViewModel.incrementScore()
        }

        val bChangePeriod = view.findViewById<ImageView>(R.id.change_period)
        bChangePeriod.setOnClickListener {
            scoreBoardFragment.togglePeriod()
        }

        val bRotationStrategy = view.findViewById<ImageView>(R.id.change_rotation_strategy)
        bRotationStrategy.setOnClickListener {
            this.ChangeZoomStrategyDialog()
        }

        val bStartTime = view.findViewById<ImageView>(R.id.start_time)
        val bResetTime = view.findViewById<ImageView>(R.id.reset_time)

        bStartTime.setOnClickListener {
            if (scoreBoardFragment.isInPause()) {
                bStartTime.setImageResource(R.drawable.time_pause)
                scoreBoardFragment.startTime()
            } else {
                bStartTime.setImageResource(R.drawable.time_start)
                scoreBoardFragment.pauseTime()
            }
            bResetTime.isEnabled = scoreBoardFragment.isInPause()
            this.updateScoreBoard()
        }

        bResetTime.setOnClickListener {
            bStartTime.setImageResource(R.drawable.time_start)
            scoreBoardFragment.resetTime()
            //this.updateScoreBoard()
        }

        lifecycleScope.launch {
            streamersViewModel.currentKey.collect { _ ->
                bStartStop.isClickable = true
            }
        }
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepare()
        genericStream.getStreamClient().setReTries(10)
    }

    override fun onStart() {
        super.onStart()

        this.scoreBoardFragment.setOnUpdate(this)

        homeTeamViewModel.setLogo(GlobalDataManager.homeTeam.color)
        awayTeamViewModel.setLogo(GlobalDataManager.awayTeam.color)

        homeTeamViewModel.setName(GlobalDataManager.homeTeam.name)
        awayTeamViewModel.setName(GlobalDataManager.awayTeam.name)

        this.updateScoreBoard()
    }

    private fun prepare() {
        val prepared = try {
            //val screenWidth = getScreenWidth(requireContext())
            //val screenHeight = getScreenHeight(requireContext())

            val screenWidth = width
            val screenHeight = height

            //var factor = screenWidth / screenHeight;
            //var factorHeight = 720
            //var factorWidth =  factorHeight * factor
            //videoSource = genericStream.videoSource

            //this.zoomLevelHandler = ZoomLevelHandler(requireContext(), videoSource)
            //this.zoomDegreeHandler = ZoomDegreeHandler(zoomLevelHandler)


            /*statusViewModel.angleDegree.observe(viewLifecycleOwner, Observer { degree ->
                var offset = this.zoomDegreeHandler.getOffsetByDegree(degree)
                this.zoomLevelHandler.withOffset(offset)
            })*/


            /*var resoutions : List<Size>
            if (videoSource is Camera1Source) {
                resoutions = (videoSource as Camera1Source).getCameraResolutions(CameraHelper.Facing.BACK)
            } else if (videoSource is Camera2Source) {
                resoutions = (videoSource as Camera2Source).getCameraResolutions(CameraHelper.Facing.BACK)
            } else {
            }*/

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

    override fun onDestroy() {
        super.onDestroy()
        genericStream.release()
    }

    override fun onConnectionStarted(url: String) {
        toast("Streaming Started")
        this.startTimer()
    }

    override fun onConnectionSuccess() {
        toast("Connected")
    }

    override fun onConnectionFailed(reason: String) {
        if (genericStream.getStreamClient().reTry(5000, reason, null)) {
            toast("Retry")
        } else {
            genericStream.stopStream()
            bStartStop.setImageResource(R.drawable.stream_icon)
            toast("Failed: $reason")
        }
    }

    override fun onNewBitrate(bitrate: Long) {
        bitrateAdapter.adaptBitrate(bitrate, genericStream.getStreamClient().hasCongestion())
        statusViewModel.setBitrate(bitrate / 1000_000f)
    }

    override fun onDisconnect() {
        statusViewModel.setBitrate(0f)
        this.stopTimer()
        toast("Disconnected")
    }

    override fun onAuthError() {
        genericStream.stopStream()
        bStartStop.setImageResource(R.drawable.stream_icon)
        toast("Auth error")
    }

    override fun onAuthSuccess() {
        toast("Auth success")
    }

    private fun updateScoreBoard() {
        this.scoreBoardFragment.getBitmapView { scoreBoardBitmap ->
            val maxFactor = 25f
            val defaultScaleX = (scoreBoardBitmap.width * 100 / width).toFloat()
            val defaultScaleY = (scoreBoardBitmap.height * 100 / height).toFloat()

            val factorX = maxFactor / defaultScaleX
            val scaleX = factorX * defaultScaleX
            val scaleY = factorX * defaultScaleY

            val imageFilter = ImageObjectFilterRender()
            imageFilter.setImage(scoreBoardBitmap)
            imageFilter.setPosition(0.15f, 0.15f)
            imageFilter.setScale(scaleX, scaleY)
            genericStream.getGlInterface().setFilter(imageFilter)
        }
    }

    override fun refresh() {
        this.updateScoreBoard()
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
        lifecycleScope.launch {
            zoomLevelHandler.upper()
        }
    }

    private fun startTimer() {
        if (job == null || job?.isActive == false) {
            job = CoroutineScope(Dispatchers.Main).launch {
                while (isActive) {
                    this@CameraFragment.txStreamingTime.text = formatHourTime(timeElapsedInSeconds)
                    delay(1000)
                    timeElapsedInSeconds++
                }
            }
        }
    }

    private fun stopTimer() {
        job?.cancel()
        job = null
        timeElapsedInSeconds = 0
    }

    private fun ChangeZoomStrategyDialog() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_change_settings, null)

        val spinnerZoomStrategies = dialogView.findViewById<Spinner>(R.id.zoom_strategies)
        val optionsZoomStrategies = listOf(
            KeyValue<KClass<*>>(SingleZoomLevelHandler::class, "At Degree with Debounce"),
            KeyValue<KClass<*>>(NoDebounceZoomLevelHandler::class, "At Degree No Debounce"),
            KeyValue<KClass<*>>(SmoothZoomLevelHandler::class, "At Degree With progressive zoom with Debounce"),
            KeyValue<KClass<*>>(NoDebounceSmoothZoomLevelHandler::class, "At Degree With progressive zoom No Debounce")
        )

        val adapterZoomServer = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, optionsZoomStrategies)
        adapterZoomServer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerZoomStrategies.adapter = adapterZoomServer
        spinnerZoomStrategies.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position) as KeyValue<KClass<*>>
                val selectedZoomHandler = selectedItem.key.constructors.first()?.call(requireContext(), videoSource)
                if (selectedZoomHandler != null)
                {
                    this@CameraFragment.zoomLevelHandler = selectedZoomHandler as IZoomLevelHandler
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }
        val defaultPositionStrategy = optionsZoomStrategies.indexOfFirst { it.key == zoomLevelHandler::class }
        spinnerZoomStrategies.setSelection(defaultPositionStrategy)

        val spinnerOffsetStrategies = dialogView.findViewById<Spinner>(R.id.offset_strategies)
        val optionsOffsetStrategies = listOf(
            KeyValue<KClass<*>>(ProgressiveOffsetDegreeHandler::class, "Progressive Left/Right Degree multiplier"),
            KeyValue<KClass<*>>(LeftRightOffsetDegreeHandler::class, "Fixed Left/Right Degree")
        )

        val adapterOffsetServer = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, optionsOffsetStrategies)
        adapterOffsetServer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerOffsetStrategies.adapter = adapterOffsetServer
        spinnerOffsetStrategies.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position) as KeyValue<KClass<*>>
                val selectedOffsetHandler = selectedItem.key.constructors.first()?.call(requireContext())
                if (selectedOffsetHandler != null)
                {
                    this@CameraFragment.offsetDegreeHandler = selectedOffsetHandler as IOffsetDegreeHandler
                }
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