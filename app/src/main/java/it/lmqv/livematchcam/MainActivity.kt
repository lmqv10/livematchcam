package it.lmqv.livematchcam


import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import it.lmqv.livematchcam.fragments.IServersFragment
import it.lmqv.livematchcam.fragments.ServersFragment
import it.lmqv.livematchcam.fragments.SportsFragment

class MainActivity : AppCompatActivity() {

    private val serverFragment : IServersFragment = ServersFragment.newInstance()
    private val sportsFragment : SportsFragment = SportsFragment.newInstance()

    private val permissions = mutableListOf(
        Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA,
    ).apply {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            this.add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayShowTitleEnabled(false) // Hide default title if needed
        }

        supportFragmentManager.beginTransaction()
            .add(R.id.serversContainer, serverFragment as Fragment).commit()

        supportFragmentManager.beginTransaction()
            .add(R.id.sportsContainer, sportsFragment).commit()

        transitionAnim(true)
        val tvVersion = findViewById<TextView>(R.id.tv_version)
        tvVersion.text = getString(R.string.version, BuildConfig.VERSION_NAME)

        val bActivityLive = this.findViewById<Button>(R.id.activity_Live)
        bActivityLive.setOnClickListener {
            startActivity(Intent(this, LiveStreamActivity::class.java))
        }

        requestPermissions()
    }

    @Suppress("DEPRECATION")
    private fun transitionAnim(isOpen: Boolean) {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val type = if (isOpen) OVERRIDE_TRANSITION_OPEN else OVERRIDE_TRANSITION_CLOSE
            overrideActivityTransition(type, R.anim.slide_in, R.anim.slide_out)
        } else {
            overridePendingTransition(R.anim.slide_in, R.anim.slide_out)
        }
    }

    private fun requestPermissions() {
        if (!hasPermissions(this)) {
            ActivityCompat.requestPermissions(this, permissions, 1)
        }
    }

    private fun hasPermissions(context: Context): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

}
