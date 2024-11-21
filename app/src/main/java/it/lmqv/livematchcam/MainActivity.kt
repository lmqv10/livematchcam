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
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.GridView
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import it.lmqv.livematchcam.utils.KeyValue
import it.lmqv.livematchcam.extensions.setShirtByColor
import it.lmqv.livematchcam.extensions.toast


class MainActivity : AppCompatActivity() {
    //, SensorEventListener {

    /*private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null

    private lateinit var xTextView: TextView
    private lateinit var xDegreeTextView: TextView

    private var x = 0f
    private var previousTimestamp: Long = 0
    */

    //private lateinit var swAutoZoomEnable : Switch
    //private lateinit var etLeftZoomDegree : EditText
    //private lateinit var etRightZoomDegree : EditText
    private lateinit var homeColorImageView : ImageView
    private lateinit var awayColorImageView : ImageView

    private lateinit var list: GridView
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

        // Set up the custom toolbar as the ActionBar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Optional: Set the title programmatically
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayShowTitleEnabled(false) // Hide default title if needed
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
/*
        val gradientDrawable = resources.getDrawable(R.drawable.shirt_stroke, theme)
        supportActionBar?.setBackgroundDrawable(gradientDrawable)
*/
        transitionAnim(true)
        val tvVersion = findViewById<TextView>(R.id.tv_version)
        tvVersion.text = getString(R.string.version, BuildConfig.VERSION_NAME)

        //list = findViewById(R.id.list)
        //createList()
        //setListAdapter(activities)

        /*xTextView = findViewById(R.id.accelerometer_x)
        xDegreeTextView = findViewById(R.id.x_degree)
        statusTextView = findViewById(R.id.status)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        var resetBtn = findViewById<Button>(R.id.reset_btn)
        resetBtn.setOnClickListener {
            this.x = 0f
        }*/

        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        /*etLeftZoomDegree = this.findViewById<EditText>(R.id.et_left_degree)
        etLeftZoomDegree.nextFocusForwardId = View.NO_ID
        etLeftZoomDegree.text = Editable.Factory.getInstance().newEditable(GlobalDataManager.leftZoomDegreeTrigger.toString())
        etLeftZoomDegree.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                etLeftZoomDegree.post(Runnable { etLeftZoomDegree.selectAll() })
            }
        }
        etLeftZoomDegree.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val degree = s.toString().toInt()
                GlobalDataManager.leftZoomDegreeTrigger = degree
            }
            override fun afterTextChanged(p0: Editable?) { }
        })*/

        /*etRightZoomDegree = this.findViewById<EditText>(R.id.et_right_degree)
        etRightZoomDegree.nextFocusForwardId = View.NO_ID
        etRightZoomDegree.text = Editable.Factory.getInstance().newEditable(GlobalDataManager.rightZoomDegreeTrigger.toString())
        etRightZoomDegree.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                etRightZoomDegree.post(Runnable { etRightZoomDegree.selectAll() })
            }
        }
        etRightZoomDegree.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val degree = s.toString().toInt()
                GlobalDataManager.rightZoomDegreeTrigger = degree
            }
            override fun afterTextChanged(p0: Editable?) { }
        })*/

        /*var switch = this.findViewById<Switch>(R.id.switch_zoom)
        switch.isChecked = GlobalDataManager.autoZoomEnabled
        switch.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            GlobalDataManager.autoZoomEnabled = isChecked
            etLeftZoomDegree.isEnabled = isChecked
            etRightZoomDegree.isEnabled = isChecked
        })*/

        var etServerUrl = this.findViewById<EditText>(R.id.et_server_url)

        val spinnerRtmpUrl : Spinner = this.findViewById(R.id.spin_rtmp_url)
        val optionsServer = listOf(
            KeyValue("rtmp://a.rtmp.youtube.com/live2", "YouTube")
        )

        val adapterServer = ArrayAdapter(this, android.R.layout.simple_spinner_item, optionsServer)
        adapterServer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        /*val defaultPositionServer = optionsServer.indexOfFirst { it.key == GlobalDataManager.server }
        if (defaultPositionServer != -1) {
            spinnerRtmpUrl.setSelection(defaultPositionServer)
        } else {
            GlobalDataManager.server = optionsServer.get(0).key
            spinnerRtmpUrl.setSelection(0)
        }*/
        GlobalDataManager.server = optionsServer.get(0).key
        spinnerRtmpUrl.setSelection(0)
        spinnerRtmpUrl.adapter = adapterServer
        spinnerRtmpUrl.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position) as KeyValue<String>
                GlobalDataManager.server = selectedItem.key
                GlobalDataManager.setServerURI(GlobalDataManager.server, GlobalDataManager.key)
                etServerUrl.text = Editable.Factory.getInstance().newEditable(GlobalDataManager.server)
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }


        var etRtmpKey = this.findViewById<EditText>(R.id.et_rtmp_key)
        etRtmpKey.nextFocusForwardId = View.NO_ID
        etRtmpKey.onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                etRtmpKey.post(Runnable { etRtmpKey.selectAll() })
            }
        }
        etRtmpKey.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val newKey = s.toString()
                GlobalDataManager.key = newKey
                GlobalDataManager.setServerURI(GlobalDataManager.server, GlobalDataManager.key)
                //toast("ServerURL: ${GlobalDataManager.getServerURI()}")
            }
            override fun afterTextChanged(p0: Editable?) { }
        })

        val spinnerRtmpKey : Spinner = this.findViewById(R.id.spin_rtmp_key)
        val optionsKeys = listOf(
            KeyValue("yyx0-at5u-b330-4avg-4kx6", "Default"),
            KeyValue("fmjw-uqav-y4ua-xd4d-3zaw", "One-Shot")
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, optionsKeys)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        /*val defaultPosition = optionsKeys.indexOfFirst { it.key == GlobalDataManager.key }
        if (defaultPosition != -1) {
            spinnerRtmpKey.setSelection(defaultPosition)
        } else {
            GlobalDataManager.server = optionsServer.get(0).key
            spinnerRtmpKey.setSelection(0)
        }*/
        GlobalDataManager.key = optionsKeys[0].key
        spinnerRtmpKey.setSelection(0)
        spinnerRtmpKey.adapter = adapter
        spinnerRtmpKey.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position) as KeyValue<String>
                GlobalDataManager.key = selectedItem.key
                //GlobalDataManager.setServerURI(GlobalDataManager.server, GlobalDataManager.key)
                etRtmpKey.text = Editable.Factory.getInstance().newEditable(GlobalDataManager.key)
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }

        val editTextHomeTeamName = this.findViewById<EditText>(R.id.et_home_team)
        val editTextAwayTeamName = this.findViewById<EditText>(R.id.et_away_team)

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

        val bActivityLive = this.findViewById<Button>(R.id.activity_Live)
        bActivityLive.setOnClickListener {
            GlobalDataManager.homeTeam.name = editTextHomeTeamName.text.toString()
            GlobalDataManager.awayTeam.name = editTextAwayTeamName.text.toString()

            startActivity(Intent(this, LiveStreamActivity::class.java))
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

    /*override fun onResume() {
        super.onResume()
        gyroscope?.also { mag ->
            sensorManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        // Deregistra il listener per risparmiare batteria
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val thresholdDegree = 1f
        event?.let {
            try
            {
                if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                    val gyroX = event.values[0]
                    val timestamp = event.timestamp
                    val deltaTime = (timestamp - previousTimestamp) / 1_000_000_000f

                    val gyroXInDegrees = Math.toDegrees(gyroX.toDouble()).toFloat()

                    if (abs(gyroXInDegrees) > thresholdDegree) {
                        this.x += gyroXInDegrees * deltaTime
                        var xout = round(this.x)
                        xDegreeTextView.text = "Degree: $xout"
                    }
                    previousTimestamp = timestamp
                }

                val x = event.values[0]
                xTextView.text = "X: $x"
            }
            catch (e: Exception) {
                statusTextView.text = e.message
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Non è necessario gestire questo per l'accelerometro
    }
    */

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

    @SuppressLint("NewApi")
    private fun createList() {
        /*activities.add(
            ActivityLink(
                Intent(this, OldApiActivity::class.java),
                getString(R.string.old_api), VERSION_CODES.JELLY_BEAN
            )
        )
        activities.add(
            ActivityLink(
                Intent(this, FromFileActivity::class.java),
                getString(R.string.from_file), VERSION_CODES.JELLY_BEAN_MR2
            )
        )
        activities.add(
            ActivityLink(
                Intent(this, ScreenActivity::class.java),
                getString(R.string.display), VERSION_CODES.LOLLIPOP
            )
        )*/
        /*activities.add(
            ActivityLink(
                Intent(this, LiveStreamActivity::class.java),
                getString(R.string.rotation_rtmp), VERSION_CODES.LOLLIPOP
            )
        )*/
    }

    /*private fun setListAdapter(activities: List<ActivityLink>) {
        list.adapter = ImageAdapter(activities)
        list.onItemClickListener =
            OnItemClickListener { _, _, position, _ ->
                if (hasPermissions(this)) {
                    val link = activities[position]
                    val minSdk = link.minSdk
                    if (Build.VERSION.SDK_INT >= minSdk) {
                        startActivity(link.intent)
                        transitionAnim(false)
                    } else {
                        showMinSdkError(minSdk)
                    }
                } else {
                    showPermissionsErrorAndRequest()
                }
            }
    }*/

    private fun showMinSdkError(minSdk: Int) {
        val named: String = when (minSdk) {
            VERSION_CODES.JELLY_BEAN_MR2 -> "JELLY_BEAN_MR2"
            VERSION_CODES.LOLLIPOP -> "LOLLIPOP"
            else -> "JELLY_BEAN"
        }
        toast("You need min Android $named (API $minSdk)")
    }

    private fun showPermissionsErrorAndRequest() {
        toast("You need permissions before")
        requestPermissions()
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
