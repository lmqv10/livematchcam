package it.lmqv.livematchcam.services.stream

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import it.lmqv.livematchcam.extensions.Logd

class StreamServiceConnector(val activityContext: Activity) : ServiceConnection {

    private var streamService: IStreamService? = null
    private var bound = false

    private var onServiceConnected : (IStreamService) -> Unit = {}
    fun setOnServiceConnected(callback: (IStreamService) -> Unit) {
        this.onServiceConnected = callback
    }

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        Logd("StreamServiceConnector :: onServiceConnected")
        val localBinder = binder as StreamService.LocalBinder
        streamService = StreamServiceProxy(activityContext, localBinder.service)
        this.onServiceConnected(streamService!!)
        bound = true
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Logd("StreamServiceConnector :: onServiceDisconnected")
        streamService?.stopPreview()
        bound = false
        streamService = null
    }

    fun bindService() {
        Logd("StreamServiceConnector :: bindService")
        val intent = Intent(activityContext, StreamService::class.java)
        ContextCompat.startForegroundService(activityContext, intent)
        activityContext.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    fun unbindService() {
        Logd("StreamServiceConnector :: stopPreview")
        streamService?.stopPreview()

        if (bound) {
            Logd("StreamServiceConnector :: unbindService")
            activityContext.unbindService(this)
            bound = false
        }
    }

    fun stopService() {
        Logd("StreamServiceConnector :: stopService")
        val intent = Intent(activityContext, StreamService::class.java)
        activityContext.stopService(intent)
    }
}