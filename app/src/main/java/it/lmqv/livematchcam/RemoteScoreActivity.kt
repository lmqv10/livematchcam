package it.lmqv.livematchcam

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import it.lmqv.livematchcam.databinding.ActivityRemoteScoreBinding
import it.lmqv.livematchcam.factories.SportsFactory
import it.lmqv.livematchcam.fragments.IRemoteControlFragment
import it.lmqv.livematchcam.fragments.IScoreBoardFragment
import it.lmqv.livematchcam.repositories.MatchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RemoteScoreActivity : AppCompatActivity(),
    IScoreBoardFragment.OnUpdateCallback {

    private lateinit var binding: ActivityRemoteScoreBinding
    private lateinit var remoteControlFragment: IRemoteControlFragment
    private lateinit var scoreBoardFragment: IScoreBoardFragment

    private lateinit var sportCollectJob : Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRemoteScoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayShowTitleEnabled(false) // Hide default title if needed
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.arrow_left) // Opzionale: icona personalizzata
    }

    override fun onStart() {
        super.onStart()

        this.sportCollectJob = lifecycleScope.launch {
            MatchRepository.sport.collectLatest { sport ->
                val sportFragmentFactory = SportsFactory.get(sport)
                remoteControlFragment = sportFragmentFactory.getRemoteControl()
                scoreBoardFragment = sportFragmentFactory.getScoreBoard()
                scoreBoardFragment.setOnUpdate(this@RemoteScoreActivity)

                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.score_board_placeholder, scoreBoardFragment as Fragment, "ScoreBoardFragmentTag")
                    .commit()

                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.container, remoteControlFragment as Fragment)
                    .commit()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        this.sportCollectJob.cancel()
    }

    override fun refresh() { }
}