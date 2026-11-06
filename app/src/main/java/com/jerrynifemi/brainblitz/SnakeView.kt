package com.jerrynifemi.brainblitz

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

/** Grid snake, drawn by hand so it stays crisp at any size. */
class SnakeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class Dir { UP, DOWN, LEFT, RIGHT }

    companion object {
        const val COLS = 18
        const val ROWS = 18
        private const val START_LENGTH = 3
    }

    private val body = ArrayDeque<Int>()
    private var direction = Dir.RIGHT
    private var queued = Dir.RIGHT

    private var food = -1
    private var score = 0
    private val random = Random.Default

    var alive = true
        private set

    var speedMs = 150L
        private set

    var onEat: (() -> Unit)? = null
    var onDie: (() -> Unit)? = null
    var onScoreChanged: ((Int) -> Unit)? = null

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val headPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val foodPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val boardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private var cell = 0f
    private var originX = 0f
    private var originY = 0f

    init {
        boardPaint.color = Color.parseColor("#14FFFFFF")
        gridPaint.color = Color.parseColor("#0DFFFFFF")
        bodyPaint.color = Color.parseColor("#3FD9C4")
        headPaint.color = Color.parseColor("#00E5C0")
        foodPaint.color = Color.parseColor("#FF6B9A")
        glowPaint.color = Color.parseColor("#33FF6B9A")
        reset()
    }

    fun reset() {
        body.clear()
        val startRow = ROWS / 2
        val startCol = COLS / 3
        for (i in 0 until START_LENGTH) body.addLast(startRow * COLS + (startCol - i))
        direction = Dir.RIGHT
        queued = Dir.RIGHT
        score = 0
        speedMs = 150L
        alive = true
        spawnFood()
        onScoreChanged?.invoke(score)
        invalidate()
    }

    fun turn(dir: Dir) {
        if (!alive) return
        val opposite = when (dir) {
            Dir.UP -> Dir.DOWN
            Dir.DOWN -> Dir.UP
            Dir.LEFT -> Dir.RIGHT
            Dir.RIGHT -> Dir.LEFT
        }
        if (opposite == direction) return
        queued = dir
    }

    /** One game tick. Returns true while the game is running. */
    fun step(): Boolean {
        if (!alive) return false
        direction = queued

        val head = body.first()
        val headRow = head / COLS
        val headCol = head % COLS
        val nextRow = when (direction) {
            Dir.UP -> headRow - 1
            Dir.DOWN -> headRow + 1
            else -> headRow
        }
        val nextCol = when (direction) {
            Dir.LEFT -> headCol - 1
            Dir.RIGHT -> headCol + 1
            else -> headCol
        }

        if (nextRow < 0 || nextRow >= ROWS || nextCol < 0 || nextCol >= COLS) {
            die()
            return false
        }

        val next = nextRow * COLS + nextCol
        // The tail tip moves away this tick, so following it is legal.
        val tail = body.last()
        if (body.contains(next) && next != tail) {
            die()
            return false
        }

        body.addFirst(next)
        if (next == food) {
            score++
            speedMs = maxOf(70L, speedMs - 5L)
            onScoreChanged?.invoke(score)
            spawnFood()
            onEat?.invoke()
        } else {
            body.removeLast()
        }

        invalidate()
        return true
    }

    private fun die() {
        alive = false
        invalidate()
        onDie?.invoke()
    }

    private fun spawnFood() {
        val free = ArrayList<Int>(COLS * ROWS)
        for (i in 0 until COLS * ROWS) if (!body.contains(i)) free.add(i)
        food = if (free.isEmpty()) -1 else free[random.nextInt(free.size)]
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, w)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pad = width * 0.02f
        cell = (width - pad * 2) / COLS
        originX = pad
        originY = pad

        val board = RectF(originX, originY, originX + cell * COLS, originY + cell * ROWS)
        canvas.drawRoundRect(board, cell * 0.6f, cell * 0.6f, boardPaint)

        // faint grid
        for (c in 1 until COLS) {
            val x = originX + c * cell
            canvas.drawLine(x, originY, x, originY + cell * ROWS, gridPaint)
        }
        for (r in 1 until ROWS) {
            val y = originY + r * cell
            canvas.drawLine(originX, y, originX + cell * COLS, y, gridPaint)
        }

        if (food >= 0) {
            val fr = food / COLS
            val fc = food % COLS
            val pulse = 1f + 0.12f * kotlin.math.sin(System.currentTimeMillis() / 220.0).toFloat()
            val cx = originX + fc * cell + cell / 2f
            val cy = originY + fr * cell + cell / 2f
            val radius = cell * 0.3f * pulse
            canvas.drawCircle(cx, cy, radius * 1.8f, glowPaint)
            canvas.drawCircle(cx, cy, radius, foodPaint)
        }

        body.forEachIndexed { i, idx ->
            val r = idx / COLS
            val c = idx % COLS
            val left = originX + c * cell
            val top = originY + r * cell
            rect.set(left + cell * 0.08f, top + cell * 0.08f, left + cell * 0.92f, top + cell * 0.92f)
            val paint = if (i == 0) headPaint else bodyPaint
            if (i > 0) {
                val fade = 1f - (i.toFloat() / (body.size + 4f)) * 0.45f
                paint.alpha = (255 * fade).toInt()
            } else {
                paint.alpha = 255
            }
            canvas.drawRoundRect(rect, cell * 0.3f, cell * 0.3f, paint)
        }
    }
}
