package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat

class JourneyPlanner : AppCompatActivity() {
    private lateinit var button: Button

    private lateinit var buttonSubway: CheckBox
    private lateinit var buttonBus: CheckBox
    private lateinit var buttonCableCar: CheckBox
    private lateinit var buttonTram: CheckBox
    private lateinit var buttonWheelChair: CheckBox



    private lateinit var edittextDep: EditText
    private lateinit var edittextArv: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journey_planner)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        edittextDep = findViewById(R.id.editTextText)
        edittextArv = findViewById(R.id.editTextText2)

        buttonBus = findViewById(R.id.busswitch)
        buttonCableCar = findViewById(R.id.cablecarswitch)
        buttonSubway = findViewById(R.id.subwaysswitch)
        buttonTram = findViewById(R.id.tramswitch)
        buttonWheelChair = findViewById(R.id.wheelchairswitch)

        button = findViewById(R.id.search)
        button.setOnClickListener(this::activityResult)
    }

    private fun activityResult(view: View) {
        val intent = Intent()
        intent.putExtra("Departure", edittextDep.text.toString())
        intent.putExtra("Arrival", edittextArv.text.toString())
        intent.putExtra("Bus", buttonBus.isActivated)
        intent.putExtra("Subway", buttonSubway.isActivated)
        intent.putExtra("CableCar", buttonCableCar.isActivated)
        intent.putExtra("Tram", buttonTram.isActivated)
        intent.putExtra("WheelChair", buttonWheelChair.isActivated)

        setResult(RESULT_OK, intent)
        finish()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }
}