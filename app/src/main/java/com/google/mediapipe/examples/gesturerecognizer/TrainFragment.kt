/*package com.google.mediapipe.examples.gesturerecognizer

import com.google.mediapipe.examples.gesturerecognizer.fragment.ChallengesCameraFragment
import com.google.mediapipe.examples.gesturerecognizer.fragment.NumberCameraFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.mediapipe.examples.gesturerecognizer.fragment.LetterCameraFragment

class TrainFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_train, container, false)

        val lettersButton = view.findViewById<MaterialButton>(R.id.lettersButton)
        val numbersButton = view.findViewById<MaterialButton>(R.id.numbersButton)
        val challengesButton = view.findViewById<MaterialButton>(R.id.challengesButton)

        lettersButton.setOnClickListener {
            // Replace with LettersPreviewFragment
            replaceFragment(LetterCameraFragment())
        }

        numbersButton.setOnClickListener {
            // Replace with NumbersPreviewFragment
            replaceFragment(NumberCameraFragment())


        }

        challengesButton.setOnClickListener {
            // Replace with ChallengesPreviewFragment
            replaceFragment(ChallengesCameraFragment())
        }

        return view
    }

    private fun replaceFragment(fragment: Fragment) {
        // Use parentFragmentManager to access the activity's fragment manager
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment) // Replace current fragment
            .addToBackStack(null) // Add transaction to back stack (optional)
            .commit()
    }
}  */

package com.google.mediapipe.examples.gesturerecognizer

import com.google.mediapipe.examples.gesturerecognizer.fragment.ChallengesCameraFragment
import com.google.mediapipe.examples.gesturerecognizer.fragment.NumberCameraFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.mediapipe.examples.gesturerecognizer.fragment.LetterCameraFragment
import android.view.animation.AnimationUtils
import androidx.cardview.widget.CardView

class TrainFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_train, container, false)

        // Récupération des boutons comme avant
        val lettersButton = view.findViewById<MaterialButton>(R.id.lettersButton)
        val numbersButton = view.findViewById<MaterialButton>(R.id.numbersButton)
        val challengesButton = view.findViewById<MaterialButton>(R.id.challengesButton)

        // Configuration des clics sur les boutons (fonctionnalité inchangée)
        lettersButton.setOnClickListener {
            // Replace with LettersPreviewFragment
            replaceFragment(LetterCameraFragment())
        }

        numbersButton.setOnClickListener {
            // Replace with NumbersPreviewFragment
            replaceFragment(NumberCameraFragment())
        }

        challengesButton.setOnClickListener {
            // Replace with ChallengesPreviewFragment
            replaceFragment(ChallengesCameraFragment())
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Animation pour l'entrée des cartes
        val fadeIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)

        // Animation séquentielle des cartes
        val cardLetters = view.findViewById<CardView>(R.id.card_letters)
        val cardNumbers = view.findViewById<CardView>(R.id.card_numbers)
        val cardChallenges = view.findViewById<CardView>(R.id.card_challenges)

        // Animation avec délai pour effet séquentiel
        cardLetters.startAnimation(fadeIn)
        cardNumbers.postDelayed({
            cardNumbers.startAnimation(fadeIn)
        }, 150)
        cardChallenges.postDelayed({
            cardChallenges.startAnimation(fadeIn)
        }, 300)
    }

    private fun replaceFragment(fragment: Fragment) {
        // Use parentFragmentManager to access the activity's fragment manager
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, fragment) // Replace current fragment
            .addToBackStack(null) // Add transaction to back stack (optional)
            .commit()
    }
}