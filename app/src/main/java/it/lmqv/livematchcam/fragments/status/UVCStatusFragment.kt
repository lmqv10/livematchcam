package it.lmqv.livematchcam.fragments.status

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentUvcStatusBinding
import it.lmqv.livematchcam.extensions.bitrateFormat
import it.lmqv.livematchcam.extensions.fpsFormat
import it.lmqv.livematchcam.extensions.resolutionFormat
import it.lmqv.livematchcam.extensions.sourceBitrateFormat
import it.lmqv.livematchcam.extensions.droppedFramesFormat
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.services.stream.NetworkHealth
import it.lmqv.livematchcam.services.stream.StreamService
import it.lmqv.livematchcam.viewmodels.StatusViewModel

class UVCStatusFragment : Fragment(), IStatusFragment {

    companion object {
        fun getInstance() = UVCStatusFragment()
    }

    private val statusViewModel: StatusViewModel by activityViewModels()
    private var _binding: FragmentUvcStatusBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUvcStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        statusViewModel.bitrate.observe(viewLifecycleOwner, Observer { data ->
            binding.bitrate.text = bitrateFormat(data)
        })

        statusViewModel.fps.observe(viewLifecycleOwner, Observer { data ->
            binding.fps.text = fpsFormat(data)
        })

        statusViewModel.sourceResolution.observe(viewLifecycleOwner, Observer { data ->
            binding.sourceResolution.text = resolutionFormat(data)
        })

        statusViewModel.sourceFps.observe(viewLifecycleOwner, Observer { data ->
            binding.sourceFps.text = fpsFormat(data)
        })

        statusViewModel.sourceBitrate.observe(viewLifecycleOwner, Observer { data ->
            binding.sourceBitrate.text = sourceBitrateFormat(data)
        })

        // --- Network Health Indicator ---
        statusViewModel.streamPerformance.observe(viewLifecycleOwner, Observer { perf ->
            // Keep UI visible if bitrate is > 0 OR if we are in Safe Mode (during restart transitions)
            val isStreaming = perf.currentBitrate > 0 || perf.cacheSize > 0 || perf.isSafeModeActive
            binding.networkHealthContainer.visibility = if (isStreaming) View.VISIBLE else View.GONE

            if (isStreaming) {
                val healthColorRes = when (perf.health) {
                    NetworkHealth.GREEN -> R.color.health_green
                    NetworkHealth.YELLOW -> R.color.health_yellow
                    NetworkHealth.RED -> R.color.health_red
                }
                binding.healthDot.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), healthColorRes)
                )
                binding.networkBitrate.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.white)
                )

                val healthTextRes = when (perf.health) {
                    NetworkHealth.GREEN -> R.string.network_health_good
                    NetworkHealth.YELLOW -> R.string.network_health_warning
                    NetworkHealth.RED -> R.string.network_health_critical
                }
                binding.networkBitrate.text = getString(healthTextRes)
                //binding.networkDropped.text = getString(R.string.network_dropped_label, droppedFramesFormat(perf.droppedFrames.toLong()))

                // Safe Mode toggle UI
                binding.safeModeToggle.visibility = View.VISIBLE
                binding.safeModeToggle.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))
                
                if (perf.isSafeModeActive) {
                    binding.bitrate.text = bitrateFormat(StreamService.SAFE_BITRATE / 1000_000f)
                    binding.fps.text = fpsFormat(StreamService.SAFE_FPS)
                    binding.bitrate.setTextColor(ContextCompat.getColor(requireContext(), R.color.health_yellow))
                    binding.fps.setTextColor(ContextCompat.getColor(requireContext(), R.color.health_yellow))
                } else {
                    binding.bitrate.text = bitrateFormat(statusViewModel.bitrate.value ?: 0f)
                    binding.fps.text = fpsFormat(statusViewModel.fps.value ?: 0)
                    binding.bitrate.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                    binding.fps.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                }
            }
        })

        binding.safeModeToggle.setOnClickListener {
            val perf = statusViewModel.streamPerformance.value ?: return@setOnClickListener
            val streamActivity = activity as? it.lmqv.livematchcam.StreamActivity ?: return@setOnClickListener
            val proxy = streamActivity.streamService
            
            if (perf.isSafeModeActive) {
                toast(getString(R.string.safe_mode_manual_disabling))
                proxy.disableSafeMode()
            } else {
                toast(getString(R.string.safe_mode_manual_enabling))
                proxy.enableSafeMode()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}