package it.lmqv.livematchcam

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import it.lmqv.livematchcam.databinding.ActivityMainBinding
import it.lmqv.livematchcam.fragments.IServersFragment
import it.lmqv.livematchcam.fragments.ServersFragment
import it.lmqv.livematchcam.fragments.SportsFragment
import it.lmqv.livematchcam.fragments.YoutubeFragment
import it.lmqv.livematchcam.viewmodels.GoogleViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val serverFragment : IServersFragment = ServersFragment.newInstance()
    private val youtubeFragment : IServersFragment = YoutubeFragment.newInstance()
    //private val sportsFragment : SportsFragment = SportsFragment.newInstance()

    private lateinit var binding: ActivityMainBinding
    private val googleViewModel: GoogleViewModel by viewModels()
    //private val youtubeViewModel: YoutubeViewModel by viewModels()

    private val permissions = mutableListOf(
        Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA,
    ).apply {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            this.add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
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

        transitionAnim(true)
        binding.tvVersion.text = getString(R.string.version, BuildConfig.VERSION_NAME)

        binding.activityLive.setOnClickListener {
            //Logd("MainActivity::startActivity::LiveStreamActivity")
            //FirebaseDataManager.getInstance().removeMatchValueEventListener()
            startActivity(Intent(this, LiveStreamActivity::class.java))
        }

        /*binding.activityYoutube.setOnClickListener {
            startActivity(Intent(this, YouTubeActivity::class.java))
        }*/

        initializeData()

        requestPermissions()

        /*val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("teams")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    val team = userSnapshot.getValue(Team::class.java)
                    Logd("team:" + team!!.name)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Logd("Failed to read data" + error.toException())
            }
        })

        val firebaseApp = FirebaseApp.getInstance()
        val options: FirebaseOptions = firebaseApp.options

        val apiKey = options.apiKey
        val appId = options.applicationId
        val projectId = options.projectId
        val databaseUrl = options.databaseUrl
        */

        /*FirebaseDataManager.getInstance()
            .fetchAccountById("846af82686b3429a85e9a2d9a14ed79a") { account ->
            //.fetchAccountById("99261ce7d8b94fc395301f57d9e61ffd") { account ->
                if (account != null) {
                    Log.d("Firebase", "Account Name: ${account.name}")
                    Log.d("Firebase", "Admin: ${account.admin}")
                    Log.d("Firebase", "Channels: ${account.channels?.joinToString()}")

                    for (match in account.matches) {
                        var score = match.value.score
                        when (score) {
                            is FootballScore -> Log.d("Firebase", "Match: ${score.home}-${score.away} | ${score.period} | ${score.currentPeriodStartTimestamp}")
                            is VolleyScore -> {
                                var sets = score.sets
                                Log.d("Firebase", "CurrentSet: ${score.currentSet} - ${score.sets}")
                            }

                        }
                    }
                } else {
                    Log.e("Firebase", "Failed to fetch account.")
                }
            }
        */

    }

    override fun onStart() {
        super.onStart()

        supportFragmentManager.beginTransaction()
            .replace(R.id.sportsContainer, SportsFragment.newInstance()).commit()

        lifecycleScope.launch {
            googleViewModel.account.collect { account ->
                if (account != null) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.serversContainer, youtubeFragment as Fragment).commit()
                } else {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.serversContainer, serverFragment as Fragment).commit()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        val accountItem = menu.findItem(R.id.menu_account)
        val accountActionView = layoutInflater.inflate(R.layout.menu_item, null)
        accountItem?.setActionView(accountActionView)

        val accountItemText = accountActionView.findViewById<TextView>(R.id.menu_text);
        //accountItemText.text = getString(R.string.google_sign_in)
        accountItemText.setOnClickListener { _ ->
            startActivity(Intent(this@MainActivity, AccountActivity::class.java))
        }

        val accountItemIcon = accountActionView.findViewById<ImageView>(R.id.menu_icon)
        accountItemIcon.setImageResource(R.drawable.user_account)
        accountItemIcon.setOnClickListener { _ ->
            startActivity(Intent(this@MainActivity, AccountActivity::class.java))
        }

        val nearbyItem = menu.findItem(R.id.menu_nearby)
        val nearbyActionView = layoutInflater.inflate(R.layout.menu_item, null)
        nearbyItem?.setActionView(nearbyActionView)

        nearbyActionView
            .findViewById<TextView>(R.id.menu_text)
            .text = getString(R.string.google_nearby)

        val nearbyItemIcon = nearbyActionView.findViewById<ImageView>(R.id.menu_icon)
        nearbyItemIcon.setImageResource(R.drawable.google_nearby)
        nearbyItemIcon.setOnClickListener { _ ->
            startActivity(Intent(this@MainActivity, YouTubeActivity::class.java))
        }

        /*val signInItem = menu.findItem(R.id.menu_sign_in)
        val signInActionView = layoutInflater.inflate(R.layout.menu_item, null)
        signInItem?.isVisible = false
        signInItem?.setActionView(signInActionView)

        signInActionView
            .findViewById<TextView>(R.id.menu_text)
            .text = getString(R.string.google_sign_in)

        val signInIcon = signInActionView
            .findViewById<ImageView>(R.id.menu_icon)
        signInIcon.setImageResource(R.drawable.login)
        signInIcon.setOnClickListener { _ ->
            val googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        val signOutItem = menu.findItem(R.id.menu_sign_out)
        val signOutActionView = layoutInflater.inflate(R.layout.menu_item, null)
        signOutItem?.isVisible = false
        signOutItem?.setActionView(signOutActionView)

        signOutActionView
            .findViewById<TextView>(R.id.menu_text)
            .text = getString(R.string.sign_out)

        val signOutIcon = signOutActionView.findViewById<ImageView>(R.id.menu_icon)
        signOutIcon.setImageResource(R.drawable.logout)
        signOutIcon.setOnClickListener { _ ->
            val googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
            googleSignInClient.signOut().addOnCompleteListener {
                googleViewModel.setAccount(null)
                invalidateOptionsMenu()
            }
        }*/

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val account = googleViewModel.account.value
        menu?.findItem(R.id.menu_account)
            ?.actionView?.findViewById<TextView>(R.id.menu_text)
            ?.text = account?.name ?: getString(R.string.google_sign_in)

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onResume() {
        super.onResume()
        initializeData()
        invalidateOptionsMenu()
    }

    private fun initializeData() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            googleViewModel.setAccount(account.account)
            //toast("sign in as ${account.account?.name}")
        } else {
            googleViewModel.setAccount(null)
        }
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
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

}
