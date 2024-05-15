package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources.NotFoundException
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Looper
import android.util.Log
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
import com.example.myapplication.tisseo.BUS
import com.example.myapplication.tisseo.BUS_RAPID_TRANSIT
import com.example.myapplication.tisseo.CABLE_CAR
import com.example.myapplication.tisseo.METRO
import com.example.myapplication.tisseo.SHUTTLE
import com.example.myapplication.tisseo.TRAMWAY
import com.example.myapplication.tisseo.TisseoApiClient
import com.example.myapplication.tisseo.TisseoOsrmUtils
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.exp

class MainActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private lateinit var buttonJourneyPlanner: Button
    private lateinit var buttonSettings: Button
    private val tisseoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    private lateinit var markerNormal: Drawable
    private lateinit var markerDanger: Drawable

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        super.onCreate(savedInstanceState)
        getInstance().load(this, sharedPreferences)
        setContentView(R.layout.activity_main)

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        val mapController = map.controller
        mapController.setZoom(13.0)
        val startCenterPoint = GeoPoint(43.6, 1.4333)
        mapController.setCenter(startCenterPoint)

        buttonJourneyPlanner = findViewById(R.id.button_journey_planner)
        buttonJourneyPlanner.setOnClickListener { openJourneyPlanner() }

        buttonSettings = findViewById(R.id.button_settings)
        buttonSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        markerNormal =
            ResourcesCompat.getDrawable(resources, R.drawable.map_marker_outline, theme)!!
        markerNormal.setTint(
            ResourcesCompat.getColor(
                resources, com.google.android.material.R.color.foreground_material_light, theme
            )
        )
        markerDanger = ResourcesCompat.getDrawable(resources, R.drawable.map_marker_alert, theme)!!
        markerDanger.setTint(
            ResourcesCompat.getColor(
                resources, com.google.android.material.R.color.error_color_material_light, theme
            )
        )

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

    data class RoadNodeForRouting(val road: Road, val cost: Double)


    private fun tisseoRouting(
        startPlace: String,
        endPlace: String,
        date: LocalDateTime,
        wheelChair: Boolean,
        bus: Boolean,
        tram: Boolean,
        subway: Boolean,
        cableCar: Boolean,
        car: Boolean,
        bike: Boolean
    ) {
        val file = ArrayList<RoadNodeForRouting>()

        val geoPointStart = TisseoOsrmUtils.addressToGeoPoint(startPlace)
        if (geoPointStart == null) {
            Toast.makeText(this, "Start place not found", Toast.LENGTH_SHORT).show()
            return
        }
        val geoPointEnd = TisseoOsrmUtils.addressToGeoPoint(endPlace)
        if (geoPointEnd == null) {
            Toast.makeText(this, "End place not found", Toast.LENGTH_SHORT).show()
            return
        }

        val listDate = listOf(
            date,
            date.plusMinutes(20),
            date.plusMinutes(40),
        )

        val roadModeList = mutableListOf("walk")
        if (bike) roadModeList.add("bike")
        if (wheelChair) roadModeList.add("wheelchair")

        val transports = ArrayList<List<String>>()
        if (bus) transports.add(listOf(SHUTTLE, BUS))
        if (tram) transports.add(listOf(TRAMWAY, BUS_RAPID_TRANSIT))
        if (subway) transports.add(listOf(METRO))
        if (cableCar) transports.add(listOf(CABLE_CAR))
        transports.add(transports.flatten())


        // <Plot a dit> : y'a pas de tram a toulouse donc le mode de transport 2 ne sert à rien

        for (newDate in listDate) {

            for (roadMode in roadModeList) {
                osrmRouting(geoPointStart, geoPointEnd, roadMode, file)
                for (transport in transports) aRouting(
                    geoPointStart,
                    geoPointEnd,
                    roadMode,
                    file,
                    newDate,
                    transport
                )
            }

            if (car) osrmRouting(geoPointStart, geoPointEnd, "car", file)
        }

        drawJourney(selectBest(file).road)
    }

    private fun selectBest(file: List<RoadNodeForRouting>) =
        file.minByOrNull(RoadNodeForRouting::cost) ?: throw NotFoundException("No road found")


    private fun aRouting(
        startPlace: GeoPoint,
        endPlace: GeoPoint,
        roadMode: String,
        file: MutableList<RoadNodeForRouting>,
        date: LocalDateTime,
        rollingStocks: List<String>
    ) {
        TisseoApiClient.journey(
            "${startPlace.latitude},${startPlace.longitude}",
            "${endPlace.latitude},${endPlace.longitude}",
            roadMode,
            "4",
            date.format(tisseoDateFormatter),
            rollingStocks.joinToString(",")
        )?.apply {
            routePlannerResult.journeys.forEach { j ->
                val road = TisseoOsrmUtils.journeyToRoad(j.journey)
                val price = price(road)
                file.add(RoadNodeForRouting(road, price))
            }
        }
    }


    private fun osrmRouting(
        startPoint: GeoPoint,
        endPoint: GeoPoint,
        mode: String,
        file: MutableList<RoadNodeForRouting>
    ) {

        val roadManager = OSRMRoadManager(this, "User")
        when (mode) {
            "bike" -> roadManager.setMean(OSRMRoadManager.MEAN_BY_BIKE)
            "car" -> roadManager.setMean(OSRMRoadManager.MEAN_BY_CAR)
            else -> roadManager.setMean(OSRMRoadManager.MEAN_BY_FOOT)
        }

        val waypoints = arrayListOf(startPoint, endPoint)

        val road = roadManager.getRoad(waypoints)
        val price = price(road)
        file.add(RoadNodeForRouting(road, price))
    }

    private fun price(road: Road) = road.mNodes.sumOf {
        (crowd(it) + light(it) + sound(it)) * it.mLength
    }

    /*
        Le but de cette fonction est de calculer à qu'elle point un chemin est pénible
        Sachant qu'une forte pénibilité sur une partie du trajet sera plus impactant pour l'utilisateur
        qu'une pénibilité moyenne sur tout le trajet.
        De plus le changement de pénibilité entre des chemins déja peu pénible aura
        un impact faible

        nous avons donc décidé de la représenter par une fonction exponentielle sur chaque critère
     *//*
        On en profite aussi pour définir les points de sensibilité
     */

    private fun crowd(roadNode: RoadNode): Double {
        val out = exp(
            if ((43.55 < roadNode.mLocation.latitude && roadNode.mLocation.latitude < 43.65) && (1.4 < roadNode.mLocation.longitude && roadNode.mLocation.longitude < 1.5)) {
                (Math.random() * 2 + 1) * sharedPreferences.getInt("crowd", 0)
            } else {
                Math.random()
            }
        )

        if (out > 10) {
            roadNode.mManeuverType *= 2
        }

        return out
    }

    private fun light(roadNode: RoadNode): Double {
        val out = exp(
            if ((43.55 < roadNode.mLocation.latitude && roadNode.mLocation.latitude < 43.65) && (1.4 < roadNode.mLocation.longitude && roadNode.mLocation.longitude < 1.5)) {
                (Math.random() * 2 + 1) * sharedPreferences.getInt("light", 0)
            } else {
                Math.random()
            }
        )

        if (out > 10) {
            roadNode.mManeuverType *= 3
        }

        return out
    }

    private fun sound(roadNode: RoadNode): Double {
        val out = exp(
            if ((43.55 < roadNode.mLocation.latitude && roadNode.mLocation.latitude < 43.65) && (1.4 < roadNode.mLocation.longitude && roadNode.mLocation.longitude < 1.5)) {
                (Math.random() * 2 + 1) * sharedPreferences.getInt("sound", 0)
            } else {
                Math.random()
            }
        )

        if (out > 10) {
            roadNode.mManeuverType *= 5
        }
        return out
    }


    private fun drawJourney(road: Road) {
        val roadOverlay: Polyline = RoadManager.buildRoadOverlay(road)
        map.overlays.clear()
        map.overlays.add(roadOverlay)


        for (i in road.mNodes.indices) {
            Log.d(
                1.toString(),
                "ajout à la map du point : latitude " + road.mNodes[i].mLocation.latitude + " longitude " + road.mNodes[i].mLocation.longitude
            )

            val node = road.mNodes[i]
            val nodeMarker = Marker(map)
            nodeMarker.setPosition(node.mLocation)
            nodeMarker.title = "Step $i"

            if (node.mManeuverType == 1) {
                nodeMarker.icon = markerNormal
                nodeMarker.snippet = node.mInstructions
            } else {
                nodeMarker.icon = markerDanger
                var string = ""

                if ((node.mManeuverType % 2) == 0) {
                    string += " Attention à la foule "
                }
                if ((node.mManeuverType % 3) == 0) {
                    string += " Attention à la lumière "
                }
                if ((node.mManeuverType % 5) == 0) {
                    string += " Attention au bruit  "
                }

                nodeMarker.snippet = string + node.mInstructions
            }


            nodeMarker.subDescription =
                Road.getLengthDurationText(this, node.mLength, node.mDuration)
            map.overlays.add(nodeMarker)
        }
    }

    private fun openJourneyPlanner() {
        val intent = Intent(this, JourneyPlanner::class.java)
        resultJourneyPlanner.launch(intent)
    }

    private var resultJourneyPlanner =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data == null) {
                    Toast.makeText(this, "Data error", Toast.LENGTH_SHORT).show()
                } else {
                    val departureAddress = data.getStringExtra("Departure")
                    if (departureAddress == null) {
                        Toast.makeText(this, "Data error", Toast.LENGTH_SHORT).show()
                    } else {
                        val arrivalAddress = data.getStringExtra("Arrival")
                        if (arrivalAddress == null) {
                            Toast.makeText(this, "Data error", Toast.LENGTH_SHORT).show()
                        } else {
                            Thread {
                                Looper.prepare()
                                try {
                                    tisseoRouting(
                                        departureAddress,
                                        arrivalAddress,
                                        LocalDateTime.now(),
                                        wheelChair = data.getBooleanExtra("WheelChair", false),
                                        car = data.getBooleanExtra("Car", false),
                                        bike = data.getBooleanExtra("Bike", false),
                                        bus = data.getBooleanExtra("Bus", true),
                                        tram = data.getBooleanExtra("Tram", true),
                                        subway = data.getBooleanExtra("Subway", true),
                                        cableCar = data.getBooleanExtra("CableCar", true),
                                    )
                                } catch (e: NotFoundException) {
                                    Log.wtf("MainActivity", e.message, e)
                                    Toast.makeText(this, "No road found", Toast.LENGTH_SHORT).show()
                                }
                            }.start()
                        }
                    }
                }
            }
        }
}
