package it.lmqv.livematchcam.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Debouncer(private val timeoutMillis: Long) {
    private var job: Job? = null

    fun submit(action: () -> Unit) {
        job?.cancel()
        job = CoroutineScope(Dispatchers.Main).launch {
            delay(timeoutMillis)
            action()
        }
    }
}
