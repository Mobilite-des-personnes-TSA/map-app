package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
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
import com.example.myapplication.tisseo.JourneyResponse
import com.example.myapplication.tisseo.METRO
import com.example.myapplication.tisseo.SHUTTLE
import com.example.myapplication.tisseo.TRAMWAY
import com.example.myapplication.tisseo.TisseoApiClient
import kotlinx.serialization.ExperimentalSerializationApi
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

    private lateinit var sharedPreferences : SharedPreferences

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
                resources,
                com.google.android.material.R.color.foreground_material_light,
                theme
            )
        )
        markerDanger = ResourcesCompat.getDrawable(resources, R.drawable.map_marker_alert, theme)!!
        markerDanger.setTint(
            ResourcesCompat.getColor(
                resources,
                com.google.android.material.R.color.error_color_material_light,
                theme
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
        wheelChair: Boolean,
        bus: Boolean,
        tram: Boolean,
        subway: Boolean,
        cableCar: Boolean,
        car: Boolean,
        bike: Boolean,
        date: LocalDateTime
    ) {
        val file = ArrayList<RoadNodeForRouting>()

        val geoPointStart = addressToGeoPoint(startPlace)
        val geoPointEnd = addressToGeoPoint(endPlace)

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
                for (transport in transports)
                    aRouting(geoPointStart, geoPointEnd, roadMode, file, newDate, transport)
            }

            if (car) osrmRouting(geoPointStart, geoPointEnd, "car", file)
        }

        drawJourney(selectBest(file).road)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun addressToGeoPoint(place: String) = TisseoApiClient.places(place, "", "fr")!!
        .placesList.place[0].let { GeoPoint(it.x, it.y) }

    private fun selectBest(file: List<RoadNodeForRouting>) =
        file.minByOrNull(RoadNodeForRouting::cost)!!


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
        )?.also {
            it.routePlannerResult.journeys.forEach { j ->
                val road = journeyToRoad(j.journey)
                val price = price(road)
                file.add(RoadNodeForRouting(road, price))
            }
        }
    }

    private fun journeyToRoad(journey: JourneyResponse.RoutePlannerResult.JourneyItem.Journey): Road {
        val road = Road()

        journey.chunks.forEach { chunk ->

            if (chunk.service != null) {
                val wkt = chunk.service.wkt
                val coordinates = wkt.substringAfter("(").substringBeforeLast(")").split(",")
                val roadNode = RoadNode()
                roadNode.mInstructions = chunk.service.text?.text
                val (long, lat) = coordinates[0].trim().split(" ")
                val units =
                    chunk.service.duration.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                val duration =
                    3600 * units[0].toInt() + 60 * units[1].toInt() + units[2].toInt()
                roadNode.mDuration = duration.toDouble()
                roadNode.mLocation = GeoPoint(lat.toDouble(), long.toDouble())
                roadNode.mManeuverType = 1
                road.mNodes.add(roadNode)
                coordinates.forEach { coordinate ->
                    val (longitude, latitude) = coordinate.trim().split(" ")
                    road.mRouteHigh.add(GeoPoint(latitude.toDouble(), longitude.toDouble()))
                }
            } else if (chunk.street != null) {
                val wkt = chunk.street.wkt
                val roadNode = RoadNode()
                roadNode.mInstructions = chunk.street.text.text
                val units =
                    chunk.street.duration.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                val duration =
                    3600 * units[0].toInt() + 60 * units[1].toInt() + units[2].toInt()
                roadNode.mDuration = duration.toDouble()
                roadNode.mLength = chunk.street.length.toDouble() / 1000
                roadNode.mLocation = GeoPoint(
                    chunk.street.startAddress.connectionPlace.latitude.toDouble(),
                    chunk.street.startAddress.connectionPlace.longitude.toDouble()
                )
                roadNode.mManeuverType = 1
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

        return road
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
     */
    /*
        On en profite aussi pour définir les points de sensibilité
     */

    private fun crowd(roadNode: RoadNode): Double {
        val out = exp(
            if ((43.55 < roadNode.mLocation.latitude && roadNode.mLocation.latitude < 43.65) &&
                (1.4 < roadNode.mLocation.longitude && roadNode.mLocation.longitude < 1.5)
            ) {
                (Math.random() * 2 + 1)* sharedPreferences.getInt("crowd",0)
            } else {
                Math.random()
            })

        if (out  > 10) {
            roadNode.mManeuverType *= 2
        }

        return out
    }

    private fun light(roadNode: RoadNode): Double {
        val out =exp(
            if ((43.55 < roadNode.mLocation.latitude && roadNode.mLocation.latitude < 43.65) &&
                (1.4 < roadNode.mLocation.longitude && roadNode.mLocation.longitude < 1.5)
            ) {
                (Math.random() * 2 + 1)* sharedPreferences.getInt("light",0)
            } else {
                Math.random()
            })

        if (out  > 10) {
            roadNode.mManeuverType *= 3
        }

        return out
    }

    private fun sound(roadNode: RoadNode): Double {
        val out =exp(
            if ((43.55 < roadNode.mLocation.latitude && roadNode.mLocation.latitude < 43.65) &&
                (1.4 < roadNode.mLocation.longitude && roadNode.mLocation.longitude < 1.5)
            ) {
                (Math.random() * 2 + 1) * sharedPreferences.getInt("sound",0)
            } else {
                Math.random()
            })

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
                result.data?.also {
                    it.getStringExtra("Departure")?.also { departureAddress ->
                        it.getStringExtra("Arrival")?.also { arrivalAddress ->
                            it.getBooleanExtra("Bus", true).also { bus ->
                                it.getBooleanExtra("Subway", true).also { subway ->
                                    it.getBooleanExtra("CableCar", true).also { cableCar ->
                                        it.getBooleanExtra("Tram", true).also { tram ->
                                            it.getBooleanExtra("WheelChair", false)
                                                .also { wheelChair ->
                                                    it.getBooleanExtra("Car", false).also { car ->
                                                        it.getBooleanExtra("Bike", false)
                                                            .also { bike ->
                                                                Thread {
                                                                    tisseoRouting(
                                                                        departureAddress,
                                                                        arrivalAddress,
                                                                        wheelChair,
                                                                        bus,
                                                                        tram,
                                                                        subway,
                                                                        cableCar,
                                                                        car,
                                                                        bike,
                                                                        LocalDateTime.now()
                                                                    )
                                                                }.start()
                                                            }
                                                    }
                                                }
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
            }
        }
}
