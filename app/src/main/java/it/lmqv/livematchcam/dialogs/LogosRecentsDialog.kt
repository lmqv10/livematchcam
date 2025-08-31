package it.lmqv.livematchcam.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import it.lmqv.livematchcam.adapters.LogoItem
import it.lmqv.livematchcam.adapters.LogosAdapter
import it.lmqv.livematchcam.databinding.DialogLogosRecentsBinding
import it.lmqv.livematchcam.utils.KeyDescription
import it.lmqv.livematchcam.preferences.RecentsLogosPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class LogosRecentsDialog constructor(
    context: Context,
    private val currentLogoURL: String,
    private val onInputConfirmed: (String) -> Unit
) : Dialog(context) {
    private var recentLogosManager = RecentsLogosPreferences(context)

    private lateinit var binding: DialogLogosRecentsBinding
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DialogLogosRecentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.confirmButton.setOnClickListener {
            val input = binding.editTextInput.text.toString().trim()
            if (input.isNotEmpty()) {
                recentLogosManager.saveRecent(KeyDescription<String>(input, input))
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

        val items = recentLogosManager.getRecents()
        var logosItems = items.map { x -> LogoItem(x.key) }
        val logosAdapter = LogosAdapter(context, logosItems)
        binding.recentsList.adapter = logosAdapter
        binding.recentsList.setOnItemClickListener { _, _, pos, _ ->
            val selectedItem = logosAdapter.getItem(pos)
            //binding.editTextInput.setText(selectedItem.imageURL)
            onInputConfirmed(selectedItem.imageURL)
            dismiss()
        }

        //binding.editTextInput.requestFocus()
        //binding.editTextInput.setSelection(binding.editTextInput.text.length)
        //window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    override fun dismiss() {
        super.dismiss()
        scope.cancel()
    }
}