package com.example.myapplication.journeyPlanner

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.MapDisplay
import com.example.myapplication.tisseo.JourneyResponse
import com.example.myapplication.tisseo.TisseoApiClient
import kotlinx.coroutines.CoroutineDispatcher
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadNode
import org.osmdroid.util.GeoPoint

import kotlinx.serialization.ExperimentalSerializationApi


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

        val geoPointStart = addressToGeoPoint(startPlace, dispatcher)
        val geoPointEnd = addressToGeoPoint(endPlace, dispatcher)

        val map = MapDisplay();

        aRouting(geoPointStart, geoPointEnd, "walk", file, dispatcher)
        aRouting(geoPointStart, geoPointEnd, "car", file, dispatcher)

        aRouting(geoPointStart, geoPointEnd, roadMode, file, dispatcher)
        map.drawJourney(selectBest(file).road);
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend private fun addressToGeoPoint(place : String, dispatcher: CoroutineDispatcher) : GeoPoint {
        val space = TisseoApiClient.places(place,"","fr",dispatcher )?.placesList?.place?.get(0)!!
        return GeoPoint(space.y, space.x)
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

    private fun printGeoPointTisseo(place: GeoPoint) : String {
        return place.longitude.toString()+","+place.latitude.toString()
    }

    private suspend fun aRouting(startPlace:GeoPoint, endPlace:GeoPoint, roadMode:String, file : ArrayList<RoadNodeForRouting>, dispatcher: CoroutineDispatcher){
        val journeyData = TisseoApiClient.journey(printGeoPointTisseo(startPlace),printGeoPointTisseo(endPlace),roadMode,"4", dispatcher)

        for(i in 0..3){
            val road = journeyToRoad(journeyData!!.routePlannerResult.journeys[i].journey)
            val cout = cout(road)

            val node = RoadNodeForRouting(road, (cout))
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


    private fun cout (road : Road): Double {
        return road.mLength
    }



}