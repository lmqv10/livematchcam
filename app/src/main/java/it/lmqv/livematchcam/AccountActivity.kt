package it.lmqv.livematchcam

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import it.lmqv.livematchcam.databinding.ActivityAccountBinding
import it.lmqv.livematchcam.viewmodels.GoogleViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountBinding
    private val googleViewModel: GoogleViewModel by viewModels()

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
            }
            invalidateOptionsMenu()
        } else {
            //toast("googleSignInLauncher::failed::${result.resultCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAccountBinding.inflate(layoutInflater)
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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.arrow_left) // Opzionale: icona personalizzata

        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            googleViewModel.setAccount(account.account)
        }

        binding.googleSignIn.setOnClickListener { _ ->
            val googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        binding.googleSignOut.setOnClickListener { _ ->
            val googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
            googleSignInClient.signOut().addOnCompleteListener {
                googleViewModel.setAccount(null)
                googleViewModel.setAccountKey(null)
            }
        }

        binding.connect.setOnClickListener { _ ->
            val accountKey = binding.accountKey.text.toString()
            googleViewModel.setAccountKey(accountKey)
        }

        binding.disconnect.setOnClickListener { _ ->
            googleViewModel.setAccountKey(null)
            binding.connect.isVisible = true
            binding.disconnect.isVisible = false
            //binding.accountKey.isEnabled = true
        }
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch {
            combine(
                googleViewModel.account,
                googleViewModel.firebaseAccountKey)
            { account, accountKey -> Pair(account, accountKey) }
            .collect { (account, accountKey) ->

                val isLogged = account != null;
                var accountName = account?.name
                binding.accountName.text = accountName ?: getString(R.string.google_sign_in);
                binding.googleSignIn.isVisible = !isLogged
                binding.googleSignOut.isVisible = isLogged
                binding.authorizedAccount.isVisible = isLogged

                binding.accountKey.text = Editable.Factory.getInstance().newEditable(accountKey ?: "")

                val isConnected = !accountName.isNullOrEmpty() && !accountKey.isNullOrEmpty()
                binding.connect.isVisible = !isConnected
                binding.disconnect.isVisible = isConnected
                //binding.accountKey.isEnabled = !isConnected
            }
        }

        /*lifecycleScope.launch {
            googleViewModel.account.collect { account ->
                val isLogged = account != null;
                binding.accountName.text = account?.name ?: getString(R.string.google_sign_in);
                binding.googleSignIn.isVisible = !isLogged
                binding.googleSignOut.isVisible = isLogged
                binding.authorizedAccount.isVisible = isLogged
            }
        }*/

        /*lifecycleScope.launch {
            googleViewModel.firebaseAccount.collect { firebaseAccount ->
                val accountName = firebaseAccount.accountName
                val accountKey = firebaseAccount.accountKey

                binding.accountKey.text = Editable.Factory.getInstance().newEditable(accountKey ?: "")

                val isConnected = !accountName.isNullOrEmpty() && !accountKey.isNullOrEmpty()
                Logd("AccountActivity. firebaseAccount $firebaseAccount");
                Logd("AccountActivity. connectedToAccount: $isConnected");
                binding.connect.isVisible = !isConnected
                binding.disconnect.isVisible = isConnected
                binding.accountKey.isEnabled = !isConnected

                /*FirebaseDataManager.getInstance()
                    .authenticateAccount(accountName, accountKey, { account ->
                        Logd("Account Name: ${account.name}")
                        Logd("Admin: ${account.admin}")
                        binding.connect.isVisible = false
                        binding.disconnect.isVisible = true
                        binding.accountKey.isEnabled = false
                    },{
                        Log.e("Firebase", "Failed to fetch account.")
                    })
                }*/
            }
        }*/
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}