package it.lmqv.livematchcam.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import it.lmqv.livematchcam.databinding.DialogDateTimePickerBinding
import java.util.*

class DateTimePickerDialog(
    context: Context,
    private val calendar: Calendar,
    private val onConfirm: (selectedDateTime: Calendar) -> Unit,
    private val onCancel: () -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogDateTimePickerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = DialogDateTimePickerBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        binding.timePicker.setIs24HourView(true)
        binding.datePicker.minDate = System.currentTimeMillis()

        binding.btnConfirm.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = binding.datePicker
            val timePicker = binding.timePicker

            calendar.set(
                datePicker.year,
                datePicker.month,
                datePicker.dayOfMonth,
                timePicker.hour,
                timePicker.minute
            )

            onConfirm(calendar)
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            onCancel()
            dismiss()
        }

        binding.datePicker.updateDate(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH))

        binding.timePicker.hour = calendar.get(Calendar.HOUR_OF_DAY)
        binding.timePicker.minute = calendar.get(Calendar.MINUTE)
    }

}
