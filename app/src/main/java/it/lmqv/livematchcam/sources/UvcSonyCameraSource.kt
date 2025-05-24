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
import java.util.Collections

class UvcSonyCameraSource: VideoSource() {
    private var cameraHelper: ICameraHelper? = null
    private var surface: Surface? = null
    private var running = false

    override fun create(width: Int, height: Int, fps: Int, rotation: Int): Boolean {
        //Logd("CameraSource::create($width, $height, $fps, $rotation)")
        return true
    }

    override fun start(surfaceTexture: SurfaceTexture) {
        //Logd("CameraSource::start")
        this.surfaceTexture = surfaceTexture
        surface = Surface(surfaceTexture)
        cameraHelper = CameraHelper()
        cameraHelper?.setStateCallback(stateCallback)
        running = true
    }

    override fun stop() {
        //Logd("CameraSource::stop")
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
            //Logd("onCameraSize ${getSize()}")
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

    private fun getSize(sourceWidth: Int = width, sourceheight: Int = height, sourceFps: Int = fps): Size { // tEst
    // private fun getSize(sourceWidth: Int = 1920, sourceheight: Int = 1080, sourceFps: Int = 60): Size { // tEst
    // private fun getSize(sourceWidth: Int = 1280, sourceheight: Int = 720, sourceFps: Int = 25): Size { // OK
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

    /*fun updatePreviewSize(sourceWidth: Int, sourceheight: Int, sourceFps: Int) {
        //Logd("updatePreviewSize $sourceWidth x $sourceheight @$sourceFps")
        //Logd("cameraStopPreview")
        cameraHelper?.stopPreview()
        //Logd("cameraRemoveSurface")
        surface?.let { cameraHelper?.removeSurface(it) }
        //Logd("cameraPreviewSize")
        cameraHelper?.previewSize = getSize(sourceWidth, sourceheight, sourceFps)
        //Logd("cameraStartPreview")
        cameraHelper?.startPreview()
        //Logd("cameraAddSurface")
        surface?.let { cameraHelper?.addSurface(it, false) }
    }*/
}