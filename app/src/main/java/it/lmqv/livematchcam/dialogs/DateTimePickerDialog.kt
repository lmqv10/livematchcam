package it.lmqv.livematchcam.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Window
import it.lmqv.livematchcam.databinding.DialogDateTimePickerBinding
import java.time.ZoneId
import java.time.ZonedDateTime

class DateTimePickerDialog(
    context: Context,
    private val onConfirm: (selectedDateTime: ZonedDateTime) -> Unit,
    private val onCancel: () -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogDateTimePickerBinding
    private var dateTime: ZonedDateTime? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = DialogDateTimePickerBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)

        binding.btnConfirm.setOnClickListener {
            val datePicker = binding.datePicker
            val timePicker = binding.timePicker

            val pickedDateTime = ZonedDateTime.of(
                datePicker.year,
                datePicker.month + 1,
                datePicker.dayOfMonth,
                timePicker.hour,
                timePicker.minute,
                0,
                0,
                ZoneId.systemDefault()
            )
            onConfirm(pickedDateTime)
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            onCancel()
            dismiss()
        }


        binding.timePicker.setIs24HourView(true)
        var initialDateTime = this.dateTime ?: ZonedDateTime.now()

        if (initialDateTime.isBefore(ZonedDateTime.now())) {
            initialDateTime = ZonedDateTime.now()
        }

        binding.datePicker.updateDate(
            initialDateTime.year,
            initialDateTime.monthValue - 1,
            initialDateTime.dayOfMonth)

        binding.timePicker.hour = initialDateTime.hour
        binding.timePicker.minute = initialDateTime.minute
        binding.datePicker.minDate = initialDateTime.toInstant().toEpochMilli()
    }

    fun setDate(dateTime: ZonedDateTime) {
        if (dateTime.isAfter(ZonedDateTime.now())) {
            this.dateTime = dateTime
        } else {
            this.dateTime = ZonedDateTime.now()
        }
    }
}
