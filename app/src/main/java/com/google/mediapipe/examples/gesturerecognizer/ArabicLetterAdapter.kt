package com.google.mediapipe.examples.gesturerecognizer

import android.widget.ImageView
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ArabicLetterAdapter(
    private val letterNames: List<String>,
    private var currentIndex: Int
) : RecyclerView.Adapter<ArabicLetterAdapter.LetterViewHolder>() {

    inner class LetterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val letterBox: TextView = view.findViewById(R.id.letterBox)
        val letterImage: ImageView = view.findViewById(R.id.letterImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LetterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_arabic_letter, parent, false)
        return LetterViewHolder(view)
    }

    override fun getItemCount(): Int = letterNames.size

    private val successfulLetters = mutableSetOf<Int>()

    override fun onBindViewHolder(holder: LetterViewHolder, position: Int) {
        holder.letterImage.elevation = 10f

        holder.letterBox.apply {
            val imageName = when (letterNames[position]) {
                "ا" -> "alef"
                "ب" -> "baa"
                "ت" -> "teh"
                "ث" -> "theh"
                "ج" -> "jeem"
                "ح" -> "hah"
                "خ" -> "khah"
                "د" -> "dal"
                "ذ" -> "thal"
                "ر" -> "raa"
                "ز" -> "zain"
                "س" -> "seen"
                "ش" -> "sheen"
                "ص" -> "sad"
                "ض" -> "dad"
                "ط" -> "taa"
                "ظ" -> "thad"
                "ع" -> "ain"
                "غ" -> "ghain"
                "ف" -> "faa"
                "ق" -> "qaf"
                "ك" -> "kaf"
                "ل" -> "lam"
                "م" -> "meem"
                "ن" -> "noon"
                "ه" -> "heh"
                "و" -> "waw"
                "ي" -> "yaa"
                "لا" -> "laa"
                "ة" -> "teh_marbuta"
                else -> "placeholder"
            }

            val resId = holder.itemView.context.resources.getIdentifier(
                imageName,
                "drawable",
                holder.itemView.context.packageName
            )

            holder.letterImage.setImageResource(
                if (resId != 0) resId else R.drawable.placeholder_image
            )

            text = letterNames[position]

            when {
                position == currentIndex -> {
                    setBackgroundResource(R.drawable.current_letter_bg)
                    textSize = 20f
                    setTextColor(Color.WHITE)
                }
                position in successfulLetters -> {
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

    // Mark success WITHOUT auto-skipping
    fun markCurrentLetterSuccess() {
        successfulLetters.add(currentIndex)
        notifyItemChanged(currentIndex)  // Update only current item
    }

    // Skip externally controlled
    fun skipToNext() {
        if (currentIndex < letterNames.size - 1) {
            currentIndex++
            notifyDataSetChanged()
        }
    }

    fun getCurrentLetter(): String = letterNames[currentIndex]
    fun getCurrentIndex(): Int = currentIndex
}