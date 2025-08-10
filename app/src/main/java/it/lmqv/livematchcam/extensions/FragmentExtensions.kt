package it.lmqv.livematchcam.extensions

import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

const val DEBUG: Boolean = true

fun Loge(message: String) {
    if (DEBUG) {
        Log.e("LMCAM", message)
    }
}

fun Logd(message: String) {
    if (DEBUG) {
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

fun Fragment.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(requireContext(), message, duration).show()
}

