package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.example.myapplication.tisseo.TisseoApiClient
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
    private lateinit var button: Button
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(R.layout.activity_main)

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        val mapController = map.controller
        mapController.setZoom(13.0)
        val startPoint = GeoPoint(43.6, 1.4333)
        mapController.setCenter(startPoint)
        Thread {
            tisseoRouting("aerodrome", "balma")
        }.start()
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
    private fun tisseoRouting(startPlace:String, endPlace:String){
        Thread{
        val journeyData = TisseoApiClient.journey(startPlace,endPlace,"walk","1")
        val geoPoints = ArrayList<GeoPoint>()
        if (journeyData != null) {
            journeyData.routePlannerResult.journeys[0].journey.chunks.forEach{chunk ->

                if (chunk.service != null){
                    val wkt = chunk.service.wkt
                    val coordinates = wkt.substringAfter("(").substringBeforeLast(")").split(",")

                    // Pour chaque paire de coordonnées, extrayez la latitude et la longitude et ajoutez-les à la liste de points
                    coordinates.forEach { coordinate ->
                        val (longitude, latitude) = coordinate.trim().split(" ")
                        geoPoints.add(GeoPoint(latitude.toDouble(), longitude.toDouble()))
                    }
                }
            }
        }
        val road = Road()
        geoPoints.forEach { point -> road.mRouteHigh.add(point)
        }
            println("Points de la route ajoutés:")
            road.mRouteHigh.forEachIndexed { index, geoPoint ->
                println("Point $index : Latitude = ${geoPoint.latitude}, Longitude = ${geoPoint.longitude}")
            }
        drawJourney(road)
        map.invalidate()
        }.start()
    }
    fun osrmRouting(startPoint:GeoPoint,endPoint: GeoPoint,mode:String){
        Thread {
            val roadManager: RoadManager = OSRMRoadManager(this,"User")
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
            val road: Road = roadManager.getRoad(waypoints)
            if (road.mStatus != Road.STATUS_OK) Toast.makeText(
                this,
                "Error when loading the road - status=" + road.mStatus,
                Toast.LENGTH_SHORT
            ).show()

            drawJourney(road)
            map.invalidate()
        }.start()
    }

    fun drawJourney(road:Road){
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
    }

}
