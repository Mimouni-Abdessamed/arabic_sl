package com.google.mediapipe.examples.gesturerecognizer

import android.widget.ImageView
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NumberAdapter(
    private val numberList: List<String>,
    private var currentIndex: Int
) : RecyclerView.Adapter<NumberAdapter.NumberViewHolder>() {

    inner class NumberViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val numberBox: TextView = view.findViewById(R.id.letterBox)
        val numberImage: ImageView = view.findViewById(R.id.letterImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NumberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_arabic_letter, parent, false)
        return NumberViewHolder(view)
    }

    override fun getItemCount(): Int = numberList.size

    private val successfulNumbers = mutableSetOf<Int>()

    override fun onBindViewHolder(holder: NumberViewHolder, position: Int) {
        holder.numberImage.elevation = 10f

        holder.numberBox.apply {
            val imageName = when (numberList[position]) {
                "0" -> "zero"
                "1" -> "one"
                "2" -> "two"
                "3" -> "three"
                "4" -> "four"
                "5" -> "five"
                "6" -> "six"
                "7" -> "seven"
                "8" -> "eight"
                "9" -> "nine"
                "10" -> "ten"
                "20" -> "twenty"
                "30" -> "thirty"
                "40" -> "forty"
                "50" -> "fifty"
                "60" -> "sixty"
                "70" -> "seventy"
                "80" -> "eighty"
                "90" -> "ninety"
                "100" -> "hundred"
                else -> "placeholder"
            }

            val resId = holder.itemView.context.resources.getIdentifier(
                imageName, "drawable", holder.itemView.context.packageName
            )

            holder.numberImage.setImageResource(
                if (resId != 0) resId else R.drawable.placeholder_image
            )

            text = numberList[position]

            when {
                position == currentIndex -> {
                    setBackgroundResource(R.drawable.current_letter_bg)
                    textSize = 20f
                    setTextColor(Color.WHITE)
                }
                position in successfulNumbers -> {
                    setBackgroundResource(R.drawable.success_letter_bg)
                    setTextColor(Color.WHITE)
                }
                else -> {
                    setBackgroundResource(R.drawable.letter_box_bg)
                    setTextColor(Color.WHITE)
                    textSize = 18f
                }
            }
        }
    }

    fun markCurrentNumberSuccess() {
        successfulNumbers.add(currentIndex)
        notifyItemChanged(currentIndex)
    }

    fun skipToNext() {
        if (currentIndex < numberList.size - 1) {
            currentIndex++
            notifyDataSetChanged()
        }
    }

    fun getCurrentNumber(): String = numberList[currentIndex]
    fun getCurrentIndex(): Int = currentIndex
}