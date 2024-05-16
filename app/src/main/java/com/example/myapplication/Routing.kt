package com.example.myapplication

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.example.myapplication.databinding.ActivityRoutingBinding


import android.content.Intent
import android.view.ViewGroup
import android.widget.Button
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
class Routing : AppCompatActivity() {

    private lateinit var binding: ActivityRoutingBinding

    private lateinit var map: MapView
    private lateinit var buttonJourneyPlanner: Button
    private lateinit var buttonSettings: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityRoutingBinding.inflate(layoutInflater)
        //setContentView(R.layout.activity_routing)
        setContentView(binding.root)
        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        //setContentView(R.layout.activity_routing)
        // TODO : Another setContentView ?

        // TODO : Keep in home page?
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Who cares?

        /*
        val navController = findNavController(R.id.nav_host_fragment_activity_routing)
        NavigationUI.setupActionBarWithNavController(this, navController)

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
         */
        val navView: BottomNavigationView = binding.navView
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_routing) as NavHostFragment
        val navController = navHostFragment.findNavController()
        setSupportActionBar(binding.toolbar)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_journey_planner, R.id.navigation_map_display
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        ViewCompat.setOnApplyWindowInsetsListener(binding.container) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
            }
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_routing)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}