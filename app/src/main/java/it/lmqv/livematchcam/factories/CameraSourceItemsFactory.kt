package it.lmqv.livematchcam

import androidx.annotation.DrawableRes
import it.lmqv.livematchcam.viewmodels.VideoSourceKind


data class CameraSourceItem(
    @DrawableRes val icon: Int,
    val videoSourceKind: VideoSourceKind
)

object CameraSourceItemsFactory{
    fun get() : List<CameraSourceItem> {
        var cameraSourceItems = VideoSourceKind.entries.map { x ->
            var resId = when (x) {
                VideoSourceKind.CAMERA2 -> R.drawable.ic_device_camera
                VideoSourceKind.UVC_SONY -> R.drawable.ic_uvc_camera
            }
            CameraSourceItem(resId, x)
        }
        return cameraSourceItems
    }
}