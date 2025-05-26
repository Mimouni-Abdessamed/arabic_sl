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
import com.google.mediapipe.examples.gesturerecognizer.*
import com.google.mediapipe.examples.gesturerecognizer.databinding.FragmentChallengeCameraBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class NumbersChallengeFragment : Fragment(), NumberRecognizerHelper.GestureRecognizerListener {

    private var _fragmentBinding: FragmentChallengeCameraBinding? = null
    private val fragmentBinding get() = _fragmentBinding!!
    private lateinit var numberRecognizerHelper: NumberRecognizerHelper
    private val viewModel: MainViewModel by activityViewModels()
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraFacing = CameraSelector.LENS_FACING_FRONT
    private lateinit var backgroundExecutor: ExecutorService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentBinding = FragmentChallengeCameraBinding.inflate(inflater, container, false)
        return fragmentBinding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backgroundExecutor = Executors.newSingleThreadExecutor()

        setupChallengeNavbar()
        fragmentBinding.viewFinder.post { setUpCamera() }

        backgroundExecutor.execute {
            numberRecognizerHelper = NumberRecognizerHelper(
                context = requireContext(),
                runningMode = RunningMode.LIVE_STREAM,
                minHandDetectionConfidence = viewModel.currentMinHandDetectionConfidence,
                minHandTrackingConfidence = viewModel.currentMinHandTrackingConfidence,
                minHandPresenceConfidence = viewModel.currentMinHandPresenceConfidence,
                currentDelegate = viewModel.currentDelegate,
                gestureRecognizerListener = this
            )
        }
    }

    private fun setupChallengeNavbar() {
        fragmentBinding.challengesNavbarViewContainer.removeAllViews()
        val navbar = ChallengesNumberNavbarView(requireContext())
        fragmentBinding.challengesNavbarViewContainer.addView(navbar)
    }

    override fun onPause() {
        super.onPause()
        viewModel.setMinHandDetectionConfidence(numberRecognizerHelper.minHandDetectionConfidence)
        viewModel.setMinHandTrackingConfidence(numberRecognizerHelper.minHandTrackingConfidence)
        viewModel.setMinHandPresenceConfidence(numberRecognizerHelper.minHandPresenceConfidence)
        viewModel.setDelegate(numberRecognizerHelper.currentDelegate)
        backgroundExecutor.execute { numberRecognizerHelper.clearGestureRecognizer() }
    }

    override fun onDestroyView() {
        _fragmentBinding = null
        super.onDestroyView()
        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
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
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera init failed")
        val cameraSelector = CameraSelector.Builder().requireLensFacing(cameraFacing).build()

        preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentBinding.viewFinder.display.rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(fragmentBinding.viewFinder.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also {
                it.setAnalyzer(backgroundExecutor) { image ->
                    numberRecognizerHelper.recognizeLiveStream(image)
                }
            }

        cameraProvider.unbindAll()
        try {
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )
            preview?.setSurfaceProvider(fragmentBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = fragmentBinding.viewFinder.display.rotation
    }

    override fun onResults(resultBundle: NumberRecognizerHelper.ResultBundle) {
        activity?.runOnUiThread {
            if (_fragmentBinding != null) {
                val numberCategories = resultBundle.results.first().gestures()
                if (numberCategories.isNotEmpty()) {
                    val recognizedNumber = numberCategories.first().firstOrNull()
                    recognizedNumber?.let { number ->
                        (fragmentBinding.challengesNavbarViewContainer.getChildAt(0) as ChallengesNumberNavbarView)
                            .handleSuccessfulRecognition(number.categoryName())
                    }
                }
                fragmentBinding.overlay.setResults(
                    resultBundle.results.first(),
                    resultBundle.inputImageHeight,
                    resultBundle.inputImageWidth,
                    RunningMode.LIVE_STREAM
                )
                fragmentBinding.overlay.invalidate()
            }
        }
    }

    override fun onError(error: String, errorCode: Int) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "NumbersChallengeFragment"
    }
}