package com.google.mediapipe.examples.gesturerecognizer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class WelcomeDialogFragment : DialogFragment() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        handlePermissionResult(isGranted)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.welcome_dialog, container, false)

        view.findViewById<MaterialButton>(R.id.btn_ok).setOnClickListener {
            handleContinueClick()
        }

        view.findViewById<MaterialButton>(R.id.btn_no).setOnClickListener {
            Toast.makeText(requireContext(), "لقد قمت برفض إذن الكاميرا. يرجى تمكينه من إعدادات التطبيق لاستخدام ميزات التي تحتاج كاميرا.", Toast.LENGTH_SHORT).show()
            navigateToMain()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setDimAmount(0.4f)
            attributes?.windowAnimations = R.style.DialogAnimation
        }

        checkExistingPermissions()
    }

    private fun checkExistingPermissions() {
        if (hasPermissions(requireContext())) {
            navigateWithDelay()
        }
    }

    private fun handleContinueClick() {
        when {
            hasPermissions(requireContext()) -> navigateToMain()
            shouldShowRationale() -> showRationaleDialog()
            else -> requestCameraPermission()
        }
    }

    private fun handlePermissionResult(isGranted: Boolean) {
        when {
            isGranted -> navigateToMain()
            else -> {
                Toast.makeText(
                    requireContext(),
                    "If you want to access the features that use the camera, authorize it from settings.",
                    Toast.LENGTH_LONG
                ).show()
                // User can still continue without permission, no forced exit
            }
        }
    }

    private fun navigateToMain() {
        if (isAdded && !isStateSaved) {
            val navHostFragment = requireActivity().supportFragmentManager
                .primaryNavigationFragment as? NavHostFragment

            navHostFragment?.navController?.navigate(R.id.action_welcome_to_home)

            view?.postDelayed({
                if (isAdded) dismissAllowingStateLoss()
            }, 300)
        }
    }

    private fun navigateWithDelay() {
        view?.postDelayed({
            navigateToMain()
        }, 1000)
    }

    private fun showRationaleDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("الكاميرا مطلوبة")
            .setMessage("إذا كنت ترغب في استخدام ميزات التطبيق التي تتطلب الكاميرا، يرجى السماح بذلك من الإعدادات.")
            .setPositiveButton("الإعدادات") { _, _ -> openAppSettings() }
            .setNegativeButton("متابعة بدون كاميرا") { _, _ ->
                Toast.makeText(requireContext(), "يمكنك استخدام التطبيق بدون الكاميرا.", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }
            .setCancelable(true)
            .show()
    }

    private fun requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun openAppSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
            startActivity(this)
        }
    }

    private fun shouldShowRationale() =
        shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)

    override fun getTheme(): Int = R.style.CustomAlertDialog

    companion object {
        fun hasPermissions(context: Context) = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
}
