// ChallengeLetterAdapter.kt
package com.google.mediapipe.examples.gesturerecognizer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChallengeLetterAdapter(
    private val letters: List<String>,
    startPosition: Int
) : RecyclerView.Adapter<ChallengeLetterAdapter.ChallengeViewHolder>() {

    inner class ChallengeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val letterBox: TextView = itemView.findViewById(R.id.letterBox)
    }

    private val completedLetters = mutableSetOf<Int>()
    var currentPosition: Int = startPosition
        private set

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_challenge_letter, parent, false)
        return ChallengeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        holder.letterBox.text = letters[position]
        if (position == currentPosition) {
            holder.letterBox.visibility = View.VISIBLE
            holder.letterBox.setBackgroundResource(R.drawable.current_letter_bg)
        } else {
            holder.letterBox.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount(): Int = letters.size

    fun markSuccess() {
        completedLetters.add(currentPosition)
        notifyItemChanged(currentPosition)
    }

    fun advanceToNext() {
        if (currentPosition < letters.size - 1) {
            currentPosition++
            notifyItemChanged(currentPosition - 1)
            notifyItemChanged(currentPosition)
        }
    }

    fun getCurrentLetter(): String = letters[currentPosition]

    fun hasMoreLetters(): Boolean = currentPosition < letters.size - 1
}