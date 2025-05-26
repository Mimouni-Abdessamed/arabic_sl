package com.google.mediapipe.examples.gesturerecognizer

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ArabicWordNavbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var isLoadingNewWord = false
    private lateinit var recyclerView: RecyclerView
    private lateinit var skipButton: Button
    private lateinit var feedbackView: TextView
    private lateinit var adapter: WordAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var confidenceThreshold = 0.7f
    private var mediaPlayer: MediaPlayer? = null

    private var completionRunnable: Runnable? = null
    private var holdGestureRunnable: Runnable? = null
    private var isHoldingCorrectGesture = false

    // Gesture tracking
    private var lastRecognizedGesture: String? = null
    private var gestureNeedsReset = false

    private val colorSequences = listOf(
        "احمر" to listOf("احمر"),
        "بنفسجي" to listOf("بنفسجيا", "بنفسجيب"),
        "اخضر" to listOf("اخضر"),
        "اسود" to listOf("اسود"),
        "بني" to listOf("بني"),
        "وردي" to listOf("وردي"),
        "ابيض" to listOf("ابيض"),
        "ازرق" to listOf("ازرق"),
    )

    private var isAtEnd = false

    init {
        initView()
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.arabic_word_navbar, this, true)
        recyclerView = findViewById(R.id.wordRecyclerView)
        skipButton = findViewById(R.id.skipWordButton)
        feedbackView = findViewById(R.id.feedbackText)

        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        setupAdapter(0)
        setupSkipButton()
        updateSkipButton()
    }

    fun setupAdapter(startIndex: Int) {
        adapter = WordAdapter(colorSequences, startIndex)
        recyclerView.adapter = adapter
        recyclerView.setItemViewCacheSize(3) // Improve video stability
    }

    private fun setupSkipButton() {
        skipButton.setOnClickListener {
            if (isAtEnd) {
                // Reset to beginning
                adapter.currentWordIndex = 0
                adapter.resetSequence()
                isAtEnd = false
                updateSkipButton()
                resetTimeout()
                gestureNeedsReset = false
                lastRecognizedGesture = null
                scrollToCurrent()
            } else {
                // Skip to next word
                adapter.skipToNextWord()
                resetTimeout()
                gestureNeedsReset = false
                lastRecognizedGesture = null
                scrollToCurrent()

                if (adapter.currentWordIndex == adapter.itemCount - 1) {
                    isAtEnd = true
                    updateSkipButton()
                }
            }
        }
    }

    fun onColorRecognized(color: String, confidence: Float) {
        if (isLoadingNewWord) return

        val targetGesture = adapter.getCurrentGesture() ?: return

        if (gestureNeedsReset) {
            if (color != lastRecognizedGesture) {
                gestureNeedsReset = false
            } else {
                return
            }
        }

        if (confidence < confidenceThreshold) {
            cancelHoldTimer()
            handleLowConfidence()
            return
        }

        if (color == targetGesture) {
            if (!isHoldingCorrectGesture) {
                isHoldingCorrectGesture = true
                holdGestureRunnable = Runnable {
                    handleCorrectGesture()
                    isHoldingCorrectGesture = false
                }
                handler.postDelayed(holdGestureRunnable!!, 1000)
            }
        } else {
            cancelHoldTimer()
            handleIncorrectGesture()
        }
    }

    private fun cancelHoldTimer() {
        holdGestureRunnable?.let { handler.removeCallbacks(it) }
        holdGestureRunnable = null
        isHoldingCorrectGesture = false
    }

    private fun handleLowConfidence() {
        showTemporaryFeedback("إشارة غير واضحة")
    }

    private fun handleCorrectGesture() {
        resetTimeout()
        adapter.markGestureSuccess()
        playSuccessSound()

        gestureNeedsReset = true
        lastRecognizedGesture = adapter.getCurrentGesture()

        handler.postDelayed({
            scrollToCurrent()
            if (adapter.currentGestureIndex == 0) {
                checkSequenceCompletion()
            } else {
                showNextGesturePrompt()
                startTimeoutTimer()
            }
        }, 1000)
    }

    private fun handleIncorrectGesture() {
        if (adapter.retryCount > 1) handleSequenceFailure()
    }

    private fun checkSequenceCompletion() {
        completionRunnable?.let { handler.removeCallbacks(it) }
        if (adapter.currentWordIndex < colorSequences.size - 1) {
            completionRunnable = Runnable {
                isLoadingNewWord = true
                adapter.skipToNextWord()
                scrollToCurrent()
                gestureNeedsReset = false
                lastRecognizedGesture = null
                handler.postDelayed({ isLoadingNewWord = false }, 2000)
            }
            handler.postDelayed(completionRunnable!!, 1000)
        } else {
            showTemporaryFeedback("أحسنت! اكتملت جميع الألوان!")
            isAtEnd = true
            updateSkipButton()
        }
    }

    private fun showNextGesturePrompt() {
        val nextGesture = adapter.getCurrentGesture()
        showTemporaryFeedback("الجزء التالي: $nextGesture")
    }

    private fun handleSequenceFailure() {
        showTemporaryFeedback("!خطأ في التسلسل، إعادة المحاولة")
        adapter.resetSequence()
        startTimeoutTimer()
    }

    private fun startTimeoutTimer() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = Runnable {
            showTemporaryFeedback("!انتهى الوقت، إعادة التسلسل")
            adapter.resetSequence()
        }
        handler.postDelayed(timeoutRunnable!!, 5000)
    }

    private fun resetTimeout() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun scrollToCurrent() {
        recyclerView.post {
            val lm = recyclerView.layoutManager as LinearLayoutManager
            val firstVisible = lm.findFirstVisibleItemPosition()
            val lastVisible = lm.findLastVisibleItemPosition()

            if (adapter.currentWordIndex < firstVisible ||
                adapter.currentWordIndex > lastVisible) {
                recyclerView.smoothScrollToPosition(adapter.currentWordIndex)
            }
        }
    }

    private fun updateSkipButton() {
        skipButton.text = if (isAtEnd) "إعادة" else "تخطي"
        skipButton.isEnabled = true
    }

    private fun showTemporaryFeedback(text: String) {
        feedbackView.text = text
        handler.postDelayed({ feedbackView.text = "" }, 2000)
    }

    private fun playSuccessSound() {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(context, R.raw.success).apply {
            start()
            setOnCompletionListener { release() }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
        mediaPlayer = null
    }
}