//package it.lmqv.livematchcam.fragments
//
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.lifecycleScope
//import com.pedro.encoder.utils.gl.TranslateTo
//import it.lmqv.livematchcam.databinding.FragmentCameraBinding
//import it.lmqv.livematchcam.extensions.launchOnStarted
//import it.lmqv.livematchcam.repositories.MatchRepository
//import it.lmqv.livematchcam.services.stream.filters.BitmapRotatorFilterRender
//import it.lmqv.livematchcam.services.stream.filters.FilterDescriptor
//import it.lmqv.livematchcam.services.stream.filters.RotatorDescriptor
//import kotlinx.coroutines.flow.combine
//import kotlinx.coroutines.launch
//
//class CameraFragment: Fragment() {
//
//    private var _binding: FragmentCameraBinding? = null
//    private val binding get() = _binding!!
//
//    companion object {
//        fun getInstance(): CameraFragment = CameraFragment()
//    }
//
//    private lateinit var spotBannerFilter: BitmapRotatorFilterRender
//    private lateinit var mainBannerFilter: BitmapRotatorFilterRender
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
//    ): View {
//        _binding = FragmentCameraBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        //Logd("CameraFragment::onViewCreated")
//
//        spotBannerFilter = BitmapRotatorFilterRender(requireContext(),
//            filterDescriptor = FilterDescriptor(maxFactor = 25f, translateTo = TranslateTo.TOP_RIGHT),
//            rotatorDescriptor = RotatorDescriptor())
//
//        mainBannerFilter = BitmapRotatorFilterRender(requireContext(),
//            filterDescriptor = FilterDescriptor(maxFactor = 70f, translateTo = TranslateTo.CENTER),
//            rotatorDescriptor = RotatorDescriptor(targetWidthDp = 300))
//
//        lifecycleScope.launch {
//            combine(
//                MatchRepository.spotBannerURL,
//                MatchRepository.spotBannerVisible
//            ) { url, visible -> Pair(url, visible)
//            }.collect { (url, visible) ->
//                launchOnStarted {
//                    spotBannerFilter.setUrls(listOf(url))
//                    spotBannerFilter.setIsVisible(visible)
//                }
//            }
//        }
//
//        lifecycleScope.launch {
//            combine(
//                MatchRepository.mainBannerURL,
//                MatchRepository.mainBannerVisible
//            ) { url, visible ->
//                Pair(url, visible)
//            }.collect { (url, visible) ->
//                launchOnStarted {
//                    mainBannerFilter.setUrls(listOf(url))
//                    mainBannerFilter.setIsVisible(visible)
//                }
//            }
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        //Logd("CameraFragment :: onCreate")
//    }
//
//    override fun onStart() {
//        super.onStart()
//        //Logd("CameraFragment :: onStart")
//    }
//
//    override fun onPause() {
//        super.onPause()
//        //Logd("CameraFragment :: onPause")
//    }
//
//    override fun onResume() {
//        super.onResume()
//        //Logd("CameraFragment :: onResume")
//
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        //Logd("CameraFragment :: onDestroy")
//        spotBannerFilter.stop()
//        mainBannerFilter.stop()
//
//    }
//
//    override fun onDestroyView() {
//        //Logd("CameraFragment :: onDestroyView")
//        super.onDestroyView()
//        _binding = null
//    }
//
////    override fun getSurfaceView(): SurfaceView {
////        return binding.surfaceView
////    }
//
////    override fun getFilters(): List<BitmapObjectFilterRender> {
////        return listOf(
////            scoreBoardFilter,
////            spotBannerFilter,
////            mainBannerFilter)
////    }
////
////    override fun setStreamService(streamService: StreamService) {
////        streamService.setConnectCheckerCallback(this)
////        streamService.setFpsListenerCallback(this)
////
////        Logd("CameraFragment :: setStreamService")
////    }
//
////    override fun getVideoSourceKind(): VideoSourceKind {
////        return VideoSourceKind.CAMERA2
////    }
//}