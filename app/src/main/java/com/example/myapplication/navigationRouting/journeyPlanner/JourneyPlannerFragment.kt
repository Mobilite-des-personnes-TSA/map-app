package com.example.myapplication.navigationRouting.journeyPlanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceFragmentCompat
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentJourneyPlannerBinding
import com.example.myapplication.navigationRouting.mapDisplay.MapDisplayFragment
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputLayout


/** TODO : Creating Data Flow
 */
class JourneyPlannerFragment : Fragment() {

    /*
    private lateinit var buttonSearch: Button
    private lateinit var buttonSubway: Chip
    private lateinit var buttonBus: Chip
    private lateinit var buttonCableCar: Chip
    private lateinit var buttonTram: Chip
    private lateinit var buttonWheelChair: Chip
    private lateinit var buttonCar: Chip
    private lateinit var buttonBike: Chip

    private lateinit var editTextDep: TextInputLayout
    private lateinit var editTextArv: TextInputLayout
     */

    private var _binding: FragmentJourneyPlannerBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val journeyPlannerViewModel =
            ViewModelProvider(this).get(JourneyPlannerViewModel::class.java)

        _binding = FragmentJourneyPlannerBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Navigate to map when Search button is clicked
        //TODO: Keep the JourneySearch info as long as the user hasn't closed the app
        binding.search.setOnClickListener {
            val data = journeyPlannerViewModel.roadState.value
            val action = JourneyPlannerFragmentDirections.actionJourneyPlannerToMapDisplay(data.road)
            findNavController().navigate(action)
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe navigation events
        findNavController()
            .currentBackStackEntry?.savedStateHandle?.getLiveData<String>("key")?.observe(viewLifecycleOwner) { result ->
            // TODO: Handle navigation event
            // Is it setup correctly ?
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }
    }
}