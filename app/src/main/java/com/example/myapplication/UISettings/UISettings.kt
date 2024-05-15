package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import com.example.myapplication.databinding.ActivitySettingBinding

class UISettings : AppCompatActivity() {

    private lateinit var binding : ActivitySettingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.searchItinerary.setOnClickListener{
            val intent = Intent(this, Routing::class.java)
            startActivity(intent)
        }

        val seekBar = binding.noiseSeekBar
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Update UI or perform actions based on the progress value
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Optional: Perform actions when the user starts interacting with the SeekBar
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Optional: Perform actions when the user stops interacting with the SeekBar
            }
        })
    }
    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }
}