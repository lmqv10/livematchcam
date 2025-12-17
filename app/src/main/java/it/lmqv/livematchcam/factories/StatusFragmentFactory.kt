package it.lmqv.livematchcam.factories

import it.lmqv.livematchcam.fragments.status.IStatusFragment
import it.lmqv.livematchcam.fragments.status.StatusFragment
import it.lmqv.livematchcam.fragments.status.UVCStatusFragment
import it.lmqv.livematchcam.viewmodels.VideoSourceKind

object StatusFragmentFactory {
    fun get(videoSourceKind: VideoSourceKind) : IStatusFragment {
        return when (videoSourceKind) {
            VideoSourceKind.CAMERA2 -> StatusFragment.Companion.getInstance()
            VideoSourceKind.UVC_SONY -> UVCStatusFragment.Companion.getInstance()
            //VideoSourceKind.UVC_CAMERA -> UVCStatusFragment.getInstance()
        }
    }
}