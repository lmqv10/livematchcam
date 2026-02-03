package it.lmqv.livematchcam

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayShowTitleEnabled(false) // Hide default title if needed
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.arrow_left) // Opzionale: icona personalizzata
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            if (preference is PreferenceScreen) {
                val fragment = SettingsFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PREFERENCE_ROOT, preference.key)
                    }
                }

                parentFragmentManager.beginTransaction()
                    .replace(R.id.settings, fragment)
                    .addToBackStack(null)
                    .commit()

                return true
            }
            return super.onPreferenceTreeClick(preference)
        }
    }
}