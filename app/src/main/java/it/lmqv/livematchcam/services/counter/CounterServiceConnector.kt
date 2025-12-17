package it.lmqv.livematchcam.services.counter

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class CounterServiceConnector(context: Context) : CoroutineScope {

    override val coroutineContext: CoroutineContext =
        Dispatchers.Main.immediate + SupervisorJob()

    private var counterListener: ICounterListener? = null
    fun setCounterListener(counterListener: ICounterListener) {
        this.counterListener = counterListener
    }

    private var counterService: CounterService? = null
    private var isBound = false

    private val _counterState = MutableStateFlow<CounterService.CounterState>(CounterService.CounterState.Stopped)
    val counterState: StateFlow<CounterService.CounterState> = _counterState

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as CounterService.CounterBinder
            counterService = binder.getService()
            isBound = true

            launch {
                counterService?.counterState?.collectLatest {
                    _counterState.value = it

                    var seconds = 0
                    when (it) {
                        is CounterService.CounterState.Running -> seconds = it.seconds
                        is CounterService.CounterState.Paused -> seconds = it.seconds
                        is CounterService.CounterState.Stopped -> seconds = 0
                    }
                    this@CounterServiceConnector.counterListener?.onTick(seconds)
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            counterService = null
            isBound = false
        }
    }

    init {
        Intent(context, CounterService::class.java).also { intent ->
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    fun startCounter() {
        counterService?.startCounter()
    }

    fun pauseCounter() {
        counterService?.pauseCounter()
    }

    fun stopCounter() {
        counterService?.stopCounter()
    }

    fun setCounter(seconds: Int) {
        counterService?.setCounter(seconds)
    }

    fun isRunning() : Boolean {
        return counterService?.isRunning() ?: false
    }

    fun unbind(context: Context) {
        if (isBound) {
            context.unbindService(connection)
            isBound = false
        }
        cancel()
    }
}