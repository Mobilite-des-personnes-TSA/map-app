package com.example.myapplication.ui.mapDisplay

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentMapDisplayBinding
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

import RoadUIState
import android.annotation.SuppressLint
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

class MapDisplayFragment : Fragment() {

    private var _binding: FragmentMapDisplayBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    //// IS FOR ME

    private val requestPermissionRequestCode = 1
    private lateinit var map: MapView
    private lateinit var button: Button
    private lateinit var mapDisplayViewModel : MapDisplayViewModel

    /** TODO : Add a button to recenter the map around the user
     *  Set a boolean, active and deactivate the button ?
     *  Each time you press reinitialized
     *  How to handle the map moving as the user moves ?
     */
    private lateinit var centerButton: Button


    //val action = MapDisplayFragmentDirections.actionMapDisplayFragmentToJourneyPlannerFragment()
    //findNavController().navigate(action)
    //TODO: For naviguation
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Obtain an instance of the MapDisplayViewModel
        val mapDisplayViewModel =
            ViewModelProvider(this).get(MapDisplayViewModel::class.java)

        _binding = FragmentMapDisplayBinding.inflate(inflater, container, false)
        // TODO : should i attach it to parent
        val root: View = binding.root

        val textView: TextView = binding.textNotifications
        mapDisplayViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        ////// IS FOR ME

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))


        // Initializing map components
        // TODO: No need to bind stuff
        // TODO: Make sure the UI starts where the user is
        // use binding.<id>
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        val mapController = map.controller
        mapController.setZoom(13.0)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO Example: Set text of a TextView
        binding.textViewTitle.text = "Map Display Fragment"

        // TODO Example: Set click listener for a button
        binding.button.setOnClickListener {
            // Handle button click here
        }

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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}