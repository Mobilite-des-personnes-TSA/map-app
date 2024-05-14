package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.textfield.TextInputLayout

class JourneyPlanner : AppCompatActivity() {
    private lateinit var button: Button

    private lateinit var buttonSubway: SwitchCompat
    private lateinit var buttonBus: SwitchCompat
    private lateinit var buttonCableCar: SwitchCompat
    private lateinit var buttonTram: SwitchCompat
    private lateinit var buttonWheelChair: SwitchCompat
    private lateinit var buttonCar: SwitchCompat
    private lateinit var buttonBike: SwitchCompat

    private lateinit var edittextDep: TextInputLayout
    private lateinit var edittextArv: TextInputLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journey_planner)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        edittextDep = findViewById(R.id.departure_place)
        edittextArv = findViewById(R.id.arrival_place)

        buttonBus = findViewById(R.id.bus)
        buttonCableCar = findViewById(R.id.cable_car)
        buttonSubway = findViewById(R.id.subways)
        buttonTram = findViewById(R.id.tram)
        buttonWheelChair = findViewById(R.id.wheel_chair)
        buttonCar = findViewById(R.id.car)
        buttonBike = findViewById(R.id.personal_by_cycle)

        button = findViewById(R.id.search)
        button.setOnClickListener(this::activityResult)
    }

    private fun activityResult(view: View) {
        val intent = Intent()
        intent.putExtra("Departure", edittextDep.editText!!.text.toString())
        intent.putExtra("Arrival", edittextArv.editText!!.text.toString())
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