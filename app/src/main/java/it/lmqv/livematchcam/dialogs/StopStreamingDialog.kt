package it.lmqv.livematchcam.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import it.lmqv.livematchcam.databinding.DialogStopStreamingBinding

class StopStreamingDialog (
    context: Context,
    private val onConfirm : (Boolean) -> Unit,
    private val onCancel: () -> Unit
) : Dialog(context) {
    private lateinit var binding: DialogStopStreamingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DialogStopStreamingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.5).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        binding.confirmButton.setOnClickListener {
            var shouldEndStream = binding.endStream.isChecked
            onConfirm(shouldEndStream)
            dismiss()
        }
        binding.cancelButton.setOnClickListener {
            onCancel
            dismiss()
        }

        binding.endStream.isChecked = false
    }
}