package com.jerrynifemi.brainblitz

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView

class ResultActivity : Activity() {

    companion object {
        const val EXTRA_SCORE = "score"
        const val EXTRA_CORRECT = "correct"
        const val EXTRA_TOTAL = "total"
        const val EXTRA_BEST = "best"
        const val EXTRA_NEW_BEST = "new_best"
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
        val categoryId = intent.getStringExtra(EXTRA_CATEGORY_ID) ?: QuestionBank.ALL_ID
        val categoryLabel = intent.getStringExtra(EXTRA_CATEGORY_LABEL)
            ?: getString(R.string.mixed_label)

        findViewById<TextView>(R.id.tvResultScore).text = score.toString()
        findViewById<TextView>(R.id.tvResultDetail).text =
            getString(R.string.result_correct_of, correct, total)
        findViewById<TextView>(R.id.tvResultBest).text =
            getString(R.string.result_best, best)
        findViewById<TextView>(R.id.tvResultLabel).text = categoryLabel.uppercase()

        val accuracy = if (total > 0) correct * 100 / total else 0
        findViewById<TextView>(R.id.tvResultVerdict).text = verdictFor(accuracy)

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

        findViewById<View>(R.id.btnHome).setOnClickListener { finish() }
    }

    private fun verdictFor(accuracy: Int): String = when {
        accuracy >= 90 -> getString(R.string.verdict_legend)
        accuracy >= 70 -> getString(R.string.verdict_sharp)
        accuracy >= 50 -> getString(R.string.verdict_solid)
        accuracy >= 30 -> getString(R.string.verdict_warming)
        else -> getString(R.string.verdict_warmup)
    }
}
