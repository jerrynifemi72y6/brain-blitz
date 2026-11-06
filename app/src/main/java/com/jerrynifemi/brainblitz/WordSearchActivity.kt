package com.jerrynifemi.brainblitz

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.TextView
import kotlin.random.Random

class WordSearchActivity : Activity() {

    companion object {
        const val SCORE_KEY = "wordsearch"
        private const val ROUND_SECONDS = 150
        private const val WORD_POINTS = 100
        private const val WARN_SECONDS = 20
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var board: WordSearchView
    private lateinit var tvScore: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvPack: TextView
    private lateinit var tvWords: TextView
    private lateinit var overlay: View
    private lateinit var tvOverlayTitle: TextView
    private lateinit var tvOverlayDetail: TextView

    private var score = 0
    private var secondsLeft = ROUND_SECONDS
    private var finished = false
    private val found = LinkedHashMap<String, Int>()
    private var warned = false

    private val tick = object : Runnable {
        override fun run() {
            if (finished) return
            secondsLeft--
            renderTimer()
            if (secondsLeft <= 0) {
                finishRound(getString(R.string.time_up), false)
            } else {
                if (secondsLeft == WARN_SECONDS && !warned) {
                    warned = true
                    Audio.play(this@WordSearchActivity, R.raw.sfx_timewarn)
                }
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wordsearch)

        board = findViewById(R.id.wordBoard)
        tvScore = findViewById(R.id.tvWsScore)
        tvTimer = findViewById(R.id.tvWsTimer)
        tvPack = findViewById(R.id.tvWsPack)
        tvWords = findViewById(R.id.tvWsWords)
        overlay = findViewById(R.id.wsOverlay)
        tvOverlayTitle = findViewById(R.id.tvWsOverlayTitle)
        tvOverlayDetail = findViewById(R.id.tvWsOverlayDetail)

        findViewById<View>(R.id.btnWsBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnWsAgain).setOnClickListener {
            overlay.visibility = View.GONE
            newRound()
        }
        findViewById<View>(R.id.btnWsHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        board.onWordFound = { word, _, colorIndex ->
            found[word] = colorIndex
            score += WORD_POINTS
            board.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            Audio.play(this, R.raw.sfx_word)
            renderScore()
            renderWords()
        }
        board.onAllFound = {
            finishRound(getString(R.string.puzzle_cleared), true)
        }

        newRound()
    }

    private fun newRound() {
        finished = false
        warned = false
        score = 0
        secondsLeft = ROUND_SECONDS
        found.clear()
        handler.removeCallbacks(tick)

        val packs = WordBank.packs(this)
        val pack = if (packs.isEmpty()) null else packs[Random.nextInt(packs.size)]
        tvPack.text = pack?.name?.uppercase() ?: getString(R.string.wordsearch_title)

        board.newPuzzle(pack?.words ?: emptyList())
        renderScore()
        renderTimer()
        renderWords()
        handler.postDelayed(tick, 1000)
    }

    private fun renderScore() {
        tvScore.text = getString(R.string.score_label, score)
    }

    private fun renderTimer() {
        tvTimer.text = String.format("%d:%02d", secondsLeft / 60, secondsLeft % 60)
        tvTimer.setTextColor(
            if (secondsLeft <= WARN_SECONDS) resources.getColor(R.color.wrong)
            else resources.getColor(R.color.text_muted)
        )
    }

    private fun renderWords() {
        val builder = SpannableStringBuilder()
        board.words.forEachIndexed { i, word ->
            if (i > 0) builder.append("   ")
            val start = builder.length
            builder.append(word)
            val end = builder.length
            val colorIndex = found[word]
            if (colorIndex != null) {
                builder.setSpan(
                    ForegroundColorSpan(WordSearchView.PALETTE[colorIndex % WordSearchView.PALETTE.size]),
                    start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                builder.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.setSpan(StrikethroughSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        tvWords.text = builder
    }

    private fun finishRound(title: String, cleared: Boolean) {
        if (finished) return
        finished = true
        handler.removeCallbacks(tick)
        Audio.play(this, if (cleared) R.raw.sfx_win else R.raw.sfx_gameover)

        val bonus = if (cleared) secondsLeft * 5 else 0
        score += bonus
        val newBest = Scores.record(this, SCORE_KEY, score)

        tvOverlayTitle.text = title
        tvOverlayDetail.text = getString(
            R.string.ws_overlay_detail,
            score,
            if (cleared) secondsLeft else 0,
            Scores.best(this, SCORE_KEY),
            if (newBest) getString(R.string.new_best_suffix) else ""
        )
        renderScore()
        overlay.alpha = 0f
        overlay.visibility = View.VISIBLE
        overlay.animate().alpha(1f).setDuration(260).start()
    }

    override fun onResume() {
        super.onResume()
        Audio.playMusic(this, R.raw.music_arcade)
        if (!finished && secondsLeft > 0) {
            handler.removeCallbacks(tick)
            handler.postDelayed(tick, 1000)
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
        Audio.pauseMusic()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
