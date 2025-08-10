package it.lmqv.livematchcam.services

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity.SENSOR_SERVICE
import androidx.fragment.app.FragmentActivity
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.repositories.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class RotationSensorService(private val activity: FragmentActivity) : SensorEventListener {

    interface OnRotationListener {
        fun onDegreesChanged(degrees: IntArray)
        fun onError(e: Exception)
    }

    private var onRotationListener: OnRotationListener? = null
    fun setOnRotationListener(listener: OnRotationListener) {
        this.onRotationListener = listener;
    }

    private var sensorManager: SensorManager = activity.getSystemService(SENSOR_SERVICE) as SensorManager
    private var rotationSensor: Sensor? = null

    private var latestOrientation = IntArray(3)
    private var offsets = IntArray(3)

    private var isEnabled = false
    private var invalidate = false
    private var isRegistered = false
    private var settingsRepository: SettingsRepository

    private var autoZoomJob: Job? = null

    init {
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        settingsRepository = SettingsRepository(activity)

        autoZoomJob = CoroutineScope(Dispatchers.Main).launch {
            settingsRepository.autoZoomEnabled.collect { enabled ->
                isEnabled = enabled
                register()
            }
        }
    }

    fun destroy() {
        autoZoomJob?.cancel()
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
        this.offsets = IntArray(this.latestOrientation.size) { i -> -this.latestOrientation[i] }
        //this.offsets = this.latestOrientation
        this.invalidate = true
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            try {
                if (this.isEnabled && event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {

                    /*val rotationVector = event.values // Il vettore di rotazione dal sensore
                    val x = rotationVector[0]
                    val y = rotationVector[1]
                    val z = rotationVector[2]
                    val w = if (rotationVector.size > 3) rotationVector[3] else Math.sqrt(1.0 - (x*x + y*y + z*z)).toFloat()
                    */

                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    //val rotationMatrix = quaternionToRotationMatrix(x, y, z, w)

                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)

                    //Log.i("SensorService", "orientation: ${orientation.joinToString(", ")}")

                    var orientationDegrees = IntArray(this.latestOrientation.size) { i ->
                        var degree = Math.toDegrees(orientation[i].toDouble()).toInt()
                        //(degree + 180) % 360 - 180
                        //Log.i("SensorService", "${i}: ${degree}")
                        //if (degree > 0) 180 - degree else degree
                        degree
                    }

                    if (!this.latestOrientation.contentEquals(orientationDegrees) || this.invalidate) {
                        this.invalidate = false
                        this.latestOrientation = orientationDegrees

                        //Log.i("SensorService", "degrees: ${orientationDegrees[0]} :: offsets: ${offsets[0]}")

                        var normalizedOrientationDegrees = IntArray(this.latestOrientation.size) { i ->
                            var offsetDegree = orientationDegrees[i] + offsets[i]
                            if (offsetDegree >= 180) {
                                offsetDegree % 180 - 180
                            } else if (offsetDegree <= -180) {
                                offsetDegree % 180
                            } else {
                                offsetDegree
                            }
                        }

                        /*Log.i("SensorService", "normalized: ${normalizedOrientationDegrees.joinToString(", ")}"
                            + ":: degrees: ${orientationDegrees.joinToString(", ")}"
                            + ":: offsets: ${offsets.joinToString(", ")}")*/

                        this.onRotationListener?.onDegreesChanged(normalizedOrientationDegrees)
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

    /*fun quaternionToRotationMatrix(x: Float, y: Float, z: Float, w: Float): FloatArray {
        val matrix = FloatArray(9)
        matrix[0] = 1 - 2 * (y * y + z * z)
        matrix[1] = 2 * (x * y - z * w)
        matrix[2] = 2 * (x * z + y * w)
        matrix[3] = 2 * (x * y + z * w)
        matrix[4] = 1 - 2 * (x * x + z * z)
        matrix[5] = 2 * (y * z - x * w)
        matrix[6] = 2 * (x * z - y * w)
        matrix[7] = 2 * (y * z + x * w)
        matrix[8] = 1 - 2 * (x * x + y * y)
        return matrix
    }

    fun quaternionToEulerAngles(x: Float, y: Float, z: Float, w: Float): FloatArray {
        val angles = FloatArray(3)

        // Yaw (Z)
        val sinyCosp = 2 * (w * z + x * y)
        val cosyCosp = 1 - 2 * (y * y + z * z)
        angles[0] = Math.atan2(sinyCosp.toDouble(), cosyCosp.toDouble()).toFloat()

        // Pitch (X)
        val sinp = 2 * (w * x - z * y)
        angles[1] = if (Math.abs(sinp) >= 1)
            Math.copySign(Math.PI / 2, sinp.toDouble()).toFloat()
        else
            Math.asin(sinp.toDouble()).toFloat()

        // Roll (Y)
        val sinrCosp = 2 * (w * y + z * x)
        val cosrCosp = 1 - 2 * (x * x + y * y)
        angles[2] = Math.atan2(sinrCosp.toDouble(), cosrCosp.toDouble()).toFloat()

        return angles
    }*/
}