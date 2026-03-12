package it.lmqv.livematchcam.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import it.lmqv.livematchcam.R
import it.lmqv.livematchcam.adapters.ImageResourcesAdapter
import it.lmqv.livematchcam.converters.toImageItems
import it.lmqv.livematchcam.databinding.DialogLogosRecentsBinding
import it.lmqv.livematchcam.utils.OptionItem
import it.lmqv.livematchcam.preferences.RecentsPreferences

class RecentsDialog(
    context: Context,
    private val currentURL: String,
    private val recentsPreferences: RecentsPreferences,
    @StringRes private val titleResId: Int = R.string.choose_logo,
    @StringRes private val hintResId: Int = R.string.logo_url_placeholder,
    @DrawableRes private val placeholderResId: Int = R.drawable.shield,
    private val onInputConfirmed: (String) -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogLogosRecentsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DialogLogosRecentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Titolo e hint parametrici
        binding.logoUrlLabel.setText(titleResId)
        binding.editTextInput.setHint(hintResId)

        binding.confirmButton.setOnClickListener {
            val input = binding.editTextInput.text.toString().trim()
            if (input.isNotEmpty()) {
                recentsPreferences.saveRecent(OptionItem<String>(input, input))
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

        binding.editTextInput.setText(currentURL)

        val items = recentsPreferences.getRecents().toImageItems()
        binding.recentsList.adapter = ImageResourcesAdapter(context, items,
            { selectedItem ->
                onInputConfirmed(selectedItem.url)
                dismiss()
            },
            { adapter, removeItem ->
                var dialog = ConfirmDialog(
                    context,
                    {
                        var items =
                            recentsPreferences.removeRecent(removeItem.url).toImageItems()
                        adapter.updateItems(items)
                    }, { }, R.string.confirm_delete_item
                )
                dialog.show()
            },
            placeholderResId)
    }
}
