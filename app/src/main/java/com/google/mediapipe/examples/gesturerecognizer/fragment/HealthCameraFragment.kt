package com.google.mediapipe.examples.gesturerecognizer.fragment

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.mediapipe.examples.gesturerecognizer.HealthRecognizerHelper
import com.google.mediapipe.examples.gesturerecognizer.MainViewModel
import com.google.mediapipe.examples.gesturerecognizer.databinding.HealthFragmentCameraBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class HealthCameraFragment : Fragment(),
    HealthRecognizerHelper.HealthRecognizerListener {

    companion object {
        private const val TAG = "HealthCameraFragment"
    }

    private var _healthFragmentCameraBinding: HealthFragmentCameraBinding? = null
    private val binding get() = _healthFragmentCameraBinding!!

    private lateinit var healthRecognizerHelper: HealthRecognizerHelper
    private val viewModel: MainViewModel by activityViewModels()
    private var defaultNumResults = 1
    private val healthRecognizerResultAdapter: ColorRecognizerResultsAdapter by lazy {
        ColorRecognizerResultsAdapter().apply {
            updateAdapterSize(defaultNumResults)
        }
    }

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT
    private lateinit var backgroundExecutor: ExecutorService

    override fun onPause() {
        super.onPause()
        if (this::healthRecognizerHelper.isInitialized) {
            viewModel.setMinHandDetectionConfidence(healthRecognizerHelper.minHandDetectionConfidence)
            viewModel.setMinHandTrackingConfidence(healthRecognizerHelper.minHandTrackingConfidence)
            viewModel.setMinHandPresenceConfidence(healthRecognizerHelper.minHandPresenceConfidence)
            viewModel.setDelegate(healthRecognizerHelper.currentDelegate)
            backgroundExecutor.execute { healthRecognizerHelper.clearGestureRecognizer() }
        }
    }

    override fun onDestroyView() {
        _healthFragmentCameraBinding = null
        super.onDestroyView()
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _healthFragmentCameraBinding =
            HealthFragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerviewResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = healthRecognizerResultAdapter
        }

        backgroundExecutor = Executors.newSingleThreadExecutor()

        binding.viewFinder.post {
            setUpCamera()
        }

        backgroundExecutor.execute {
            healthRecognizerHelper = HealthRecognizerHelper(
                viewModel.currentMinHandDetectionConfidence,
                viewModel.currentMinHandTrackingConfidence,
                viewModel.currentMinHandPresenceConfidence,
                viewModel.currentDelegate,
                RunningMode.LIVE_STREAM,
                requireContext(),
                this@HealthCameraFragment
            )
        }
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(cameraFacing)
            .build()

        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(backgroundExecutor) { image ->
                    recognizeGesture(image)
                }
            }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun recognizeGesture(imageProxy: ImageProxy) {
        healthRecognizerHelper.recognizeLiveStream(imageProxy)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = binding.viewFinder.display.rotation
    }

    override fun onResults(resultBundle: HealthRecognizerHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (_healthFragmentCameraBinding != null) {
                val gestures = resultBundle.results.first().gestures()
                if (gestures.isNotEmpty()) {
                    healthRecognizerResultAdapter.updateResults(gestures.first())
                    val (recognizedGesture, confidence) =
                        healthRecognizerResultAdapter.getCurrentColorAndScore(0)

                    // Use the updated method here:
                    binding.arabicHealthNavbarView.onHealthRecognized(
                        recognizedGesture ?: "",
                        confidence ?: 0f
                    )
                } else {
                    healthRecognizerResultAdapter.updateResults(emptyList())
                }

                binding.overlay.setResults(
                    resultBundle.results.first(),
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    RunningMode.LIVE_STREAM
                )

                binding.overlay.invalidate()
            }
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            healthRecognizerResultAdapter.updateResults(emptyList())
        }
    }
}
