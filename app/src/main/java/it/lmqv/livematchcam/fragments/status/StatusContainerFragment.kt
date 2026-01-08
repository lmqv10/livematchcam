package it.lmqv.livematchcam.fragments.status

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import it.lmqv.livematchcam.databinding.FragmentStatusContaierBinding
import it.lmqv.livematchcam.extensions.launchOnResumed
import it.lmqv.livematchcam.factories.StatusFragmentFactory
import it.lmqv.livematchcam.handlers.zoom.IZoomLevelHandler
import it.lmqv.livematchcam.handlers.zoom.NoDebounceExtraSmoothZoomLevelHandler
import it.lmqv.livematchcam.services.stream.IVideoSourceZoomHandler
import it.lmqv.livematchcam.viewmodels.StatusViewModel
import it.lmqv.livematchcam.viewmodels.StreamConfigurationViewModel
import it.lmqv.livematchcam.viewmodels.VideoSourceKind
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

class StatusContainerFragment: Fragment(),
    StatusFragment.OnZoomButtonClickListener {

    private var _binding: FragmentStatusContaierBinding? = null
    private val binding get() = _binding!!

    private lateinit var zoomLevelHandler: IZoomLevelHandler

    companion object {
        fun getInstance(): StatusContainerFragment = StatusContainerFragment()
    }

    private val streamConfigurationViewModel: StreamConfigurationViewModel by activityViewModels()

    private var videoSourceKind : VideoSourceKind? = null
    private val statusViewModel: StatusViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentStatusContaierBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launchOnResumed {
            combine(
                streamConfigurationViewModel.fps,
                streamConfigurationViewModel.videoCaptureFormat,
                streamConfigurationViewModel.videoSourceKind
            ) { fps, videoCaptureFormat, videoSourceKind -> Triple(fps, videoCaptureFormat, videoSourceKind) }
            .distinctUntilChanged()
            .collect { (fps, videoCaptureFormat, videoSourceKind) ->
                if (fps != null && videoCaptureFormat != null) {
                    statusViewModel.setSourceResolution(videoCaptureFormat.height)
                    statusViewModel.setSourceFps(fps)
                    //statusViewModel.setVideoSourceKind(videoSourceKind)
                }

                if (this.videoSourceKind != videoSourceKind) {
                    this.videoSourceKind = videoSourceKind

                    childFragmentManager.beginTransaction()
                        .replace(binding.statusContainer.id,
                            StatusFragmentFactory.get(videoSourceKind) as Fragment)
                        .commit()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onZoomIn() {
        zoomLevelHandler.increase()
    }

    override fun onZoomOut() {
        zoomLevelHandler.decrease()
    }

    fun setFps(fps: Int) {
        statusViewModel.setFPS(fps)
    }

    fun setBitrate(bitrate: Long) {
        statusViewModel.setBitrate(bitrate / 1000_000f)
    }

    fun setVideoSourceZoomHandler(videoSourceZoomHandler: IVideoSourceZoomHandler) {
        zoomLevelHandler = NoDebounceExtraSmoothZoomLevelHandler(
            requireActivity(),
            videoSourceZoomHandler)
    }
}