package it.lmqv.livematchcam.sources

import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.view.Surface
import com.herohan.uvcapp.CameraHelper
import com.herohan.uvcapp.ICameraHelper
import com.pedro.encoder.input.sources.OrientationForced
import com.pedro.encoder.input.sources.video.VideoSource
import com.serenegiant.usb.Size
import com.serenegiant.usb.UVCCamera
import it.lmqv.livematchcam.extensions.Logd
import java.util.Collections

class UvcSonyCameraSource: VideoSource() {
    private var cameraHelper: ICameraHelper? = null
    private var surface: Surface? = null
    private var running = false

    override fun create(width: Int, height: Int, fps: Int, rotation: Int): Boolean {
        //Logd("create($width, $height, $fps, $rotation)")
        return true
    }

    override fun start(surfaceTexture: SurfaceTexture) {
        //Logd("start")
        this.surfaceTexture = surfaceTexture
        surface = Surface(surfaceTexture)
        cameraHelper = CameraHelper()
        cameraHelper?.setStateCallback(stateCallback)
        running = true
    }

    override fun stop() {
        //Logd("stop")
        surface?.let { cameraHelper?.removeSurface(it) }
        surface?.release()
        surface = null
        cameraHelper?.release()
        cameraHelper = null
        running = false
    }

    override fun release() {}

    override fun isRunning(): Boolean = running

    override fun getOrientationConfig(): OrientationForced = OrientationForced.LANDSCAPE

    private val stateCallback: ICameraHelper.StateCallback = object : ICameraHelper.StateCallback {
        override fun onAttach(device: UsbDevice) {
            //Logd("onAttach ${device.deviceName}")
            cameraHelper?.selectDevice(device)
        }

        override fun onDeviceOpen(device: UsbDevice, isFirstOpen: Boolean) {
            //Logd("openCamera ${device.deviceName}")
            cameraHelper?.openCamera()
        }

        override fun onCameraOpen(device: UsbDevice) {
            //Logd("onCameraOpen ${device.deviceName}")
            cameraHelper?.previewSize = getSize()
            cameraHelper?.startPreview()
            surface?.let { cameraHelper?.addSurface(it, false) }
        }

        override fun onCameraClose(device: UsbDevice) {
            //Logd("onCameraClose ${device.deviceName}")
        }
        override fun onDeviceClose(device: UsbDevice) {
            //Logd("onDeviceClose ${device.deviceName}")
        }
        override fun onDetach(device: UsbDevice) {
            //Logd("onDetach ${device.deviceName}")
        }
        override fun onCancel(device: UsbDevice) {
            //Logd("onCancel ${device.deviceName}")
        }
    }

    //private fun getSize(sourceWidth: Int = 1920, sourceheight: Int = 1080, sourceFps: Int = 30): Size {
    //private fun getSize(sourceWidth: Int = width, sourceheight: Int = height, sourceFps: Int = fps): Size {
    private fun getSize(sourceWidth: Int = 1280, sourceheight: Int = 720, sourceFps: Int = 30): Size {
        var currentSize: Size? = null
        val sizeList: List<Size> = cameraHelper?.supportedSizeList?.toMutableList() ?: listOf()

        if (sizeList.isNotEmpty()) {
            Collections.sort(
                sizeList
            ) { o1: Size, o2: Size -> o2.width * o2.height - o1.width * o1.height }
            for (size in sizeList) {
                if (size.type == UVCCamera.DEFAULT_PREVIEW_FRAME_FORMAT &&
                    size.width == sourceWidth &&
                    size.height == sourceheight) {

                    var cameraFps: Int = sourceFps
                    for (fpsItem in size.fpsList) {
                        if (sourceFps >= fpsItem)
                        {
                            cameraFps = fpsItem
                            break
                        }
                    }

                    currentSize = Size(
                        UVCCamera.DEFAULT_PREVIEW_FRAME_FORMAT,
                        sourceWidth,
                        sourceheight,
                        cameraFps,
                        ArrayList<Int>(size.fpsList)
                    )
                    break
                }
            }
        }

        if (currentSize == null) {
            currentSize = Size(
                UVCCamera.DEFAULT_PREVIEW_FRAME_FORMAT,
                UVCCamera.DEFAULT_PREVIEW_WIDTH,
                UVCCamera.DEFAULT_PREVIEW_HEIGHT,
                UVCCamera.DEFAULT_PREVIEW_FPS,
                ArrayList<Int>(UVCCamera.DEFAULT_PREVIEW_FPS)
            )
        }

        return currentSize
    }

    //private var frameCount = 0
    //private var lastFpsTimestamp = System.currentTimeMillis()

    /*private fun emitFrameRate() {
        frameCount++
        val currentTime = System.currentTimeMillis()
        val elapsedSeconds = (currentTime - lastFpsTimestamp) / 1000.0

        if (elapsedSeconds >= 1.0) { // Update FPS every second
            val fps = frameCount / elapsedSeconds
            Logd("SurfaceTexture FPS:: ${"%.2f".format(fps)}")

            // Reset counters
            frameCount = 0
            lastFpsTimestamp = currentTime
        }
    }*/
}