package com.example.myapplication.navigationRouting.mapDisplay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.bonuspack.routing.RoadNode
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import androidx.navigation.fragment.navArgs
import kotlin.coroutines.coroutineContext


data class RoadUIState(
    val road : Road,
    //TODO : add a color ?
    val nuisance : Double
)

class MapDisplayViewModel : ViewModel() {

    // Update the user position, send it asLiveData()
    private val _userPosition = MutableLiveData<GeoPoint>()
    val userPosition: LiveData<GeoPoint> = _userPosition

    // When knew info on Road
    private val _roadState = MutableLiveData<RoadUIState>()
    val roadState: LiveData<RoadUIState> = _roadState

    /** Updates the Road to be drawn in the UI
     */
    suspend fun updateRoadState(newRoad: Road, newNuisance : Double) {
        // TODO : find a way to link to Args
        _roadState.value = RoadUIState(newRoad, newNuisance)
    }

    /**
     * TODO : Suspendable and cancelable coroutine
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    suspend fun drawJourney(road: Road, map : MapView, normal : Drawable, danger:Drawable){
        val roadOverlay: Polyline = RoadManager.buildRoadOverlay(road)
        map.overlays.clear()
        map.overlays.add(roadOverlay)

        for (i in road.mNodes.indices) {
            Log.d(
                1.toString(),
                "ajout à la map du point : latitude "
                        + road.mNodes[i].mLocation.latitude
                        + " longitude "
                        + road.mNodes[i].mLocation.longitude
            )

            val node = road.mNodes[i]
            val nodeMarker = Marker(map)
            nodeMarker.setPosition(node.mLocation)
            nodeMarker.title = "Step $i"

            if (node.mManeuverType == 1) {
                nodeMarker.icon = normal
                nodeMarker.snippet = node.mInstructions
            } else {
                nodeMarker.icon = danger
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
                Road.getLengthDurationText(this.viewModelScope as Context, node.mLength, node.mDuration)
            map.overlays.add(nodeMarker)
        }
    }
}