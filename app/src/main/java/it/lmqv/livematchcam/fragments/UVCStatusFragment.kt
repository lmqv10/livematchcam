package it.lmqv.livematchcam.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.databinding.FragmentStatusBinding
import it.lmqv.livematchcam.databinding.FragmentUvcStatusBinding
import it.lmqv.livematchcam.services.RotationSensorService
import it.lmqv.livematchcam.repositories.SettingsRepository
import it.lmqv.livematchcam.utils.KeyValue
import it.lmqv.livematchcam.extensions.bitrateFormat
import it.lmqv.livematchcam.extensions.degreeFormat
import it.lmqv.livematchcam.extensions.fpsFormat
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.extensions.singleDecimalFormat
import it.lmqv.livematchcam.viewmodels.StatusViewModel
import it.lmqv.livematchcam.viewmodels.UVCStatusViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

class UVCStatusFragment : Fragment() {

    companion object {
        fun newInstance() = UVCStatusFragment()
    }

    private val statusViewModel: UVCStatusViewModel by activityViewModels()
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}