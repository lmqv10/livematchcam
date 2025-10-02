package it.lmqv.livematchcam.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.annotation.StringRes
import it.lmqv.livematchcam.databinding.DialogConfirmBinding
import it.lmqv.livematchcam.databinding.DialogStartStreamingBinding

class ConfirmDialog (
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
}