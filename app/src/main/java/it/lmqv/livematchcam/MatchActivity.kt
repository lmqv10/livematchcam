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
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import it.lmqv.livematchcam.databinding.ActivityMatchBinding
import it.lmqv.livematchcam.extensions.dpToPx
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.services.CounterService
import it.lmqv.livematchcam.services.youtube.YouTubeClientProvider
import it.lmqv.livematchcam.viewmodels.AccountViewModel
import it.lmqv.livematchcam.viewmodels.FloatingActionsViewModel
import it.lmqv.livematchcam.views.AnimateImageVIew
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.getValue

interface INavigateDrawerActivity {
    fun navigateAsDrawerSelection(@IdRes destinationId: Int)
    fun navigateAsStartActivity(activityClass: Class<out Activity>)
}

class MatchActivity : AppCompatActivity(), INavigateDrawerActivity {

    private val accountViewModel: AccountViewModel by viewModels()
    private val floatingActionsViewModel: FloatingActionsViewModel by viewModels()

    private lateinit var binding: ActivityMatchBinding

    private lateinit var headerView: View
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController

    private val topLevelsFragments = setOf(
        R.id.serverConfigurationFragment,
        R.id.youtubeConfigurationFragment,
        R.id.youtubeStreamFragment
    )

    private val permissions = mutableListOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    ).apply {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            this.add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMatchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
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

        setSupportActionBar(binding.customToolbar.toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayShowTitleEnabled(false) // Hide default title if needed
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            topLevelsFragments,
            binding.matchDrawerLayout
        )

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(binding.matchNavView, navController)

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val currentId = navController.currentDestination?.id
                    if (currentId in topLevelsFragments) {
                        finish()
                    } else {
                        navController.popBackStack()
                    }
                }
            }
        )

        if (Build.VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.slide_in, R.anim.slide_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in, R.anim.slide_out)
        }

        binding.tvVersion.text = getString(R.string.version, BuildConfig.VERSION_NAME)

        headerView = binding.matchNavView.getHeaderView(0)
        headerView.setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
            binding.matchDrawerLayout.closeDrawers()
        }

        requestPermissions()

        startService(Intent(this, CounterService::class.java))

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                floatingActionsViewModel.actions.collect { actions ->
                    var actionsContainer = binding.floatingActionContainer
                    actionsContainer.removeAllViews()
                    if (actions.isNotEmpty()) {
                        actionsContainer.visibility = View.VISIBLE

                        actions.forEachIndexed { index, action ->
                            val imageView = AnimateImageVIew(
                                ContextThemeWrapper(this@MatchActivity, R.style.AppTheme),
                                null, 0, R.style.defaultImageViewStyle
                            ).apply {
                                setImageResource(action.iconRes)
                                contentDescription = action.contentDescription
                                setOnClickListener { action.onClick() }
                            }
                            actionsContainer.addView(imageView)

                            if (index != actions.lastIndex) {
                                val space = Space(this@MatchActivity).apply {
                                    layoutParams = LinearLayout.LayoutParams(
                                        10.dpToPx(this@MatchActivity),
                                        LinearLayout.LayoutParams.MATCH_PARENT
                                    )
                                }
                                actionsContainer.addView(space)
                            }
                        }
                    } else {
                        actionsContainer.visibility = View.GONE
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                accountViewModel.authState.collectLatest { state ->
                    if (accountViewModel.isLogged()) {
                        YouTubeClientProvider
                            .initialize(this@MatchActivity, accountViewModel.accountName()) { message ->
                                CoroutineScope(Dispatchers.Main).launch {
                                    toast(message, Toast.LENGTH_SHORT)
                                }
                            }

                        binding.matchNavView.menu.findItem(R.id.youtubeConfigurationFragment).isVisible = true
                        binding.matchNavView.menu.findItem(R.id.youtubeStreamFragment).isVisible = true

                        binding.matchNavView.getHeaderView(0)
                            .findViewById<TextView>(R.id.textAccountEmail).text = accountViewModel.accountName()
                    } else {

                        binding.matchNavView.menu.findItem(R.id.youtubeConfigurationFragment).isVisible = false
                        binding.matchNavView.menu.findItem(R.id.youtubeStreamFragment).isVisible = false

                        binding.matchNavView.getHeaderView(0)
                            .findViewById<TextView>(R.id.textAccountEmail).text = getString(R.string.google_sign_in)
                    }
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        accountViewModel.updateLastSignedInAccount()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
    }

    override fun navigateAsDrawerSelection(@IdRes destinationId: Int) {
        val navController = findNavController(binding.navHostFragment.id)
        navController.navigate(destinationId, null, NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, false)
            .build()
        )
    }

    override fun navigateAsStartActivity(activityClass: Class<out Activity>) {
        startActivity(Intent(this, activityClass))
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
