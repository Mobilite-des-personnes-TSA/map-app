package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.example.myapplication.tisseo.TRAMWAY
import com.example.myapplication.tisseo.TisseoApiClient
import kotlinx.serialization.ExperimentalSerializationApi
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

    data class RoadNodeForRouting(val road: Road, val cost: Double)


    private fun tisseoRouting(
        startPlace: String,
        endPlace: String,
        wheelChair: Boolean,
        bus: Boolean,
        tram: Boolean,
        subway: Boolean,
        cableCar: Boolean,
        date: LocalDateTime
    ) {
        val file = ArrayList<RoadNodeForRouting>()

        val geoPointStart = addressToGeoPoint(startPlace)
        val geoPointEnd = addressToGeoPoint(endPlace)

        var roadMode = "walk"
        if (wheelChair) {
            roadMode = "wheelchair"
        }

        val listList = ArrayList<List<String>>()



        if (bus) {
            if (tram) {
                if (subway) {
                    if (cableCar) {
                        listList.add(listOf(METRO, TRAMWAY, CABLE_CAR, BUS_RAPID_TRANSIT, BUS))
                    } else {
                        listList.add(listOf(METRO, TRAMWAY, BUS_RAPID_TRANSIT, BUS))
                    }
                } else {
                    if (cableCar) {
                        listList.add(listOf(TRAMWAY, CABLE_CAR, BUS_RAPID_TRANSIT, BUS))
                    } else {
                        listList.add(listOf(TRAMWAY, BUS_RAPID_TRANSIT, BUS))
                    }
                }
            } else {
                if (subway) {
                    if (cableCar) {
                        listList.add(listOf(METRO, CABLE_CAR, BUS_RAPID_TRANSIT, BUS))
                    } else {
                        listList.add(listOf(METRO, BUS_RAPID_TRANSIT, BUS))
                    }
                } else {
                    if (cableCar) {
                        listList.add(listOf(CABLE_CAR, BUS_RAPID_TRANSIT, BUS))
                    } else {
                        listList.add(listOf(BUS_RAPID_TRANSIT, BUS))
                    }
                }
            }


        } else {
            if (tram) {
                if (subway) {
                    if (cableCar) {
                        listList.add(listOf(METRO, TRAMWAY, CABLE_CAR))
                    } else {
                        listList.add(listOf(METRO, TRAMWAY))
                    }
                } else {
                    if (cableCar) {
                        listList.add(listOf(TRAMWAY, CABLE_CAR))
                    } else {
                        listList.add(listOf(TRAMWAY))
                    }
                }
            } else {
                if (subway) {
                    if (cableCar) {
                        listList.add(listOf(METRO, CABLE_CAR))
                    } else {
                        listList.add(listOf(METRO))
                    }
                } else {
                    if (cableCar) {
                        listList.add(listOf(CABLE_CAR))
                    }
                }

            }
        }


        // <Plot a dit> : y'a pas de tram a toulouse donc le mode de transport 2 ne sert à rien

        val listDate = listOf(
            date,
            date.plusMinutes(20),
            date.plusMinutes(40),
        )

        for (list in listList)
            for (newDate in listDate)
                aRouting(geoPointStart, geoPointEnd, roadMode, file, newDate, list)

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
                val duration = 3600 * units[0].toInt() + 60 * units[1].toInt() + units[2].toInt()
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
                val units =
                    chunk.street.duration.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                val duration = 3600 * units[0].toInt() + 60 * units[1].toInt() + units[2].toInt()
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

        return road
    }

    /*
        Le but de cette fonction est de calculer à qu'elle point un chemin est pénible
        Sachant qu'une forte pénibilité sur une partie du trajet sera plus impactant pour l'utilisateur
        qu'une pénibilité moyenne sur tout le trajet.
        De plus le changement de pénibilité entre des chemins déja peu pénible aura
        un impact faible

        nous avons donc décidé de la représenter par une fonction exponentielle sur chaque critère
     */
    private fun price(road: Road) = road.mNodes.sumOf {
        (exp(crown(it)) + exp(light(it)) + exp(sound(it))) * it.mLength
    }

    /*
        Ces fonctions servent à redonner les informations sur les routes
        et de les comparer aux sensibilités de l'utilisateur
     */
    @Suppress("UNUSED_PARAMETER")
    private fun crown(roadNode: RoadNode) = 1.0

    @Suppress("UNUSED_PARAMETER")
    private fun light(roadNode: RoadNode) = 1.0

    @Suppress("UNUSED_PARAMETER")
    private fun sound(roadNode: RoadNode) = 1.0


    private fun drawJourney(road: Road) {
        val roadOverlay: Polyline = RoadManager.buildRoadOverlay(road)
        map.overlays.clear()
        map.overlays.add(roadOverlay)
        val nodeIcon = ResourcesCompat.getDrawable(resources, R.drawable.marker_node, theme)
        for (i in road.mNodes.indices) {
            Log.d(
                1.toString(),
                "ajout à la map du point : latitude " + road.mNodes[i].mLocation.latitude + " longitude " + road.mNodes[i].mLocation.longitude
            )

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
                            it.getBooleanExtra("Bus", true).also { bus ->
                                it.getBooleanExtra("Subway", true).also { subway ->
                                    it.getBooleanExtra("CableCar", true).also { cableCar ->
                                        it.getBooleanExtra("Tram", true).also { tram ->
                                            it.getBooleanExtra("WheelChair", false)
                                                .also { wheelChair ->
                                                    Thread {
                                                        tisseoRouting(
                                                            departureAddress,
                                                            arrivalAddress,
                                                            wheelChair,
                                                            bus,
                                                            tram,
                                                            subway,
                                                            cableCar,
                                                            LocalDateTime.now()
                                                        )
                                                    }.start()
                                                }
                                        }
                                    }
                                }

                            }
                        }
