package it.lmqv.livematchcam

import android.os.Bundle
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

class AccountActivity : BaseActivity() {

    private lateinit var binding: ActivityAccountBinding

    private val firebaseAccountFragment = FirebaseAccountFragment.newInstance()
    private val youtubeAccountFragment = YoutubeAccountFragment.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                    val settings = firebaseAccountData.settings
                    val transaction =  supportFragmentManager.beginTransaction()
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