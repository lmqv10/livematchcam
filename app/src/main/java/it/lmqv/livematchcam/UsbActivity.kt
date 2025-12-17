package it.lmqv.livematchcam

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class UsbActivity : Activity() {

    companion object {
        const val ACTION_USB_GRANTED = "com.example.app.action.USB_PERMISSION_GRANTED"
        const val EXTRA_USB_DEVICE = "com.example.app.extra.USB_DEVICE"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var attempts = 0
    private val maxAttempts = 6

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Logd("UsbActivity::onCreate")
        @Suppress("DEPRECATION")
        val device = intent?.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
        //Logd("UsbActivity::device ${device}")
        checkPermissionAndFinish(device)
    }

    private fun checkPermissionAndFinish(device: UsbDevice?) {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        //Logd("UsbActivity::checkPermissionAndFinish ${device}")
        if (device == null) {
            //Logd("UsbActivity::checkPermissionAndFinish null Finish")
            finish()
            return
        }

        //Logd("UsbActivity::hasPermission ${device}")
        if (usbManager.hasPermission(device)) {
            //Logd("UsbActivity::notifyPermissionGranted ${device}")
            notifyPermissionGranted(device)
            //Logd("UsbActivity::notifyPermissionGranted finish")
            finish()
            return
        }

        if (attempts < maxAttempts) {
            attempts++
            //Logd("UsbActivity::new attempt ${attempts}")
            handler.postDelayed({ checkPermissionAndFinish(device) }, 200)
            return
        }

        //Logd("UsbActivity::finished attempts ${attempts}. Finish")
        finish()
    }

    private fun notifyPermissionGranted(device: UsbDevice) {
        //Logd("UsbActivity::notifyPermissionGranted ${device}")
        val broadcast = Intent(ACTION_USB_GRANTED).apply {
            putExtra(EXTRA_USB_DEVICE, device)
        }
        //Logd("UsbActivity::sendBroadcast ${device}")
        sendBroadcast(broadcast)
    }
}