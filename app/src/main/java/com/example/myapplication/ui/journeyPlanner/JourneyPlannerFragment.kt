package com.example.myapplication.ui.journeyPlanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentJourneyPlannerBinding


/** TODO :
 *
 */

/** TODO : Creating the database
 *  Setting up Database and repositories
 *  Creating Data Flow
 */
class JourneyPlannerFragment : Fragment() {

    private var _binding: FragmentJourneyPlannerBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

//
    private lateinit var button: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val journeyPlannerViewModel =
            ViewModelProvider(this).get(JourneyPlannerViewModel::class.java)
        _binding = FragmentJourneyPlannerBinding.inflate(inflater, container, false)

        // TODO: Bind to .xml
        // TODO : If saved instance
        //setContentView(R.layout.)

        val root: View = binding.root

        val textView: TextVew = binding.textDashboard

        journeyPlannerViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        button = findViewById(R.id.search)
        button.setOnClickListener {
            //TODO: JourneyPlanning ie. tisseoRouting
            //  Navigate to Map
            //  Keep the JourneySearch info as long as the user hasn't closed the app
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}