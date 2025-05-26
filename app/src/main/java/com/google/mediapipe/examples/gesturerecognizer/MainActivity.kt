package com.google.mediapipe.examples.gesturerecognizer

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.mediapipe.examples.gesturerecognizer.HomeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale

class MainActivity : AppCompatActivity() {
    lateinit var bottomNavigationView: BottomNavigationView
    private val prefsName = "app_prefs"
    private val firstLaunchKey = "first_launch"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set Arabic locale
        Locale.setDefault(Locale("ar"))
        val config = Configuration()
        config.setLocale(Locale("ar"))
        resources.updateConfiguration(config, resources.displayMetrics)

        setContentView(R.layout.activity_main)

        // First launch check
        val sharedPrefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        if (sharedPrefs.getBoolean(firstLaunchKey, true)) {
            showWelcomeDialog()
            sharedPrefs.edit().putBoolean(firstLaunchKey, false).apply()
        }

        bottomNavigationView = findViewById(R.id.bottom_navigation)

        // Initial fragment load
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

    private fun showWelcomeDialog() {
        val dialog = WelcomeDialogFragment()
        dialog.show(supportFragmentManager, "WelcomeDialog")
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
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment !is HomeFragment) {
            switchFragment(HomeFragment())
            bottomNavigationView.selectedItemId = R.id.nav_home
        } else {
            super.onBackPressed()
        }
    }
}