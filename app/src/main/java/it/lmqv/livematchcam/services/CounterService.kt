package it.lmqv.livematchcam.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Binder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CounterService : Service() {
    private val binder = LocalBinder()
    private var job: Job? = null
    private var secondsCount = 0
    private var isPaused = false
    
    private val _counterState = MutableStateFlow<CounterState>(CounterState.Stopped)
    val counterState = _counterState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)

    inner class LocalBinder : Binder() {
        fun getService(): CounterService = this@CounterService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun startCounter() {
        if (job?.isActive == true && !isPaused) return
        
        if (isPaused) {
            isPaused = false
            _counterState.value = CounterState.Running(secondsCount)
            return
        }
        
        secondsCount = 0
        _counterState.value = CounterState.Running(secondsCount)
        
        job = scope.launch {
            while (isActive) {
                if (!isPaused) {
                    secondsCount++
                    _counterState.value = CounterState.Running(secondsCount)
                    delay(1000)
                } else {
                    delay(100) // Small delay when paused to reduce CPU usage
                }
            }
        }
    }

    fun pauseCounter() {
        if (job?.isActive == true && !isPaused) {
            isPaused = true
            _counterState.value = CounterState.Paused(secondsCount)
        }
    }

    fun stopCounter() {
        job?.cancel()
        secondsCount = 0
        isPaused = false
        _counterState.value = CounterState.Stopped
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        job?.cancel()
    }

    sealed class CounterState {
        object Stopped : CounterState()
        data class Running(val seconds: Int) : CounterState()
        data class Paused(val seconds: Int) : CounterState()
    }
}