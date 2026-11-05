package com.jerrynifemi.brainblitz

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class CategoryActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        val container = findViewById<LinearLayout>(R.id.listCategories)
        val categories = QuestionBank.categories(this)

        // "Everything" first, then the individual categories.
        addRow(container, QuestionBank.ALL_ID, getString(R.string.mixed_label), null)
        categories.forEach { addRow(container, it.id, it.name, it.questions.size) }

        findViewById<TextView>(R.id.tvCategoryCount).text =
            getString(R.string.category_count, categories.size, categories.sumOf { it.questions.size })
    }

    private fun addRow(container: LinearLayout, id: String, label: String, count: Int?) {
        val row = layoutInflater.inflate(R.layout.item_category, container, false)
        row.findViewById<TextView>(R.id.tvCategoryName).text = label
        row.findViewById<TextView>(R.id.tvCategoryBest).text =
            getString(R.string.best_chip, Scores.best(this, id))
        val counter = row.findViewById<TextView>(R.id.tvCategoryCount)
        if (count == null) counter.text = getString(R.string.all_questions_chip)
        else counter.text = getString(R.string.question_chip, count)
        row.setOnClickListener {
            startActivity(
                Intent(this, QuizActivity::class.java).apply {
                    putExtra(QuizActivity.EXTRA_CATEGORY_ID, id)
                    putExtra(QuizActivity.EXTRA_CATEGORY_LABEL, label)
                }
            )
        }
        container.addView(row)
    }
}
