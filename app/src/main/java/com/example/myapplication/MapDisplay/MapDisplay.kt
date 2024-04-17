package com.example.myapplication

import MapDisplayViewModel
import RoadUIState
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

import androidx.lifecycle.repeatOnLifecycle
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline


class MapDisplay : AppCompatActivity() {
    private val requestPermissionRequestCode = 1
    private lateinit var map: MapView
    private lateinit var button: Button
    private lateinit var viewModel : MapDisplayViewModel

    /** TODO : Add a button to recenter the map around the user
     *  Set a boolean, active and deactivate the button ?
     *  Each time you press reinitialized
     *  How to handle the map moving as the user moves ?
     */
    private lateinit var centerButton: Button

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // How will we be loading the settings ?
        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(R.layout.activity_main)

        // Initializing map components
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        val mapController = map.controller
        mapController.setZoom(13.0)

        // Obtain an instance of the MapDisplayViewModel
        viewModel = ViewModelProvider(this).get(MapDisplayViewModel::class.java)

        // Observe were the user is located
        viewModel.userPosition.observe(this, Observer { userPosition ->
            mapController.setCenter(GeoPoint(userPosition))
            // TODO: Make sure the UI starts where the user is
        })

        // Observe were the user is located
        viewModel.roadState.observe(this, Observer { road ->
            // TODO: Update the UI
            //  RoadUIState into Road
            drawJourney(road as Road)
        })

        //Listens to change activity
        //TODO: Make the JourneyPlanner a Navigation
        //  Need reference to the LaunchJourney Button
        button = findViewById(R.id.button)
        button.setOnClickListener {
            val intent = Intent(this, JourneyPlanner::class.java)
            startActivity(intent)
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

    //  What permission are we asking for again ?
    //  If not inherently necessary might be better not to ask it systematically
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionsToRequest = ArrayList<String>()
        var i = 0
        while (i < grantResults.size) {
            permissionsToRequest.add(permissions[i])
            i++
        }
        if (permissionsToRequest.size > 0) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                requestPermissionRequestCode
            )
        }
    }

    /**
     * TODO : Suspendable and cancelable coroutine
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    fun drawJourney(road: Road){

        val roadOverlay: Polyline = RoadManager.buildRoadOverlay(road)
        map.overlays.add(roadOverlay)
        val nodeIcon = resources.getDrawable(R.drawable.marker_node,theme)

        // TODO: Segmenting it into several poly-lines to be drawn periodically so that the user isnt stuck
        for (i in road.mNodes.indices) {
            val node = road.mNodes[i]
            val nodeMarker = Marker(map)
            nodeMarker.setPosition(node.mLocation)
            nodeMarker.icon = nodeIcon
            nodeMarker.title = "Step $i"
            nodeMarker.snippet = node.mInstructions

            nodeMarker.subDescription = Road.getLengthDurationText(this,node.mLength,node.mDuration)
            map.overlays.add(nodeMarker)
        }
    }
}
