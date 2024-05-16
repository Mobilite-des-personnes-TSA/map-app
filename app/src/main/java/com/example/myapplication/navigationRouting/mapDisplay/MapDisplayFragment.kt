package com.example.myapplication.navigationRouting.mapDisplay

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

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.time.format.DateTimeFormatter

class MapDisplayFragment : Fragment() {

    //val args : MapDisplayFragmentArgs by navArgs()

    // TODO : These two buttons
    private lateinit var buttonJourneyPlanner: Button
    private lateinit var buttonSettings: Button

    private lateinit var markerNormal: Drawable
    private lateinit var markerDanger: Drawable
    private lateinit var button: Button
    private lateinit var mapDisplayViewModel : MapDisplayViewModel
    private val requestPermissionRequestCode = 1
    private lateinit var map: MapView

    private var _binding: FragmentMapDisplayBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        view?.let { super.onViewCreated(it, savedInstanceState) }

        // Obtain an instance of the MapDisplayViewModel
        val mapDisplayViewModel = ViewModelProvider(this).get(MapDisplayViewModel::class.java)

        _binding = FragmentMapDisplayBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // User preferences
        Configuration.getInstance().load(this.context,
            this.context?.let { PreferenceManager.getDefaultSharedPreferences(it) })

        map = binding.map
        // Initializing map components
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.controller.setZoom(13.0)

        val startCenterPoint = GeoPoint(43.6, 1.4333)
        map.controller.setCenter(startCenterPoint)

        // Draw the map when recieved
        /*
        CoroutineScope(Dispatchers.IO).launch {
            mapDisplayViewModel.drawJourney(args.mapToDraw, binding.map, markerNormal, markerDanger)
        }
         */

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapDisplayViewModel = ViewModelProvider(this).get(MapDisplayViewModel::class.java)
        // Observe were the user is located
        /*
        mapDisplayViewModel.userPosition.observe(viewLifecycleOwner, Observer { userPosition ->

            binding.map.controller.setCenter(GeoPoint(userPosition))
            // Make sure the UI starts where the user is
        })
        */

        button = binding.buttonJourneyPlanner

        //TODO: Button to go back
        button.setOnClickListener {
            findNavController().navigate(MapDisplayFragmentDirections.actionMapDisplayToJourneyPlanner())
            // Possibly add data if needed
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}