package it.lmqv.livematchcam

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.AttributeSet
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
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
    private val sportsFragment : SportsFragment = SportsFragment.newInstance()

    private lateinit var binding: ActivityMainBinding
    private val googleViewModel: GoogleViewModel by viewModels()

    private val permissions = mutableListOf(
        Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA,
    ).apply {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            this.add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val GOOGLE_APIS_AUTH_YOUTUBE : String = "https://www.googleapis.com/auth/youtube"
    private val CLIENT_ID : String = "54641307189-6181k175ei3m80jnvot27qkfhfvmteqt.apps.googleusercontent.com"

    private val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(GOOGLE_APIS_AUTH_YOUTUBE))
        .requestServerAuthCode(CLIENT_ID, true)
        .build()

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if (task.isSuccessful) {
                val account = task.result
                googleViewModel.setAccount(account.account)
                //toast("sign in as ${account.account?.name}")
            } else {
                // Handle sign-in failure.
                //toast(task.exception?.message.toString())
            }
            invalidateOptionsMenu()
        } else {
            //toast("googleSignInLauncher::failed::${result.resultCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayShowTitleEnabled(false) // Hide default title if needed
        }

        supportFragmentManager.beginTransaction()
            .add(R.id.sportsContainer, sportsFragment).commit()

        transitionAnim(true)
        binding.tvVersion.text = getString(R.string.version, BuildConfig.VERSION_NAME)

        binding.activityLive.setOnClickListener {
            startActivity(Intent(this, LiveStreamActivity::class.java))
        }
        binding.activityYoutube.setOnClickListener {
            startActivity(Intent(this, YouTubeActivity::class.java))
        }

        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            googleViewModel.setAccount(account.account)
            //toast("sign in as ${account.account?.name}")
        }

        requestPermissions()
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
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
        return super.onCreateView(name, context, attrs)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        val signInItem = menu.findItem(R.id.menu_sign_in)
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
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val account = googleViewModel.account.value
        val isLoggedIn = account != null

        menu?.findItem(R.id.menu_sign_in)?.isVisible = !isLoggedIn
        menu?.findItem(R.id.menu_sign_out)?.isVisible = isLoggedIn
        menu?.findItem(R.id.menu_sign_out)
            ?.actionView?.findViewById<TextView>(R.id.menu_text)
            ?.text = account?.name ?: ""

        return super.onPrepareOptionsMenu(menu)
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
