package it.lmqv.livematchcam


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import it.lmqv.livematchcam.utils.KeyValue
import it.lmqv.livematchcam.extensions.setShirtByColor
import it.lmqv.livematchcam.extensions.toast
import it.lmqv.livematchcam.fragments.IServersFragment
import it.lmqv.livematchcam.fragments.ServersFragment

class MainActivity : AppCompatActivity() {

    private val serverFragment : IServersFragment = ServersFragment.newInstance()

    private lateinit var homeColorImageView : ImageView
    private lateinit var awayColorImageView : ImageView

    //private lateinit var list: GridView
    //private val activities: MutableList<ActivityLink> = mutableListOf()

    private val permissions = mutableListOf(
        Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA,
    ).apply {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            this.add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayShowTitleEnabled(false) // Hide default title if needed
        }

        supportFragmentManager
            .beginTransaction()
            .add(R.id.serversContainer, serverFragment as Fragment)
            .commit()

        transitionAnim(true)
        val tvVersion = findViewById<TextView>(R.id.tv_version)
        tvVersion.text = getString(R.string.version, BuildConfig.VERSION_NAME)


        val editTextHomeTeamName = this.findViewById<EditText>(R.id.et_home_team)
        val editTextAwayTeamName = this.findViewById<EditText>(R.id.et_away_team)

        val bActivityLive = this.findViewById<Button>(R.id.activity_Live)
        bActivityLive.setOnClickListener {
            //toast(serverFragment.getServerURI());
            GlobalDataManager.homeTeam.name = editTextHomeTeamName.text.toString()
            GlobalDataManager.awayTeam.name = editTextAwayTeamName.text.toString()

            startActivity(Intent(this, LiveStreamActivity::class.java))
        }

        editTextHomeTeamName.nextFocusForwardId = View.NO_ID
        editTextAwayTeamName.nextFocusForwardId = View.NO_ID
        editTextHomeTeamName.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                editTextHomeTeamName.post(Runnable { editTextHomeTeamName.selectAll() })
            }
        }
        editTextAwayTeamName.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                editTextAwayTeamName.post(Runnable { editTextAwayTeamName.selectAll() })
            }
        }

        this.homeColorImageView = this.findViewById<ImageView>(R.id.home_color)
        this.homeColorImageView.setOnClickListener {
            showColorPickerDialog(this.homeColorImageView, GlobalDataManager.homeTeam)
        }
        GlobalDataManager.homeTeam.color = Color.WHITE
        this.homeColorImageView.setShirtByColor(GlobalDataManager.homeTeam.color)

        this.awayColorImageView = this.findViewById<ImageView>(R.id.away_color)
        this.awayColorImageView.setOnClickListener {
            showColorPickerDialog(this.awayColorImageView, GlobalDataManager.awayTeam)
        }
        GlobalDataManager.awayTeam.color = Color.BLACK
        this.awayColorImageView.setShirtByColor(GlobalDataManager.awayTeam.color)

        requestPermissions()
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
            if (ActivityCompat.checkSelfPermission(context, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    private fun showColorPickerDialog(shirtImage:ImageView, team: Team) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.color_picker_dialog, null)
        val dialog = AlertDialog.Builder(this, R.style.AppDialogTheme)
            .setView(dialogView)
            .create()

        /* val colorsMap = mapOf(
             R.id.tShirtBlack to Color.BLACK,
             R.id.tShirtWhite to Color.WHITE,
             R.id.tShirtGreen to android.R.color.holo_green_light,
             R.id.tShirtRed to android.R.color.holo_red_light,
         )

         colorsMap.forEach { (key, value) ->
             //var xx = ColorImageView(
             dialogView.findViewById<View>(key).setOnClickListener {
                 val layeredDrawable = ContextCompat.getDrawable(this, R.drawable.layered_mask) as LayerDrawable
                 val maskLayer = layeredDrawable.findDrawableByLayerId(R.id.mask_layer)
                 maskLayer.setTint(value)
                 shirtImage.setImageDrawable(layeredDrawable)
                 team.color = value
                 dialog.dismiss()
             }
         }*/

        dialogView.findViewById<View>(R.id.tShirtBlack).setOnClickListener {
            team.color = Color.BLACK
            shirtImage.setShirtByColor(team.color)
            dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.tShirtWhite).setOnClickListener {
            team.color = Color.WHITE
            shirtImage.setShirtByColor(team.color)
            dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.tShirtGreen).setOnClickListener {
            team.color = ContextCompat.getColor(this, R.color.GREEN)
            shirtImage.setShirtByColor(team.color)
            dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.tShirtRed).setOnClickListener {
            team.color = Color.RED
            shirtImage.setShirtByColor(team.color)
            dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.tShirtBlue).setOnClickListener {
            team.color = Color.BLUE
            shirtImage.setShirtByColor(team.color)
            dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.tShirtOrange).setOnClickListener {
            team.color = ContextCompat.getColor(this, R.color.ORANGE)
            shirtImage.setShirtByColor(team.color)
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.tShirtCornFlowerBlue).setOnClickListener {
            team.color = ContextCompat.getColor(this, R.color.CORNFLOWERBLUE)
            shirtImage.setShirtByColor(team.color)
            dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.tShirtDarkBlue).setOnClickListener {
            team.color = ContextCompat.getColor(this, R.color.DARKBLUE)
            shirtImage.setShirtByColor(team.color)
            dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.tShirtYellow).setOnClickListener {
            team.color = ContextCompat.getColor(this, R.color.YELLOW)
            shirtImage.setShirtByColor(team.color)
            dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.tShirtPink).setOnClickListener {
            team.color = ContextCompat.getColor(this, R.color.PINK)
            shirtImage.setShirtByColor(team.color)
            dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.tShirtViolet).setOnClickListener {
            team.color = ContextCompat.getColor(this, R.color.VIOLET)
            shirtImage.setShirtByColor(team.color)
            dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.tShirtSlateBlue).setOnClickListener {
            team.color = ContextCompat.getColor(this, R.color.SLATEBLUE)
            shirtImage.setShirtByColor(team.color)
            dialog.dismiss()
        }
        dialog.show()

    }

    /*private fun getLayerMask(color: Int) : Drawable {
        val layeredDrawable = ContextCompat.getDrawable(this, R.drawable.shirt_layers) as LayerDrawable
        val maskLayer = layeredDrawable.findDrawableByLayerId(R.id.mask_layer)
        maskLayer.setTint(color)
        return layeredDrawable
    }*/
}
