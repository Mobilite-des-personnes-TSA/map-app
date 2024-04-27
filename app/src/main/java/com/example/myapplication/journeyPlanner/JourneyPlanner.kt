package com.example.myapplication.journeyPlanner

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.tisseo.JourneyResponse
import com.example.myapplication.tisseo.PlacesResponse
import com.example.myapplication.tisseo.TisseoApiClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadNode
import org.osmdroid.util.GeoPoint
import kotlin.math.pow


/** TODO :
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

    data class RoadNodeForRouting(
        val road: Road,
        val f: Double
    )


    private suspend fun tisseoRouting (startPlace:String, endPlace:String, roadMode:String, dispatcher: CoroutineDispatcher){
        val file =ArrayList<RoadNodeForRouting>()
        file.clear()

        val roadNode = RoadNode()
        val geoPoint = adddresseToGeoPoint(startPlace,dispatcher)
        roadNode.mDuration = 0.0
        roadNode.mLocation = GeoPoint(geoPoint.latitude,geoPoint.longitude)
        val road = Road()
        road.mNodes.add(roadNode)

        var node = RoadNodeForRouting(road, heuristique(startPlace, endPlace, roadMode, dispatcher))
        file.add(node)

        while(road.mNodes[road.mNodes.size].mLocation.equals(adddresseToGeoPoint(endPlace,dispatcher))){
            node = selectBest(file)
            aRouting(geoPointToAddresse(node.road.mNodes[node.road.mNodes.size].mLocation,dispatcher), endPlace, roadMode, file, dispatcher)
        }

    }

    private suspend fun adddresseToGeoPoint(place : String, dispatcher: CoroutineDispatcher) : GeoPoint {
        val space = TisseoApiClient.places(place,"","fr" , dispatcher)?.placesList?.place?.get(0) as PlacesResponse.PlacesList.Place.PublicPlace
        return GeoPoint(space.x, space.y)
    }

    private suspend fun geoPointToAddresse(place : GeoPoint, dispatcher: CoroutineDispatcher) : String {
        val space = TisseoApiClient.places("",place.toString(),"fr" , dispatcher)?.placesList?.place?.get(0) as PlacesResponse.PlacesList.Place.PublicPlace
        return space.label
    }

    private fun selectBest(file : ArrayList<RoadNodeForRouting>) : RoadNodeForRouting {
        var out = file[0]

        for (node in file){
            if (out.f > node.f){
                out = node
            }
        }
        return out
    }

    private suspend fun aRouting(startPlace:String, endPlace:String, roadMode:String, file : ArrayList<RoadNodeForRouting>, dispatcher: CoroutineDispatcher){
        val journeyData = TisseoApiClient.journey(startPlace,endPlace,roadMode,"4", Dispatchers.Unconfined)

        for(i in 0..3){
            val road = journeyToRoad(journeyData!!.routePlannerResult.journeys[i].journey)

            val littleRoad = littleRoadNotGood(road)
            val heuristic = heuristique(littleRoad.mNodes[littleRoad.mNodes.size].mLocation.toString(), endPlace, roadMode, dispatcher)
            val cout = cout(littleRoad)

            val node = RoadNodeForRouting(littleRoad, (heuristic+cout))
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

    private fun impossibleRoad(roadNode: RoadNode): Boolean {
        return false
    }

    private suspend fun heuristique (startPlace:String, endPlace:String, roadMode:String, dispatcher: CoroutineDispatcher): Double {
        val start = adddresseToGeoPoint(startPlace,dispatcher)
        val end = adddresseToGeoPoint(endPlace,dispatcher)

        return (start.longitude - end.longitude).pow(2.0) + (start.latitude - end.latitude).pow(2.0)
    }

    private fun cout (road : Road): Double {
        return road.mLength
    }


    private fun littleRoadNotGood (road: Road) : Road {
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

}