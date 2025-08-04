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
import android.view.MenuItem
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import it.lmqv.livematchcam.auth.AuthResult
import it.lmqv.livematchcam.databinding.ActivityMatchBinding
import it.lmqv.livematchcam.services.CounterService
import it.lmqv.livematchcam.viewmodels.AccountViewModel
import it.lmqv.livematchcam.viewmodels.FloatingActionsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

        setSupportActionBar(binding.customToolbar.toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayShowTitleEnabled(false) // Hide default title if needed
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(binding.matchNavHostFragment.id) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.serversFragment,
                R.id.matchInfoFragment,
                R.id.youTubeFragment
            ),
            binding.matchDrawerLayout
        )

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(binding.matchNavView, navController)

        transitionAnim(true)
        binding.tvVersion.text = getString(R.string.version, BuildConfig.VERSION_NAME)

        headerView = binding.matchNavView.getHeaderView(0)
        headerView.setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
            binding.matchDrawerLayout.closeDrawers()
        }

        requestPermissions()

        startService(Intent(this, CounterService::class.java))
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launchWhenStarted {
            floatingActionsViewModel.actions.collect { actions ->
                var actionsContainer = binding.floatingActionContainer
                actionsContainer.removeAllViews()
                if (actions.isNotEmpty()) {
                    actionsContainer.visibility = View.VISIBLE

                    actions.forEach { action ->
                        val imageView = ImageView(
                            ContextThemeWrapper(this@MatchActivity, R.style.AppTheme),
                            null, 0, R.style.defaultImageViewStyle
                        ).apply {
                            setImageResource(action.iconRes)
                            contentDescription = action.contentDescription
                            setOnClickListener { action.onClick() }
                        }
                        actionsContainer.addView(imageView)
                    }
                } else {
                    actionsContainer.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            accountViewModel.authState.collectLatest { state ->
                binding.matchNavView
                    .getHeaderView(0)
                    .findViewById<TextView>(R.id.textAccountEmail)
                    .text = accountViewModel.accountDesc()

                val youTubeFragment = binding.matchNavView.menu.findItem(R.id.youTubeFragment)
                youTubeFragment.isVisible = state is AuthResult.Authenticated
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        val menuItem = menu.findItem(R.id.menu_settings)
        val drawable = menuItem.icon
        drawable?.let {
            it.setTint(ContextCompat.getColor(this, R.color.primary))
            menuItem.icon = it
        }
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(binding.matchNavHostFragment.id)
        return NavigationUI.navigateUp(navController, appBarConfiguration)
    }

    override fun navigateAsDrawerSelection(@IdRes destinationId: Int) {
        val navController = findNavController(binding.matchNavHostFragment.id)
        navController.navigate(destinationId, null, NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, false)
            .build()
        )
    }

    override fun navigateAsStartActivity(activityClass: Class<out Activity>)
    {
        startActivity(Intent(this, activityClass))
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
