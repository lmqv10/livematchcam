package it.lmqv.livematchcam.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import it.lmqv.livematchcam.services.helpers.CounterServiceConnector

class CounterViewModel(application: Application) : AndroidViewModel(application) {
    private val serviceConnector = CounterServiceConnector(application)

    val counterState = serviceConnector.counterState

    fun isRunning() : Boolean = serviceConnector.isRunning()
    fun startCounter() = serviceConnector.startCounter()
    fun pauseCounter() = serviceConnector.pauseCounter()
    fun stopCounter() = serviceConnector.stopCounter()
    fun setCounter(seconds: Int) = serviceConnector.setCounter(seconds)

    override fun onCleared() {
        serviceConnector.unbind(getApplication())
    }
}