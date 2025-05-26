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
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.mediapipe.examples.gesturerecognizer.ColorRecognizerHelper
import com.google.mediapipe.examples.gesturerecognizer.MainViewModel
import com.google.mediapipe.examples.gesturerecognizer.R
import com.google.mediapipe.examples.gesturerecognizer.databinding.WordFragmentCameraBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class WordCameraFragment : Fragment(),
    ColorRecognizerHelper.ColorRecognizerListener {

    companion object {
        private const val TAG = "ColorCameraFragment"
    }

    private var _wordFragmentCameraBinding: WordFragmentCameraBinding? = null
    private val wordFragmentCameraBinding
        get() = _wordFragmentCameraBinding!!

    private lateinit var colorRecognizerHelper: ColorRecognizerHelper
    private val viewModel: MainViewModel by activityViewModels()
    private var defaultNumResults = 1
    private val colorRecognizerResultAdapter: ColorRecognizerResultsAdapter by lazy {
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
        if (this::colorRecognizerHelper.isInitialized) {
            viewModel.setMinHandDetectionConfidence(colorRecognizerHelper.minHandDetectionConfidence)
            viewModel.setMinHandTrackingConfidence(colorRecognizerHelper.minHandTrackingConfidence)
            viewModel.setMinHandPresenceConfidence(colorRecognizerHelper.minHandPresenceConfidence)
            viewModel.setDelegate(colorRecognizerHelper.currentDelegate)
            backgroundExecutor.execute { colorRecognizerHelper.clearGestureRecognizer() }
        }
    }

    override fun onDestroyView() {
        _wordFragmentCameraBinding = null
        super.onDestroyView()
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _wordFragmentCameraBinding =
            WordFragmentCameraBinding.inflate(inflater, container, false)
        return wordFragmentCameraBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(wordFragmentCameraBinding.recyclerviewResults) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = colorRecognizerResultAdapter
        }

        backgroundExecutor = Executors.newSingleThreadExecutor()

        wordFragmentCameraBinding.viewFinder.post {
            setUpCamera()
        }

        backgroundExecutor.execute {
            colorRecognizerHelper = ColorRecognizerHelper(
                viewModel.currentMinHandDetectionConfidence,
                viewModel.currentMinHandTrackingConfidence,
                viewModel.currentMinHandPresenceConfidence,
                viewModel.currentDelegate,
                RunningMode.LIVE_STREAM,
                requireContext(),
                this@WordCameraFragment
            )
        }
    }

    private fun setUpCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(wordFragmentCameraBinding.viewFinder.display.rotation)
            .build()

        imageAnalyzer =
            ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(wordFragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(backgroundExecutor) { image ->
                        recognizeColor(image)
                    }
                }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )
            preview?.setSurfaceProvider(wordFragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun recognizeColor(imageProxy: ImageProxy) {
        colorRecognizerHelper.recognizeLiveStream(
            imageProxy = imageProxy,
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            wordFragmentCameraBinding.viewFinder.display.rotation
    }

    override fun onResults(
        resultBundle: ColorRecognizerHelper.ResultBundle
    ) {
        activity?.runOnUiThread {
            if (_wordFragmentCameraBinding != null) {
                val colorCategories = resultBundle.results.first().gestures()
                if (colorCategories.isNotEmpty()) {
                    colorRecognizerResultAdapter.updateResults(
                        colorCategories.first()
                    )
                    val (recognizedColor, confidence) = colorRecognizerResultAdapter.getCurrentColorAndScore(0)
                    wordFragmentCameraBinding.arabicWordNavbarView.onColorRecognized(
                        recognizedColor ?: "",
                        confidence ?: 0f
                    )
                } else {
                    colorRecognizerResultAdapter.updateResults(emptyList())
                }

                wordFragmentCameraBinding.overlay.setResults(
                    resultBundle.results.first(),
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    RunningMode.LIVE_STREAM
                )

                wordFragmentCameraBinding.overlay.invalidate()
            }
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            colorRecognizerResultAdapter.updateResults(emptyList())
        }
    }
}