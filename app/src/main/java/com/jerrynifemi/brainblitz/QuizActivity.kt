package com.jerrynifemi.brainblitz

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView

class QuizActivity : Activity() {

    companion object {
        const val EXTRA_CATEGORY_ID = "category_id"
        const val EXTRA_CATEGORY_LABEL = "category_label"
        private const val QUESTION_COUNT = 10
        private const val TIME_MS = 15_000L
        private const val TICK_MS = 50L
    }

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var tvScore: TextView
    private lateinit var tvStreak: TextView
    private lateinit var tvCounter: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var tvFeedback: TextView
    private lateinit var timeLeftLabel: TextView
    private lateinit var timeBar: ProgressBar

    private val optionViews = ArrayList<TextView>()

    private var questions: List<Question> = emptyList()
    private var index = 0
    private var score = 0
    private var correctCount = 0
    private var streak = 0
    private var answered = false
    private var timeLeftMs = TIME_MS
    private var timer: CountDownTimer? = null

    private var categoryId = QuestionBank.ALL_ID
    private lateinit var categoryLabel: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        categoryId = intent.getStringExtra(EXTRA_CATEGORY_ID) ?: QuestionBank.ALL_ID
        categoryLabel = intent.getStringExtra(EXTRA_CATEGORY_LABEL) ?: getString(R.string.mixed_label)

        tvScore = findViewById(R.id.tvScore)
        tvStreak = findViewById(R.id.tvStreak)
        tvCounter = findViewById(R.id.tvCounter)
        tvCategory = findViewById(R.id.tvQuizCategory)
        tvQuestion = findViewById(R.id.tvQuestion)
        tvFeedback = findViewById(R.id.tvFeedback)
        timeBar = findViewById(R.id.timeBar)
        timeLeftLabel = findViewById(R.id.tvSeconds)

        optionViews.clear()
        optionViews.add(findViewById(R.id.option1))
        optionViews.add(findViewById(R.id.option2))
        optionViews.add(findViewById(R.id.option3))
        optionViews.add(findViewById(R.id.option4))

        tvCategory.text = categoryLabel.uppercase()

        findViewById<View>(R.id.btnQuit).setOnClickListener { finish() }

        val pool = QuestionBank.questionsFor(this, categoryId).shuffled()
        questions = pool.take(QUESTION_COUNT).map { shuffleOptions(it) }

        if (questions.isEmpty()) {
            tvQuestion.text = getString(R.string.no_questions)
            optionViews.forEach { it.visibility = View.INVISIBLE }
            return
        }

        updateScoreLabels()
        renderQuestion()
    }

    /** Keeps the same correct answer but randomises where it sits. */
    private fun shuffleOptions(q: Question): Question {
        val pairs = q.options.mapIndexed { i, text -> text to (i == q.answerIndex) }.shuffled()
        return q.copy(
            options = pairs.map { it.first },
            answerIndex = pairs.indexOfFirst { it.second }
        )
    }

    private fun renderQuestion() {
        val q = questions[index]
        answered = false

        tvCounter.text = getString(R.string.question_counter, index + 1, questions.size)
        tvQuestion.text = q.text
        tvFeedback.text = ""
        tvFeedback.visibility = View.INVISIBLE

        optionViews.forEachIndexed { i, view ->
            view.text = q.options.getOrElse(i) { "" }
            view.visibility = if (i < q.options.size) View.VISIBLE else View.INVISIBLE
            view.setBackgroundResource(R.drawable.bg_option)
            view.isEnabled = true
            view.alpha = 1f
            view.setOnClickListener { onOptionTapped(i) }
        }

        startTimer()
    }

    private fun startTimer() {
        timer?.cancel()
        timeLeftMs = TIME_MS
        timeBar.progressTintList = ColorStateList.valueOf(color(R.color.accent_alt))
        timeBar.max = 1000
        timeBar.progress = 1000
        timeLeftLabel.text = "15s"

        timer = object : CountDownTimer(TIME_MS, TICK_MS) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftMs = millisUntilFinished
                timeBar.progress = ((millisUntilFinished * 1000) / TIME_MS).toInt()
                timeLeftLabel.text = String.format("%ds", (millisUntilFinished / 1000) + 1)
                if (millisUntilFinished <= 4000) {
                    timeBar.progressTintList = ColorStateList.valueOf(color(R.color.wrong))
                }
            }

            override fun onFinish() {
                if (!answered) onTimeout()
            }
        }.start()
    }

    private fun onOptionTapped(tapped: Int) {
        if (answered) return
        answered = true
        timer?.cancel()
        optionViews[tapped].performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

        val q = questions[index]
        val isCorrect = tapped == q.answerIndex
        optionViews[q.answerIndex].setBackgroundResource(R.drawable.bg_option_correct)

        if (isCorrect) {
            correctCount++
            streak++
            val speedBonus = (timeLeftMs / 1000).toInt() * 5
            val streakBonus = minOf(streak - 1, 5) * 20
            val gained = 100 + speedBonus + streakBonus
            score += gained
            showFeedback(getString(R.string.feedback_correct, gained), R.color.accent_alt)
        } else {
            streak = 0
            optionViews[tapped].setBackgroundResource(R.drawable.bg_option_wrong)
            showFeedback(getString(R.string.feedback_wrong), R.color.wrong)
        }

        updateScoreLabels()
        lockOptions()
        handler.postDelayed({ advance() }, 1150)
    }

    private fun onTimeout() {
        if (answered) return
        answered = true
        streak = 0
        val q = questions[index]
        optionViews[q.answerIndex].setBackgroundResource(R.drawable.bg_option_correct)
        showFeedback(getString(R.string.feedback_timeout), R.color.wrong)
        updateScoreLabels()
        lockOptions()
        handler.postDelayed({ advance() }, 1350)
    }

    private fun showFeedback(text: String, colorRes: Int) {
        tvFeedback.text = text
        tvFeedback.setTextColor(color(colorRes))
        tvFeedback.visibility = View.VISIBLE
    }

    private fun lockOptions() {
        optionViews.forEach {
            it.isEnabled = false
            it.setOnClickListener(null)
        }
    }

    private fun updateScoreLabels() {
        tvScore.text = getString(R.string.score_label, score)
        tvStreak.text = if (streak >= 2) getString(R.string.streak_label, streak)
        else getString(R.string.streak_empty)
    }

    private fun advance() {
        index++
        if (index >= questions.size) {
            finishRound()
        } else {
            renderQuestion()
        }
    }

    private fun finishRound() {
        timer?.cancel()
        val newBest = Scores.record(this, categoryId, score)
        if (categoryId != QuestionBank.ALL_ID) {
            Scores.record(this, QuestionBank.ALL_ID, score)
        }

        startActivity(
            Intent(this, ResultActivity::class.java).apply {
                putExtra(ResultActivity.EXTRA_SCORE, score)
                putExtra(ResultActivity.EXTRA_CORRECT, correctCount)
                putExtra(ResultActivity.EXTRA_TOTAL, questions.size)
                putExtra(ResultActivity.EXTRA_BEST, Scores.best(this@QuizActivity, categoryId))
                putExtra(ResultActivity.EXTRA_NEW_BEST, newBest)
                putExtra(ResultActivity.EXTRA_CATEGORY_ID, categoryId)
                putExtra(ResultActivity.EXTRA_CATEGORY_LABEL, categoryLabel)
            }
        )
        finish()
    }

    /** Context.getColor is API 23+, so fall back on old devices. */
    private fun color(resId: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) getColor(resId)
        else @Suppress("DEPRECATION") resources.getColor(resId)

    override fun onDestroy() {
        timer?.cancel()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
