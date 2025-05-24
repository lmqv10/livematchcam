package it.lmqv.livematchcam.services.helpers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import it.lmqv.livematchcam.services.CounterService
import kotlinx.coroutines.flow.StateFlow

class CounterServiceConnector(context: Context) {
    private var counterService: CounterService? = null
    private var isBound = false
    
    val counterState: StateFlow<CounterService.CounterState>?
        get() = counterService?.counterState

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as CounterService.LocalBinder
            counterService = binder.getService()
            isBound = true
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

    fun unbind(context: Context) {
        if (isBound) {
            context.unbindService(connection)
            isBound = false
        }
    }
}