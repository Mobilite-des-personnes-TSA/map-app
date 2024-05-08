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
import com.example.myapplication.tisseo.JourneyResponse
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
import kotlin.math.exp
import java.util.Date

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

    data class RoadNodeForRouting(
        val road: Road,
        val cost: Double,

    )


    private fun tisseoRouting (startPlace:String, endPlace:String, roadMode:String, date : Date){
        val file =ArrayList<RoadNodeForRouting>()
        file.clear()

        val geoPointStart = addressToGeoPoint(startPlace)
        val geoPointEnd = addressToGeoPoint(endPlace)

        val newDate20 = date
        newDate20.minutes = date.minutes+20

        val newDate40 = date
        newDate40.minutes = date.minutes+40

        val listAll = ArrayList<Int>()
        listAll.add(1);listAll.add(2); listAll.add(3)

        val list12 = ArrayList<Int>()
        list12.add(1);list12.add(2)

        val list13 = ArrayList<Int>()
        list13.add(1);list13.add(3)

        val list32 = ArrayList<Int>()
        list32.add(2); list32.add(3)

        val list1 = ArrayList<Int>()
        list1.add(1)

        val list2 = ArrayList<Int>()
        list2.add(2)

        val list3 = ArrayList<Int>()
        list3.add(3)


        val listList = ArrayList< ArrayList<Int>>()
        listList.add(listAll);listList.add(list3);listList.add(list1);listList.add(list13)

        //listList.add(list2);listList.add(list12);listList.add(list32)
        // y'a pas de tram a toulouse donc le mode de transport 2 ne sert à rien

        val listDate = ArrayList<Date>()
        listDate.add(date); listDate.add(newDate20); listDate.add(newDate40)

        for (list in listList){
            for(newDate in listDate){
                aRouting(geoPointStart, geoPointEnd, roadMode, file, newDate, list)
            }
        }

        drawJourney(selectBest(file).road)


    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun addressToGeoPoint(place : String) : GeoPoint {
        val space = TisseoApiClient.places(place,"","fr" )?.placesList?.place?.get(0)!!
        return GeoPoint(space.y, space.x)
    }


    private fun printGeoPointTisseo(place: GeoPoint) : String {
        return place.longitude.toString()+","+place.latitude.toString()
    }

    private fun printDateTisseo(date : Date) :String {
        return ""+(date.year+1900)+"-"+(date.month+1)+"-"+date.date+" "+date.hours+":"+date.minutes
    }

    private fun printRollingStockTisseo(rollingStocks : ArrayList<Int> ) :String {
        var out = ""

        for(index in rollingStocks){
            out += "commercial_mode:$index"
            if (index!=rollingStocks.get(rollingStocks.size-1)) out+=","
        }
        return out
    }

    private fun selectBest(file : ArrayList<RoadNodeForRouting>) : RoadNodeForRouting {
        var out = file[0]

        for (node in file){
            if (out.cost> node.cost){
                out = node
            }
        }
        return out
    }


    private fun aRouting(startPlace:GeoPoint, endPlace:GeoPoint, roadMode:String, file : ArrayList<RoadNodeForRouting>, date : Date, rollingStocks :ArrayList<Int> ){
        val journeyData = TisseoApiClient.journey(printGeoPointTisseo(startPlace),printGeoPointTisseo(endPlace),roadMode,"4", printDateTisseo(date), printRollingStockTisseo(rollingStocks))

        for(i in 0..3){
            val road = journeyToRoad(journeyData!!.routePlannerResult.journeys[i].journey)

            val price = price(road)

            val node = RoadNodeForRouting(road, price)
            file.add(node)
        }
    }

    private fun journeyToRoad(journey :  JourneyResponse.RoutePlannerResult.JourneyItem.Journey ): Road {
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

        return road
    }

    /*
        Le but de cette fonction est de calculé à qu'elle point un chemin est pénible
        Sachant qu'une forte pénibilité sur une partie du trajet sera plus impactant pour l'utilisateur
        qu'une pénibilité moyenne sur tout le trajet.
        De plus le changement de pénabilité entre des chemin déja peu pénible aura
        un impact faible

        nous avons donc décider de la représenter par une fonction exponentiel sur chaque critère
     */
    private fun price (road : Road): Double {
        var out = 0.0

        for( portion in road.mNodes){
            val coefCrown = crown(portion)
            val coefLight = light(portion)
            val coefSound = sound(portion)

            out += (exp(coefCrown)+ exp(coefLight) + exp(coefSound)) * portion.mLength
        }

        return out
    }

    /*
        Ces fonctions servent à redonner les informations sur les routes
        et de les comparer au sensibilité de l'utilisateur
     */
    private fun crown (roadNode: RoadNode) : Double{
        return 1.0
    }
    private fun light (roadNode: RoadNode) : Double{
        return 1.0
    }
    private fun sound (roadNode: RoadNode) : Double{
        return 1.0
    }


    private fun drawJourney(road: Road) {
        val roadOverlay: Polyline = RoadManager.buildRoadOverlay(road)
        map.overlays.clear()
        map.overlays.add(roadOverlay)
        val nodeIcon = ResourcesCompat.getDrawable(resources, R.drawable.marker_node, theme)
        for (i in road.mNodes.indices) {
            Log.d(1.toString(), "ajout à la map du point : latitude " + road.mNodes[i].mLocation.latitude + " longitude "+road.mNodes[i].mLocation.longitude)

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
                            Thread {
                                val date = Date()
                                tisseoRouting(departureAddress, arrivalAddress, "walk", date)
                            }.start()
                        }
                    }
                }
            }

        }
}
