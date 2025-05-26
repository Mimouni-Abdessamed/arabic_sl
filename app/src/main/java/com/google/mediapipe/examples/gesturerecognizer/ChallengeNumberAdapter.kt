package com.google.mediapipe.examples.gesturerecognizer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChallengeNumberAdapter(
    private val numbers: List<String>,
    startPosition: Int
) : RecyclerView.Adapter<ChallengeNumberAdapter.ChallengeViewHolder>() {

    inner class ChallengeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val numberBox: TextView = itemView.findViewById(R.id.letterBox)
    }

    private val completedNumbers = mutableSetOf<Int>()
    var currentPosition: Int = startPosition
        private set

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_challenge_letter, parent, false)
        return ChallengeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        holder.numberBox.text = numbers[position]
        if (position == currentPosition) {
            holder.numberBox.visibility = View.VISIBLE
            holder.numberBox.setBackgroundResource(R.drawable.current_letter_bg)
        } else {
            holder.numberBox.visibility = View.INVISIBLE
        }
    }

    override fun getItemCount(): Int = numbers.size

    fun markSuccess() {
        completedNumbers.add(currentPosition)
        notifyItemChanged(currentPosition)
    }

    fun advanceToNext() {
        if (currentPosition < numbers.size - 1) {
            currentPosition++
            notifyItemChanged(currentPosition - 1)
            notifyItemChanged(currentPosition)
        }
    }

    fun getCurrentNumber(): String = numbers[currentPosition]

    fun hasMoreNumbers(): Boolean = currentPosition < numbers.size - 1
}