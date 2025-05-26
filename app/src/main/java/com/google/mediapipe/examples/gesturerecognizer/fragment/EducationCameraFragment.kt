package com.google.mediapipe.examples.gesturerecognizer.fragment

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.mediapipe.examples.gesturerecognizer.*
import com.google.mediapipe.examples.gesturerecognizer.databinding.EducationFragmentCameraBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class EducationCameraFragment : Fragment(),
    EducationRecognizerHelper.EducationRecognizerListener {

    companion object {
        private const val TAG = "EducationCameraFragment"
    }

    private var _binding: EducationFragmentCameraBinding? = null
    private val binding get() = _binding!!

    private lateinit var educationRecognizerHelper: EducationRecognizerHelper
    private val viewModel: MainViewModel by activityViewModels()
    private val resultAdapter: ColorRecognizerResultsAdapter by lazy {
        ColorRecognizerResultsAdapter().apply { updateAdapterSize(1) }
    }

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT
    private lateinit var backgroundExecutor: ExecutorService

    override fun onPause() {
        super.onPause()
        if (this::educationRecognizerHelper.isInitialized) {
            viewModel.setMinHandDetectionConfidence(educationRecognizerHelper.minHandDetectionConfidence)
            viewModel.setMinHandTrackingConfidence(educationRecognizerHelper.minHandTrackingConfidence)
            viewModel.setMinHandPresenceConfidence(educationRecognizerHelper.minHandPresenceConfidence)
            viewModel.setDelegate(educationRecognizerHelper.currentDelegate)
            backgroundExecutor.execute { educationRecognizerHelper.clearGestureRecognizer() }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = EducationFragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerviewResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = resultAdapter
        }

        backgroundExecutor = Executors.newSingleThreadExecutor()

        binding.viewFinder.post { setUpCamera() }

        backgroundExecutor.execute {
            educationRecognizerHelper = EducationRecognizerHelper(
                viewModel.currentMinHandDetectionConfidence,
                viewModel.currentMinHandTrackingConfidence,
                viewModel.currentMinHandPresenceConfidence,
                viewModel.currentDelegate,
                RunningMode.LIVE_STREAM,
                requireContext(),
                this@EducationCameraFragment
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
            .requireLensFacing(cameraFacing).build()

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
                    recognizeEducation(image)
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

    private fun recognizeEducation(imageProxy: ImageProxy) {
        educationRecognizerHelper.recognizeLiveStream(imageProxy)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = binding.viewFinder.display.rotation
    }

    override fun onResults(resultBundle: EducationRecognizerHelper.ResultBundle) {
        activity?.runOnUiThread {
            val educationCategories = resultBundle.results.first().gestures()
            if (educationCategories.isNotEmpty()) {
                resultAdapter.updateResults(educationCategories.first())
                val (recognized, confidence) = resultAdapter.getCurrentColorAndScore(0)
                binding.arabicEducationNavbarView.onEducationWordRecognized(
                    recognized ?: "",
                    confidence ?: 0f
                )
            } else {
                resultAdapter.updateResults(emptyList())
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

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            resultAdapter.updateResults(emptyList())
        }
    }
}
