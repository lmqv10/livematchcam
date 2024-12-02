package it.lmqv.livematchcam.services

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity.SENSOR_SERVICE
import androidx.fragment.app.FragmentActivity
import it.lmqv.livematchcam.repositories.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RotationSensorService(private val activity: FragmentActivity) : SensorEventListener {

    interface OnRotationListener {
        fun onDegreeChanged(degree: Int)
        fun onError(e: Exception)
    }

    private var onRotationListener: OnRotationListener? = null
    fun setOnRotationListener(listener: OnRotationListener) {
        this.onRotationListener = listener;
    }

    private var sensorManager: SensorManager = activity.getSystemService(SENSOR_SERVICE) as SensorManager
    private var rotationSensor: Sensor? = null

    private var latest = 0
    private var offset = 0
    private var isEnabled = false
    private var invalidate = false
    private var isRegistered = false
    private var settingsRepository: SettingsRepository

    init {
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        settingsRepository = SettingsRepository(activity)

        CoroutineScope(Dispatchers.Main).launch {
            settingsRepository.autoZoomEnabled.collect { enabled ->
                isEnabled = enabled
                register()
            }
        }
    }

    fun register() {
        rotationSensor?.also { mag ->
            unregister()
            if (isEnabled) {
                sensorManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_NORMAL)
                isRegistered = true
            }
        }
    }

    fun unregister() {
        if (this.isRegistered) {
            sensorManager.unregisterListener(this)
            this.isRegistered = false
        }
    }

    fun initialize() {
        this.offset = -this.latest
        invalidate()
        //Log.i("SensorService", "Initialize Offset: ${offset}")
    }

    fun invalidate() {
        //Log.i("SensorService", "Invalidate")
        this.invalidate = true
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            try {
                if (this.isEnabled && event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)

                    val degree = Math.toDegrees(orientation[0].toDouble()).toInt()

                    if (this.latest != degree || this.invalidate) {
                        //Log.i("SensorService", "Degree: $degree - latest: $latest - offset: $offset - offsetDegree: ${this.offset + this.latest}")
                        this.latest = degree
                        this.invalidate = false
                        var offsetDegree = this.offset + this.latest
                        this.onRotationListener?.onDegreeChanged(offsetDegree)
                    } else { }
                } else { }
            } catch (e: Exception) {
                this.onRotationListener?.onError(e)
            }
        }
    }

    override fun onAccuracyChanged(event: Sensor?, p1: Int) {
        // not needed
        //event?.let {
            //Log.i("SensorService", "isWakeUpSensor: ${event.isWakeUpSensor} - accuracy: ${p1}")
        //}
    }
}