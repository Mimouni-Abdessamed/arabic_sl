package com.google.mediapipe.examples.gesturerecognizer

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.sl.HomeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale

class MainActivity : AppCompatActivity() {
    lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuration de la locale en arabe
        Locale.setDefault(Locale("ar"))
        val config = Configuration()
        config.setLocale(Locale("ar"))
        resources.updateConfiguration(config, resources.displayMetrics)

        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // Chargement initial sans animation
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchFragment(HomeFragment())
                R.id.nav_translate -> switchFragment(TranslatFragment())
                R.id.nav_train -> switchFragment(TrainFragment())
                R.id.nav_dictionary -> switchFragment(DictionaryFragment())
            }
            true
        }
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .replace(R.id.fragment_container, fragment)
            .commit()

        updateBottomNavVisibility(fragment)
    }

    private fun updateBottomNavVisibility(fragment: Fragment) {
        if (fragment is HomeFragment) {
            hideBottomNavWithAnimation()
        } else {
            showBottomNavWithAnimation()
        }
    }

    internal fun showBottomNavWithAnimation() {
        if (bottomNavigationView.visibility != View.VISIBLE) {
            bottomNavigationView.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.bottom_nav_show)
            )
            bottomNavigationView.visibility = View.VISIBLE
        }
    }

    internal fun hideBottomNavWithAnimation() {
        if (bottomNavigationView.visibility == View.VISIBLE) {
            bottomNavigationView.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.bottom_nav_hide)
            )
            bottomNavigationView.visibility = View.GONE
        }
    }
}