import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.osmdroid.util.GeoPoint

import com.example.myapplication.tisseo.TisseoApiClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.bonuspack.routing.RoadNode
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/** TODO:
 *  Create a flow to pass Data to MapDisplay
 *  Instantiate the coroutines
 *  Link to Tisséo API and its Data
 *  Event handling?
 *  Naviguation ?
 *  UI States
 *
 *  Handle LifeCycle?
 */

// Miror TisseoApiClient Data Classes
data class RoadUIState(
    val nodes: List<NodeUIState>
)
data class NodeUIState(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val noiseCoefficient: Double
)

class MapDisplayViewModel : ViewModel() {

    // Update the user position, send it asLiveData()
    private val _userPosition = MutableLiveData<GeoPoint>()
    val userPosition: LiveData<GeoPoint> = _userPosition

    // When knew info on Road                                           Should it be a StateFlow?
    private val _roadState = MutableLiveData<RoadUIState>()
    val roadState: LiveData<RoadUIState> = _roadState

    /** Updates the user's current position
     */
    suspend fun updateUserPosition() {
        // Is it a suspend function ? or a coroutines ?
        // Simulate loading data from a repository
        //TODO: Implement updating user Position
        _userPosition.value = GeoPoint(43.6, 1.4333)
    }

    /** Updates the Road to be drawn in the UI
     */
    suspend fun updateRoadState(newState: RoadUIState) {
        //TODO: Implement UIState updater
        //  Beware Data transformation
        _roadState.value = newState
    }

    //Instantiate Repository to link to Tisséo API
        //private val repository =

    // LiveData or StateFlow to hold data
        //val data = repository.getData()

    // Function to fetch data from repository
        /** fun fetchData() {
                viewModelScope.launch {
                    repository.fetchData()
                }
            }*/


    /** Draw a Map using the tisseoApi
     *
     */
    suspend fun tisseoRouting(startPlace:String, endPlace:String, dispatcher : CoroutineDispatcher){

        viewModelScope.launch {
            val journeyData = TisseoApiClient.journey(startPlace,endPlace,"walk","1", dispatcher = dispatcher)
            val road = Road()

            // TODO : All of this process here should create a RoadUIState
            if (journeyData != null) {
                journeyData.routePlannerResult.journeys[0].journey.chunks.forEach{chunk ->

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
            // The "as" should disappear
            updateRoadState(road as RoadUIState)
        }
    }

    /** Function to Draw a route independent of public transportation
     *  TODO : Why use separate functions ?
     */
    fun osrmRouting(startPoint:GeoPoint,endPoint: GeoPoint,mode:String, dispatcher : CoroutineDispatcher){

        viewModelScope.launch {
            val roadManager: RoadManager = OSRMRoadManager(this, "User")

            when (mode) {
                "bike" -> {
                    (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_BIKE)
                }
                "car" -> {
                    (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_CAR)
                }
                else -> {(roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_FOOT)}
            }

            val waypoints = ArrayList<GeoPoint>()
            waypoints.add(startPoint)
            waypoints.add(endPoint)

            //TODO : Should make a suspend function out of this
            withContext(dispatcher) {
                val road: Road = roadManager.getRoad(waypoints)

                if (road.mStatus != Road.STATUS_OK) {
                    Toast.makeText(
                        this,
                        "Error when loading the road - status=" + road.mStatus,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // The "as" should disappear
                    updateRoadState(road as RoadUIState)
                }
            }
        }
    }




}