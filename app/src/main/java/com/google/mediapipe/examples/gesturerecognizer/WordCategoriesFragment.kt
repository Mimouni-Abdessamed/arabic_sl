package com.google.mediapipe.examples.gesturerecognizer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.mediapipe.examples.gesturerecognizer.R
import com.google.mediapipe.examples.gesturerecognizer.fragment.EducationCameraFragment
import com.google.mediapipe.examples.gesturerecognizer.fragment.HealthCameraFragment
import com.google.mediapipe.examples.gesturerecognizer.fragment.VerbCameraFragment
import com.google.mediapipe.examples.gesturerecognizer.fragment.WordCameraFragment

class WordCategoriesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_word_categories, container, false)

        val colorsButton = view.findViewById<MaterialButton>(R.id.colors_button)
        val verbsButton = view.findViewById<MaterialButton>(R.id.verbs_button)
        val educationButton = view.findViewById<MaterialButton>(R.id.education_button)
        val healthButton = view.findViewById<MaterialButton>(R.id.health_button)



        colorsButton.setOnClickListener {
            openWordCameraFragment()
        }
        verbsButton.setOnClickListener {
            openVerbCameraFragment()
        }
        educationButton.setOnClickListener {
            openeducationCameraFragment()
        }
        healthButton.setOnClickListener {
            openhealthCameraFragment()
        }

        return view
    }

    private fun openWordCameraFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, WordCameraFragment())
            .addToBackStack(null)
            .commit()
    }
    private fun openVerbCameraFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, VerbCameraFragment())
            .addToBackStack(null)
            .commit()
    }
    private fun openeducationCameraFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, EducationCameraFragment())
            .addToBackStack(null)
            .commit()
    }
    private fun openhealthCameraFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HealthCameraFragment())
            .addToBackStack(null)
            .commit()
    }
}
