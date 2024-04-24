package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import com.example.myapplication.JourneyPlanner.JourneyPlannerViewModel
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.example.myapplication.tisseo.JourneyResponse
import com.example.myapplication.tisseo.TisseoApiClient
import kotlinx.coroutines.Dispatchers
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.bonuspack.routing.RoadNode
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline


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

    private suspend fun tisseoRouting(startPlace:String, endPlace:String){
            val journeyData = TisseoApiClient.journey(startPlace,endPlace,"walk","1", Dispatchers.Unconfined)


    }

    private fun JourneyToRoad(journey :  JourneyResponse.RoutePlannerResult.JourneyItem.Journey ): Road {
        val road = Road()

            journey.chunks.forEach{chunk ->

                if (chunk.service != null){
                    val wkt = chunk.service.wkt
                    val coordinates = wkt.substringAfter("(").substringBeforeLast(")").split(",")
                    val roadNode = RoadNode()
                    roadNode.mInstructions = chunk.service.text?.text
                    val (long, lat) = coordinates[0].trim().split(" ")
                    val units = chunk.service.duration.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    val duration = 3600 * units[0].toInt() +60 * units[1].toInt() + units[2].toInt()
                    roadNode.mDuration = duration.toDouble()
                    roadNode.mLocation = GeoPoint(lat.toDouble(),long.toDouble())
                    road.mNodes.add(roadNode)
                    coordinates.forEach { coordinate ->
                        val (longitude, latitude) = coordinate.trim().split(" ")
                        road.mRouteHigh.add(GeoPoint(latitude.toDouble(),longitude.toDouble()))
                    }
                } else if(chunk.street != null){
                    val wkt = chunk.street.wkt
                    val roadNode = RoadNode()
                    roadNode.mInstructions = chunk.street.text.text
                    val units = chunk.street.duration.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    val duration = 3600 * units[0].toInt() +60 * units[1].toInt() + units[2].toInt()
                    roadNode.mDuration = duration.toDouble()
                    roadNode.mLength = chunk.street.length.toDouble()/1000
                    roadNode.mLocation = GeoPoint(chunk.street.startAddress.connectionPlace.latitude.toDouble(),chunk.street.startAddress.connectionPlace.longitude.toDouble())
                    road.mNodes.add(roadNode)
                    val intermediateCoordinates = wkt.substringAfter("(")
                    val coordinates:List<String> = if(intermediateCoordinates[0] == '('){
                        wkt.substringAfter("((").substringBeforeLast("))").split(",")
                    }else{
                        wkt.substringAfter("(").substringBeforeLast(")").split(",")
                    }

                    coordinates.forEach{coordinate ->
                        val (longitude,latitude) = coordinate.trim().split(" ")
                        road.mRouteHigh.add(GeoPoint(latitude.toDouble(),longitude.toDouble()))

                    }
                }
            }

        return road;
    }

}