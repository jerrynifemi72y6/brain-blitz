package com.jerrynifemi.brainblitz

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import kotlin.math.abs

class SnakeActivity : Activity() {

    companion object {
        const val SCORE_KEY = "snake"
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var snakeView: SnakeView
    private lateinit var tvScore: TextView
    private lateinit var tvBest: TextView
    private lateinit var overlay: View
    private lateinit var tvOverlayTitle: TextView
    private lateinit var tvOverlayDetail: TextView

    private var running = false
    private var gameOver = false
    private var score = 0

    private val ticker = object : Runnable {
        override fun run() {
            if (!running) return
            val alive = snakeView.step()
            if (alive) {
                handler.postDelayed(this, snakeView.speedMs)
            } else {
                endGame()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_snake)

        snakeView = findViewById(R.id.snakeView)
        tvScore = findViewById(R.id.tvSnakeScore)
        tvBest = findViewById(R.id.tvSnakeBest)
        overlay = findViewById(R.id.snakeOverlay)
        tvOverlayTitle = findViewById(R.id.tvSnakeOverlayTitle)
        tvOverlayDetail = findViewById(R.id.tvSnakeOverlayDetail)

        findViewById<View>(R.id.btnSnakeBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnSnakeAgain).setOnClickListener {
            overlay.visibility = View.GONE
            startGame()
        }
        findViewById<View>(R.id.btnSnakeHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        findViewById<View>(R.id.btnUp).setOnClickListener { snakeView.turn(SnakeView.Dir.UP) }
        findViewById<View>(R.id.btnDown).setOnClickListener { snakeView.turn(SnakeView.Dir.DOWN) }
        findViewById<View>(R.id.btnLeft).setOnClickListener { snakeView.turn(SnakeView.Dir.LEFT) }
        findViewById<View>(R.id.btnRight).setOnClickListener { snakeView.turn(SnakeView.Dir.RIGHT) }

        snakeView.onScoreChanged = { value ->
            score = value
            tvScore.text = getString(R.string.score_label, value)
        }
        snakeView.onEat = { Audio.play(this, R.raw.sfx_snake_eat) }
        snakeView.onDie = { Audio.play(this, R.raw.sfx_snake_die) }

        enableSwipes()
        tvBest.text = getString(R.string.best_chip, Scores.best(this, SCORE_KEY))
    }

    private fun enableSwipes() {
        var downX = 0f
        var downY = 0f
        snakeView.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val dx = event.x - downX
                    val dy = event.y - downY
                    if (maxOf(abs(dx), abs(dy)) > view.width * 0.05f) {
                        if (abs(dx) > abs(dy)) {
                            snakeView.turn(if (dx > 0) SnakeView.Dir.RIGHT else SnakeView.Dir.LEFT)
                        } else {
                            snakeView.turn(if (dy > 0) SnakeView.Dir.DOWN else SnakeView.Dir.UP)
                        }
                    }
                    true
                }

                else -> true
            }
        }
    }

    private fun startGame() {
        handler.removeCallbacks(ticker)
        gameOver = false
        snakeView.reset()
        tvBest.text = getString(R.string.best_chip, Scores.best(this, SCORE_KEY))
        running = true
        handler.postDelayed(ticker, 350)
    }

    private fun endGame() {
        running = false
        gameOver = true
        handler.removeCallbacks(ticker)
        val newBest = Scores.record(this, SCORE_KEY, score)
        tvBest.text = getString(R.string.best_chip, Scores.best(this, SCORE_KEY))
        tvOverlayTitle.text = getString(R.string.snake_over)
        tvOverlayDetail.text = getString(
            R.string.m3_overlay_detail,
            score,
            Scores.best(this, SCORE_KEY),
            if (newBest) getString(R.string.new_best_suffix) else ""
        )
        overlay.alpha = 0f
        overlay.visibility = View.VISIBLE
        overlay.animate().alpha(1f).setDuration(240).start()
    }

    override fun onResume() {
        super.onResume()
        Audio.playMusic(this, R.raw.music_arcade)
        if (!gameOver && !running) {
            running = true
            handler.postDelayed(ticker, 250)
        }
    }

    override fun onPause() {
        super.onPause()
        running = false
        handler.removeCallbacks(ticker)
        Audio.pauseMusic()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
