package it.lmqv.livematchcam

import android.Manifest.permission.CAMERA
import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import com.jiangdg.ausbc.utils.ToastUtils
import it.lmqv.livematchcam.fragments.UVCCameraFragment
import it.lmqv.livematchcam.handlers.offset.ManualZoomLevel

class UVCStreamActivity : AppCompatActivity() {

    private val cameraFragment = UVCCameraFragment.getInstance()
    //private lateinit var usbManager: UsbManager
    //private val ACTION_USB_PERMISSION = "it.lmqv.livematchcam.USB_PERMISSION"

    /*private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            device?.let {
                                openUsbDevice(it)
                            }
                        } else {
                            Toast.makeText(context, "Permesso USB negato", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }*/
    //private lateinit var usbReceiver: UsbPermissionReceiver


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_stream)
        //requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        supportActionBar?.hide()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            )
        }

        val hasCameraPermission = PermissionChecker.checkSelfPermission(this, CAMERA)
        val hasStoragePermission =
            PermissionChecker.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
        if (hasCameraPermission != PermissionChecker.PERMISSION_GRANTED || hasStoragePermission != PermissionChecker.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA)) {
                ToastUtils.show(R.string.permission_tip)
            }
            ActivityCompat.requestPermissions(
                this,
                arrayOf(CAMERA, WRITE_EXTERNAL_STORAGE, RECORD_AUDIO),
                REQUEST_CAMERA
            )
            return
        }

        /*usbReceiver = UsbPermissionReceiver()
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        val usbDevices = usbManager.deviceList
        if (usbDevices.isNotEmpty()) {
            for (device in usbDevices.values) {
                requestUsbPermission(device)
            }
        }*/

    }

    /*private fun requestUsbPermission(device: UsbDevice) {
        if (!usbManager.hasPermission(device)) {
            toast("Request permission on ${device.deviceName}")
            val permissionIntent = PendingIntent.getBroadcast(
                this,
                0,
                Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE)

            usbManager.requestPermission(device, permissionIntent)
        } else {
            toast("Already got permission on ${device.deviceName}")
            openUsbDevice(device)
        }
    }*/

    /*override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA -> {
                val hasCameraPermission = PermissionChecker.checkSelfPermission(this, CAMERA)
                if (hasCameraPermission == PermissionChecker.PERMISSION_DENIED) {
                    ToastUtils.show(R.string.permission_tip)
                    return
                }
            }
            REQUEST_STORAGE -> {
                val hasCameraPermission =
                    PermissionChecker.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
                if (hasCameraPermission == PermissionChecker.PERMISSION_DENIED) {
                    ToastUtils.show(R.string.permission_tip)
                    return
                }
            }
            else -> {
            }
        }
    }*/

    /*private fun openUsbDevice(device: UsbDevice) {
        toast("openUsbDevice ${device.deviceName}")
        val usbInterface = device.getInterface(0)
        val usbEndpoint = usbInterface.getEndpoint(0) // Leggi/scrivi dati

        val connection = usbManager.openDevice(device)
        connection?.apply {
            claimInterface(usbInterface, true)
            // Esegui operazioni di lettura/scrittura
            releaseInterface(usbInterface)
            close()
        }
    }*/

    /*override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            0 -> {
                val hasCameraPermission = PermissionChecker.checkSelfPermission(this, CAMERA)
                if (hasCameraPermission == PermissionChecker.PERMISSION_DENIED) {
                    toast("You have already denied permission access. Go to the Settings page to turn on permissions 2")
                    return
                } else {
                    toast("GRANTED 2")
                }
            }
            else -> {
            }
        }
    }*/

    override fun onStart() {
        super.onStart()

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, cameraFragment)
            .commit()
    }


    /*override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ -> finish() }
            .setNegativeButton("No", null)
            .show()
    }*/

    companion object {
        private const val REQUEST_CAMERA = 0
        private const val REQUEST_STORAGE = 1
    }
}