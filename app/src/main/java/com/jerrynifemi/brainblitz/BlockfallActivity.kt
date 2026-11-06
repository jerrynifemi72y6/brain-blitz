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

class BlockfallActivity : Activity() {

    companion object {
        const val SCORE_KEY = "blockfall"
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var blockView: BlockfallView
    private lateinit var tvScore: TextView
    private lateinit var tvLines: TextView
    private lateinit var tvLevel: TextView
    private lateinit var overlay: View
    private lateinit var tvOverlayTitle: TextView
    private lateinit var tvOverlayDetail: TextView

    private var running = false
    private var gameOver = false

    private val ticker = object : Runnable {
        override fun run() {
            if (!running) return
            blockView.tick()
            if (running) handler.postDelayed(this, blockView.gravityMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blockfall)

        blockView = findViewById(R.id.blockView)
        tvScore = findViewById(R.id.tvBfScore)
        tvLines = findViewById(R.id.tvBfLines)
        tvLevel = findViewById(R.id.tvBfLevel)
        overlay = findViewById(R.id.bfOverlay)
        tvOverlayTitle = findViewById(R.id.tvBfOverlayTitle)
        tvOverlayDetail = findViewById(R.id.tvBfOverlayDetail)

        findViewById<View>(R.id.btnBfBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnBfAgain).setOnClickListener {
            overlay.visibility = View.GONE
            startGame()
        }
        findViewById<View>(R.id.btnBfHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        findViewById<View>(R.id.btnBfLeft).setOnClickListener {
            if (blockView.moveLeft()) Audio.play(this, R.raw.sfx_rotate, 0.4f)
        }
        findViewById<View>(R.id.btnBfRight).setOnClickListener {
            if (blockView.moveRight()) Audio.play(this, R.raw.sfx_rotate, 0.4f)
        }
        findViewById<View>(R.id.btnBfRotate).setOnClickListener {
            if (blockView.rotatePiece()) Audio.play(this, R.raw.sfx_rotate)
        }
        findViewById<View>(R.id.btnBfDrop).setOnClickListener {
            blockView.hardDrop()
            handler.removeCallbacks(ticker)
            if (running) handler.postDelayed(ticker, blockView.gravityMs)
        }

        blockView.onStatsChanged = { score, lines, level ->
            tvScore.text = getString(R.string.score_label, score)
            tvLines.text = getString(R.string.lines_label, lines)
            tvLevel.text = getString(R.string.level_label, level)
        }
        blockView.onPlace = { Audio.play(this, R.raw.sfx_swap, 0.7f) }
        blockView.onLineClear = { count ->
            Audio.play(this, R.raw.sfx_line_clear)
            if (count >= 4) Audio.play(this, R.raw.sfx_levelup, 0.8f)
        }
        blockView.onGameOver = { endGame() }

        enableSwipes()
    }

    private fun enableSwipes() {
        var downX = 0f
        var downY = 0f
        blockView.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val dx = event.x - downX
                    val dy = event.y - downY
                    if (dy > view.height * 0.18f && abs(dy) > abs(dx)) {
                        blockView.hardDrop()
                        handler.removeCallbacks(ticker)
                        if (running) handler.postDelayed(ticker, blockView.gravityMs)
                    } else if (abs(dx) > view.width * 0.08f) {
                        if (dx > 0) blockView.moveRight() else blockView.moveLeft()
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
        blockView.reset()
        running = true
        handler.postDelayed(ticker, blockView.gravityMs)
    }

    private fun endGame() {
        if (gameOver) return
        running = false
        gameOver = true
        handler.removeCallbacks(ticker)
        Audio.play(this, R.raw.sfx_gameover)
        val newBest = Scores.record(this, SCORE_KEY, blockView.score)
        tvOverlayTitle.text = getString(R.string.block_over)
        tvOverlayDetail.text = getString(
            R.string.m3_overlay_detail,
            blockView.score,
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
        if (blockView.running && !gameOver) {
            running = true
            handler.removeCallbacks(ticker)
            handler.postDelayed(ticker, blockView.gravityMs)
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
