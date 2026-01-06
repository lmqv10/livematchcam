package it.lmqv.livematchcam.fragments.status

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
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
import it.lmqv.livematchcam.services.RotationSensorService
import it.lmqv.livematchcam.repositories.SettingsRepository
import it.lmqv.livematchcam.utils.OptionItem
import it.lmqv.livematchcam.extensions.bitrateFormat
import it.lmqv.livematchcam.extensions.degreeFormat
import it.lmqv.livematchcam.extensions.fpsFormat
import it.lmqv.livematchcam.extensions.hideSystemUI
import it.lmqv.livematchcam.extensions.launchOnStarted
import it.lmqv.livematchcam.extensions.resolutionFormat
import it.lmqv.livematchcam.extensions.singleDecimalFormat
import it.lmqv.livematchcam.viewmodels.StatusViewModel
import kotlinx.coroutines.launch

interface IStatusFragment //<T> where T: Fragment

class StatusFragment : Fragment(), IStatusFragment,
    RotationSensorService.OnRotationListener {

    companion object {
        fun getInstance() = StatusFragment()
    }

    interface OnZoomButtonClickListener {
        fun onZoomIn()
        fun onZoomOut()
    }
    private var listener: OnZoomButtonClickListener? = null

    private val statusViewModel: StatusViewModel by activityViewModels()
    private var _binding: FragmentStatusBinding? = null
    private val binding get() = _binding!!

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var rotationSensorService: RotationSensorService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        settingsRepository = SettingsRepository(requireContext())

        rotationSensorService = RotationSensorService(requireActivity())
        rotationSensorService.setOnRotationListener(this)

        _binding = FragmentStatusBinding.inflate(inflater, container, false)

        this.launchOnStarted {
            settingsRepository.autoZoomEnabled.collect { isEnabled ->
                binding.autoZoomSwitch.isChecked = isEnabled

                binding.angleDegreeX.isEnabled = isEnabled
                binding.angleDegreeZ.isEnabled = isEnabled
                binding.zoomLevel.isEnabled = isEnabled
                binding.resetRotation.isEnabled = isEnabled
                binding.zoomOffset.isEnabled = isEnabled

                binding.leftDegree.isEnabled = isEnabled
                binding.rightDegree.isEnabled = isEnabled
            }
        }

        this.launchOnStarted {
            settingsRepository.leftDegree.collect { degree ->
                binding.leftDegree.text = degreeFormat(degree)
            }
        }

        this.launchOnStarted {
            settingsRepository.rightDegree.collect { degree -> binding.rightDegree.text = degreeFormat(degree) }
        }

        this.launchOnStarted {
            settingsRepository.initialZoom.collect { zoom ->
                binding.initialZoom.text = singleDecimalFormat(zoom, "x")
                rotationSensorService.initialize()
            }
        }

        statusViewModel.sourceResolution.observe(viewLifecycleOwner, Observer { data ->
            binding.sourceResolution.text = resolutionFormat(data)
        })

        statusViewModel.sourceFps.observe(viewLifecycleOwner, Observer { data ->
            binding.sourceFps.text = fpsFormat(data)
        })

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

        statusViewModel.angleDegrees.observe(viewLifecycleOwner, Observer { degrees ->
            binding.angleDegreeX.text = degreeFormat(degrees[0])
            binding.angleDegreeZ.text = degreeFormat(degrees[2])
        })

        statusViewModel.zoomLevel.observe(viewLifecycleOwner, Observer { data ->
            binding.zoomLevel.text = singleDecimalFormat(data)
        })

        binding.autoZoomSwitch.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                settingsRepository.setAutoZoom(isChecked)
            }
        }

        binding.resetRotation.setOnClickListener {
            this.rotationSensorService.initialize()
        }

        binding.leftDegree.setOnClickListener {
            this.editDegrees()
        }
        binding.arrowLeftDegree.setOnClickListener {
            this.editDegrees()
        }

        binding.rightDegree.setOnClickListener {
            this.editDegrees()
        }
        binding.arrowRightDegree.setOnClickListener {
            this.editDegrees()
        }

        binding.zoomOffset.setOnClickListener {
            this.editZoomOffset()
        }

        binding.zoomOut.setOnClickListener {
            listener?.onZoomOut()
        }

        binding.zoomIn.setOnClickListener {
            listener?.onZoomIn()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rotationSensorService.destroy()
        _binding = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as? OnZoomButtonClickListener
            ?: throw ClassCastException("$parentFragment must implement OnZoomButtonClickListener")
    }

    override fun onResume() {
        super.onResume()
        this.rotationSensorService.register()
    }

    override fun onPause() {
        super.onPause()
        this.rotationSensorService.unregister()
    }

    override fun onDegreesChanged(degrees: IntArray) {
        statusViewModel.setAngleDegrees(degrees)
    }

    override fun onError(e: Exception) {
        binding.angleDegreeX.text = getString(R.string.stream_key)
        binding.angleDegreeZ.text = getString(R.string.stream_key)
    }

    // TODO : Improve
    @SuppressLint("DefaultLocale")
    private fun editZoomOffset() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_edit_offset, null)

        val title = dialogView.findViewById<TextView>(R.id.edit_title)
        title.text = getString(R.string.zoom_offset)

        val numberPicker = dialogView.findViewById<NumberPicker>(R.id.edit_offset)
        val floatValues = Array(5) {
            i -> OptionItem((i + 1) * 0.1f, String.format("%.1f", (i + 1) * 0.1))
        }

        numberPicker.minValue = 0
        numberPicker.maxValue = floatValues.size - 1
        numberPicker.wrapSelectorWheel = false
        numberPicker.displayedValues = floatValues.map { x -> x.description }.toTypedArray()

        lifecycleScope.launch {
            settingsRepository.zoomOffset.collect { offset ->
                val selected = floatValues.first { x -> x.key == offset }
                numberPicker.value = floatValues.indexOf(selected)
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                lifecycleScope.launch {
                    val valueRef = numberPicker.value
                    val value = floatValues[valueRef].key
                    settingsRepository.setZoomOffset(value)
                }

                dialog.dismiss()
                requireActivity().hideSystemUI()
            }
            .create()

        dialog.setOnShowListener {
            requireActivity().hideSystemUI()
        }

        dialog.show()
    }

    // TODO : Improve
    private fun editDegrees() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_edit_degree, null)

        val titleLeft = dialogView.findViewById<TextView>(R.id.edit_left_degree_title)
        titleLeft.text = getString(R.string.left_degree)

        val titleRight = dialogView.findViewById<TextView>(R.id.edit_right_degree_title)
        titleRight.text = getString(R.string.right_degree)

        val numberPickerLeft = dialogView.findViewById<NumberPicker>(R.id.edit_left_degree_picker)
        numberPickerLeft.minValue = 0
        numberPickerLeft.maxValue = 90
        numberPickerLeft.wrapSelectorWheel = false
        lifecycleScope.launch {
            settingsRepository.leftDegree.collect { degree -> numberPickerLeft.value = degree }
        }

        val numberPickerRight = dialogView.findViewById<NumberPicker>(R.id.edit_right_degree_picker)
        numberPickerRight.minValue = 0
        numberPickerRight.maxValue = 90
        numberPickerRight.wrapSelectorWheel = false
        lifecycleScope.launch {
            settingsRepository.rightDegree.collect { degree -> numberPickerRight.value = degree }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                val valueLeft = numberPickerLeft.value
                val valueRight = numberPickerRight.value

                lifecycleScope.launch {
                    settingsRepository.setLeftDegree(valueLeft)
                }
                lifecycleScope.launch {
                    settingsRepository.setRightDegree(valueRight)
                }

                dialog.dismiss()
                requireActivity().hideSystemUI()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                requireActivity().hideSystemUI()
            }
            .create()

        dialog.setOnShowListener {
            requireActivity().hideSystemUI()
        }
        dialog.show()
    }
}