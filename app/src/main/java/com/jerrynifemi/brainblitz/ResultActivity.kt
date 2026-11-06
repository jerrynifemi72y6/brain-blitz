package com.jerrynifemi.brainblitz

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.TextView

class ResultActivity : Activity() {

    companion object {
        const val EXTRA_SCORE = "score"
        const val EXTRA_CORRECT = "correct"
        const val EXTRA_TOTAL = "total"
        const val EXTRA_BEST = "best"
        const val EXTRA_NEW_BEST = "new_best"
        const val EXTRA_MAX_STREAK = "max_streak"
        const val EXTRA_CATEGORY_ID = "category_id"
        const val EXTRA_CATEGORY_LABEL = "category_label"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val score = intent.getIntExtra(EXTRA_SCORE, 0)
        val correct = intent.getIntExtra(EXTRA_CORRECT, 0)
        val total = intent.getIntExtra(EXTRA_TOTAL, 10)
        val best = intent.getIntExtra(EXTRA_BEST, 0)
        val newBest = intent.getBooleanExtra(EXTRA_NEW_BEST, false)
        val maxStreak = intent.getIntExtra(EXTRA_MAX_STREAK, 0)
        val categoryId = intent.getStringExtra(EXTRA_CATEGORY_ID) ?: QuestionBank.ALL_ID
        val categoryLabel = intent.getStringExtra(EXTRA_CATEGORY_LABEL)
            ?: getString(R.string.mixed_label)

        val scoreView = findViewById<TextView>(R.id.tvResultScore)
        scoreView.text = score.toString()
        scoreView.scaleX = 0.6f
        scoreView.scaleY = 0.6f
        scoreView.animate()
            .scaleX(1f).scaleY(1f)
            .setDuration(420)
            .setInterpolator(OvershootInterpolator(1.6f))
            .start()

        findViewById<TextView>(R.id.tvResultDetail).text =
            getString(R.string.result_correct_of, correct, total)
        findViewById<TextView>(R.id.tvResultBest).text =
            getString(R.string.result_best, best)
        findViewById<TextView>(R.id.tvResultLabel).text = categoryLabel.uppercase()

        val perfect = correct == total && total > 0
        Audio.play(this, if (perfect) R.raw.sfx_win else R.raw.sfx_levelup, 0.8f)
        val accuracy = if (total > 0) correct * 100 / total else 0
        findViewById<TextView>(R.id.tvResultVerdict).text =
            if (perfect) getString(R.string.verdict_perfect) else verdictFor(accuracy)

        findViewById<TextView>(R.id.tvResultStreak).text =
            if (maxStreak >= 2) getString(R.string.result_streak, maxStreak) else ""

        findViewById<View>(R.id.tvNewBest).visibility = if (newBest) View.VISIBLE else View.GONE

        findViewById<View>(R.id.btnPlayAgain).setOnClickListener {
            startActivity(
                Intent(this, QuizActivity::class.java).apply {
                    putExtra(QuizActivity.EXTRA_CATEGORY_ID, categoryId)
                    putExtra(QuizActivity.EXTRA_CATEGORY_LABEL, categoryLabel)
                }
            )
            finish()
        }

        findViewById<View>(R.id.btnHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    private fun verdictFor(accuracy: Int): String = when {
        accuracy >= 90 -> getString(R.string.verdict_legend)
        accuracy >= 70 -> getString(R.string.verdict_sharp)
        accuracy >= 50 -> getString(R.string.verdict_solid)
        accuracy >= 30 -> getString(R.string.verdict_warming)
        else -> getString(R.string.verdict_warmup)
    }
}
