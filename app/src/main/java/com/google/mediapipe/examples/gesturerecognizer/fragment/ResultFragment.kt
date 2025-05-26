package com.google.mediapipe.examples.gesturerecognizer.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.mediapipe.examples.gesturerecognizer.QuizFragment
import com.google.mediapipe.examples.gesturerecognizer.R

class ResultFragment : Fragment() {

    companion object {
        fun newInstance(score: Int, total: Int): ResultFragment {
            val fragment = ResultFragment()
            val args = Bundle()
            args.putInt("SCORE", score)
            args.putInt("TOTAL", total)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_result, container, false)

        val scoreText = view.findViewById<TextView>(R.id.scoreText)
        val retryButton = view.findViewById<Button>(R.id.retryButton)

        val score = arguments?.getInt("SCORE") ?: 0
        val total = arguments?.getInt("TOTAL") ?: 0

        scoreText.text = "تقييمك هو : $score / $total"

        retryButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, QuizFragment())
                .commit()
        }

        return view
    }
}
