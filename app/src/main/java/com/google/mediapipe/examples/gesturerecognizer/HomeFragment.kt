package com.example.sl

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.mediapipe.examples.gesturerecognizer.MainActivity
import com.google.mediapipe.examples.gesturerecognizer.R

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Animation améliorée du logo
        val logo = view.findViewById<View>(R.id.app_logo)
        val animation = AnimationUtils.loadAnimation(context, R.anim.logo_scale).apply {
            // Option: Ajouter un interpolator pour un effet plus fluide
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
        }
        logo.startAnimation(animation)

        // Relancer l'animation au clic sur le logo
        logo.setOnClickListener {
            it.startAnimation(animation)
        }

        // Configuration des clics (inchangé)
        setupCardClick(view, R.id.card_translate, R.id.nav_translate)
        setupCardClick(view, R.id.card_train, R.id.nav_train)
        setupCardClick(view, R.id.card_dictionary, R.id.nav_dictionary)
    }

    private fun setupCardClick(view: View, cardId: Int, destinationId: Int) {
        view.findViewById<CardView>(cardId).setOnClickListener {
            (activity as? MainActivity)?.apply {
                showBottomNavWithAnimation()
                bottomNavigationView.selectedItemId = destinationId
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.hideBottomNavWithAnimation()
    }
}