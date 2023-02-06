package com.example.mapsindoorsgettingstartedkotlinbasic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import com.mapsindoors.mapssdk.RouteLeg
import java.sql.SQLSyntaxErrorException

class RouteLegFragment : Fragment() {
    private var mRouteLeg: RouteLeg? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_route_leg, container, false)
    }

    override fun onViewCreated(
        view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Assigning views
        val stepsTxtView = view.findViewById<TextView>(R.id.steps_text_view)
        var stepsString = ""
        //TODO: Create a string to describe the steps.
        for (i in mRouteLeg!!.steps.indices) {
            val routeStep = mRouteLeg!!.steps[i]
            stepsString += """
                Step ${i +1}: ${routeStep.maneuver}${System.lineSeparator()}
            """.trimIndent()
        }
        stepsTxtView.text = stepsString
    }

    companion object {
        fun newInstance(routeLeg: RouteLeg?): RouteLegFragment {
            val fragment = RouteLegFragment()
            fragment.mRouteLeg = routeLeg
            return fragment
        }
    }
}