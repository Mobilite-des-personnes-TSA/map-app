package com.example.myapplication.ui.journeyPlanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.MapDisplay
import com.example.myapplication.journeyPlanner.JourneyPlanner
import com.example.myapplication.tisseo.JourneyResponse
import com.example.myapplication.tisseo.TisseoApiClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.ExperimentalSerializationApi
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadNode
import org.osmdroid.util.GeoPoint

class JourneyPlannerViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is dashboard Fragment"
    }
    val text: LiveData<String> = _text

    /** Complete Searches
     * URL : https://api.tisseo.fr/v2/places.<format>?...paramètres...
     *
     *
     */

    /** Searches
     *   URL : https://api.tisseo.fr/v2/jouneys.<format>?...paramètres...
     */

    /** Flow of Journeys
     *  When modifying any of the parameters
     *  recall the coroutine
     *  Flow should be handled in the Tisseo Api
     *
     */

    data class RoadNodeForRouting(
        val road: Road,
        val f: Double
    )

    private suspend fun tisseoRouting (startPlace:String, endPlace:String, roadMode:String, dispatcher: CoroutineDispatcher){
        val file =ArrayList<JourneyPlanner.RoadNodeForRouting>()
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

    private fun selectBest(file : ArrayList<JourneyPlanner.RoadNodeForRouting>) : JourneyPlanner.RoadNodeForRouting {
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

    private suspend fun aRouting(startPlace: GeoPoint, endPlace: GeoPoint, roadMode:String, file : ArrayList<JourneyPlanner.RoadNodeForRouting>, dispatcher: CoroutineDispatcher){
        val journeyData = TisseoApiClient.journey(printGeoPointTisseo(startPlace),printGeoPointTisseo(endPlace),roadMode,"4", dispatcher)

        for(i in 0..3){
            val road = journeyToRoad(journeyData!!.routePlannerResult.journeys[i].journey)
            val cout = cout(road)

            val node = JourneyPlanner.RoadNodeForRouting(road, (cout))
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