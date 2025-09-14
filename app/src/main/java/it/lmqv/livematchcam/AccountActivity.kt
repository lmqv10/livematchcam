package it.lmqv.livematchcam

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
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import it.lmqv.livematchcam.databinding.ActivityAccountBinding
import it.lmqv.livematchcam.extensions.hideKeyboard
import it.lmqv.livematchcam.extensions.showEditStringDialog
import it.lmqv.livematchcam.viewmodels.AccountViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountBinding
    private val accountViewModel: AccountViewModel by viewModels()

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            accountViewModel.handleSignInResult(result.data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
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

        binding.googleSignIn.setOnClickListener { _ ->
            signInLauncher.launch(accountViewModel.getSignInIntent())
        }

        binding.googleSignOut.setOnClickListener { _ ->
            accountViewModel.signOut()
        }

        binding.accountKey.setOnClickListener {
            val sourceKey = binding.accountKey.text.toString()
            showEditStringDialog(R.string.account_key, sourceKey, arrayOf()) { updatedAccountKey ->
                binding.accountKey.text = updatedAccountKey
                accountViewModel.setAccountKey(updatedAccountKey)
                binding.accountKey.hideKeyboard()
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    accountViewModel.authState,
                    accountViewModel.firebaseAccountKey
                ) { state, accountKey -> Pair(state, accountKey) }
                .collect { (state, accountKey) ->

                    val isLogged = accountViewModel.isLogged()
                    val accountDesc = accountViewModel.accountDesc()

                    binding.accountName.text = accountDesc
                    binding.googleSignIn.isVisible = !isLogged
                    binding.googleSignOut.isVisible = isLogged
                    binding.authorizedAccount.isVisible = isLogged

                    binding.accountKey.text = accountKey ?: ""

                    val isConnected =
                        !accountDesc.isNullOrEmpty() && !accountKey.isNullOrEmpty()

                    val resIcon = if (isConnected) R.drawable.cloud_check else R.drawable.cloud_cross
                    TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(binding.accountKey, resIcon, 0, 0, 0)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}