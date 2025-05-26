package com.google.mediapipe.examples.gesturerecognizer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.mediapipe.examples.gesturerecognizer.fragment.*

class TrainFragment : Fragment() {

    private var isTrainingExpanded = false
    private var isChallengesExpanded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_train, container, false)

        // Training Section
        val trainingExpandButton = view.findViewById<MaterialButton>(R.id.btn_training_expand)
        val lettersButton = view.findViewById<MaterialButton>(R.id.lettersButton)
        val numbersButton = view.findViewById<MaterialButton>(R.id.numbersButton)
        val wordsButton = view.findViewById<MaterialButton>(R.id.wordsButton)

        // Challenges Section
        val challengesExpandButton = view.findViewById<MaterialButton>(R.id.btn_challenges_expand)
        val lettersChallengeBtn = view.findViewById<MaterialButton>(R.id.letters_challenge_btn)
        val numbersChallengeBtn = view.findViewById<MaterialButton>(R.id.numbers_challenge_btn)

        // Quiz Section
        val quizButton = view.findViewById<MaterialButton>(R.id.quizButton)

        // Click Listeners
        trainingExpandButton.setOnClickListener { toggleTrainingButtons(view) }
        challengesExpandButton.setOnClickListener { toggleChallengesButtons(view) }

        lettersButton.setOnClickListener { replaceFragment(LetterCameraFragment()) }
        numbersButton.setOnClickListener { replaceFragment(NumberCameraFragment()) }
        wordsButton.setOnClickListener { replaceFragment(WordCategoriesFragment()) }
        lettersChallengeBtn.setOnClickListener { replaceFragment(LettersChallengeFragment()) }
        numbersChallengeBtn.setOnClickListener { replaceFragment(NumbersChallengeFragment()) }
        quizButton.setOnClickListener { replaceFragment(QuizFragment()) }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fadeIn = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
        val cardTraining = view.findViewById<CardView>(R.id.card_training)
        val cardChallenges = view.findViewById<CardView>(R.id.card_challenges)
        val cardQuiz = view.findViewById<CardView>(R.id.card_quiz)

        cardTraining.startAnimation(fadeIn)
        cardChallenges.postDelayed({ cardChallenges.startAnimation(fadeIn) }, 150)
        cardQuiz.postDelayed({ cardQuiz.startAnimation(fadeIn) }, 300)
    }

    private fun toggleTrainingButtons(view: View) {
        val subButtons = view.findViewById<View>(R.id.sub_buttons_container)
        val expandButton = view.findViewById<MaterialButton>(R.id.btn_training_expand)

        if (isChallengesExpanded) toggleChallengesButtons(view)

        if (isTrainingExpanded) {
            collapseSection(subButtons, expandButton, R.drawable.ic_arrow_down)
        } else {
            expandSection(subButtons, expandButton, R.drawable.ic_arrow_up)
        }
        isTrainingExpanded = !isTrainingExpanded
    }

    private fun toggleChallengesButtons(view: View) {
        val subButtons = view.findViewById<View>(R.id.challenges_sub_container)
        val expandButton = view.findViewById<MaterialButton>(R.id.btn_challenges_expand)

        if (isTrainingExpanded) toggleTrainingButtons(view)

        if (isChallengesExpanded) {
            collapseSection(subButtons, expandButton, R.drawable.ic_arrow_down)
        } else {
            expandSection(subButtons, expandButton, R.drawable.ic_arrow_up)
        }
        isChallengesExpanded = !isChallengesExpanded
    }

    private fun expandSection(
        subButtons: View,
        expandButton: MaterialButton,
        iconRes: Int
    ) {
        subButtons.visibility = View.VISIBLE
        subButtons.alpha = 0f
        subButtons.translationY = -20f
        subButtons.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
        expandButton.icon = resources.getDrawable(iconRes, null)
    }

    private fun collapseSection(
        subButtons: View,
        expandButton: MaterialButton,
        iconRes: Int
    ) {
        subButtons.animate()
            .alpha(0f)
            .translationY(-20f)
            .setDuration(300)
            .withEndAction { subButtons.visibility = View.GONE }
        expandButton.icon = resources.getDrawable(iconRes, null)
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