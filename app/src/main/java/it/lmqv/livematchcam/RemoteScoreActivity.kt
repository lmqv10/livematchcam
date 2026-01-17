package it.lmqv.livematchcam

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import it.lmqv.livematchcam.databinding.ActivityRemoteScoreBinding
import it.lmqv.livematchcam.factories.sports.SportsFactory
import it.lmqv.livematchcam.fragments.sports.BaseScoreBoardFragment
import it.lmqv.livematchcam.fragments.sports.IRemoteControlFragment
import it.lmqv.livematchcam.fragments.sports.IScoreBoardFragment
import it.lmqv.livematchcam.repositories.MatchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RemoteScoreActivity : BaseActivity(),
    IScoreBoardFragment.OnUpdateCallback {

    private lateinit var binding: ActivityRemoteScoreBinding
    private lateinit var remoteControlFragment: IRemoteControlFragment
    private lateinit var scoreBoardFragment: IScoreBoardFragment<BaseScoreBoardFragment>

    private lateinit var sportCollectJob : Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRemoteScoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

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