package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.preference.PreferenceManager
import com.example.myapplication.tisseo.JourneyResponse
import com.example.myapplication.tisseo.PlacesResponse
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
import kotlin.math.pow


class MainActivity : AppCompatActivity() {
    private val requestPermissionRequestCode = 1
    private lateinit var map: MapView
    private lateinit var button: Button
    private lateinit var buttonSettigs: Button


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

        button = findViewById(R.id.button_journy)
        button.setOnClickListener(this::openJourneyPlanner)

        buttonSettigs = findViewById(R.id.button_settings)
        buttonSettigs.setOnClickListener {
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

        tisseoRouting("Métro Rangueil Toulouse", "Place du capitol Toulouse", "walk")

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
        val h: Double,
        val g: Double,
        val father : RoadNodeForRouting?
    )


    private fun tisseoRouting (startPlace:String, endPlace:String, roadMode:String){
        val file =ArrayList<RoadNodeForRouting>()
        file.clear()

        val roadNode = RoadNode()
        val geoPoint = addressToGeoPoint(startPlace)
        roadNode.mDuration = 0.0
        roadNode.mLocation = GeoPoint(geoPoint.latitude,geoPoint.longitude)

        val road = Road()
        road.mNodes.add(roadNode)

        var node = RoadNodeForRouting(road, heuristic(startPlace, endPlace, roadMode),0.0, null)
        file.add(node)

        while(node.road.mNodes[node.road.mNodes.size].mLocation.equals(addressToGeoPoint(endPlace))){
            node = selectBest(file)
            aRouting(geoPointToAddress(node.road.mNodes[node.road.mNodes.size].mLocation), endPlace, roadMode, node , file)
        }

        drawJourney(regroupRoad(node))
    }

    private fun regroupRoad (lastNode :  RoadNodeForRouting) : Road {
        val out = Road ()
        var father = lastNode.father
        var actual = lastNode

        while (father != null){
            actual = father
            father = actual.father

            for(littleRoad in actual.road.mNodes){
               out.mNodes.add(littleRoad)
            }
        }

        return out
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun addressToGeoPoint(place : String) : GeoPoint {
        val space = TisseoApiClient.places(place,"","fr" )?.placesList?.place?.get(0) as PlacesResponse.PlacesList.Place.PublicPlace
        return GeoPoint(space.x, space.y)
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun geoPointToAddress(place : GeoPoint) : String {
        val space = TisseoApiClient.places("",place.toString(),"fr" )?.placesList?.place?.get(0) as PlacesResponse.PlacesList.Place.PublicPlace
        return space.label
    }

    private fun selectBest(file : ArrayList<RoadNodeForRouting>) : RoadNodeForRouting {
        var out = file[0]

        for (node in file){
            if (out.g + out.h > node.g + node.h){
                out = node
            }
        }
        return out
    }

    private fun keepTheBest(file : ArrayList<RoadNodeForRouting>, new  : RoadNodeForRouting){
        var index = 0
        var old = file[0].road
        val locFinal = new.road.mNodes[new.road.mNodes.size].mLocation

        while(index < file.size && old.mNodes[old.mNodes.size].mLocation !=locFinal){
            old = file[index].road
            index ++
        }

        if(index< file.size){
            if(file[index].g > new.g){
                file.removeAt(index)
                file.add(new)
            }
        }else {
            file.add(new)
        }
    }

    private fun aRouting(startPlace:String, endPlace:String, roadMode:String, father : RoadNodeForRouting, file : ArrayList<RoadNodeForRouting>){
        val journeyData = TisseoApiClient.journey(startPlace,endPlace,roadMode,"4")

        for(i in 0..3){
            val road = journeyToRoad(journeyData!!.routePlannerResult.journeys[i].journey)

            val littleRoad = goodPartOfRoad(road)
            val heuristic = heuristic(littleRoad.mNodes[littleRoad.mNodes.size].mLocation.toString(), endPlace, roadMode)
            val price = price(littleRoad, father)

            val node = RoadNodeForRouting(littleRoad, heuristic, price, father)
            keepTheBest(file, node)
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

    private fun impossibleRoad(roadNode: RoadNode): Boolean {
        return false
    }

    private fun heuristic (startPlace:String, endPlace:String, roadMode:String): Double {
        val start = addressToGeoPoint(startPlace)
        val end = addressToGeoPoint(endPlace)

        return (start.longitude - end.longitude).pow(2.0) + (start.latitude - end.latitude).pow(2.0)
    }

    private fun price (road : Road, father : RoadNodeForRouting): Double {
        return road.mLength + father.h
    }


    private fun goodPartOfRoad (road: Road) : Road {
        val sousRoad = Road ()
        var valide = true
        var index = 0

        while(valide && index < road.mNodes.size){
            if (impossibleRoad(road.mNodes[index])){
                valide=false
            } else{
                sousRoad.mNodes.add(road.mNodes[index])
                index++
            }
        }

        return sousRoad
    }


    fun drawJourney(road: Road) {
        val roadOverlay: Polyline = RoadManager.buildRoadOverlay(road)
        map.overlays.clear()
        map.overlays.add(roadOverlay)
        val nodeIcon = resources.getDrawable(R.drawable.marker_node, theme)
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

    private fun openJourneyPlanner(view: View) {
        val intent = Intent(this, JourneyPlanner::class.java)
        resultJourneyPlanner.launch(intent)
    }

    private var resultJourneyPlanner =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.also {
                    it.getStringExtra("Departure")?.also { departureAddress ->
                        it.getStringExtra("Arrival")?.also { arrivalAddress ->
                            tisseoRouting(departureAddress, arrivalAddress, "walk")
                        }
                    }
                }
            }

        }
}
