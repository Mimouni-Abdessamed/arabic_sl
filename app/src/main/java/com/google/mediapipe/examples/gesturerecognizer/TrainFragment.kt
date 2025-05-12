package com.google.mediapipe.examples.gesturerecognizer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.mediapipe.examples.gesturerecognizer.fragment.*

class TrainFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_train, container, false)

        val lettersButton = view.findViewById<MaterialButton>(R.id.lettersButton)
        val numbersButton = view.findViewById<MaterialButton>(R.id.numbersButton)
        val challengesButton = view.findViewById<MaterialButton>(R.id.challengesButton)
        val wordsButton = view.findViewById<MaterialButton>(R.id.wordsButton)
        val quizButton = view.findViewById<MaterialButton>(R.id.quizButton)

        lettersButton.setOnClickListener {
            replaceFragment(LetterCameraFragment())
        }

        numbersButton.setOnClickListener {
            replaceFragment(NumberCameraFragment())
        }

        challengesButton.setOnClickListener {
            replaceFragment(ChallengesCameraFragment())
        }

        wordsButton.setOnClickListener {
            // TODO: Replace with your actual fragment
            replaceFragment(LetterCameraFragment())
        }

        quizButton.setOnClickListener {
            // TODO: Replace with your actual fragment
            replaceFragment(QuizFragment())
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fadeIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)

        val cardLetters = view.findViewById<CardView>(R.id.card_letters)
        val cardNumbers = view.findViewById<CardView>(R.id.card_numbers)
        val cardChallenges = view.findViewById<CardView>(R.id.card_challenges)
        val cardWords = view.findViewById<CardView>(R.id.card_words)
        val cardQuiz = view.findViewById<CardView>(R.id.card_quiz)

        cardLetters.startAnimation(fadeIn)
        cardNumbers.postDelayed({ cardNumbers.startAnimation(fadeIn) }, 150)
        cardChallenges.postDelayed({ cardChallenges.startAnimation(fadeIn) }, 300)
        cardWords.postDelayed({ cardWords.startAnimation(fadeIn) }, 450)
        cardQuiz.postDelayed({ cardQuiz.startAnimation(fadeIn) }, 600)
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
