package it.lmqv.livematchcam

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import it.lmqv.livematchcam.databinding.ActivityAccountBinding
import it.lmqv.livematchcam.fragments.firebase.FirebaseAccountFragment
import it.lmqv.livematchcam.fragments.youtube.YoutubeAccountFragment
import it.lmqv.livematchcam.repositories.MatchRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountBinding

    private val firebaseAccountFragment = FirebaseAccountFragment.newInstance()
    private val youtubeAccountFragment = YoutubeAccountFragment.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayShowTitleEnabled(false) // Hide default title if needed
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.arrow_left) // Opzionale: icona personalizzata

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.firebaseAccountContainer, firebaseAccountFragment)
            .replace(R.id.youtubeAccountContainer, youtubeAccountFragment)
            .hide(youtubeAccountFragment)
            .commit()

         lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                MatchRepository.firebaseAccountData.collectLatest { firebaseAccountData ->
                    var settings = firebaseAccountData.settings
                    var transaction =  supportFragmentManager.beginTransaction()
                    if (settings.youTubeEnabled && firebaseAccountData.guid.isNotEmpty()) {
                        transaction.show(youtubeAccountFragment)
                    } else {
                        transaction.hide(youtubeAccountFragment)
                    }
                    transaction.commit()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}