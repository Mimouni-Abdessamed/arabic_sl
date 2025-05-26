package com.google.mediapipe.examples.gesturerecognizer

import android.media.MediaPlayer
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.os.Handler
import android.os.Looper

class NumberNavbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var actionButton: Button
    private lateinit var adapter: NumberAdapter

    private val numberList = listOf(
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
        "20", "30", "40", "50", "60", "70", "80", "90", "100"
    )

    private val handler = Handler(Looper.getMainLooper())
    private var skipRunnable: Runnable? = null
    private var successRunnable: Runnable? = null
    private var isSuccessHandled = false
    private var isCheckingHold = false

    init {
        initView()
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.number_navbar, this, true)
        recyclerView = findViewById(R.id.letterRecyclerView)
        actionButton = findViewById(R.id.skipButton)

        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, true)
        setupAdapter(0)
        updateButtonText()

        actionButton.setOnClickListener {
            cancelPendingSkip()
            if (isLastNumber()) {
                setupAdapter(0)
                actionButton.text = "تخطي"
            } else {
                adapter.skipToNext()
                updateButtonText()
            }
            isSuccessHandled = false
            scrollToCurrent()
        }
    }

    private fun setupAdapter(startIndex: Int) {
        adapter = NumberAdapter(numberList, startIndex)
        recyclerView.adapter = adapter
        updateButtonText()
    }

    private fun scrollToCurrent() {
        recyclerView.smoothScrollToPosition(adapter.getCurrentIndex())
    }

    private fun isLastNumber(): Boolean = adapter.getCurrentIndex() == numberList.lastIndex

    private fun updateButtonText() {
        actionButton.text = if (isLastNumber()) "اعادة المحاوله" else "تخطي"
    }

    fun getCurrentNumber(): String = adapter.getCurrentNumber()

    fun onNumberRecognized(number: String, confidence: Float) {
        val current = adapter.getCurrentNumber()
        if (!isSuccessHandled && number == current && confidence >= 0.8f) {
            if (!isCheckingHold) {
                isCheckingHold = true
                successRunnable = Runnable {
                    isSuccessHandled = true
                    cancelPendingSkip()
                    adapter.markCurrentNumberSuccess()
                    playSuccessSound()
                    scheduleSkipToNext()
                    isCheckingHold = false
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

    private fun scheduleSkipToNext() {
        skipRunnable = Runnable {
            adapter.skipToNext()
            isSuccessHandled = false
            scrollToCurrent()
            updateButtonText()
        }
        handler.postDelayed(skipRunnable!!, 1000)
    }

    private fun cancelPendingSkip() {
        skipRunnable?.let {
            handler.removeCallbacks(it)
            skipRunnable = null
        }
    }

    private fun playSuccessSound() {
        val mediaPlayer = MediaPlayer.create(context, R.raw.success)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener { it.release() }
    }
}