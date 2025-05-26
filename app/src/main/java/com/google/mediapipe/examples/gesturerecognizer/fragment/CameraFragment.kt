package com.google.mediapipe.examples.gesturerecognizer.fragment

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.mediapipe.examples.gesturerecognizer.GestureRecognizerHelper
import com.google.mediapipe.examples.gesturerecognizer.MainActivity
import com.google.mediapipe.examples.gesturerecognizer.MainViewModel
import com.google.mediapipe.examples.gesturerecognizer.R
import com.google.mediapipe.examples.gesturerecognizer.databinding.FragmentCameraBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraFragment : Fragment(), GestureRecognizerHelper.GestureRecognizerListener {

    companion object {
        private const val TAG = "CameraFragment"
        private const val MIN_CONFIDENCE = 0.85
        private const val GESTURE_HOLD_DURATION = 2000L
        private const val GESTURE_COOLDOWN = 1000L
    }

    private var cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val fragmentCameraBinding get() = _fragmentCameraBinding!!

    private lateinit var gestureRecognizerHelper: GestureRecognizerHelper
    private val viewModel: MainViewModel by activityViewModels()

    // Camera variables
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT

    // Background executor for image analysis
    private lateinit var backgroundExecutor: ExecutorService

    // Gesture recognition state
    private var lastGesture: String? = null
    private var gestureStartTime: Long = 0
    private val gestureHandler = Handler(Looper.getMainLooper())
    private val gestureStabilityRunnable = Runnable { onGestureStabilized() }

    // Accumulated gesture result text
    private val concatenatedLetters = StringBuilder()
    private var isCapturing = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        return fragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initComponents()
        setupCamera()
    }

    // Initialize UI components and gesture recognizer
    private fun initComponents() {
        backgroundExecutor = Executors.newSingleThreadExecutor()

        // Initialize gesture recognizer helper in background
        backgroundExecutor.execute {
            gestureRecognizerHelper = GestureRecognizerHelper(
                context = requireContext(),
                runningMode = RunningMode.LIVE_STREAM,
                minHandDetectionConfidence = viewModel.currentMinHandDetectionConfidence,
                minHandTrackingConfidence = viewModel.currentMinHandTrackingConfidence,
                minHandPresenceConfidence = viewModel.currentMinHandPresenceConfidence,
                currentDelegate = viewModel.currentDelegate,
                gestureRecognizerListener = this
            )
        }

        // Set up button listeners
        fragmentCameraBinding.btnToggleCapture.setOnClickListener { toggleCapture() }
        fragmentCameraBinding.btnClear.setOnClickListener { clearText() }
        fragmentCameraBinding.btnSwitchCamera.setOnClickListener { switchCamera() }
        fragmentCameraBinding.btnGallery.setOnClickListener { navigateToGallery() }
        fragmentCameraBinding.btnAddSpace.setOnClickListener { addSpace() }
    }

    // Navigates to gallery fragment
    private fun navigateToGallery() {
        (activity as? MainActivity)?.run {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, GalleryFragment())
                .commitNowAllowingStateLoss()
        }
    }

    // Enable or disable capturing
    private fun toggleCapture() {
        isCapturing = !isCapturing
        updateCaptureButton()
        resetGestureDetection()

        Toast.makeText(
            context,
            if (isCapturing) getString(R.string.capture_enabled) else getString(R.string.capture_disabled),
            Toast.LENGTH_SHORT
        ).show()
    }

    // Update play/pause icon
    private fun updateCaptureButton() {
        fragmentCameraBinding.btnToggleCapture.setImageResource(
            if (isCapturing) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    // Append a space if not already present
    private fun addSpace() {
        if (concatenatedLetters.isNotEmpty() && !concatenatedLetters.endsWith(" ")) {
            concatenatedLetters.append(" ")
            updateTextDisplay()

            // Animate space button for feedback
            val button = fragmentCameraBinding.btnAddSpace
            button.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(100)
                .withEndAction {
                    button.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .start()
                }
                .start()
        }
    }

    // Clear text area
    private fun clearText() {
        concatenatedLetters.clear()
        updateTextDisplay()
        Toast.makeText(context, getString(R.string.text_cleared), Toast.LENGTH_SHORT).show()
    }

    // Animate and update the text area
    private fun updateTextDisplay() {
        fragmentCameraBinding.concatenatedLetters.text = concatenatedLetters.toString()
        val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.gesture_pop)
        fragmentCameraBinding.concatenatedLetters.startAnimation(animation)
    }

    // Switch between front and back camera
    private fun switchCamera() {
        cameraFacing = if (cameraFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        bindCameraUseCases()
    }

    // Initialize camera preview and analyzer
    @SuppressLint("UnsafeOptInUsageError")
    private fun setupCamera() {
        fragmentCameraBinding.viewFinder.post {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
            cameraProviderFuture.addListener(
                {
                    cameraProvider = cameraProviderFuture.get()
                    bindCameraUseCases()
                }, ContextCompat.getMainExecutor(requireContext())
            )
        }
    }

    // Bind camera preview and image analyzer
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")
        val cameraSelector = CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        fragmentCameraBinding.overlay.setIsFrontCamera(cameraFacing == CameraSelector.LENS_FACING_FRONT)

        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(backgroundExecutor) { image ->
                    if (isCapturing) {
                        recognizeHand(image)
                    } else {
                        image.close()
                    }
                }
            }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    // Call the gesture recognizer helper
    private fun recognizeHand(imageProxy: ImageProxy) {
        gestureRecognizerHelper.recognizeLiveStream(imageProxy)
    }

    // Handle configuration changes
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = fragmentCameraBinding.viewFinder.display.rotation
    }

    // Callback for gesture recognition results
    override fun onResults(resultBundle: GestureRecognizerHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (_fragmentCameraBinding == null) return@runOnUiThread

            val currentGesture = resultBundle.results.first().gestures()
                .firstOrNull()?.maxByOrNull { it.score() }
                ?.takeIf { it.score() >= MIN_CONFIDENCE }
                ?.categoryName()

            currentGesture?.let { gesture ->
                fragmentCameraBinding.currentGesture.text = gesture

                if (gesture != lastGesture) {
                    lastGesture = gesture
                    gestureStartTime = System.currentTimeMillis()
                    gestureHandler.removeCallbacks(gestureStabilityRunnable)
                    gestureHandler.postDelayed(gestureStabilityRunnable, GESTURE_HOLD_DURATION)

                    fragmentCameraBinding.gestureProgress.visibility = View.VISIBLE
                    fragmentCameraBinding.gestureProgress.progress = 0
                } else {
                    val progress = ((System.currentTimeMillis() - gestureStartTime) /
                            GESTURE_HOLD_DURATION.toFloat()).coerceAtMost(1f)
                    (fragmentCameraBinding.gestureProgress as CircularProgressIndicator).progress =
                        (progress * 100).toInt()
                }
            } ?: run {
                resetGestureDetection()
            }

            updateOverlay(resultBundle)
        }
    }

    // Called when a gesture is held long enough
    private fun onGestureStabilized() {
        if (!isCapturing) return

        lastGesture?.let { gesture ->
            concatenatedLetters.append(gesture)
            updateTextDisplay()

            fragmentCameraBinding.currentGesture.animate()
                .scaleY(1.5f)
                .scaleX(1.5f)
                .setDuration(200)
                .withEndAction {
                    fragmentCameraBinding.currentGesture.animate()
                        .scaleY(1f)
                        .scaleX(1f)
                        .setDuration(200)
                        .start()
                }
                .start()

            gestureHandler.postDelayed({ resetGestureDetection() }, GESTURE_COOLDOWN)
        }
    }

    // Reset current gesture state
    private fun resetGestureDetection() {
        lastGesture = null
        with(fragmentCameraBinding) {
            currentGesture.text = ""
            gestureProgress.visibility = View.INVISIBLE
        }
        gestureHandler.removeCallbacks(gestureStabilityRunnable)
    }

    // Draw overlay landmarks and gesture annotations
    private fun updateOverlay(resultBundle: GestureRecognizerHelper.ResultBundle) {
        fragmentCameraBinding.overlay.setResults(
            resultBundle.results.first(),
            resultBundle.inputImageHeight,
            resultBundle.inputImageWidth,
            RunningMode.LIVE_STREAM
        )
        fragmentCameraBinding.overlay.invalidate()
    }

    // Handle errors from gesture recognizer
    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_prefix) + error,
                Toast.LENGTH_SHORT
            ).show()
            Log.e(TAG, "Recognition error: $error (Code: $errorCode)")
            resetGestureDetection()
        }
    }

    // Clean up resources when view is destroyed
    override fun onDestroyView() {
        super.onDestroyView()
        _fragmentCameraBinding = null

        gestureHandler.removeCallbacks(gestureStabilityRunnable)
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
    }
}
