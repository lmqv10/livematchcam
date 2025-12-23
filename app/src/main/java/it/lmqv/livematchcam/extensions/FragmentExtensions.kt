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