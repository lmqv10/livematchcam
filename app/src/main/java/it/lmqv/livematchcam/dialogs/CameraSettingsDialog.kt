package it.lmqv.livematchcam.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.annotation.StringRes
import it.lmqv.livematchcam.databinding.DialogConfirmBinding
import it.lmqv.livematchcam.extensions.toOptionItems

class CameraSettingsDialog (
    context: Context,
    private val onConfirm : () -> Unit,
    private val onCancel: () -> Unit,
    @StringRes private val resMessageId: Int
) : Dialog(context) {
    private lateinit var binding: DialogConfirmBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DialogConfirmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.5).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        binding.dialogConfirmMessage.text = context.getString(resMessageId)

        binding.confirmButton.setOnClickListener {
            onConfirm()
            dismiss()
        }
        binding.cancelButton.setOnClickListener {
            onCancel()
            dismiss()
        }
    }

//    private fun changeVideoSettingsDialog() {
//        val inflater = LayoutInflater.from(this)
//        val dialogView = inflater.inflate(R.layout.dialog_camera_settings, null)
//
//        val spinnerVideoSource = dialogView.findViewById<Spinner>(R.id.video_source)
//        val optionsVideoSource = VideoSourceKind.entries
//
//        val adapter = ArrayAdapter(
//            this,
//            android.R.layout.simple_spinner_item,
//            optionsVideoSource
//        )
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        spinnerVideoSource.isEnabled = !this.streamService.isStreaming()
//        spinnerVideoSource.adapter = adapter
//        spinnerVideoSource.onItemSelectedListener =
//            object : AdapterView.OnItemSelectedListener {
//
//                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
//                    val selected = parent.getItemAtPosition(position) as VideoSourceKind
//
//                    val current = streamConfigurationViewModel.videoSourceKind.value
//                    if (current != selected) {
//                        streamConfigurationViewModel.setVideoSourceKind(selected)
//                    }
//                }
//
//                override fun onNothingSelected(parent: AdapterView<*>) {}
//            }
//
//        val defaultIndex = optionsVideoSource.indexOf(
//            streamConfigurationViewModel.videoSourceKind.value
//        )
//        spinnerVideoSource.setSelection(defaultIndex)
//
//        val optionsVideoResolutions = this.streamService
//            .getCameraResolutions().toOptionItems()
//
//        val spinnerVideoResolutions = dialogView.findViewById<Spinner>(R.id.video_resolutions)
////        val optionsVideoResolutions = listOf(
////            OptionItem(1080, "1920x1080p"),
////            OptionItem(720, "1280x720p")
////        )
//
//        val adapterVideoResolutions = ArrayAdapter(this, android.R.layout.simple_spinner_item, optionsVideoResolutions)
//        adapterVideoResolutions.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        spinnerVideoResolutions.isEnabled = !this.streamService.isStreaming()
//        spinnerVideoResolutions.adapter = adapterVideoResolutions
//        @Suppress("UNCHECKED_CAST")
//        spinnerVideoResolutions.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
//                val selectedItem = parent.getItemAtPosition(position) as OptionItem<Int>
//                var selectedItemValue = selectedItem.key
//                val height = this@StreamActivity.streamConfigurationViewModel.resolution.value
//                if (height != selectedItemValue) {
//                    this@StreamActivity.streamConfigurationViewModel.setResolution(selectedItemValue)
//                }
//            }
//            override fun onNothingSelected(parent: AdapterView<*>) { }
//        }
//        val defaultResolution = optionsVideoResolutions.indexOfFirst {
//            it.key == this@StreamActivity.streamConfigurationViewModel.resolution.value
//        }
//        spinnerVideoResolutions.setSelection(defaultResolution)
//
//        val spinnerVideoFps = dialogView.findViewById<Spinner>(R.id.video_fps)
//        val optionsVideoFps = listOf(
//            OptionItem(20, "20fps"),
//            OptionItem(25, "25fps"),
//            OptionItem(30, "30fps"),
//            OptionItem(60, "60fps")
//        )
//
//        val adapterVideoFps = ArrayAdapter(this, android.R.layout.simple_spinner_item, optionsVideoFps)
//        adapterVideoFps.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        spinnerVideoFps.isEnabled = !this.streamService.isStreaming()
//        spinnerVideoFps.adapter = adapterVideoFps
//        @Suppress("UNCHECKED_CAST")
//        spinnerVideoFps.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
//                val selectedItem = parent.getItemAtPosition(position) as OptionItem<Int>
//                var selectedItemValue = selectedItem.key
//                var fps = this@StreamActivity.streamConfigurationViewModel.fps.value
//                if (fps != selectedItemValue) {
//                    this@StreamActivity.streamConfigurationViewModel.setFps(selectedItemValue)
//                }
//            }
//            override fun onNothingSelected(parent: AdapterView<*>) { }
//        }
//        val defaultVideoFps = optionsVideoFps.indexOfFirst { it.key == this@StreamActivity.streamConfigurationViewModel.fps.value }
//        spinnerVideoFps.setSelection(defaultVideoFps)
//
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .setPositiveButton("OK") { dialog, _ ->
//                dialog.dismiss()
//                hideSystemUI()
//            }
//            .create()
//
//        dialog.setOnShowListener {
//            hideSystemUI()
//        }
//
//        dialog.show()
//    }
}