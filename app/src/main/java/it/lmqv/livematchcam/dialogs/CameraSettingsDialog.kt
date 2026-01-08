package it.lmqv.livematchcam.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import it.lmqv.livematchcam.CameraSourceAdapter
import it.lmqv.livematchcam.CameraSourceItem
import it.lmqv.livematchcam.CameraSourceItemsFactory
import it.lmqv.livematchcam.databinding.DialogCameraSettingsBinding
import it.lmqv.livematchcam.extensions.toOptionItems
import it.lmqv.livematchcam.factories.EncoderFpsItemsFactory
import it.lmqv.livematchcam.services.stream.IStreamService
import it.lmqv.livematchcam.services.stream.VideoCaptureFormat
import it.lmqv.livematchcam.utils.OptionItem
import it.lmqv.livematchcam.viewmodels.VideoSourceKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CameraSettingsDialog (
    context: Context,
    private val streamService: IStreamService,
    private val onChangeVideoSource : (videoSourceKind: VideoSourceKind) -> Unit,
    private val onChangeResolution: (videoCaptureFormat: VideoCaptureFormat) -> Unit,
    private val onChangeFps: (selectedFps: Int) -> Unit,
) : Dialog(context) {
    private val binding: DialogCameraSettingsBinding = DialogCameraSettingsBinding.inflate(LayoutInflater.from(context))
    private var selectedVideoCaptureFormat: VideoCaptureFormat? = null

    private val dialogScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        setContentView(binding.root)

        this.initVideoSourceSpinner()
        this.initEncoderFpsSpinner()

        setOnDismissListener {
            dialogScope.cancel()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.5).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

//        binding.cancelButton.setOnClickListener {
//            dismiss()
//        }

        dialogScope.launch {
            this@CameraSettingsDialog.streamService.videoCaptureFormats.collect { videoCaptureFormats ->
                var optionsVideoCaptureFormat = videoCaptureFormats.toOptionItems()
                val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, optionsVideoCaptureFormat)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.videoResolutions.adapter = adapter
                @Suppress("UNCHECKED_CAST")
                binding.videoResolutions.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        val selectedItem = parent.getItemAtPosition(position) as OptionItem<VideoCaptureFormat>
                        var selectedCameraSourceParameters = selectedItem.key
                        onChangeResolution(selectedCameraSourceParameters)
                    }
                    override fun onNothingSelected(parent: AdapterView<*>) { }
                }

                val defaultResolution = optionsVideoCaptureFormat.indexOfFirst {
                    it.key.width == this@CameraSettingsDialog.selectedVideoCaptureFormat?.width &&
                    it.key.height == this@CameraSettingsDialog.selectedVideoCaptureFormat?.height
                }

                binding.videoResolutions.setSelection(defaultResolution)
            }
        }
    }

    fun setEnabled(enabled: Boolean) : CameraSettingsDialog  {
        binding.videoResolutions.isEnabled = enabled
        binding.encoderFps.isEnabled = enabled
        return this
    }

    fun setVideoSource(videoSourceKind: VideoSourceKind) : CameraSettingsDialog {
        val selectedIndex = (binding.videoSource.adapter as CameraSourceAdapter)
            .getSelectedIndex(videoSourceKind)

        binding.videoSource.setSelection(selectedIndex)
        return this
    }

    fun setVideoCaptureFormat(videoCaptureFormat: VideoCaptureFormat?) : CameraSettingsDialog {
        this.selectedVideoCaptureFormat = videoCaptureFormat
        return this
    }

    fun setEncoderFps(fps: Int?) : CameraSettingsDialog {
        val defaultVideoFps = EncoderFpsItemsFactory.get()
            .indexOfFirst { it.key == fps }
        binding.encoderFps.setSelection(defaultVideoFps)
        return this
    }

    private fun initVideoSourceSpinner() {
        var cameraSourceItems = CameraSourceItemsFactory.get()
        binding.videoSource.adapter = CameraSourceAdapter(context, cameraSourceItems)
        binding.videoSource.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val selected = parent.getItemAtPosition(position) as CameraSourceItem
                    onChangeVideoSource(selected.videoSourceKind)
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    private fun initEncoderFpsSpinner() {

        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, EncoderFpsItemsFactory.get())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.encoderFps.adapter = adapter
        @Suppress("UNCHECKED_CAST")
        binding.encoderFps.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position) as OptionItem<Int>
                var selectedFps = selectedItem.key
                onChangeFps(selectedFps)

            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }

    }
}