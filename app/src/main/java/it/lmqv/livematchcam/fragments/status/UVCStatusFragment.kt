package it.lmqv.livematchcam.fragments.status

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import it.lmqv.livematchcam.databinding.FragmentUvcStatusBinding
import it.lmqv.livematchcam.extensions.bitrateFormat
import it.lmqv.livematchcam.extensions.fpsFormat
import it.lmqv.livematchcam.extensions.resolutionFormat
import it.lmqv.livematchcam.extensions.sourceBitrateFormat
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}