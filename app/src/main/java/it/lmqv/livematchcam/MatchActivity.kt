package it.lmqv.livematchcam

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import it.lmqv.livematchcam.services.counter.CounterService
import it.lmqv.livematchcam.services.youtube.YouTubeClientProvider
import it.lmqv.livematchcam.viewmodels.FloatingActionsViewModel
import it.lmqv.livematchcam.viewmodels.FirebaseAccountViewModel
import it.lmqv.livematchcam.viewmodels.GoogleAccountViewModel
import it.lmqv.livematchcam.viewmodels.CounterViewModel
import it.lmqv.livematchcam.views.AnimateImageVIew
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.getValue
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import coil.request.ImageRequest
import it.lmqv.livematchcam.dialogs.PreferencesDialogFragment
import it.lmqv.livematchcam.extensions.Logd
import it.lmqv.livematchcam.repositories.MatchRepository
import kotlinx.coroutines.flow.combine

interface INavigateDrawerActivity {
    fun navigateAsDrawerSelection(@IdRes destinationId: Int)
    fun navigateAsStartActivity(activityClass: Class<out Activity>)
}

class MatchActivity : BaseActivity(), INavigateDrawerActivity {

    private val googleAccountViewModel: GoogleAccountViewModel by viewModels()
    private val firebaseAccountViewModel: FirebaseAccountViewModel by viewModels()
    private val floatingActionsViewModel: FloatingActionsViewModel by viewModels()
    //private val counterViewModel: CounterViewModel by viewModels()

    private lateinit var binding: ActivityMatchBinding

    private lateinit var headerView: View
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController
    //private var navControllerState: Bundle? = null

    private val topLevelsFragments = setOf(
        R.id.serverConfigurationFragment,
        R.id.firebaseConfigurationFragment,
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
        //Logd("MatchActivity::onCreate")

        binding = ActivityMatchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvVersion.text = getString(R.string.version, BuildConfig.VERSION_NAME)

        floatingActionsViewModel.setNavigator(this)

        setSupportActionBar(binding.customToolbar.toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayShowTitleEnabled(false) // Hide default title if needed
        }

        headerView = binding.matchNavView.getHeaderView(0)
        headerView.setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
            binding.matchDrawerLayout.post {
                binding.matchDrawerLayout.closeDrawer(GravityCompat.START, true)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                floatingActionsViewModel.actions.collect { actions ->
                    var actionsContainer = binding.floatingActionContainer
                    actionsContainer.visibility = View.GONE
                    actionsContainer.removeAllViews()

                    if (actions.isNotEmpty()) {
                        actions.forEachIndexed { index, action ->
                            val imageView = AnimateImageVIew(
                                ContextThemeWrapper(this@MatchActivity, R.style.AppTheme),
                                null, 0, R.style.DefaultImageViewStyle
                            ).apply {
                                id = action.id
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
                        actionsContainer.visibility = View.VISIBLE
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    MatchRepository.firebaseAccountData,
                    firebaseAccountViewModel.authState
                ) { firebaseAccountData, authState -> Pair(firebaseAccountData, authState) }
                    .collectLatest { (firebaseAccountData, authState) ->

                    if (firebaseAccountViewModel.isLogged()) {
                        binding.matchNavView.getHeaderView(0)
                            .findViewById<TextView>(R.id.textFirebaseAccountEmail).text = firebaseAccountViewModel.accountName()
                    } else {
                        binding.matchNavView.getHeaderView(0)
                            .findViewById<TextView>(R.id.textFirebaseAccountEmail).text = getString(R.string.sign_in)
                    }

                    var firebaseMenuItem = binding.matchNavView.menu.findItem(R.id.firebaseConfigurationFragment)

                    var title = firebaseAccountData.title
                    if (title.isEmpty()) {
                        firebaseMenuItem.title = ContextCompat.getString(this@MatchActivity, R.string.firebase_custom)
                    } else {
                        firebaseMenuItem.title = title
                    }

                    var sourceLogoUrl = firebaseAccountData.logoURL
                    if (sourceLogoUrl.isEmpty()) {
                        firebaseMenuItem.icon = ContextCompat.getDrawable(this@MatchActivity, R.drawable.ic_link)
                    } else {
                        val drawable = coil.ImageLoader(this@MatchActivity)
                            .execute(
                                ImageRequest.Builder(this@MatchActivity)
                                    .data(sourceLogoUrl)
                                    .placeholder(R.drawable.refresh)
                                    .error(R.drawable.ic_link)
                                    .allowHardware(false)
                                    .build()
                            )
                            .drawable

                        if (drawable != null) {
                            firebaseMenuItem.icon = drawable
                        }
                    }

                    var settings = firebaseAccountData.settings
                    val isLogged = googleAccountViewModel.isLogged()
                    binding.matchNavView.menu.findItem(R.id.youtubeConfigurationFragment).isVisible = settings.youTubeEnabled && isLogged
                    binding.matchNavView.menu.findItem(R.id.youtubeStreamFragment).isVisible = settings.youTubeEnabled && isLogged
                    binding.matchNavView.getHeaderView(0).findViewById<TextView>(R.id.textAccountEmail).isVisible = settings.youTubeEnabled && isLogged

                    var hasStreams = firebaseAccountData.streams.isNotEmpty()
                    //binding.matchNavView.menu.findItem(R.id.serverConfigurationFragment).isVisible = firebaseAccountViewModel.hasAccountKey()
                    binding.matchNavView.menu.findItem(R.id.firebaseConfigurationFragment).isVisible = firebaseAccountViewModel.hasAccountKey() && hasStreams

                    floatingActionsViewModel.setFirebaseAccountData(firebaseAccountData)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                googleAccountViewModel.authState.collectLatest { state ->
                    if (googleAccountViewModel.isLogged()) {
                        var accountName = googleAccountViewModel.accountName()
                        toast(getString(R.string.logged_in, accountName))

                        YouTubeClientProvider
                            .initialize(this@MatchActivity, accountName) { message ->
                                CoroutineScope(Dispatchers.Main).launch {
                                    toast(message, Toast.LENGTH_SHORT)
                                }
                            }
//
//                        binding.matchNavView.menu.findItem(R.id.youtubeConfigurationFragment).isVisible = true
//                        binding.matchNavView.menu.findItem(R.id.youtubeStreamFragment).isVisible = true

                        binding.matchNavView.getHeaderView(0)
                            .findViewById<TextView>(R.id.textAccountEmail).text = accountName
                    } else {

//                        binding.matchNavView.menu.findItem(R.id.youtubeConfigurationFragment).isVisible = false
//                        binding.matchNavView.menu.findItem(R.id.youtubeStreamFragment).isVisible = false

                        binding.matchNavView.getHeaderView(0)
                            .findViewById<TextView>(R.id.textAccountEmail).text = getString(R.string.sign_in)
                    }
                }
            }
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        if (savedInstanceState == null) {
            val prefs = getSharedPreferences("nav_state", MODE_PRIVATE)
            val lastDest = prefs.getInt("last_dest", R.id.serverConfigurationFragment)

            if (navController.graph.findNode(lastDest) != null &&
                lastDest != R.id.serverConfigurationFragment) {
                navController.navigate(lastDest)
                floatingActionsViewModel.setMenuItem(lastDest)
            }
        }

        appBarConfiguration = AppBarConfiguration(
            topLevelsFragments,
            binding.matchDrawerLayout
        )

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(binding.matchNavView, navController)

        binding.matchNavView.itemIconTintList = null
        //binding.matchNavView.itemTextColor = null

        binding.matchNavView.setNavigationItemSelectedListener { menuItem ->
            val destinationId = menuItem.itemId

            navController.navigate(destinationId, null,
                NavOptions.Builder()
                    .setPopUpTo(navController.graph.startDestinationId, false)
                    .build())

            getSharedPreferences("nav_state", MODE_PRIVATE)
                .edit {
                    putInt(
                        "last_dest",
                        navController.currentDestination?.id ?: R.id.serverConfigurationFragment
                    )
                }

            binding.matchDrawerLayout.post {
                binding.matchDrawerLayout.closeDrawer(GravityCompat.START, true)
                floatingActionsViewModel.setMenuItem(destinationId)
            }

            true
        }

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

        requestPermissions()
    }

    override fun onStart() {
        super.onStart()
        //Logd("MatchActivity::onStart")
    }

    override fun onPause() {
        super.onPause()
        //Logd("MatchActivity::onPause")
        //navControllerState = navController.saveState()

//        getSharedPreferences("nav_state", MODE_PRIVATE)
//            .edit {
//                putInt(
//                    "last_dest",
//                    navController.currentDestination?.id ?: R.id.serverConfigurationFragment
//                )
//            }
        //toast("MatchActivity::OnPause")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //Logd("MatchActivity::OnSaveInstanceState")
//        navControllerState?.let {
//            outState.putBundle("nav_state", it)
//        }
    }

    override fun onResume() {
        super.onResume()
        Logd("MatchActivity::onResume")
        googleAccountViewModel.updateLastSignedInAccount()
        firebaseAccountViewModel.updateLastSignedInAccount()
    }

    override fun onDestroy() {
        super.onDestroy()
        Logd("MatchActivity::onDestroy")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
       menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
//                val dialog = PreferencesDialogFragment
//                    .newInstance(R.xml.root_preferences)
//                dialog.show(supportFragmentManager, "preferences_dialog")

                // Esempio 2: Carica solo una sezione specifica (usando rootKey)
                //val dialog = PreferencesDialogFragment.newInstance(
                //    R.xml.root_preferences,
                //    rootKey = "video_settings"
                //)
                //dialog.show(supportFragmentManager, "preference_dialog")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
