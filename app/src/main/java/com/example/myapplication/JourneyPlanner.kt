package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat

class JourneyPlanner : AppCompatActivity() {
    private lateinit var button: Button
    private lateinit var edittextDep: EditText
    private lateinit var edittextArv: EditText
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

        edittextDep = findViewById(R.id.editTextText)
        edittextArv = findViewById(R.id.editTextText2)
        button = findViewById(R.id.search)
        button.setOnClickListener(this::activityResult)
    }

    private fun activityResult(view: View) {
        val intent = Intent()
        intent.putExtra("Departure", edittextDep.text.toString())
        intent.putExtra("Arrival", edittextArv.text.toString())
        setResult(RESULT_OK, intent)
        finish()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }
}