package com.google.mediapipe.examples.gesturerecognizer.fragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.mediapipe.examples.gesturerecognizer.R

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class PermissionsFragment : Fragment() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                navigateToMainFragment()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permission required to use camera features",
                    Toast.LENGTH_LONG
                ).show()
                requireActivity().finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use lifecycleScope to ensure navigation happens after fragment is attached
        lifecycleScope.launchWhenStarted {
            if (hasPermissions(requireContext())) {
                navigateToMainFragment()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun navigateToMainFragment() {
        Navigation.findNavController(requireActivity(), R.id.fragment_container)
            .navigate(R.id.action_permissions_to_home)
    }

    companion object {
        fun hasPermissions(context: Context) = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}