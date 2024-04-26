package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.JourneyPlanner.JourneyPlannerViewModel
import com.example.myapplication.tisseo.JourneyResponse
import com.example.myapplication.tisseo.TisseoApiClient
import kotlinx.coroutines.Dispatchers
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadNode
import org.osmdroid.util.GeoPoint
import java.lang.Math.pow


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

    data class RoadNodeForRouting(
        val road: Road?,
        val f: Double
    )


    private suspend fun TisseoRouting (startPlace:String, endPlace:String, roadMode:String){
        val file =ArrayList<RoadNodeForRouting>();
        file.clear();

        val roadNode = RoadNode();
        val geoPoint = AdddresseToGeoPoint(startPlace);
        roadNode.mDuration = 0.0;
        roadNode.mLocation = GeoPoint(geoPoint.latitude,geoPoint.longitude);
        val road = Road();
        road.mNodes.add(roadNode)

        var node = RoadNodeForRouting(road, Heuristique(startPlace, endPlace, roadMode));
        file.add(node);

        while(road.mNodes.get(road.mNodes.size).mLocation.equals(AdddresseToGeoPoint(endPlace))){
            node = SelectBest(file);
            ARouting(GeoPointToAddresse(road.mNodes.get(road.mNodes.size).mLocation), endPlace, roadMode, file);
        }

    }

    //TO DO
    private fun AdddresseToGeoPoint(place : String) : GeoPoint {
        return GeoPoint();
    }

    private fun GeoPointToAddresse(place : GeoPoint) : String {
        return "";
    }

    private fun SelectBest(file : ArrayList<RoadNodeForRouting>) : RoadNodeForRouting {
        var out = file.get(0);

        for (node in file){
            if (out.f > node.f){
                out = node;
            }
        }
        return out;
    }

    private suspend fun ARouting(startPlace:String, endPlace:String, roadMode:String, file : ArrayList<RoadNodeForRouting>){
        val journeyData = TisseoApiClient.journey(startPlace,endPlace,roadMode,"4", Dispatchers.Unconfined);

        for(i in 0..3){
            val road = JourneyToRoad(journeyData!!.routePlannerResult.journeys[i].journey);

            val littleRoad = LittleRoadNotGood(road);
            val heuristic = Heuristique(littleRoad.mNodes.get(littleRoad.mNodes.size).mLocation.toString(), endPlace, roadMode);
            val cout = Cout(littleRoad);

            val node = RoadNodeForRouting(littleRoad, (heuristic+cout));
            file.add(node);
        }
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

    private fun NotGoodRoad(roadNode: RoadNode): Boolean {
        return false;
    }

    private fun Heuristique (startPlace:String, endPlace:String, roadMode:String): Double {
        val start = AdddresseToGeoPoint(startPlace);
        val end = AdddresseToGeoPoint(endPlace);

        return pow((start.longitude - end.longitude),2.0) + pow((start.latitude - end.latitude),2.0);
    }

    private fun Cout (road : Road): Double {
        return road.mLength;
    }


    private fun LittleRoadNotGood (road: Road) : Road {
        var sousRoad = Road ();
        var valide = true;
        var index = 0

        while(valide && index < road.mNodes.size){
            if (NotGoodRoad(road.mNodes[index])){
                valide=false;
            } else{
                sousRoad.mNodes.add(road.mNodes[index]);
                index++;
            }
        }

        return sousRoad;
    }

}