package com.google.mediapipe.examples.gesturerecognizer

import android.content.Context
import android.media.MediaPlayer
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.Button
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.os.Handler
import android.os.Looper

class ChallengesNumberNavbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private lateinit var challengeRecycler: RecyclerView
    private lateinit var actionButton: Button
    private lateinit var adapter: ChallengeNumberAdapter

    private val handler = Handler(Looper.getMainLooper())
    private var pendingAdvancement: Runnable? = null
    private var successRunnable: Runnable? = null
    private var isProcessingSuccess = false
    private var isCheckingHold = false

    private val challengeNumbers = listOf(
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
        "20", "30", "40", "50", "60", "70", "80", "90", "100"
    )

    init {
        initChallengeView()
    }

    private fun initChallengeView() {
        LayoutInflater.from(context).inflate(R.layout.challenge_navbar, this, true)
        challengeRecycler = findViewById(R.id.challengeRecycler)
        actionButton = findViewById(R.id.btnSkipChallenge)

        setupRecyclerView()
        setupButton()
        updateButtonText()
    }

    private fun setupRecyclerView() {
        val shuffledNumbers = challengeNumbers.shuffled()
        challengeRecycler.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        challengeRecycler.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE -> true // Block swipe gestures
                else -> false
            }
        }

        adapter = ChallengeNumberAdapter(shuffledNumbers, 0)
        challengeRecycler.adapter = adapter
    }

    private fun setupButton() {
        actionButton.setOnClickListener {
            if (isLastChallenge()) {
                val newShuffledNumbers = challengeNumbers.shuffled()
                adapter = ChallengeNumberAdapter(newShuffledNumbers, 0)
                challengeRecycler.adapter = adapter
                challengeRecycler.smoothScrollToPosition(0)
                actionButton.text = "تخطي"
            } else {
                adapter.advanceToNext()
                challengeRecycler.smoothScrollToPosition(adapter.currentPosition)
                updateButtonText()
            }
        }
    }

    private fun isLastChallenge(): Boolean = !adapter.hasMoreNumbers()

    private fun updateButtonText() {
        actionButton.text = if (isLastChallenge()) "اعادة المحاوله" else "تخطي"
    }

    fun handleSuccessfulRecognition(recognizedNumber: String) {
        if (isProcessingSuccess) return

        if (recognizedNumber == adapter.getCurrentNumber()) {
            if (!isCheckingHold) {
                isCheckingHold = true
                successRunnable = Runnable {
                    pendingAdvancement?.let { handler.removeCallbacks(it) }

                    MediaPlayer.create(context, R.raw.success).apply {
                        setOnCompletionListener { release() }
                        start()
                    }

                    adapter.markSuccess()
                    isProcessingSuccess = true

                    pendingAdvancement = Runnable {
                        if (adapter.hasMoreNumbers()) {
                            adapter.advanceToNext()
                            challengeRecycler.smoothScrollToPosition(adapter.currentPosition)
                            updateButtonText()
                        }
                        isProcessingSuccess = false
                        isCheckingHold = false
                    }
                    handler.postDelayed(pendingAdvancement!!, 1000)
                }
                handler.postDelayed(successRunnable!!, 1000)
            }
        } else {
            if (isCheckingHold) {
                handler.removeCallbacks(successRunnable!!)
                isCheckingHold = false
            }
        }
    }

    fun getCurrentChallengeNumber(): String = adapter.getCurrentNumber()
}