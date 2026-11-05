package com.jerrynifemi.brainblitz

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvBest = findViewById<TextView>(R.id.tvBest)
        tvBest.text = getString(R.string.best_score_value, Scores.best(this, QuestionBank.ALL_ID))

        findViewById<View>(R.id.btnPlay).setOnClickListener {
            startQuiz(QuestionBank.ALL_ID, getString(R.string.mixed_label))
        }

        val container = findViewById<LinearLayout>(R.id.listCategories)
        container.removeAllViews()
        QuestionBank.categories(this).forEach { category ->
            val row = layoutInflater.inflate(R.layout.item_category, container, false)
            row.findViewById<TextView>(R.id.tvCategoryName).text = category.name
            row.findViewById<TextView>(R.id.tvCategoryBest).text =
                getString(R.string.category_best, Scores.best(this, category.id))
            row.setOnClickListener { startQuiz(category.id, category.name) }
            container.addView(row)
        }
    }

    private fun startQuiz(categoryId: String, label: String) {
        startActivity(
            Intent(this, QuizActivity::class.java).apply {
                putExtra(QuizActivity.EXTRA_CATEGORY_ID, categoryId)
                putExtra(QuizActivity.EXTRA_CATEGORY_LABEL, label)
            }
        )
    }

    override fun onResume() {
        super.onResume()
        // Refresh bests when coming back from a round.
        findViewById<TextView>(R.id.tvBest).text =
            getString(R.string.best_score_value, Scores.best(this, QuestionBank.ALL_ID))

        val container = findViewById<LinearLayout>(R.id.listCategories)
        val categories = QuestionBank.categories(this)
        for (i in 0 until minOf(container.childCount, categories.size)) {
            container.getChildAt(i).findViewById<TextView>(R.id.tvCategoryBest).text =
                getString(R.string.category_best, Scores.best(this, categories[i].id))
        }
    }
}
