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

class VerbAdapter(
    private val verbData: List<Pair<String, List<String>>>,
    var currentWordIndex: Int
) : RecyclerView.Adapter<VerbAdapter.VerbViewHolder>() {

    private val successfulVerbs = mutableSetOf<Pair<Int, Int>>()
    var currentVerbIndex = 0
    var retryCount = 0
    private val verbStates = mutableMapOf<Pair<Int, Int>, VerbState>()

    enum class VerbState { PENDING, CORRECT, INCORRECT }

    inner class VerbViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val verbText: TextView = view.findViewById(R.id.wordText)
        val progressText: TextView = view.findViewById(R.id.progressText)
        val playerView: PlayerView = view.findViewById(R.id.wordVideo)
        var player: ExoPlayer? = null
        var boundPosition: Int = -1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerbViewHolder {
        return VerbViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_arabic_word, parent, false)
        )
    }

    override fun onBindViewHolder(holder: VerbViewHolder, position: Int) {
        val (displayName, verbParts) = verbData[position]
        val isCurrentVerb = position == currentWordIndex
        val allVerbsCompleted = verbParts.indices.all { index ->
            verbStates[position to index] == VerbState.CORRECT
        }

        // Clean up previous binding
        holder.player?.release()
        holder.player = null
        holder.playerView.player = null
        holder.boundPosition = position

        // Configure view appearance
        with(holder.verbText) {
            when {
                allVerbsCompleted -> setBackgroundResource(R.drawable.success_letter_bg)
                isCurrentVerb -> setBackgroundResource(R.drawable.current_letter_bg)
                else -> setBackgroundResource(R.drawable.letter_box_bg)
            }
            textSize = if (isCurrentVerb) 20f else 18f
            setTextColor(android.graphics.Color.WHITE)
            text = displayName
        }

        holder.progressText.text = if (isCurrentVerb) {
            "${currentVerbIndex + 1}/${verbParts.size}"
        } else {
            ""
        }

        // Video initialization
        if (isCurrentVerb) {
            initializeVideoPlayer(holder, displayName)
        } else {
            holder.playerView.visibility = View.INVISIBLE
        }
    }

    private fun initializeVideoPlayer(holder: VerbViewHolder, displayName: String) {
        val context = holder.itemView.context
        val videoName = when (displayName) {
            "يتيمم" -> "yatayamam"
            "اصمت" -> "shutup"
            "يسافر" -> "travel"
            "يشرب" -> "drink"
            "يشم" -> "smell"
            "يفكر" -> "think"
            "ينظر" -> "watch"
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

    override fun getItemCount(): Int = verbData.size

    override fun onViewRecycled(holder: VerbViewHolder) {
        holder.player?.release()
        holder.player = null
        holder.playerView.player = null
        super.onViewRecycled(holder)
    }

    fun getCurrentSequence(): List<String> = verbData[currentWordIndex].second

    fun getCurrentVerb(): String? = getCurrentSequence().getOrNull(currentVerbIndex)

    fun markVerbSuccess() {
        verbStates[currentWordIndex to currentVerbIndex] = VerbState.CORRECT
        successfulVerbs.add(currentWordIndex to currentVerbIndex)

        if (currentVerbIndex < getCurrentSequence().lastIndex) {
            currentVerbIndex++
        } else {
            if ((0..getCurrentSequence().lastIndex).all { index ->
                    verbStates[currentWordIndex to index] == VerbState.CORRECT
                }) {
                currentVerbIndex = 0
            }
        }
        retryCount = 0
        notifyItemChanged(currentWordIndex)
    }

    fun skipToNextVerb() {
        if (currentWordIndex < verbData.size - 1) {
            currentWordIndex++
            resetSequence()
            notifyDataSetChanged()
        }
    }

    fun resetSequence() {
        currentVerbIndex = 0
        retryCount = 0
        getCurrentSequence().indices.forEach { index ->
            verbStates[currentWordIndex to index] = VerbState.PENDING
        }
        notifyItemChanged(currentWordIndex)
    }
}