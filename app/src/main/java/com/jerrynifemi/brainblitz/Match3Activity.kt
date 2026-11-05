package com.jerrynifemi.brainblitz

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.random.Random

/**
 * Sugar Rush: an 8x8 swap-to-match puzzle.
 * Tap a candy, tap a neighbour, line up three or more to clear it.
 */
class Match3Activity : Activity() {

    companion object {
        const val SCORE_KEY = "match3"
        private const val N = 8
        private const val TYPE_COUNT = 6
        private const val START_MOVES = 25
        private const val POINTS_PER_TILE = 60

        private val SYMBOLS = arrayOf("🍬", "🍭", "🍫", "🍩", "🍪", "🧁")
        private val COLORS = arrayOf("#FF6B9A", "#7C4DFF", "#8B5A2B", "#FFB84D", "#00E5C0", "#4DA3FF")
    }

    private val board = IntArray(N * N)
    private val tiles = ArrayList<TextView>(N * N)
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var tvScore: TextView
    private lateinit var tvMoves: TextView
    private lateinit var overlay: View
    private lateinit var tvOverlayTitle: TextView
    private lateinit var tvOverlayDetail: TextView

    private var score = 0
    private var moves = START_MOVES
    private var selected = -1
    private var busy = false
    private var gameOver = false
    private var cell = 40

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match3)

        tvScore = findViewById(R.id.tvM3Score)
        tvMoves = findViewById(R.id.tvM3Moves)
        overlay = findViewById(R.id.m3Overlay)
        tvOverlayTitle = findViewById(R.id.tvM3OverlayTitle)
        tvOverlayDetail = findViewById(R.id.tvM3OverlayDetail)

        findViewById<View>(R.id.btnM3Back).setOnClickListener { finish() }
        findViewById<View>(R.id.btnM3Again).setOnClickListener {
            overlay.visibility = View.GONE
            newGame()
        }
        findViewById<View>(R.id.btnM3Home).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }

        computeCellSize()
        buildGrid()
        newGame()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun computeCellSize() {
        val width = resources.displayMetrics.widthPixels - dp(24) * 2
        cell = width / N
    }

    private fun buildGrid() {
        val container = findViewById<LinearLayout>(R.id.boardContainer)
        container.removeAllViews()
        tiles.clear()

        for (r in 0 until N) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            for (c in 0 until N) {
                val tile = TextView(this)
                val params = LinearLayout.LayoutParams(cell, cell)
                val gap = dp(2)
                params.setMargins(gap, gap, gap, gap)
                tile.layoutParams = params
                tile.gravity = Gravity.CENTER
                tile.textSize = (cell / resources.displayMetrics.scaledDensity) * 0.42f
                tile.isClickable = true
                tile.setOnClickListener { onTileTapped(tiles.indexOf(tile)) }
                tiles.add(tile)
                row.addView(tile)
            }
            container.addView(row)
        }
    }

    private fun rnd(): Int = Random.nextInt(TYPE_COUNT)

    private fun index(r: Int, c: Int) = r * N + c

    private fun newGame() {
        handler.removeCallbacksAndMessages(null)
        score = 0
        moves = START_MOVES
        selected = -1
        busy = false
        gameOver = false

        var attempt = 0
        do {
            for (r in 0 until N) for (c in 0 until N) {
                var value = rnd()
                var guard = 0
                while (guard < 30 && createsMatchAt(r, c, value)) {
                    value = rnd()
                    guard++
                }
                board[index(r, c)] = value
            }
            attempt++
        } while (!hasPossibleMove() && attempt < 40)

        renderAll()
        updateHeader()
    }

    /** Would putting [value] at (r,c) already make a run of three? */
    private fun createsMatchAt(r: Int, c: Int, value: Int): Boolean {
        if (c >= 2 && board[index(r, c - 1)] == value && board[index(r, c - 2)] == value) return true
        if (r >= 2 && board[index(r - 1, c)] == value && board[index(r - 2, c)] == value) return true
        return false
    }

    private fun bindTile(i: Int) {
        val tile = tiles[i]
        val value = board[i]
        if (value < 0) {
            tile.text = ""
            tile.background = null
            return
        }
        tile.text = SYMBOLS[value]
        val bg = GradientDrawable()
        bg.shape = GradientDrawable.RECTANGLE
        bg.cornerRadius = cell * 0.28f
        bg.setColor(Color.parseColor(COLORS[value] + "E6"))
        bg.setStroke(dp(1), Color.parseColor("#33FFFFFF"))
        tile.background = bg
        tile.alpha = 1f
        tile.scaleX = 1f
        tile.scaleY = 1f
        tile.translationY = 0f
    }

    private fun renderAll() {
        for (r in 0 until N) {
            for (c in 0 until N) {
                val i = index(r, c)
                bindTile(i)
                val tile = tiles[i]
                tile.translationY = -cell * 1.4f
                tile.animate().translationY(0f).setDuration(170).setStartDelay((r * 14).toLong()).start()
            }
        }
    }

    private fun updateHeader() {
        tvScore.text = getString(R.string.score_label, score)
        tvMoves.text = getString(R.string.moves_label, moves)
    }

    private fun isAdjacent(a: Int, b: Int): Boolean {
        val ar = a / N; val ac = a % N
        val br = b / N; val bc = b % N
        return Math.abs(ar - br) + Math.abs(ac - bc) == 1
    }

    private fun onTileTapped(i: Int) {
        if (busy || gameOver || i < 0) return
        tiles[i].performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

        if (selected == -1) {
            selected = i
            highlight()
            return
        }
        if (selected == i) {
            selected = -1
            highlight()
            return
        }
        if (!isAdjacent(selected, i)) {
            selected = i
            highlight()
            return
        }

        val a = selected
        val b = i
        selected = -1
        highlight()

        swap(a, b)
        bindTile(a)
        bindTile(b)

        if (findMatches().isEmpty()) {
            handler.postDelayed({
                swap(a, b)
                bindTile(a)
                bindTile(b)
                shake(a)
                shake(b)
            }, 130)
            return
        }

        moves--
        updateHeader()
        busy = true
        handler.postDelayed({ resolve(1) }, 120)
    }

    private fun highlight() {
        for (i in tiles.indices) {
            val tile = tiles[i]
            val target = if (i == selected) 1.14f else 1f
            tile.animate().scaleX(target).scaleY(target).setDuration(120).start()
            tile.alpha = if (i == selected) 1f else if (selected == -1) 1f else 0.75f
        }
        if (selected >= 0) tiles[selected].alpha = 1f
    }

    private fun shake(i: Int) {
        val tile = tiles[i]
        tile.animate().translationX(dp(6).toFloat()).setDuration(60).withEndAction {
            tile.animate().translationX(-dp(6).toFloat()).setDuration(60).withEndAction {
                tile.animate().translationX(0f).setDuration(60).start()
            }
        }.start()
    }

    private fun swap(a: Int, b: Int) {
        val tmp = board[a]
        board[a] = board[b]
        board[b] = tmp
    }

    private fun findMatches(): Set<Int> {
        val found = HashSet<Int>()

        for (r in 0 until N) {
            var runStart = 0
            for (c in 1..N) {
                val same = c < N && board[index(r, c)] >= 0 && board[index(r, c)] == board[index(r, runStart)]
                if (!same) {
                    if (c - runStart >= 3) for (k in runStart until c) found.add(index(r, k))
                    runStart = c
                }
            }
        }
        for (c in 0 until N) {
            var runStart = 0
            for (r in 1..N) {
                val same = r < N && board[index(r, c)] >= 0 && board[index(r, c)] == board[index(runStart, c)]
                if (!same) {
                    if (r - runStart >= 3) for (k in runStart until r) found.add(index(k, c))
                    runStart = r
                }
            }
        }
        return found
    }

    private fun hasPossibleMove(): Boolean {
        for (r in 0 until N) {
            for (c in 0 until N) {
                val i = index(r, c)
                if (c + 1 < N) {
                    swap(i, index(r, c + 1))
                    val has = findMatches().isNotEmpty()
                    swap(i, index(r, c + 1))
                    if (has) return true
                }
                if (r + 1 < N) {
                    swap(i, index(r + 1, c))
                    val has = findMatches().isNotEmpty()
                    swap(i, index(r + 1, c))
                    if (has) return true
                }
            }
        }
        return false
    }

    private fun resolve(combo: Int) {
        val matches = findMatches()
        if (matches.isEmpty()) {
            busy = false
            if (!hasPossibleMove()) {
                reshuffle()
            } else if (moves <= 0) {
                endGame()
            }
            return
        }

        score += matches.size * POINTS_PER_TILE * combo
        updateHeader()

        for (i in matches) {
            val tile = tiles[i]
            tile.animate().scaleX(1.25f).scaleY(1.25f).alpha(0f).setDuration(140).start()
        }

        handler.postDelayed({
            collapse(matches)
            renderAll()
            handler.postDelayed({ resolve(combo + 1) }, 190)
        }, 150)
    }

    private fun collapse(matches: Set<Int>) {
        for (c in 0 until N) {
            val kept = ArrayList<Int>()
            for (r in N - 1 downTo 0) {
                val i = index(r, c)
                if (!matches.contains(i)) kept.add(board[i])
            }
            var r = N - 1
            for (value in kept) {
                board[index(r, c)] = value
                r--
            }
            while (r >= 0) {
                board[index(r, c)] = rnd()
                r--
            }
        }
    }

    private fun reshuffle() {
        for (r in 0 until N) for (c in 0 until N) {
            var value = rnd()
            var guard = 0
            while (guard < 30 && createsMatchAt(r, c, value)) {
                value = rnd()
                guard++
            }
            board[index(r, c)] = value
        }
        var attempt = 0
        while (!hasPossibleMove() && attempt < 40) {
            for (r in 0 until N) for (c in 0 until N) {
                var value = rnd()
                var guard = 0
                while (guard < 30 && createsMatchAt(r, c, value)) {
                    value = rnd()
                    guard++
                }
                board[index(r, c)] = value
            }
            attempt++
        }
        renderAll()
    }

    private fun endGame() {
        gameOver = true
        val newBest = Scores.record(this, SCORE_KEY, score)
        tvOverlayTitle.text = getString(R.string.m3_over)
        tvOverlayDetail.text = getString(
            R.string.m3_overlay_detail,
            score,
            Scores.best(this, SCORE_KEY),
            if (newBest) getString(R.string.new_best_suffix) else ""
        )
        overlay.alpha = 0f
        overlay.visibility = View.VISIBLE
        overlay.animate().alpha(1f).setDuration(260).start()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
