package com.google.mediapipe.examples.gesturerecognizer

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.LinkedList
import java.util.Queue

class DictionaryFragment : Fragment() {

    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var signPhraseTextView: TextView
    private lateinit var searchEditText: EditText
    private lateinit var videoQueue: Queue<String>
    private lateinit var handler: Handler
    private var isPlayingQueue = false

    private val wordToVideoMap = mapOf(
        // Default welcome
        "مرحبا" to "مرحبا.mp4", "المرحبا" to "مرحبا.mp4",

        // Numbers
        "0" to "صفر.mp4", "٠" to "صفر.mp4", "صفر" to "صفر.mp4", "الصفر" to "صفر.mp4",
        "1" to "واحد.mp4", "١" to "واحد.mp4", "واحد" to "واحد.mp4", "الواحد" to "واحد.mp4",
        "2" to "اثنين.mp4", "٢" to "اثنين.mp4", "اثنين" to "اثنين.mp4", "الاثنين" to "اثنين.mp4",
        "3" to "ثلاثة.mp4", "٣" to "ثلاثة.mp4", "ثلاثة" to "ثلاثة.mp4", "الثلاثة" to "ثلاثة.mp4",
        "4" to "أربعة.mp4", "٤" to "أربعة.mp4", "أربعة" to "أربعة.mp4", "الأربعة" to "أربعة.mp4",
        "اربعة" to "أربعة.mp4", "الاربعة" to "أربعة.mp4", // Added normalized versions
        "5" to "خمسة.mp4", "٥" to "خمسة.mp4", "خمسة" to "خمسة.mp4", "الخمسة" to "خمسة.mp4",
        "6" to "ستة.mp4", "٦" to "ستة.mp4", "ستة" to "ستة.mp4", "الستة" to "ستة.mp4",
        "7" to "سبعة.mp4", "٧" to "سبعة.mp4", "سبعة" to "سبعة.mp4", "السبعة" to "سبعة.mp4",
        "8" to "ثمانية.mp4", "٨" to "ثمانية.mp4", "ثمانية" to "ثمانية.mp4", "الثمانية" to "ثمانية.mp4",
        "9" to "تسعة.mp4", "٩" to "تسعة.mp4", "تسعة" to "تسعة.mp4", "التسعة" to "تسعة.mp4",
        "10" to "عشرة.mp4", "١٠" to "عشرة.mp4", "عشرة" to "عشرة.mp4", "العشرة" to "عشرة.mp4",
        "20" to "عشرين.mp4", "٢٠" to "عشرين.mp4", "عشرين" to "عشرين.mp4", "العشرين" to "عشرين.mp4",
        "30" to "ثلاثون.mp4", "٣٠" to "ثلاثون.mp4", "ثلاثون" to "ثلاثون.mp4", "الثلاثون" to "ثلاثون.mp4",
        "40" to "أربعون.mp4", "٤٠" to "أربعون.mp4", "أربعون" to "أربعون.mp4", "الأربعون" to "أربعون.mp4",
        "اربعون" to "أربعون.mp4", "الاربعون" to "أربعون.mp4", // Added normalized versions
        "50" to "خمسون.mp4", "٥٠" to "خمسون.mp4", "خمسون" to "خمسون.mp4", "الخمسون" to "خمسون.mp4",
        "60" to "ستين.mp4", "٦٠" to "ستين.mp4", "ستين" to "ستين.mp4", "الستين" to "ستين.mp4",
        "70" to "سبعون.mp4", "٧٠" to "سبعون.mp4", "سبعون" to "سبعون.mp4", "السبعون" to "سبعون.mp4",
        "80" to "ثمانون.mp4", "٨٠" to "ثمانون.mp4", "ثمانون" to "ثمانون.mp4", "الثمانون" to "ثمانون.mp4",
        "90" to "تسعين.mp4", "٩٠" to "تسعين.mp4", "تسعين" to "تسعين.mp4", "التسعين" to "تسعين.mp4",
        "100" to "مئة.mp4", "١٠٠" to "مئة.mp4", "مئة" to "مئة.mp4", "المئة" to "مئة.mp4",

        // Letters (unchanged)
        "ال" to "ال.mp4", "ال ال" to "ال.mp4",
        "ة" to "ة.mp4", "التاء المربوطة" to "ة.mp4",
        "ت" to "ت.mp4", "التاء" to "ت.mp4",
        "ث" to "ث.mp4", "الثاء" to "ث.mp4",
        "خ" to "خ.mp4", "الخاء" to "خ.mp4",
        "ذ" to "ذ.mp4", "الذال" to "ذ.mp4",
        "ر" to "ر.mp4", "الراء" to "ر.mp4",
        "ز" to "ز.mp4", "الزاي" to "ز.mp4",
        "س" to "س.mp4", "السين" to "س.mp4",
        "ش" to "ش.mp4", "الشين" to "ش.mp4",
        "ص" to "ص.mp4", "الصاد" to "ص.mp4",
        "ض" to "ض.mp4", "الضاد" to "ض.mp4",
        "ط" to "ط.mp4", "الطاء" to "ط.mp4",
        "ظ" to "ظ.mp4", "الظاء" to "ظ.mp4",
        "ع" to "ع.mp4", "العين" to "ع.mp4",
        "غ" to "غ.mp4", "الغين" to "غ.mp4",
        "ف" to "ف.mp4", "الفاء" to "ف.mp4",
        "ق" to "ق.mp4", "القاف" to "ق.mp4",
        "لا" to "لا.mp4", "اللام ألف" to "لا.mp4",
        "م" to "م.mp4", "الميم" to "م.mp4",
        "ن" to "ن.mp4", "النون" to "ن.mp4",
        "ه" to "ه.mp4", "الهاء" to "ه.mp4",
        "و" to "و.mp4", "الواو" to "و.mp4",
        "ي" to "ي(1).mp4", "الياء" to "ي(1).mp4",

        // Animals - keeping original أ names but adding normalized versions
        "ذبابة" to "ذبابة.mp4", "الذبابة" to "ذبابة.mp4",
        "زرافة" to "زرافة.mp4", "الزرافة" to "زرافة.mp4",
        "سمك" to "سمك.mp4", "السمك" to "سمك.mp4",
        "عنكبوت" to "عنكبوت.mp4", "العنكبوت" to "عنكبوت.mp4",
        "فيل" to "فيل.mp4", "الفيل" to "فيل.mp4",
        "قطة" to "قطة.mp4", "القطة" to "قطة.mp4",
        "كلب" to "كلب.mp4", "الكلب" to "كلب.mp4",
        "نحلة" to "نحلة.mp4", "النحلة" to "نحلة.mp4",
        "نسر" to "نسر.mp4", "النسر" to "نسر.mp4",
        "نمر" to "نمر.mp4", "النمر" to "نمر.mp4",
        "أرنب" to "أرنب.mp4", "الأرنب" to "أرنب.mp4",
        "ارنب" to "أرنب.mp4", "الارنب" to "أرنب.mp4", // Added normalized versions
        "أسد" to "أسد.mp4", "الأسد" to "أسد.mp4",
        "اسد" to "أسد.mp4", "الاسد" to "أسد.mp4", // Added normalized versions
        "ثعبان" to "ثعبان.mp4", "الثعبان" to "ثعبان.mp4",
        "حصان" to "حصان.mp4", "الحصان" to "حصان.mp4",
        "خفاش" to "خفاش.mp4", "الخفاش" to "خفاش.mp4",

        // Colors - keeping original أ names but adding normalized versions
        "أبيض" to "أبيض.mp4", "الأبيض" to "أبيض.mp4",
        "ابيض" to "أبيض.mp4", "الابيض" to "أبيض.mp4", // Added normalized versions
        "أحمر" to "أحمر(1).mp4", "الأحمر" to "أحمر(1).mp4",
        "احمر" to "أحمر(1).mp4", "الاحمر" to "أحمر(1).mp4", // Added normalized versions
        "أخضر" to "أخضر.mp4", "الأخضر" to "أخضر.mp4",
        "اخضر" to "أخضر.mp4", "الاخضر" to "أخضر.mp4", // Added normalized versions
        "أزرق سماوي" to "أزرق سماوي.mp4",
        "ازرق سماوي" to "أزرق سماوي.mp4", // Added normalized version
        "أزرق" to "أزرق.mp4", "الأزرق" to "أزرق.mp4",
        "ازرق" to "أزرق.mp4", "الازرق" to "أزرق.mp4", // Added normalized versions
        "أسود" to "أسود.mp4", "الأسود" to "أسود.mp4",
        "اسود" to "أسود.mp4", "الاسود" to "أسود.mp4", // Added normalized versions
        "أصفر" to "أصفر.mp4", "الأصفر" to "أصفر.mp4",
        "اصفر" to "أصفر.mp4", "الاصفر" to "أصفر.mp4", // Added normalized versions
        "برتقالي" to "برتقالي.mp4", "البرتقالي" to "برتقالي.mp4",
        "بنفسجي" to "بنفسجي.mp4", "البنفسجي" to "بنفسجي.mp4",
        "بني" to "بني.mp4", "البني" to "بني.mp4",

        // Family - keeping original أ names but adding normalized versions
        "عم" to "عم.mp4", "العم" to "عم.mp4",
        "أب" to "أب.mp4", "الأب" to "أب.mp4",
        "اب" to "أب.mp4", "الاب" to "أب.mp4", // Added normalized versions
        "ابن" to "ابن.mp4", "الابن" to "ابن.mp4",
        "ابنة" to "ابنة.mp4", "الابنة" to "ابنة.mp4",
        "أخت" to "أخت.mp4", "الأخت" to "أخت.mp4",
        "اخت" to "أخت.mp4", "الاخت" to "أخت.mp4", // Added normalized versions
        "أسرة" to "أسرة.mp4", "الأسرة" to "أسرة.mp4",
        "اسرة" to "أسرة.mp4", "الاسرة" to "أسرة.mp4", // Added normalized versions
        "أم" to "أم.mp4", "الأم" to "أم.mp4",
        "ام" to "أم.mp4", "الام" to "أم.mp4", // Added normalized versions
        "جدة" to "جدة.mp4", "الجدة" to "جدة.mp4",
        "زوجة" to "زوجة.mp4", "الزوجة" to "زوجة.mp4",
        "عمة" to "عمة.mp4", "العمة" to "عمة.mp4",

        // Verbs (unchanged as they start with ي)
        "يبيع" to "يبيع.mp4",
        "يخرج" to "يخرج.mp4",
        "يخيط" to "يخيط.mp4",
        "يدفع" to "يدفع.mp4",
        "يستعطف" to "يستعطف.mp4",
        "يصرخ" to "يصرخ.mp4",
        "يصور" to "يصور.mp4",
        "يطلب" to "يطلب.mp4",
        "يعاقب" to "يعاقب.mp4",
        "يفتح" to "يفتح.mp4",
        "ينام" to "ينام.mp4",
        "ينزع" to "ينزع.mp4",
        "ينصح" to "ينصح.mp4"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dictionary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference.child("sign_videos")
        videoQueue = LinkedList()
        handler = Handler(Looper.getMainLooper())

        playerView = view.findViewById(R.id.playerView)
        signPhraseTextView = view.findViewById(R.id.signPhraseTextView)
        searchEditText = view.findViewById(R.id.searchEditText)

        setupPlayer()
        setupSearchButton(view)
        setupEditTextListener() // Optional: Setup Enter key listener
        showWelcomeVideo()
    }

    private fun showWelcomeVideo() {
        signPhraseTextView.text = "إشارة مرحبا"
        playSingleVideo("مرحبا.mp4")
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(requireContext()).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        if (isPlayingQueue && videoQueue.isNotEmpty()) {
                            handler.postDelayed({ playNextVideoInQueue() }, 500)
                        } else {
                            seekTo(0)
                            play()
                        }
                    }
                }
            })
            volume = 0f
        }
        playerView.player = player
    }

    private fun setupSearchButton(view: View) {
        view.findViewById<Button>(R.id.searchButton).setOnClickListener {
            val searchText = searchEditText.text.toString().trim()
            hideKeyboard()
            if (searchText.isNotEmpty()) {
                processInputText(searchText)
            } else {
                showError("الرجاء إدخال كلمة أو عبارة")
            }
        }
    }

    // Optional: Setup Enter key listener
    private fun setupEditTextListener() {
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val searchText = searchEditText.text.toString().trim()
                hideKeyboard()
                if (searchText.isNotEmpty()) {
                    processInputText(searchText)
                    true
                } else {
                    showError("الرجاء إدخال كلمة أو عبارة")
                    false
                }
            } else {
                false
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun processInputText(input: String) {
        videoQueue.clear()
        val words = input.split("\\s+".toRegex())
        val notFoundWords = mutableListOf<String>()
        val suggestedWords = mutableMapOf<String, List<String>>()

        words.forEach { word ->
            if (wordToVideoMap.containsKey(word)) {
                videoQueue.add(wordToVideoMap[word]!!)
            } else {
                notFoundWords.add(word)
                val suggestions = getSimilarWords(word).take(3)
                if (suggestions.isNotEmpty()) {
                    suggestedWords[word] = suggestions
                }
            }
        }

        when {
            videoQueue.isNotEmpty() -> {
                isPlayingQueue = words.size > 1
                signPhraseTextView.text = "إشارة: $input"

                if (notFoundWords.isNotEmpty()) {
                    showSuggestionDialog(notFoundWords, suggestedWords)
                }

                if (isPlayingQueue) {
                    playNextVideoInQueue()
                } else {
                    playSingleVideo(videoQueue.poll())
                }
            }
            else -> {
                if (notFoundWords.isNotEmpty()) {
                    showSuggestionDialog(notFoundWords, suggestedWords)
                }
                playerView.visibility = View.GONE
            }
        }
    }

    private fun showSuggestionDialog(notFoundWords: List<String>, suggestedWords: Map<String, List<String>>) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_suggestions, null).apply {
            // Remove any default background
            setBackgroundResource(0)
        }

        val titleTextView = dialogView.findViewById<TextView>(R.id.titleTextView)
        val messageTextView = dialogView.findViewById<TextView>(R.id.messageTextView)
        val suggestionsContainer = dialogView.findViewById<LinearLayout>(R.id.suggestionsContainer)

        titleTextView.text = "لم يتم العثور على ${notFoundWords.joinToString("، ")}"

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
            .setView(dialogView)
            .setPositiveButton("إغلاق") { dialog, _ -> dialog.dismiss() }
            .create()

        // Remove default dialog padding and background
        dialog.window?.let { window ->
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.decorView.setBackgroundColor(Color.TRANSPARENT)
            window.setDimAmount(0.5f) // Adjust dim amount as needed
        }

        if (suggestedWords.isEmpty()) {
            messageTextView.text = "لا توجد اقتراحات متاحة"
        } else {
            messageTextView.text = "اختر من الاقتراحات:"

            suggestedWords.forEach { (wrongWord, suggestions) ->
                val wordHeader = TextView(requireContext()).apply {
                    text = "بدلاً من '$wrongWord':"
                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    setPadding(
                        resources.getDimensionPixelSize(R.dimen.dialog_horizontal_padding),
                        16.dpToPx(),
                        resources.getDimensionPixelSize(R.dimen.dialog_horizontal_padding),
                        8.dpToPx()
                    )
                    textSize = 16f
                }
                suggestionsContainer.addView(wordHeader)

                suggestions.forEach { suggestion ->
                    val button = Button(requireContext()).apply {
                        text = suggestion
                        setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                        background = ContextCompat.getDrawable(requireContext(), R.drawable.suggestion_button_bg)
                        setOnClickListener {
                            replaceWordInSearch(wrongWord, suggestion)
                            dialog.dismiss()
                        }
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(
                                resources.getDimensionPixelSize(R.dimen.dialog_horizontal_padding),
                                4.dpToPx(),
                                resources.getDimensionPixelSize(R.dimen.dialog_horizontal_padding),
                                4.dpToPx()
                            )
                        }
                    }
                    suggestionsContainer.addView(button)
                }
            }
        }

        dialog.show()

        // Adjust dialog window attributes if needed
        dialog.window?.attributes?.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
        dialog.window?.attributes?.height = ViewGroup.LayoutParams.WRAP_CONTENT
    }

    private fun replaceWordInSearch(wrongWord: String, correctWord: String) {
        val currentText = searchEditText.text.toString()
        val newText = currentText.replace(wrongWord, correctWord, ignoreCase = true)
        searchEditText.setText(newText)
        searchEditText.setSelection(newText.length)
        hideKeyboard()
        processInputText(newText)
    }

    private fun getSimilarWords(input: String): List<String> {
        val inputNormalized = normalizeArabic(input)
        val suggestions = mutableListOf<Pair<String, Int>>()

        wordToVideoMap.keys.forEach { dictWord ->
            val distance = calculateSimilarity(inputNormalized, normalizeArabic(dictWord))
            if (distance <= 2) {
                suggestions.add(Pair(dictWord, distance))
            }
        }

        return suggestions.sortedBy { it.second }
            .take(3)
            .map { it.first }
    }

    private fun normalizeArabic(text: String): String {
        return text.lowercase()
            .replace("[إأآا]".toRegex(), "ا")  // Replace all Alef variations with standard ا
            .replace("ال", "")                 // Remove definite article
            .replace("ة", "ه")                 // Replace Ta Marbuta with Ha
            .replace("ى", "ي")                 // Replace Alif Maqsura with Ya
            .replace("ئ", "ء")                 // Replace Yeh with Hamza with Hamza
            .replace("[ًٌٍَُِّْ]".toRegex(), "") // Remove Arabic diacritics (tashkeel)
            .trim()
    }

    private fun calculateSimilarity(s1: String, s2: String): Int {
        if (s1 == s2) return 0

        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) {
            for (j in 0..s2.length) {
                when {
                    i == 0 -> dp[i][j] = j
                    j == 0 -> dp[i][j] = i
                    else -> {
                        val substitutionCost = if (s1[i-1] == s2[j-1]) 0 else 1
                        dp[i][j] = minOf(
                            dp[i-1][j] + 1,
                            dp[i][j-1] + 1,
                            dp[i-1][j-1] + substitutionCost
                        )
                    }
                }
            }
        }
        return dp[s1.length][s2.length]
    }

    private fun playNextVideoInQueue() {
        if (videoQueue.isNotEmpty()) {
            playSingleVideo(videoQueue.poll())
        } else {
            isPlayingQueue = false
        }
    }

    private fun playSingleVideo(fileName: String) {
        try {
            player.stop()
            player.clearMediaItems()
            playerView.visibility = View.VISIBLE

            storageRef.child(fileName).downloadUrl.addOnSuccessListener { uri ->
                player.setMediaItem(MediaItem.fromUri(uri))
                player.prepare()
                player.playWhenReady = true
            }.addOnFailureListener {
                if (isPlayingQueue) {
                    playNextVideoInQueue()
                } else {
                    showError("تعذر تحميل الفيديو")
                    playerView.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            Log.e("DictionaryFragment", "Playback error", e)
            if (isPlayingQueue) {
                playNextVideoInQueue()
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        player.release()
        handler.removeCallbacksAndMessages(null)
    }
}