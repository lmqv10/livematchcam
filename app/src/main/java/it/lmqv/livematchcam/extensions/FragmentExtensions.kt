package it.lmqv.livematchcam.extensions

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import it.lmqv.livematchcam.BuildConfig
import kotlinx.coroutines.launch

fun Loge(message: String) {
    if (BuildConfig.DEBUG) {
        Log.e("LMCAM", message)
    }
}

fun Logd(message: String) {
    if (BuildConfig.DEBUG) {
        Log.d("LMCAM", message)
    }
}

fun Fragment.launchOnStarted(delegate: suspend () -> Unit) {
    this.lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            delegate()
        }
    }
}
fun Fragment.launchOnCreated(delegate: suspend () -> Unit) {
    this.lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
            delegate()
        }
    }
}
fun Fragment.launchOnResumed(delegate: suspend () -> Unit) {
    this.lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delegate()
        }
    }
}

//        fragment.showEditStringDialogForView(
//            view = view,
//            title = R.string.team_name,
//            initialValue = initialValue,
//            sport = sport
//        ) { updatedTeamName ->
//
//        }

//fun Fragment.showEditStringDialogForView(
//    view: View,
//    @StringRes title: Int,
//    initialValue: String,
//    sport: Sports? = null,
//    onResult: (String) -> Unit
//) {
//    val requestKey = "edit_dialog_${view.id}_${System.identityHashCode(view)}"
//
//    childFragmentManager.setFragmentResultListener(requestKey, viewLifecycleOwner)
//    { _, bundle ->
//        val result = bundle.getString(EditStringDialogFragment.RESULT_VALUE) ?: return@setFragmentResultListener
//        onResult(result)
//    }
//
//    EditStringDialogFragment.newInstance(
//        title = title,
//        initialValue = initialValue,
//        sport = sport,
//        requestKey = requestKey
//    ).show(childFragmentManager, "EditDialog_$requestKey")
//}