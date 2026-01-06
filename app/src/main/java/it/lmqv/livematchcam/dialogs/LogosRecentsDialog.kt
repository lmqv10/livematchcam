package it.lmqv.livematchcam.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.adapters.LogosAdapter
import it.lmqv.livematchcam.converters.toLogoItems
import it.lmqv.livematchcam.databinding.DialogLogosRecentsBinding
import it.lmqv.livematchcam.utils.OptionItem
import it.lmqv.livematchcam.preferences.RecentsLogosPreferences

class LogosRecentsDialog (
    context: Context,
    private val currentLogoURL: String,
    private val onInputConfirmed: (String) -> Unit
) : Dialog(context) {
    private var recentLogosManager = RecentsLogosPreferences(context)

    private lateinit var binding: DialogLogosRecentsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DialogLogosRecentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.confirmButton.setOnClickListener {
            val input = binding.editTextInput.text.toString().trim()
            if (input.isNotEmpty()) {
                recentLogosManager.saveRecent(OptionItem<String>(input, input))
            }
            onInputConfirmed(input)
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.editTextInput.windowToken, 0)
            dismiss()
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.clearButton.setOnClickListener {
            binding.editTextInput.setText("")
            onInputConfirmed("")
            dismiss()
        }

        binding.editTextInput.setText(currentLogoURL)

        val items = recentLogosManager.getRecents().toLogoItems()
        binding.recentsList.adapter = LogosAdapter(context, items,
            { selectedItem ->
                onInputConfirmed(selectedItem.imageURL)
                dismiss()
            },
            { adapter, removeItem ->
                var dialog = ConfirmDialog(
                    context,
                    {
                        var items =
                            recentLogosManager.removeRecent(removeItem.imageURL).toLogoItems()
                        adapter.updateItems(items)
                    }, { }, R.string.confirm_delete_item
                )
                dialog.show()
            })
    }
}