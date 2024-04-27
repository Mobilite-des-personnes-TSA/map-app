package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.preference.PreferenceManager
import com.example.myapplication.tisseo.TisseoApiClient
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


class MainActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private lateinit var buttonJourneyPlanner: Button
    private lateinit var buttonSettings: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(R.layout.activity_main)

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        val mapController = map.controller
        mapController.setZoom(13.0)
        val startCenterPoint = GeoPoint(43.6, 1.4333)
        mapController.setCenter(startCenterPoint)

        buttonJourneyPlanner = findViewById(R.id.button_journey_planner)
        buttonJourneyPlanner.setOnClickListener(this::openJourneyPlanner)

        buttonSettings = findViewById(R.id.button_settings)
        buttonSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topView)) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
            }
            WindowInsetsCompat.CONSUMED
        }


    }

    override fun onResume() {
        super.onResume()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume() //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onPause() {
        super.onPause()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause()  //needed for compass, my location overlays, v6.0.0 and up
    }

    private fun tisseoRouting(startPlace: String, endPlace: String) {
        Thread {
            val journeyData = TisseoApiClient.journey(startPlace, endPlace, "walk", "1")
            val road = Road()
            if (journeyData != null) {
                journeyData.routePlannerResult.journeys[0].journey.chunks.forEach { chunk ->

                    if (chunk.service != null) {
                        val wkt = chunk.service.wkt
                        val coordinates =
                            wkt.substringAfter("(").substringBeforeLast(")").split(",")
                        val roadNode = RoadNode()
                        roadNode.mInstructions = chunk.service.text?.text
                        val (long, lat) = coordinates[0].trim().split(" ")
                        val units = chunk.service.duration.split(":".toRegex())
                            .dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                        val duration =
                            3600 * units[0].toInt() + 60 * units[1].toInt() + units[2].toInt()
                        roadNode.mDuration = duration.toDouble()
                        roadNode.mLocation = GeoPoint(lat.toDouble(), long.toDouble())
                        road.mNodes.add(roadNode)
                        coordinates.forEach { coordinate ->
                            val (longitude, latitude) = coordinate.trim().split(" ")
                            road.mRouteHigh.add(GeoPoint(latitude.toDouble(), longitude.toDouble()))
                        }
                    } else if (chunk.street != null) {
                        val wkt = chunk.street.wkt
                        val roadNode = RoadNode()
                        roadNode.mInstructions = chunk.street.text.text
                        val units = chunk.street.duration.split(":".toRegex())
                            .dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                        val duration =
                            3600 * units[0].toInt() + 60 * units[1].toInt() + units[2].toInt()
                        roadNode.mDuration = duration.toDouble()
                        roadNode.mLength = chunk.street.length.toDouble() / 1000
                        roadNode.mLocation = GeoPoint(
                            chunk.street.startAddress.connectionPlace.latitude.toDouble(),
                            chunk.street.startAddress.connectionPlace.longitude.toDouble()
                        )
                        road.mNodes.add(roadNode)
                        val intermediateCoordinates = wkt.substringAfter("(")
                        val coordinates: List<String> = if (intermediateCoordinates[0] == '(') {
                            wkt.substringAfter("((").substringBeforeLast("))").split(",")
                        } else {
                            wkt.substringAfter("(").substringBeforeLast(")").split(",")
                        }

                        coordinates.forEach { coordinate ->
                            val (longitude, latitude) = coordinate.trim().split(" ")
                            road.mRouteHigh.add(GeoPoint(latitude.toDouble(), longitude.toDouble()))

                        }
                    }
                }
            }


            /*println("Points de la route ajoutés:")
            road.mRouteHigh.forEachIndexed { index, geoPoint ->
                println("Point $index : Latitude = ${geoPoint.latitude}, Longitude = ${geoPoint.longitude}")
            }*/
            drawJourney(road)
            map.invalidate()
        }.start()
    }

    @Suppress("unused")
    fun osrmRouting(startPoint: GeoPoint, endPoint: GeoPoint, mode: String) {
        Thread {
            val roadManager: RoadManager = OSRMRoadManager(this, "User")
            when (mode) {
                "bike" -> {
                    (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_BIKE)
                }

                "car" -> {
                    (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_CAR)
                }

                else -> {
                    (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_FOOT)
                }
            }

            val waypoints = ArrayList<GeoPoint>()
            waypoints.add(startPoint)
            waypoints.add(endPoint)
            val road: Road = roadManager.getRoad(waypoints)
            if (road.mStatus != Road.STATUS_OK) Toast.makeText(
                this,
                "Error when loading the road - status=" + road.mStatus,
                Toast.LENGTH_SHORT
            ).show()

            drawJourney(road)
            map.invalidate()
        }.start()
    }

    private fun drawJourney(road: Road) {
        val roadOverlay: Polyline = RoadManager.buildRoadOverlay(road)
        map.overlays.clear()
        map.overlays.add(roadOverlay)
        val nodeIcon = ResourcesCompat.getDrawable(resources, R.drawable.marker_node, theme)
        for (i in road.mNodes.indices) {
            val node = road.mNodes[i]
            val nodeMarker = Marker(map)
            nodeMarker.setPosition(node.mLocation)
            nodeMarker.icon = nodeIcon
            nodeMarker.title = "Step $i"
            nodeMarker.snippet = node.mInstructions

            nodeMarker.subDescription =
                Road.getLengthDurationText(this, node.mLength, node.mDuration)
            map.overlays.add(nodeMarker)
        }
    }

    private fun openJourneyPlanner(@Suppress("UNUSED_PARAMETER") view: View) {
        val intent = Intent(this, JourneyPlanner::class.java)
        resultJourneyPlanner.launch(intent)
    }

    private var resultJourneyPlanner =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.also {
                    it.getStringExtra("Departure")?.also { departureAddress ->
                        it.getStringExtra("Arrival")?.also { arrivalAddress ->
                            tisseoRouting(departureAddress, arrivalAddress)
                        }
                    }
                }
            }

        }
}
