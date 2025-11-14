//package it.lmqv.livematchcam
//
//import android.content.pm.ActivityInfo
//import android.os.Build
//import android.os.Bundle
//import android.view.View
//import android.view.WindowInsets
//import android.view.WindowInsetsController
//import android.view.WindowManager
//import androidx.activity.viewModels
//import androidx.appcompat.app.AppCompatActivity
//import androidx.appcompat.widget.Toolbar
//import androidx.core.widget.TextViewCompat
//import androidx.lifecycle.Lifecycle
//import androidx.lifecycle.lifecycleScope
//import androidx.lifecycle.repeatOnLifecycle
//import it.lmqv.livematchcam.databinding.ActivityFirebaseAccountBinding
//import it.lmqv.livematchcam.extensions.hideKeyboard
//import it.lmqv.livematchcam.extensions.showEditStringDialog
//import it.lmqv.livematchcam.extensions.toast
//import it.lmqv.livematchcam.viewmodels.FirebaseAccountViewModel
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.combine
//import kotlinx.coroutines.launch
//
//class FirebaseAccountActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityFirebaseAccountBinding
//    private val firebaseAccountViewModel: FirebaseAccountViewModel by viewModels()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        binding = ActivityFirebaseAccountBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        //requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
//        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            @Suppress("DEPRECATION")
//            window.setDecorFitsSystemWindows(false)
//            window.insetsController?.let { controller ->
//                controller.hide(WindowInsets.Type.systemBars())
//                controller.systemBarsBehavior =
//                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//            }
//        } else {
//            @Suppress("DEPRECATION")
//            window.setFlags(
//                WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN
//            )
//
//            @Suppress("DEPRECATION")
//            window.decorView.systemUiVisibility = (
//                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                            or View.SYSTEM_UI_FLAG_FULLSCREEN
//                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                    )
//        }
//
//        val toolbar: Toolbar = findViewById(R.id.toolbar)
//        setSupportActionBar(toolbar)
//        if (supportActionBar != null) {
//            supportActionBar!!.setDisplayShowTitleEnabled(false) // Hide default title if needed
//        }
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.setHomeAsUpIndicator(R.drawable.arrow_left) // Opzionale: icona personalizzata
//
//        binding.accountName.setOnClickListener {
//            val sourceName = binding.accountName.text.toString()
//            showEditStringDialog(R.string.account, sourceName, arrayOf()) { updatedAccountName ->
//                binding.accountName.text = updatedAccountName
//
//                if (updatedAccountName.isNotEmpty()) {
//                    firebaseAccountViewModel.signIn(updatedAccountName)
//                    toast(getString(R.string.logged_in, updatedAccountName))
//                } else {
//                    firebaseAccountViewModel.signOut {
//                        CoroutineScope(Dispatchers.Main).launch {
//                            toast(getString(R.string.logged_out))
//                        }
//                    }
//                }
//
//                binding.accountName.hideKeyboard()
//            }
//        }
//
//        lifecycleScope.launch {
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                combine(
//                    firebaseAccountViewModel.authState,
//                    firebaseAccountViewModel.firebaseAccountKey
//                ) { state, accountKey -> Pair(state, accountKey) }
//                .collect { (state, accountKey) ->
//                    val accountName = firebaseAccountViewModel.accountName()
//
//                    binding.accountName.text = accountName ?: ""
//                    binding.accountKey.text = accountKey ?: ""
//
//                    val isConnected =
//                        !accountName.isNullOrEmpty() && !accountKey.isNullOrEmpty()
//
//                    val resIcon = if (isConnected) R.drawable.cloud_check else R.drawable.cloud_cross
//                    TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(binding.accountKey, resIcon, 0, 0, 0)
//                }
//            }
//        }
//    }
//
//    override fun onSupportNavigateUp(): Boolean {
//        finish()
//        return true
//    }
//}