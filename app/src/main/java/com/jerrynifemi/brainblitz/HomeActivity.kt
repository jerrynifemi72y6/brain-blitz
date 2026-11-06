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
            Audio.play(this, R.raw.sfx_tap)
            startActivity(Intent(this, CategoryActivity::class.java))
        }
        findViewById<View>(R.id.cardWordSearch).setOnClickListener {
            Audio.play(this, R.raw.sfx_tap)
            startActivity(Intent(this, WordSearchActivity::class.java))
        }
        findViewById<View>(R.id.cardMatch3).setOnClickListener {
            Audio.play(this, R.raw.sfx_tap)
            startActivity(Intent(this, Match3Activity::class.java))
        }
        findViewById<View>(R.id.cardSnake).setOnClickListener {
            Audio.play(this, R.raw.sfx_tap)
            startActivity(Intent(this, SnakeActivity::class.java))
        }
        findViewById<View>(R.id.cardBlock).setOnClickListener {
            Audio.play(this, R.raw.sfx_tap)
            startActivity(Intent(this, BlockfallActivity::class.java))
        }

        findViewById<View>(R.id.btnSound).setOnClickListener {
            val next = !Audio.enabled(this)
            Audio.setEnabled(this, next)
            renderSoundButton()
            Audio.play(this, R.raw.sfx_tap)
        }

        val categories = QuestionBank.categories(this)
        val questionTotal = categories.sumOf { it.questions.size }
        findViewById<TextView>(R.id.tvTriviaMeta).text =
            getString(R.string.mode_trivia_meta, categories.size, questionTotal)

        renderSoundButton()
    }

    private fun renderSoundButton() {
        val button = findViewById<TextView>(R.id.btnSound)
        val on = Audio.enabled(this)
        button.text = getString(if (on) R.string.sound_on else R.string.sound_off)
        button.setTextColor(resources.getColor(if (on) R.color.accent_alt else R.color.text_muted))
    }

    override fun onResume() {
        super.onResume()
        Audio.playMusic(this, R.raw.music_menu)
        renderSoundButton()

        findViewById<TextView>(R.id.tvTriviaBest).text =
            getString(R.string.best_chip, Scores.best(this, QuestionBank.ALL_ID))
        findViewById<TextView>(R.id.tvWordBest).text =
            getString(R.string.best_chip, Scores.best(this, WordSearchActivity.SCORE_KEY))
        findViewById<TextView>(R.id.tvMatchBest).text =
            getString(R.string.best_chip, Scores.best(this, Match3Activity.SCORE_KEY))
        findViewById<TextView>(R.id.tvSnakeBest).text =
            getString(R.string.best_chip, Scores.best(this, SnakeActivity.SCORE_KEY))
        findViewById<TextView>(R.id.tvBlockBest).text =
            getString(R.string.best_chip, Scores.best(this, BlockfallActivity.SCORE_KEY))
    }

    override fun onPause() {
        super.onPause()
        Audio.pauseMusic()
    }
}
