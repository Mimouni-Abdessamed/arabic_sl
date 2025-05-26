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
import com.google.mediapipe.examples.gesturerecognizer.*
import com.google.mediapipe.examples.gesturerecognizer.databinding.VerbCameraFragmentBinding
import com.google.mediapipe.examples.verbrecognizer.fragment.VerbRecognizerResultsAdapter
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class VerbCameraFragment : Fragment(), VerbRecognizerHelper.VerbRecognizerListener {

    companion object {
        private const val TAG = "VerbCameraFragment"
    }

    private var _binding: VerbCameraFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var verbRecognizerHelper: VerbRecognizerHelper
    private val viewModel: MainViewModel by activityViewModels()
    private var defaultNumResults = 1
    private val verbRecognizerResultAdapter: VerbRecognizerResultsAdapter by lazy {
        VerbRecognizerResultsAdapter().apply {
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
        if (this::verbRecognizerHelper.isInitialized) {
            viewModel.setMinHandDetectionConfidence(verbRecognizerHelper.minHandDetectionConfidence)
            viewModel.setMinHandTrackingConfidence(verbRecognizerHelper.minHandTrackingConfidence)
            viewModel.setMinHandPresenceConfidence(verbRecognizerHelper.minHandPresenceConfidence)
            viewModel.setDelegate(verbRecognizerHelper.currentDelegate)
            backgroundExecutor.execute { verbRecognizerHelper.clearVerbRecognizer() }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = VerbCameraFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding.recyclerviewResults) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = verbRecognizerResultAdapter
        }

        // Initialize verb sequence in navbar
        binding.arabicVerbNavbarView.setupAdapter(0)

        backgroundExecutor = Executors.newSingleThreadExecutor()

        binding.viewFinder.post {
            setUpCamera()
        }

        backgroundExecutor.execute {
            verbRecognizerHelper = VerbRecognizerHelper(
                viewModel.currentMinHandDetectionConfidence,
                viewModel.currentMinHandTrackingConfidence,
                viewModel.currentMinHandPresenceConfidence,
                viewModel.currentDelegate,
                RunningMode.LIVE_STREAM,
                requireContext(),
                this@VerbCameraFragment
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
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")
        val cameraSelector = CameraSelector.Builder().requireLensFacing(cameraFacing).build()

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
                    recognizeVerb(image)
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

    private fun recognizeVerb(imageProxy: ImageProxy) {
        verbRecognizerHelper.recognizeLiveStream(imageProxy = imageProxy)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = binding.viewFinder.display.rotation
    }

    override fun onResults(resultBundle: VerbRecognizerHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (_binding != null) {
                val verbCategories = resultBundle.results.first().gestures()
                if (verbCategories.isNotEmpty()) {
                    verbRecognizerResultAdapter.updateResults(verbCategories.first())

                    val (recognizedVerb, confidence) = verbRecognizerResultAdapter.getCurrentVerbAndScore(0)
                    binding.arabicVerbNavbarView.onVerbRecognized(
                        recognizedVerb ?: "",
                        confidence ?: 0f
                    )
                } else {
                    verbRecognizerResultAdapter.updateResults(emptyList())
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
            verbRecognizerResultAdapter.updateResults(emptyList())
        }
    }
}