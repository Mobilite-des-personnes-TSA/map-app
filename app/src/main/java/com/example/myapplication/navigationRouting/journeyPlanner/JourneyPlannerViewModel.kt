package com.example.myapplication.navigationRouting.journeyPlanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.navigationRouting.journeyPlanner.JourneyPlannerFragment
import com.example.myapplication.navigationRouting.mapDisplay.MapDisplayFragment
import com.example.myapplication.tisseo.JourneyResponse
import com.example.myapplication.tisseo.TisseoApiClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.ExperimentalSerializationApi
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadNode
import org.osmdroid.util.GeoPoint

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewModelScope
import com.example.myapplication.tisseo.BUS
import com.example.myapplication.tisseo.BUS_RAPID_TRANSIT
import com.example.myapplication.tisseo.CABLE_CAR
import com.example.myapplication.tisseo.METRO
import com.example.myapplication.tisseo.SHUTTLE
import com.example.myapplication.tisseo.TRAMWAY
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.exp

data class RoadState(
    val road: Road,
    val nuisance: Double
)

class JourneyPlannerViewModel : ViewModel() {

    private val tisseoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private lateinit var sharedPreferences : SharedPreferences

    private val _text = MutableLiveData<String>().apply {
        value = "This is Journey Planner Fragment"
    }

    private val _roadState = MutableLiveData<RoadState>()
    val roadState: LiveData<RoadState> = _roadState

    // TODO : Check the coefficients
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

    private suspend fun osrmRouting(
        startPoint: GeoPoint,
        endPoint: GeoPoint,
        mode: String,
        roads: MutableList<RoadState>,
        dispatcher: CoroutineDispatcher
    ) {
        withContext(dispatcher){
            val roadManager = OSRMRoadManager(this.coroutineContext as Context, "User")
            when (mode) {
                "bike" -> roadManager.setMean(OSRMRoadManager.MEAN_BY_BIKE)
                "car" -> roadManager.setMean(OSRMRoadManager.MEAN_BY_CAR)
                else -> roadManager.setMean(OSRMRoadManager.MEAN_BY_FOOT)
                //TODO : No wheelchair handling ?
            }
            val waypoints = arrayListOf(startPoint, endPoint)
            val road = roadManager.getRoad(waypoints)
            val price = price(road)
            roads.add(RoadState(road, price))
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun addressToGeoPoint(place: String, dispatcher: CoroutineDispatcher) = TisseoApiClient.places(place, "", "fr", dispatcher)!!
        .placesList.place[0].let { GeoPoint(it.x, it.y) }

    private suspend fun selectBest(roads: List<RoadState>) =
        roads.minByOrNull(RoadState::nuisance)!!

    private suspend fun printGeoPointTisseo(place: GeoPoint) : String {
        return place.longitude.toString()+","+place.latitude.toString()
    }

    private fun price(road : Road) = road.mNodes.sumOf {
        (crowd(it) + light(it) + sound(it) * it.mLength)
    }

    private suspend fun tisseoRouting(
        startPlace: GeoPoint,
        endPlace: GeoPoint,
        roadMode: String,
        roads: MutableList<RoadState>,
        date: LocalDateTime,
        rollingStocks: List<String>,
        dispatcher: CoroutineDispatcher
    ){
        viewModelScope.launch{
            TisseoApiClient.journey(
                "${startPlace.latitude},${startPlace.longitude}",
                "${endPlace.latitude},${endPlace.longitude}",
                roadMode,
                "4",
                date.format(tisseoDateFormatter),
                rollingStocks.joinToString(","),
                dispatcher
            )?.also {
                it.routePlannerResult.journeys.forEach { j ->
                    val road = journeyToRoad(j.journey, dispatcher)
                    val price = price(road)
                    roads.add(RoadState(road, price))
                }
            }
        }
    }

    /** Generates a route based on the criteria of the user,
     *  Tries multiple routes and selects the best, update UIRoadState
     *
     */
    private suspend fun routing (
        startPlace: String,
        endPlace: String,
        wheelChair: Boolean,
        bus: Boolean,
        tram: Boolean,
        subway: Boolean,
        cableCar: Boolean,
        car: Boolean,
        bike: Boolean,
        date: LocalDateTime,
        standardMode:String,
        dispatcher: CoroutineDispatcher
    ){
        val roads = ArrayList<RoadState>()

        viewModelScope.launch{
            val geoPointStart = addressToGeoPoint(startPlace, dispatcher)
            val geoPointEnd = addressToGeoPoint(endPlace, dispatcher)

            val listDate = listOf(
                date,
                date.plusMinutes(20),
                date.plusMinutes(40),
            )

            val standardModeList = mutableListOf("walk")
            if (bike) standardModeList.add("bike")
            if (wheelChair) standardModeList.add("wheelchair")
            if (car) standardModeList.add("car")

            val transportsList = ArrayList<List<String>>()
            if (bus) transportsList.add(listOf(SHUTTLE, BUS))
            if (tram) transportsList.add(listOf(TRAMWAY, BUS_RAPID_TRANSIT))
            if (subway) transportsList.add(listOf(METRO))
            if (cableCar) transportsList.add(listOf(CABLE_CAR))
            transportsList.add(transportsList.flatten())

            val map = MapDisplayFragment();

            for (newDate in listDate) {

                for (standardMode in standardModeList) {
                    osrmRouting(geoPointStart, geoPointEnd, standardMode, roads, dispatcher)
                    for (transport in transportsList)
                        tisseoRouting(
                            geoPointStart,
                            geoPointEnd,
                            standardMode,
                            roads,
                            newDate,
                            transport,
                            dispatcher
                        )
                }
                if (car) osrmRouting(geoPointStart, geoPointEnd, "car", roads, dispatcher)
            }
            _roadState.value = RoadState(selectBest(roads).road, price(selectBest(roads).road));
        }
    }


    /** Gets a JourneyResponse from the Tisseo API and turns it to a road
     *
     */
    suspend fun journeyToRoad(
        journey :  JourneyResponse.RoutePlannerResult.JourneyItem.Journey,
        dispatcher : CoroutineDispatcher
    ): Road {
        val road = Road()
        withContext(dispatcher){
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
        }
        return road
    }
}



