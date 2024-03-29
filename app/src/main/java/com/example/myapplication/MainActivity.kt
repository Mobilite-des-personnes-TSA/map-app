package com.example.myapplication

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline


class MainActivity : AppCompatActivity() {
    private val requestPermissionRequestCode = 1
    private lateinit var map: MapView
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //handle permissions first, before map is created. not depicted here

        //load/initialize the osmdroid configuration, this can be done
        // This won't work unless you have imported this: org.osmdroid.config.Configuration.*
        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, if you abuse osm's
        //tile servers will get you banned based on this string.

        //inflate and create the map
        setContentView(R.layout.activity_main)

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        val mapController = map.controller
        mapController.setZoom(13.0)
        val startPoint = GeoPoint(43.6, 1.4333)
        mapController.setCenter(startPoint)
        Thread {
            val roadManager: RoadManager = OSRMRoadManager(this,"User")
            (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_FOOT)
            val waypoints = ArrayList<GeoPoint>()
            waypoints.add(startPoint)
            val endPoint = GeoPoint(43.7, 1.233)
            waypoints.add(endPoint)
            val road: Road = roadManager.getRoad(waypoints)
            if (road.mStatus != Road.STATUS_OK) Toast.makeText(
                this,
                "Error when loading the road - status=" + road.mStatus,
                Toast.LENGTH_SHORT
            ).show()

            val roadOverlay: Polyline = RoadManager.buildRoadOverlay(road)
            map.overlays.add(roadOverlay)
            val nodeIcon = resources.getDrawable(R.drawable.marker_node,theme)
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

            map.invalidate()
        }.start()



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

}
