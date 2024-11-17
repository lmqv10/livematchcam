package it.lmqv.livematchcam

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Bundle
import android.util.Range
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity.SENSOR_SERVICE
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.gl.render.filters.`object`.ImageObjectFilterRender
import com.pedro.encoder.input.sources.audio.MicrophoneSource
import com.pedro.encoder.input.sources.video.Camera1Source
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.encoder.input.sources.video.VideoSource
import com.pedro.library.generic.GenericStream
import com.pedro.library.util.BitrateAdapter
import it.lmqv.livematchcam.utils.Debouncer
import it.lmqv.livematchcam.utils.KeyValue
import it.lmqv.livematchcam.utils.ScreenUtils
import it.lmqv.livematchcam.utils.toast
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

class CameraFragment: Fragment(), ConnectChecker,
    ScoreBoardFragment.OnUpdateCallback, SwipeSurfaceView.OnSwipeGesture,
    SensorEventListener {

    companion object {
        fun getInstance(): CameraFragment = CameraFragment()
    }


    private lateinit var sensorManager: SensorManager
    //private var gyroscope: Sensor? = null
    private var rotationSensor: Sensor? = null
    //private var xDegreeMovement = 0f

    private var latestAzimuth = 0
    private var azimuthOffset = 0

    //private var previousTimestamp: Long = 0
    private lateinit var xDegreeTextView: TextView
    private lateinit var zoomLevelTextView: TextView
    private lateinit var initialZoomTextView: TextView

    private lateinit var leftDegreeTextView: TextView
    private lateinit var rightDegreeTextView: TextView

    private val decimalFormat = DecimalFormat("#.0")

    val genericStream: GenericStream by lazy {
        GenericStream(requireContext(), this).apply {
            //getGlInterface().autoHandleOrientation = true
            getStreamClient().setBitrateExponentialFactor(0.5f)
        }
    }
    private lateinit var videoSource: VideoSource

    private var shouldZoom : Boolean = false
    private var initialZoomLevel = 1f
    private var currentDegree = 0
    //private var currentZoomValue = 0f
    private var zoomLevel = 0f

    private var isMute : Boolean = false

    private lateinit var homeTeam : TextView
    private lateinit var homeScore : TextView
    private lateinit var awayTeam : TextView
    private lateinit var awayScore : TextView

    //private lateinit var editTextNumber: EditText

    private var scoreBoardFragment: ScoreBoardFragment? = null
    private lateinit var surfaceView: SwipeSurfaceView
    private lateinit var bStartStop: ImageView
    private lateinit var txtBitrate: TextView

    private val zoomDebouncer = Debouncer(750)
    private val width = 1280
    private val height = 720
    private val vBitrate = 5000 * 1000
    private var fps = 25
    private var rotation = 0
    private val sampleRate = 32000
    private val isStereo = true
    private val aBitrate = 128 * 1000
    //private var recordPath = ""
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
        val view = inflater.inflate(R.layout.fragment_camera, container, false)

        val audioManager = requireActivity().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        this.isMute = audioManager.isMicrophoneMute
        val bSwitchMicrophone = view.findViewById<ImageView>(R.id.microphone)
        if (this.isMute) {
            bSwitchMicrophone.setImageResource(R.drawable.microphone_off)
        } else {
            bSwitchMicrophone.setImageResource(R.drawable.microphone_on)
        }
        this.bStartStop = view.findViewById(R.id.b_start_stop)

        sensorManager = requireActivity().getSystemService(SENSOR_SERVICE) as SensorManager
        //gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        xDegreeTextView = view.findViewById(R.id.x_degree)
        zoomLevelTextView = view.findViewById(R.id.zoom_level)
        zoomLevelTextView.text = "n/a"
        initialZoomTextView = view.findViewById(R.id.initial_zoom)
        initialZoomTextView.text = decimalFormat.format(initialZoomLevel)

        txtBitrate = view.findViewById(R.id.txt_bitrate)
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
            val serverUri = GlobalDataManager.getServerURI() // "rtmp://a.rtmp.youtube.com/live2/fmjw-uqav-y4ua-xd4d-3zaw"

            if (!genericStream.isStreaming) {
                genericStream.startStream(serverUri)
                bStartStop.setImageResource(R.drawable.stream_stop_icon)
            } else {
                genericStream.stopStream()
                bStartStop.setImageResource(R.drawable.stream_icon)
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
        /*bSwitchCamera.setOnClickListener {
            when (val source = genericStream.videoSource) {
                is Camera1Source -> source.switchCamera()
                is Camera2Source -> source.switchCamera()
                is CameraXSource -> source.switchCamera()
            }
        }*/


        val homeTeamName = GlobalDataManager.homeTeam.name
        val awayTeamName = GlobalDataManager.awayTeam.name

        homeTeam = view.findViewById(R.id.home_team)
        homeTeam.text = homeTeamName

        awayTeam = view.findViewById(R.id.away_team)
        awayTeam.text = awayTeamName

        val scoreBoardFragment : ScoreBoardFragment = ScoreBoardFragment.newInstance( homeTeamName, awayTeamName)

        homeScore = view.findViewById(R.id.home_score)
        awayScore = view.findViewById(R.id.away_score)

        /*var homeColor = GlobalDataManager.homeTeam.color
        var awayColor = GlobalDataManager.awayTeam.color

        this.scoreBoardFragment?.setHomeLogo(homeColor)
        this.scoreBoardFragment?.setAwayLogo(awayColor)*/

        childFragmentManager.beginTransaction()
            .replace(R.id.score_board_placeholder, scoreBoardFragment, "ScoreBoardFragmentTag")
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
        }

        val bHomeScoreMinus = view.findViewById<ImageView>(R.id.home_score_minus)
        bHomeScoreMinus.setOnClickListener {
            scoreBoardFragment.addHomeScore(-1)
            homeScore.text = "" + scoreBoardFragment.currentHomeScore
            this.updateScoreBoard()
        }

        val bHomeScoreAdd = view.findViewById<ImageView>(R.id.home_score_add)
        bHomeScoreAdd.setOnClickListener {
            scoreBoardFragment.addHomeScore()
            homeScore.text = "" + scoreBoardFragment.currentHomeScore
            this.updateScoreBoard()
        }

        val bAwayScoreMinus = view.findViewById<ImageView>(R.id.away_score_minus)
        bAwayScoreMinus.setOnClickListener {
            scoreBoardFragment.addAwayScore(-1)
            awayScore.text = "" + scoreBoardFragment.currentAwayScore
            this.updateScoreBoard()
        }

        val bAwayScoreAdd = view.findViewById<ImageView>(R.id.away_score_add)
        bAwayScoreAdd.setOnClickListener {
            scoreBoardFragment.addAwayScore()
            awayScore.text = "" + scoreBoardFragment.currentAwayScore
            this.updateScoreBoard()
        }

        val bChangePeriod = view.findViewById<ImageView>(R.id.change_period)
        bChangePeriod.setOnClickListener {
            scoreBoardFragment.togglePeriod()
            this.updateScoreBoard()
        }

        val bStartTime = view.findViewById<ImageView>(R.id.start_time)
        val bResetTime = view.findViewById<ImageView>(R.id.reset_time)

        bStartTime.setOnClickListener {
            if (scoreBoardFragment.isInPause) {
                bStartTime.setImageResource(R.drawable.time_pause)
                scoreBoardFragment.startTime()

            } else {
                bStartTime.setImageResource(R.drawable.time_start)
                scoreBoardFragment.pauseTime()
            }
            bResetTime.isEnabled = scoreBoardFragment.isInPause
            //bResetTime.visibility = if (scoreBoardFragment.isInPause) View.VISIBLE else View.GONE
            this.updateScoreBoard()
        }

        bResetTime.setOnClickListener {
            bStartTime.setImageResource(R.drawable.time_start)
            scoreBoardFragment.resetTime()
            this.updateScoreBoard()
        }

        val resetBtn = view.findViewById<ImageView>(R.id.reset_rotation)
        resetBtn.setOnClickListener {
            azimuthOffset = -latestAzimuth
            //this.xDegreeMovement = 0f
        }

        val switch = view.findViewById<Switch>(R.id.switch_zoom)
        switch.isChecked = GlobalDataManager.autoZoomEnabled
        if (!GlobalDataManager.autoZoomEnabled) {
            xDegreeTextView.text = "Off"
            xDegreeTextView.setTextColor(ContextCompat.getColor(requireActivity(), R.color.GRAY))
            zoomLevelTextView.setTextColor(ContextCompat.getColor(requireActivity(), R.color.GRAY))
            resetBtn.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.GRAY))
            resetBtn.isEnabled = false
        }

        switch.setOnCheckedChangeListener { _, isChecked ->
            GlobalDataManager.autoZoomEnabled = isChecked
            if (isChecked) {
                rotationSensor?.also { mag ->
                    sensorManager.unregisterListener(this)
                    sensorManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_NORMAL)
                }
                xDegreeTextView.text = "..."
                xDegreeTextView.setTextColor(
                    ContextCompat.getColor(
                        requireActivity(),
                        R.color.white
                    )
                )
                zoomLevelTextView.setTextColor(
                    ContextCompat.getColor(
                        requireActivity(),
                        R.color.white
                    )
                )
                resetBtn.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.white))
                resetBtn.isEnabled = true
            } else {
                if (videoSource is Camera1Source) {
                    (videoSource as Camera1Source).setZoom(this.initialZoomLevel.toInt())
                } else if (videoSource is Camera2Source) {
                    (videoSource as Camera2Source).setZoom(this.initialZoomLevel)
                }

                sensorManager.unregisterListener(this)
                xDegreeTextView.text = "Off"
                xDegreeTextView.setTextColor(
                    ContextCompat.getColor(
                        requireActivity(),
                        R.color.GRAY
                    )
                )
                zoomLevelTextView.setTextColor(
                    ContextCompat.getColor(
                        requireActivity(),
                        R.color.GRAY
                    )
                )
                resetBtn.setColorFilter(ContextCompat.getColor(requireActivity(), R.color.GRAY))
                resetBtn.isEnabled = false
            }
        }

        leftDegreeTextView = view.findViewById(R.id.left_degree)
        leftDegreeTextView.text = "L:" + GlobalDataManager.leftZoomDegreeTrigger.toString() + "°"
        leftDegreeTextView.setOnClickListener {
            this.editLeftDegree()
        }

        rightDegreeTextView = view.findViewById(R.id.right_degree)
        rightDegreeTextView.text = "R:" + GlobalDataManager.rightZoomDegreeTrigger.toString() + "°"
        rightDegreeTextView.setOnClickListener {
            this.editRightDegree()
        }

        val zoomOffset = view.findViewById<ImageView>(R.id.zoom_offset)
        zoomOffset.setOnClickListener {
            this.editZoomOffset()
        }
        return view
    }

    private fun editZoomOffset() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_edit_degree, null)

        val title = dialogView.findViewById<TextView>(R.id.edit_degree_title)
        title.text = getString(R.string.zoom_offset)

        val numberPicker = dialogView.findViewById<NumberPicker>(R.id.edit_degree_picker)
        val floatValues = Array(5) {
            i -> KeyValue((i + 1) * 0.1f, String.format("%.1f", (i + 1) * 0.1))
        }

        numberPicker.minValue = 0
        numberPicker.maxValue = floatValues.size - 1
        numberPicker.displayedValues = floatValues.map { x -> x.description }.toTypedArray()

        val selected = floatValues.filter { x -> x.key == GlobalDataManager.zoomOffset }.first()
        numberPicker.value = floatValues.indexOf(selected)
        numberPicker.wrapSelectorWheel = false // This allows continuous scrolling

        numberPicker.setOnValueChangedListener { _, _, newVal ->
            val value = floatValues[newVal].key
            GlobalDataManager.zoomOffset = value
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                toast("Zoom Offset: ${GlobalDataManager.zoomOffset}")
                dialog.dismiss()
                hideSystemUI()
            }
            .create()

        dialog.setOnShowListener {
            hideSystemUI()
        }

        dialog.show()
    }

    private fun editLeftDegree() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_edit_degree, null)

        val title = dialogView.findViewById<TextView>(R.id.edit_degree_title)
        title.text = getString(R.string.left_degree)

        val numberPicker = dialogView.findViewById<NumberPicker>(R.id.edit_degree_picker)
        numberPicker.minValue = 0
        numberPicker.maxValue = 90
        numberPicker.value = GlobalDataManager.leftZoomDegreeTrigger
        numberPicker.wrapSelectorWheel = false // This allows continuous scrolling

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                val value = numberPicker.value
                leftDegreeTextView.text = "L:$value°"
                GlobalDataManager.leftZoomDegreeTrigger = value
                dialog.dismiss()
                hideSystemUI()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                hideSystemUI()
            }
            .create()

        dialog.setOnShowListener {
            hideSystemUI()
        }
        dialog.show()
    }

    private fun editRightDegree() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_edit_degree, null)

        val title = dialogView.findViewById<TextView>(R.id.edit_degree_title)
        title.text = getString(R.string.right_degree)

        val numberPicker = dialogView.findViewById<NumberPicker>(R.id.edit_degree_picker)
        numberPicker.minValue = 0
        numberPicker.maxValue = 90
        numberPicker.value = GlobalDataManager.rightZoomDegreeTrigger
        numberPicker.wrapSelectorWheel = false // This allows continuous scrolling

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                val value = numberPicker.value
                rightDegreeTextView.text = "R:$value°"
                GlobalDataManager.rightZoomDegreeTrigger = value
                dialog.dismiss()
                hideSystemUI()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                hideSystemUI()
            }
            .create()

        dialog.setOnShowListener {
            hideSystemUI()
        }

        dialog.show()
    }

    private fun hideSystemUI() {
        requireActivity().window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }

    override fun onResume() {
        super.onResume()
        if (GlobalDataManager.autoZoomEnabled) {
            rotationSensor?.also { mag ->
                sensorManager.unregisterListener(this)
                sensorManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    /*fun setOrientationMode(isVertical: Boolean) {
        val wasOnPreview = genericStream.isOnPreview
        genericStream.release()
        rotation = if (isVertical) 90 else 0
        prepare()
        if (wasOnPreview) genericStream.startPreview(surfaceView)
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepare()
        genericStream.getStreamClient().setReTries(10)
    }

    override fun onStart() {
        super.onStart()

        this.scoreBoardFragment = childFragmentManager.findFragmentByTag("ScoreBoardFragmentTag") as? ScoreBoardFragment
        this.scoreBoardFragment?.setOnUpdate(this)

        val homeColor = GlobalDataManager.homeTeam.color
        val awayColor = GlobalDataManager.awayTeam.color

        this.scoreBoardFragment?.setHomeLogo(homeColor)
        this.scoreBoardFragment?.setAwayLogo(awayColor)

        this.updateScoreBoard()
        toast("ServerURI: ${GlobalDataManager.getServerURI()}", Toast.LENGTH_LONG)
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
            videoSource = genericStream.videoSource

            /*var resoutions : List<Size>
            if (videoSource is Camera1Source) {
                resoutions = (videoSource as Camera1Source).getCameraResolutions(CameraHelper.Facing.BACK)
            } else if (videoSource is Camera2Source) {
                resoutions = (videoSource as Camera2Source).getCameraResolutions(CameraHelper.Facing.BACK)
            } else {
            }*/

            genericStream.prepareVideo(screenWidth, screenHeight, vBitrate, rotation = rotation, fps = fps) &&
            genericStream.prepareAudio(sampleRate, isStereo, aBitrate)

            /*if (videoSource is Camera1Source) {
                //var zooms = (videoSource as Camera2Source).getZoomRange()
                //this.initialZoomLevel = zooms.lower // (zooms.upper - zooms.lower) * 0.2f + zooms.lower
                this.zoomLevel = (videoSource as Camera1Source).getZoom().toFloat()
                (videoSource as Camera1Source).enableVideoStabilization()
            } else if (videoSource is Camera2Source) {
                //var zooms = (videoSource as Camera2Source).getZoomRange()
                //this.initialZoomLevel = zooms.lower // (zooms.upper - zooms.lower) * 0.2f + zooms.lower
                this.zoomLevel = (videoSource as Camera2Source).getZoom()
                (videoSource as Camera2Source).enableVideoStabilization()
            } else {
                zoomLevel = 1.0f
            }*/
            //this.initialZoomLevel = this.zoomLevel

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
        txtBitrate.text = String.format(Locale.getDefault(), "%.1f mb/s", bitrate / 1000_000f)
    }

    override fun onDisconnect() {
        txtBitrate.text = String()
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

    override fun onSensorChanged(event: SensorEvent?) {
        //val thresholdDegree = 0.5f
        //val threshold = 0f

        var refLeftAngle = GlobalDataManager.leftZoomDegreeTrigger
        var refRightAngle = GlobalDataManager.rightZoomDegreeTrigger
        event?.let {
            try
            {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)

                    val azimuthSensor = Math.toDegrees(-orientation[0].toDouble()).toInt()
                    var azimuthDegree = this.azimuthOffset + azimuthSensor
                    this.latestAzimuth = azimuthSensor

                    if ((currentDegree > -refRightAngle && currentDegree < refLeftAngle) && (azimuthDegree >= refLeftAngle || azimuthDegree <= -refRightAngle)) {
                        this.zoomLevel = GlobalDataManager.zoomOffset
                        shouldZoom = true
                    } else if ((currentDegree >= refLeftAngle || currentDegree <= -refRightAngle) && (azimuthDegree > -refRightAngle && azimuthDegree < refLeftAngle)) {
                        this.zoomLevel = 0f
                        shouldZoom = true
                    } else {
                        //shouldZoom = false
                    }

                    if (shouldZoom) {
                        shouldZoom = false
                        //zoomDebouncer.submit {
                            val updatedZoomLevel = this.initialZoomLevel + this.zoomLevel

                            if (videoSource is Camera1Source) {
                                (videoSource as Camera1Source).setZoom(updatedZoomLevel.toInt())
                            } else if (videoSource is Camera2Source) {
                                (videoSource as Camera2Source).setZoom(updatedZoomLevel)
                            } else {
                            }
                        //}
                    }

                    this.currentDegree = azimuthDegree
                    xDegreeTextView.text = "${abs(azimuthDegree.toInt())}°"
                    zoomLevelTextView.text = decimalFormat.format(this.initialZoomLevel + this.zoomLevel)
                }
/*
                if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                    val gyroX = event.values[0]
                    val timestamp = event.timestamp
                    val deltaTime = (timestamp - previousTimestamp) / 1_000_000_000f

                    if (abs(gyroX) > threshold) {
                        val gyroXInDegrees = Math.toDegrees(gyroX.toDouble()).toFloat()
                        if (abs(gyroXInDegrees) > thresholdDegree) {
                            this.xDegreeMovement += gyroXInDegrees * deltaTime

                            var xRoundDegree = round(this.xDegreeMovement)

                            if (xRoundDegree >= -360 && xRoundDegree <= 360) {
                                if (xRoundDegree != this.currentDegree) {
                                    if ((currentDegree >= -refRightAngle && currentDegree <= refLeftAngle) && (xRoundDegree > refLeftAngle || xRoundDegree < -refRightAngle)) {
                                        this.zoomLevel = 0.5f
                                        shouldZoom = true
                                    } else if ((currentDegree > refLeftAngle || currentDegree < -refRightAngle) && (xRoundDegree >= -refRightAngle && xRoundDegree <= refLeftAngle)) {
                                        this.zoomLevel = 0f
                                        shouldZoom = true
                                    } else {
                                        shouldZoom = false;
                                    }

                                    var zoomValue = this.initialZoomLevel + this.zoomLevel
                                    if (shouldZoom) {
                                        shouldZoom = false;
                                        if (videoSource is Camera1Source) {
                                            (videoSource as Camera1Source).setZoom(zoomValue.toInt())
                                        } else if (videoSource is Camera2Source) {
                                            (videoSource as Camera2Source).setZoom(zoomValue)
                                        } else {
                                        }
                                    }
                                    this.currentDegree = xRoundDegree
                                    xDegreeTextView.text = "$xRoundDegree°"
                                    zoomLevelTextView.text = decimalFormat.format(zoomValue)
                                    xDegreeTextView.setTextColor(
                                        ContextCompat.getColor(
                                            requireActivity(),
                                            R.color.white
                                        )
                                    )
                                    zoomLevelTextView.setTextColor(
                                        ContextCompat.getColor(
                                            requireActivity(),
                                            R.color.white
                                        )
                                    )
                                }
                            } else {
                                this.zoomLevel = 0f
                                var zoomValue = this.initialZoomLevel + this.zoomLevel
                                xDegreeTextView.text = "N/A"
                                zoomLevelTextView.text = decimalFormat.format(zoomValue)
                                xDegreeTextView.setTextColor(
                                    ContextCompat.getColor(
                                        requireActivity(),
                                        R.color.YELLOW
                                    )
                                )
                                zoomLevelTextView.setTextColor(
                                    ContextCompat.getColor(
                                        requireActivity(),
                                        R.color.YELLOW
                                    )
                                )
                            }
                        }
                    }

                    previousTimestamp = timestamp
                }
 */
            }
            catch (e: Exception) {
                xDegreeTextView.text = "ERR"
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Non è necessario gestire questo per l'accelerometro
    }

    private fun updateScoreBoard() {
        val view = this.scoreBoardFragment?.view
        view?.post {

            val width = view.width
            val height = view.height
            var factorFragment = width.toDouble() / height.toDouble()

            val screenWidth = ScreenUtils.getScreenWidth(requireContext())
            val screenHeight = ScreenUtils.getScreenHeight(requireContext())
            var factor = screenWidth.toDouble() / screenHeight.toDouble()

            //var factorHeight = 720
            //var factorWidth =  factorHeight * factor

            var scaleX = 15f //(scaleY * factor).toFloat();
            var scaleY = 15f

            if (width > 0 && height > 0) {
                val scoreBoardBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(scoreBoardBitmap)
                view.draw(canvas)

                val imageFilter = ImageObjectFilterRender()
                imageFilter.setImage(scoreBoardBitmap)
                imageFilter.setPosition(0f, 0f)
                imageFilter.setScale(scaleX, scaleY)
                genericStream.getGlInterface().setFilter(imageFilter)
            }
        }
    }

    override fun refresh() {
        this.updateScoreBoard()
    }

    override fun swipeUp() {
        this.initialZoomLevel += 0.1f
        var zoomValue = this.initialZoomLevel + this.zoomLevel
        if (videoSource is Camera1Source) {
            (videoSource as Camera1Source).setZoom(zoomValue.toInt())
        } else if (videoSource is Camera2Source) {
            (videoSource as Camera2Source).setZoom(zoomValue)
        } else { }

        zoomLevelTextView.text =  decimalFormat.format(zoomValue)
        initialZoomTextView.text = decimalFormat.format(initialZoomLevel)
        //Toast.makeText(context, "Zoom: $initialZoomLevel", Toast.LENGTH_SHORT).show()
    }

    override fun swipeDown() {
        this.initialZoomLevel -= 0.2f
        this.initialZoomLevel = max(1f, this.initialZoomLevel)

        var zoomValue = this.initialZoomLevel + this.zoomLevel
        when (videoSource) {
            is Camera1Source -> {
                (videoSource as Camera1Source).setZoom(zoomValue.toInt())
            }

            is Camera2Source -> {
                (videoSource as Camera2Source).setZoom(zoomValue)
            }

            else -> { }
        }

        zoomLevelTextView.text = decimalFormat.format(zoomValue)
        initialZoomTextView.text = decimalFormat.format(initialZoomLevel)
        //Toast.makeText(context, "Zoom: $initialZoomLevel", Toast.LENGTH_SHORT).show()
    }

    override fun swipeLeft() {
        when (videoSource) {
            is Camera1Source -> {
                this.initialZoomLevel = (videoSource as Camera1Source).getZoomRange().lower.toFloat()
                (videoSource as Camera1Source).setZoom(this.zoomLevel.toInt() + this.initialZoomLevel.toInt())
            }

            is Camera2Source -> {
                this.initialZoomLevel = (videoSource as Camera2Source).getZoomRange().lower
                (videoSource as Camera2Source).setZoom(this.zoomLevel + this.initialZoomLevel)
            }

            else -> { }
        }

        var zoomValue = this.initialZoomLevel + this.zoomLevel
        zoomLevelTextView.text = decimalFormat.format(zoomValue)
        initialZoomTextView.text = decimalFormat.format(initialZoomLevel)
        //Toast.makeText(context, "Zoom: $initialZoomLevel", Toast.LENGTH_SHORT).show()
    }

    override fun swipeRight() {
        when (videoSource) {
            is Camera1Source -> {
                this.initialZoomLevel = (videoSource as Camera1Source).getZoomRange().upper.toFloat()
                (videoSource as Camera1Source).setZoom(this.zoomLevel.toInt() + this.initialZoomLevel.toInt())
            }

            is Camera2Source -> {
                this.initialZoomLevel = (videoSource as Camera2Source).getZoomRange().upper
                (videoSource as Camera2Source).setZoom(this.zoomLevel + this.initialZoomLevel)
            }
        }

        var zoomValue = this.initialZoomLevel + this.zoomLevel
        zoomLevelTextView.text = decimalFormat.format(zoomValue)
        initialZoomTextView.text = decimalFormat.format(initialZoomLevel)
        //Toast.makeText(context, "Zoom: $initialZoomLevel", Toast.LENGTH_SHORT).show()
    }
}