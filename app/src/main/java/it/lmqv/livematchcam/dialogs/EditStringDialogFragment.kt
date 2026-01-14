package it.lmqv.livematchcam.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.text.InputFilter
import android.view.View.OnFocusChangeListener
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import it.lmqv.livematchcam.databinding.DialogFragmentEditTextBinding
import it.lmqv.livematchcam.factories.TeamNameInputFiltersFactory
import it.lmqv.livematchcam.handlers.EditStringDialogContext

class EditStringDialogFragment : DialogFragment() {

    private var _binding: DialogFragmentEditTextBinding? = null
    private val binding get() = _binding!!

    private lateinit var editStringDialogContext: EditStringDialogContext
    private var filters: Array<InputFilter> = arrayOf()

    companion object {
        private const val ARG_DIALOG_CONTEXT = "dialog_context"
        const val RESULT_VALUE = "result_value"

        fun newInstance(
            editStringDialogContext: EditStringDialogContext
        ): EditStringDialogFragment {
            return EditStringDialogFragment().apply {
               arguments = Bundle().apply {
                    putParcelable(ARG_DIALOG_CONTEXT, editStringDialogContext)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let { args ->
            this.editStringDialogContext = args.getParcelable<EditStringDialogContext>(ARG_DIALOG_CONTEXT)!!

            var sport = editStringDialogContext.sport
            this.filters = if (sport != null) {
                TeamNameInputFiltersFactory.get(sport)
            } else {
                arrayOf()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFragmentEditTextBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        setupViews()
        showKeyboard()
    }

    private fun setupViews() {

        with(binding) {
            tvTitle.text = getString(editStringDialogContext.resTitleId)

            with(etInput) {
                setText(editStringDialogContext.initialValue)
                setSelection(editStringDialogContext.initialValue.length)
                onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        post(Runnable { selectAll() })
                    }
                }
                filters = this@EditStringDialogFragment.filters
                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        saveAndDismiss()
                        true
                    } else {
                        false
                    }
                }
            }

            cancelButton.setOnClickListener {
                dismiss()
            }
            confirmButton.setOnClickListener {
                saveAndDismiss()
            }
        }
    }

    private fun saveAndDismiss() {
        val newValue = binding.etInput.text.toString().trim()
        parentFragmentManager.setFragmentResult(editStringDialogContext.requestKey, bundleOf(RESULT_VALUE to newValue))
        dismiss()
    }

    private fun showKeyboard() {
        binding.etInput.requestFocus()
        binding.etInput.postDelayed({
            val imm = getSystemService(requireContext(), InputMethodManager::class.java)
            imm?.showSoftInput(binding.etInput, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.etInput.windowToken, 0)
    }

    override fun onDestroyView() {
        hideKeyboard()
        super.onDestroyView()
        _binding = null
    }
}
