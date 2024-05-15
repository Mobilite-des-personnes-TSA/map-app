package com.example.myapplication.journeyPlanner

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.MapDisplay
import com.example.myapplication.R
import com.example.myapplication.tisseo.JourneyResponse
import com.example.myapplication.tisseo.TisseoApiClient
import kotlinx.coroutines.CoroutineDispatcher
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadNode
import org.osmdroid.util.GeoPoint

import kotlinx.serialization.ExperimentalSerializationApi


/** TODO : Done copying
 *
 */

/** TODO : Creating the database
 *  Setting up Database and repositories
 *  Creating Data Flow
 */
class JourneyPlanner : AppCompatActivity() {

    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journey_planner)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
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
}