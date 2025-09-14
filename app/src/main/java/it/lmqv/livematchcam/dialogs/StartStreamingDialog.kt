package it.lmqv.livematchcam.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import it.lmqv.livematchcam.databinding.DialogStartStreamingBinding

class StartStreamingDialog (
    context: Context,
    private val onConfirm : () -> Unit,
    private val onCancel: () -> Unit
) : Dialog(context) {
    private lateinit var binding: DialogStartStreamingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DialogStartStreamingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.5).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        binding.confirmButton.setOnClickListener {
            onConfirm()
            dismiss()
        }
        binding.cancelButton.setOnClickListener {
            onCancel()
            dismiss()
        }
    }
}