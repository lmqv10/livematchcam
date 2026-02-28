package it.lmqv.livematchcam.handlers

import android.os.Parcelable
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import it.lmqv.livematchcam.dialogs.EditStringDialogFragment
import it.lmqv.livematchcam.factories.sports.Sports
import kotlinx.parcelize.Parcelize

data class DialogContext(
    val fragment: Fragment,
    val bindingView: View,
    @StringRes val resTitleId: Int,
    val initialValue: String,
    val sports: Sports? = null
)

@Parcelize
data class EditStringDialogContext(
    @StringRes val resTitleId: Int,
    val initialValue: String,
    val requestKey: String,
    val sport: Sports?
): Parcelable

object DialogHandler {
    fun editText(context: DialogContext, onResult: (String) -> Unit) {
        with(context.fragment) {
            var bindingView = context.bindingView
            val requestKey = "edit_text_request_key_${bindingView.id}_${System.identityHashCode(bindingView)}"

            val lifecycleOwner = if (view != null) viewLifecycleOwner else this
            childFragmentManager.setFragmentResultListener(requestKey, lifecycleOwner)
            { _, bundle ->
                val result = bundle.getString(EditStringDialogFragment.RESULT_VALUE) ?: return@setFragmentResultListener
                onResult(result)
            }

            EditStringDialogFragment
                .newInstance(EditStringDialogContext(
                    resTitleId = context.resTitleId,
                    initialValue = context.initialValue,
                    requestKey = requestKey,
                    sport = context.sports
                ))
                .show(childFragmentManager, "Dialog_$requestKey")
        }
    }
}