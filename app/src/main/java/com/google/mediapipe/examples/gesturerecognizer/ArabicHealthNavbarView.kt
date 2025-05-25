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

class ArabicHealthNavbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val recyclerView: RecyclerView
    private val skipButton: Button
    private val feedbackView: TextView
    private lateinit var adapter: HealthAdapter

    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var completionRunnable: Runnable? = null
    private var holdGestureRunnable: Runnable? = null
    private var isHoldingCorrectGesture = false
    private var gestureNeedsReset = false
    private val confidenceThreshold = 0.8f

    private var mediaPlayer: MediaPlayer? = null

    private val healthSequences = listOf(
        "دواء" to listOf("دواء"),
        "احرص" to listOf("احرص"),
        "اكسجين" to listOf("اكسجين"),
        "الم" to listOf("الم"),
        "حمى" to listOf("حمى"),
        "دم" to listOf("دم"),
        "كحة" to listOf("كحة"),
        "نظافة" to listOf("نظافة")
    )

    init {
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
        adapter = HealthAdapter(healthSequences, startIndex)
        recyclerView.adapter = adapter
    }

    private fun setupSkipButton() {
        skipButton.setOnClickListener {
            if (adapter.currentWordIndex < adapter.itemCount - 1) {
                // Normal skip to next word
                adapter.skipToNextWord()
                scrollToCurrent()
                resetTimeout()
                gestureNeedsReset = false
                updateSkipButton()
                showTemporaryFeedback("تم تخطي الكلمة")
            } else {
                // Last word reached — this button acts as "Redo"
                adapter.currentWordIndex = 0
                adapter.resetSequence()
                scrollToCurrent()
                resetTimeout()
                gestureNeedsReset = false
                updateSkipButton()
                showTemporaryFeedback("تمت إعادة التسلسل من البداية")
            }
        }
    }

    /**
     * Call this method whenever a gesture is recognized.
     * Handles hold duration, success/failure, retries, and reset logic.
     */
    fun onHealthRecognized(word: String, confidence: Float) {
        val targetGesture = adapter.getCurrentGesture() ?: return

        if (gestureNeedsReset) {
            // Ignore until reset to prevent repeated recognition
            return
        }

        if (confidence < confidenceThreshold) {
            cancelHoldTimer()
            showTemporaryFeedback("إشارة غير واضحة")
            return
        }

        if (word == targetGesture) {
            if (!isHoldingCorrectGesture) {
                isHoldingCorrectGesture = true
                holdGestureRunnable = Runnable {
                    handleCorrectGesture()
                    isHoldingCorrectGesture = false
                    gestureNeedsReset = true
                }
                handler.postDelayed(holdGestureRunnable!!, 1000) // Hold 1 second
            }
        } else {
            cancelHoldTimer()
            if (adapter.retryCount > 1) {
                handleSequenceFailure()
            } else {
                adapter.markGestureIncorrect()
                showTemporaryFeedback("إشارة خاطئة، حاول مرة أخرى")
            }
        }
    }

    private fun cancelHoldTimer() {
        holdGestureRunnable?.let { handler.removeCallbacks(it) }
        holdGestureRunnable = null
        isHoldingCorrectGesture = false
    }

    private fun handleCorrectGesture() {
        resetTimeout()
        adapter.markGestureSuccess()
        scrollToCurrent()
        playSuccessSound()

        if (adapter.currentGestureIndex < adapter.getCurrentSequence().lastIndex) {
            showTemporaryFeedback("الجزء التالي: ${adapter.getCurrentGesture()}")
            startTimeoutTimer()
        } else {
            // Finished all parts of this word
            checkSequenceCompletion()
        }
    }

    private fun checkSequenceCompletion() {
        completionRunnable?.let { handler.removeCallbacks(it) }
        if (adapter.currentWordIndex < adapter.itemCount - 1) {
            completionRunnable = Runnable {
                adapter.skipToNextWord()
                scrollToCurrent()
                gestureNeedsReset = false
                updateSkipButton()
                showTemporaryFeedback("الانتقال للكلمة التالية")
            }
            handler.postDelayed(completionRunnable!!, 1000)
        } else {
            showTemporaryFeedback("أحسنت! اكتملت جميع الكلمات الصحية!")
            updateSkipButton()
        }
    }

    private fun handleSequenceFailure() {
        showTemporaryFeedback("!خطأ في التسلسل، إعادة المحاولة")
        adapter.resetSequence()
        startTimeoutTimer()
        gestureNeedsReset = false
    }

    private fun startTimeoutTimer() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = Runnable {
            showTemporaryFeedback("!انتهى الوقت، إعادة التسلسل")
            adapter.resetSequence()
            gestureNeedsReset = false
        }
        handler.postDelayed(timeoutRunnable!!, 5000)
    }

    private fun resetTimeout() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun scrollToCurrent() {
        recyclerView.smoothScrollToPosition(adapter.currentWordIndex)
    }

    private fun updateSkipButton() {
        if (adapter.currentWordIndex < adapter.itemCount - 1) {
            skipButton.text = "تخطي"
            skipButton.isEnabled = true
        } else {
            // Last word - change to redo button
            skipButton.text = "إعادة"
            skipButton.isEnabled = true
        }
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
    }
}
