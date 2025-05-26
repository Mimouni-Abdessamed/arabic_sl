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

class ArabicVerbNavbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val recyclerView: RecyclerView
    private val skipButton: Button
    private val feedbackView: TextView
    private lateinit var adapter: VerbAdapter

    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var completionRunnable: Runnable? = null
    private var holdGestureRunnable: Runnable? = null
    private var isHoldingCorrectGesture = false
    private var gestureNeedsReset = false
    private val confidenceThreshold = 0.7f

    private var mediaPlayer: MediaPlayer? = null

    private val verbSequences = listOf(
        "يتيمم" to listOf("يتيمما", "يتيممب"),
        "اصمت" to listOf("اصمت"),
        "يسافر" to listOf("يسافر"),
        "يشرب" to listOf("يشرب"),
        "يشم" to listOf("يشم"),
        "يفكر" to listOf("يفكر"),
        "ينظر" to listOf("ينظر")
    )

    private var isAtEnd = false

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
        adapter = VerbAdapter(verbSequences, startIndex)
        recyclerView.adapter = adapter
        recyclerView.setItemViewCacheSize(3)
    }

    private fun setupSkipButton() {
        skipButton.setOnClickListener {
            if (isAtEnd) {
                adapter.currentWordIndex = 0
                adapter.resetSequence()
                scrollToCurrent()
                isAtEnd = false
                updateSkipButton()
                resetTimeout()
                gestureNeedsReset = false
            } else {
                adapter.skipToNextVerb()
                scrollToCurrent()
                resetTimeout()
                gestureNeedsReset = false

                if (adapter.currentWordIndex == adapter.itemCount - 1) {
                    isAtEnd = true
                    updateSkipButton()
                }
            }
        }
    }

    fun onVerbRecognized(word: String, confidence: Float) {
        val targetVerb = adapter.getCurrentVerb() ?: return

        if (confidence < confidenceThreshold) {
            cancelHoldTimer()
            showTemporaryFeedback("إشارة غير واضحة")
            return
        }

        if (word == targetVerb) {
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

    private fun handleCorrectGesture() {
        resetTimeout()
        adapter.markVerbSuccess()
        playSuccessSound()
        gestureNeedsReset = true

        handler.postDelayed({
            scrollToCurrent()
            if (adapter.currentVerbIndex == 0) {
                checkSequenceCompletion()
            } else {
                showNextVerbPrompt()
                startTimeoutTimer()
            }
        }, 1000)
    }

    private fun handleIncorrectGesture() {
        if (adapter.retryCount > 1) handleSequenceFailure()
    }

    private fun checkSequenceCompletion() {
        completionRunnable?.let { handler.removeCallbacks(it) }
        if (adapter.currentWordIndex < verbSequences.size - 1) {
            completionRunnable = Runnable {
                adapter.skipToNextVerb()
                scrollToCurrent()
                gestureNeedsReset = false
            }
            handler.postDelayed(completionRunnable!!, 1000)
        } else {
            showTemporaryFeedback("أحسنت! اكتملت جميع الأفعال!")
            isAtEnd = true
            updateSkipButton()
        }
    }

    private fun showNextVerbPrompt() {
        val nextVerb = adapter.getCurrentVerb()
        showTemporaryFeedback("الجزء التالي: $nextVerb")
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