package it.lmqv.livematchcam.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import it.lmqv.livematchcam.databinding.DialogTimePickerBinding

class TimePickerDialog(
    context: Context,
    private val seconds: Int = 0,
    private val onConfirm: (seconds: Int) -> Unit,
    private val onCancel: () -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogTimePickerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = DialogTimePickerBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        binding.minutePicker.minValue = 0
        binding.minutePicker.maxValue = 120

        binding.secondPicker.minValue = 0
        binding.secondPicker.maxValue = 59

        binding.minutePicker.value = seconds / 60
        binding.secondPicker.value = seconds % 60

        binding.timeReset.setOnClickListener {
            binding.minutePicker.value = 0
            binding.secondPicker.value = 0
        }

        binding.btnConfirm.setOnClickListener {
            var seconds = (binding.minutePicker.value * 60) + binding.secondPicker.value
            onConfirm(seconds)
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            onCancel()
            dismiss()
        }
    }
}
