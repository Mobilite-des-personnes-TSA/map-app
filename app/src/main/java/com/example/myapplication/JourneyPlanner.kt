package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputLayout

class JourneyPlanner : AppCompatActivity() {
    private lateinit var button: Button

    private lateinit var buttonSubway: Chip
    private lateinit var buttonBus: Chip
    private lateinit var buttonCableCar: Chip
    private lateinit var buttonTram: Chip
    private lateinit var buttonWheelChair: Chip
    private lateinit var buttonCar: Chip
    private lateinit var buttonBike: Chip

    private lateinit var editTextDep: TextInputLayout
    private lateinit var editTextArv: TextInputLayout
    @SuppressLint( "ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journey_planner)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setHomeActionContentDescription(1)


        editTextDep = findViewById(R.id.departure_place)
        editTextArv = findViewById(R.id.arrival_place)

        buttonBus = findViewById(R.id.bus)
        buttonCableCar = findViewById(R.id.cable_car)
        buttonSubway = findViewById(R.id.subways)
        buttonTram = findViewById(R.id.tram)
        buttonWheelChair = findViewById(R.id.wheel_chair)
        buttonCar = findViewById(R.id.car)
        buttonBike = findViewById(R.id.personal_by_cycle)

        button = findViewById(R.id.search)
        button.setOnClickListener { activityResult() }
    }

    private fun activityResult() {
        val intent = Intent()
        intent.putExtra("Departure", editTextDep.editText!!.text.toString())
        intent.putExtra("Arrival", editTextArv.editText!!.text.toString())
        intent.putExtra("Bus", buttonBus.isChecked)
        intent.putExtra("Subway", buttonSubway.isChecked)
        intent.putExtra("CableCar", buttonCableCar.isChecked)
        intent.putExtra("Tram", buttonTram.isChecked)
        intent.putExtra("WheelChair", buttonWheelChair.isChecked)
        intent.putExtra("Car", buttonCar.isChecked)
        intent.putExtra("Bike", buttonBike.isChecked)


        setResult(RESULT_OK, intent)
        finish()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }
}