package com.google.mediapipe.examples.gesturerecognizer

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView

class HealthAdapter(
    private val wordData: List<Pair<String, List<String>>>,
    var currentWordIndex: Int
) : RecyclerView.Adapter<HealthAdapter.WordViewHolder>() {

    private val successfulGestures = mutableSetOf<Pair<Int, Int>>()
    var currentGestureIndex = 0
    var retryCount = 0
    private val gestureStates = mutableMapOf<Pair<Int, Int>, LetterState>()

    enum class LetterState { PENDING, CORRECT, INCORRECT }

    inner class WordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val wordText: TextView = view.findViewById(R.id.wordText)
        val progressText: TextView = view.findViewById(R.id.progressText)
        val playerView: PlayerView = view.findViewById(R.id.wordVideo)
        var player: ExoPlayer? = null
        var boundPosition: Int = -1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        return WordViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_arabic_word, parent, false)
        )
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val (displayName, gestureParts) = wordData[position]
        val isCurrentWord = position == currentWordIndex
        val allGesturesCompleted = gestureParts.indices.all { index ->
            gestureStates[position to index] == LetterState.CORRECT
        }

        // Clean up previous binding
        holder.player?.release()
        holder.player = null
        holder.playerView.player = null
        holder.boundPosition = position

        with(holder.wordText) {
            when {
                allGesturesCompleted -> setBackgroundResource(R.drawable.success_letter_bg)
                isCurrentWord -> setBackgroundResource(R.drawable.current_letter_bg)
                else -> setBackgroundResource(R.drawable.letter_box_bg)
            }
            textSize = if (isCurrentWord) 20f else 18f
            setTextColor(android.graphics.Color.WHITE)
            text = displayName
        }

        holder.progressText.text = if (isCurrentWord) {
            "${currentGestureIndex + 1}/${gestureParts.size}"
        } else {
            ""
        }

        if (isCurrentWord) {
            initializeVideoPlayer(holder, displayName)
        } else {
            holder.playerView.visibility = View.INVISIBLE
        }
    }

    private fun initializeVideoPlayer(holder: WordViewHolder, displayName: String) {
        val context = holder.itemView.context
        val videoName = when (displayName) {
            "دواء" -> "medicament"
            "احرص" -> "becareful"
            "اكسجين" -> "oxygen"
            "الم" -> "pain"
            "حمى" -> "fever"
            "دم" -> "blood"
            "كحة" -> "caugh"
            "نظافة" -> "cleaness"
            else -> null
        } ?: return

        val resId = context.resources.getIdentifier(videoName, "raw", context.packageName)
        if (resId == 0) return

        val uri = Uri.parse("android.resource://${context.packageName}/$resId")
        val vto = holder.playerView.viewTreeObserver

        vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (holder.boundPosition != currentWordIndex) {
                    holder.playerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    return
                }

                if (holder.playerView.width > 0 && holder.playerView.height > 0) {
                    holder.playerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val player = ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(uri))
                        repeatMode = Player.REPEAT_MODE_ONE
                        prepare()
                        play()
                    }
                    holder.player = player
                    holder.playerView.player = player
                    holder.playerView.visibility = View.VISIBLE
                }
            }
        })
    }

    override fun getItemCount(): Int = wordData.size

    override fun onViewRecycled(holder: WordViewHolder) {
        holder.player?.release()
        holder.player = null
        holder.playerView.player = null
        super.onViewRecycled(holder)
    }

    // Existing HealthAdapter functions
    fun getCurrentSequence(): List<String> = wordData[currentWordIndex].second
    fun getCurrentGesture(): String? = getCurrentSequence().getOrNull(currentGestureIndex)

    fun markGestureSuccess() {
        gestureStates[currentWordIndex to currentGestureIndex] = LetterState.CORRECT
        if (currentGestureIndex < getCurrentSequence().lastIndex) {
            currentGestureIndex++
        } else {
            if ((0..getCurrentSequence().lastIndex).all { index ->
                    gestureStates[currentWordIndex to index] == LetterState.CORRECT
                }) {
                currentGestureIndex = 0
            }
        }
        retryCount = 0
        notifyItemChanged(currentWordIndex)
    }

    fun markGestureIncorrect() {
        gestureStates[currentWordIndex to currentGestureIndex] = LetterState.INCORRECT
        retryCount++
        notifyItemChanged(currentWordIndex)
    }

    fun resetSequence() {
        currentGestureIndex = 0
        retryCount = 0
        getCurrentSequence().indices.forEach { index ->
            gestureStates[currentWordIndex to index] = LetterState.PENDING
        }
        notifyItemChanged(currentWordIndex)
    }

    fun skipToNextWord() {
        if (currentWordIndex < wordData.size - 1) {
            currentWordIndex++
            resetSequence()
            notifyDataSetChanged()
        }
    }
}