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
                "0" -> "num_0"
                "1" -> "num_1"
                "2" -> "num_2"
                "3" -> "num_3"
                "4" -> "num_4"
                "5" -> "num_5"
                "6" -> "num_6"
                "7" -> "num_7"
                "8" -> "num_8"
                "9" -> "num_9"
                "10" -> "num_10"
                "20" -> "num_20"
                "30" -> "num_30"
                "40" -> "num_40"
                "50" -> "num_50"
                "60" -> "num_60"
                "70" -> "num_70"
                "80" -> "num_80"
                "90" -> "num_90"
                "100" -> "num_100"
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