package com.google.mediapipe.examples.gesturerecognizer

import android.content.Context
import android.graphics.Rect
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ArabicEducationNavbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var skipButton: Button
    private lateinit var feedbackView: TextView
    private lateinit var adapter: EducationAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var confidenceThreshold = 0.7f
    private var mediaPlayer: MediaPlayer? = null

    private var completionRunnable: Runnable? = null
    private var holdGestureRunnable: Runnable? = null
    private var isHoldingCorrectGesture = false

    private var gestureNeedsReset = false
    private var lastRecognizedGesture: String? = null

    private val educationSequences = listOf(
        "جامعة" to listOf("جامعة"),
        "جواب" to listOf("جواب"),
        "لغة عربية" to listOf("لغة عربية"),
        "ممتاز" to listOf("ممتاز"),
    )

    init {
        initView()
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.arabic_education_navbar, this, true)
        recyclerView = findViewById(R.id.educationRecyclerView)
        skipButton = findViewById(R.id.skipEducationButton)
        feedbackView = findViewById(R.id.feedbackText)

        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.addItemDecoration(HorizontalSpaceItemDecoration(24))
        setupAdapter(0)
        setupSkipButton()
        updateSkipButton()
    }

    fun setupAdapter(startIndex: Int) {
        adapter = EducationAdapter(educationSequences, startIndex)
        recyclerView.adapter = adapter
        recyclerView.setItemViewCacheSize(3)
    }

    private fun setupSkipButton() {
        skipButton.setOnClickListener {
            if (adapter.currentWordIndex < adapter.itemCount - 1) {
                adapter.skipToNextWord()
                scrollToCurrent()
                resetTimeout()
                gestureNeedsReset = false
                lastRecognizedGesture = null
                updateSkipButton()
                showTemporaryFeedback("تم تخطي الكلمة")
            } else {
                adapter.currentWordIndex = 0
                adapter.resetSequence()
                scrollToCurrent()
                resetTimeout()
                gestureNeedsReset = false
                lastRecognizedGesture = null
                updateSkipButton()
                showTemporaryFeedback("تمت إعادة التسلسل من البداية")
            }
        }
    }

    fun onEducationWordRecognized(word: String, confidence: Float) {
        val targetGesture = adapter.getCurrentGesture() ?: return

        if (confidence < confidenceThreshold) {
            cancelHoldTimer()
            handleLowConfidence()
            return
        }

        if (gestureNeedsReset) {
            if (word != lastRecognizedGesture) {
                gestureNeedsReset = false
            } else {
                return
            }
        }

        lastRecognizedGesture = word

        if (word == targetGesture) {
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

        handler.postDelayed({
            scrollToCurrent()
            if (adapter.isSequenceCompleted()) {
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
        if (adapter.currentWordIndex < educationSequences.size - 1) {
            completionRunnable = Runnable {
                adapter.skipToNextWord()
                scrollToCurrent()
                gestureNeedsReset = false
                lastRecognizedGesture = null
                updateSkipButton()
            }
            handler.postDelayed(completionRunnable!!, 1000)
        } else {
            showTemporaryFeedback("أحسنت! اكتملت جميع الكلمات التعليمية!")
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
        gestureNeedsReset = false
        lastRecognizedGesture = null
    }

    private fun startTimeoutTimer() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = Runnable {
            showTemporaryFeedback("!انتهى الوقت، إعادة التسلسل")
            adapter.resetSequence()
            gestureNeedsReset = false
            lastRecognizedGesture = null
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
            } else {
                val view = lm.findViewByPosition(adapter.currentWordIndex)
                if (view != null) {
                    val left = view.left
                    val right = view.right
                    val width = recyclerView.width
                    if (left < 0 || right > width) {
                        recyclerView.smoothScrollToPosition(adapter.currentWordIndex)
                    }
                }
            }
        }
    }

    private fun updateSkipButton() {
        skipButton.text = if (adapter.currentWordIndex < adapter.itemCount - 1) "تخطي" else "إعادة"
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

    class HorizontalSpaceItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            if (position != parent.adapter?.itemCount?.minus(1)) {
                outRect.right = space
            }
        }
    }
}
