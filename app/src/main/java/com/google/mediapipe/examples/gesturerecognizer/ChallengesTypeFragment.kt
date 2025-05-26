package com.google.mediapipe.examples.gesturerecognizer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.mediapipe.examples.gesturerecognizer.fragment.LettersChallengeFragment
import com.google.mediapipe.examples.gesturerecognizer.fragment.NumbersChallengeFragment


class ChallengesTypeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_challenges_type, container, false)

        val lettersBtn = view.findViewById<MaterialButton>(R.id.letters_challenges)
        val numbersBtn = view.findViewById<MaterialButton>(R.id.numbers_challenges)
        val wordsBtn = view.findViewById<MaterialButton>(R.id.words_challenges)

        lettersBtn.setOnClickListener {
            replaceFragment(LettersChallengeFragment())
        }

        numbersBtn.setOnClickListener {
            replaceFragment(NumbersChallengeFragment())
        }

        wordsBtn.setOnClickListener {
            replaceFragment(LettersChallengeFragment())
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fadeIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)

        val cardLetters = view.findViewById<CardView>(R.id.card_letters_challenges)
        val cardNumbers = view.findViewById<CardView>(R.id.card_numbers_challenges)
        val cardWords = view.findViewById<CardView>(R.id.card_words_challenges)

        cardLetters.startAnimation(fadeIn)
        cardNumbers.postDelayed({ cardNumbers.startAnimation(fadeIn) }, 150)
        cardWords.postDelayed({ cardWords.startAnimation(fadeIn) }, 300)
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
