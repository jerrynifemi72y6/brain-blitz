package com.jerrynifemi.brainblitz

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView

class HomeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        findViewById<View>(R.id.cardTrivia).setOnClickListener {
            startActivity(Intent(this, CategoryActivity::class.java))
        }
        findViewById<View>(R.id.cardWordSearch).setOnClickListener {
            startActivity(Intent(this, WordSearchActivity::class.java))
        }
        findViewById<View>(R.id.cardMatch3).setOnClickListener {
            startActivity(Intent(this, Match3Activity::class.java))
        }

        val categories = QuestionBank.categories(this)
        val questionTotal = categories.sumOf { it.questions.size }
        findViewById<TextView>(R.id.tvTriviaMeta).text =
            getString(R.string.mode_trivia_meta, categories.size, questionTotal)
    }

    override fun onResume() {
        super.onResume()
        findViewById<TextView>(R.id.tvTriviaBest).text =
            getString(R.string.best_chip, Scores.best(this, QuestionBank.ALL_ID))
        findViewById<TextView>(R.id.tvWordBest).text =
            getString(R.string.best_chip, Scores.best(this, WordSearchActivity.SCORE_KEY))
        findViewById<TextView>(R.id.tvMatchBest).text =
            getString(R.string.best_chip, Scores.best(this, Match3Activity.SCORE_KEY))
    }
}
