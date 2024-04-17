package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import com.example.myapplication.JourneyPlanner.JourneyPlannerViewModel
import org.osmdroid.util.GeoPoint

/** TODO :
 *
 */

/** TODO : Creating the database
 *  Setting up Database and repositories
 *  Creating Data Flow
 */
class JourneyPlanner : AppCompatActivity() {

    private lateinit var button: Button
    private lateinit var viewModel : JourneyPlannerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journey_planner)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                //          .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        button = findViewById(R.id.search)
        button.setOnClickListener {
        //TODO: JourneyPlanning ie. tisseoRouting
        //  Navigate to Map
        //  Keep the JourneySearch info as long as the user hasn't closed the app
        }

    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }
}