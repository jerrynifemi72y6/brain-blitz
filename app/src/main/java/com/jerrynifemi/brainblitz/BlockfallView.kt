package com.jerrynifemi.brainblitz

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

/**
 * Falling block puzzle: ten columns, twenty rows, seven shapes.
 * Rotations are done by turning the shape inside its 4x4 box.
 */
class BlockfallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        const val COLS = 10
        const val ROWS = 20

        // each shape is four (x, y) pairs inside a 4x4 box
        private val SHAPES = arrayOf(
            intArrayOf(0, 1, 1, 1, 2, 1, 3, 1), // I
            intArrayOf(0, 0, 0, 1, 1, 1, 2, 1), // J
            intArrayOf(2, 0, 0, 1, 1, 1, 2, 1), // L
            intArrayOf(1, 0, 2, 0, 0, 1, 1, 1), // S
            intArrayOf(0, 0, 1, 0, 1, 1, 2, 1), // Z
            intArrayOf(0, 0, 1, 0, 0, 1, 1, 1), // O
            intArrayOf(1, 0, 0, 1, 1, 1, 2, 1)  // T
        )

        private val COLORS = intArrayOf(
            0x00000000,
            Color.parseColor("#4DA3FF"),
            Color.parseColor("#7C4DFF"),
            Color.parseColor("#FFB84D"),
            Color.parseColor("#00E5C0"),
            Color.parseColor("#FF6B9A"),
            Color.parseColor("#E8E8FF"),
            Color.parseColor("#B388FF")
        )
    }

    private val board = IntArray(COLS * ROWS)
    private val random = Random.Default

    private var cells = IntArray(8)
    private var colorIndex = 1
    private var px = 3
    private var py = 0

    private var nextCells = IntArray(8)
    private var nextColor = 1

    var score = 0
        private set
    var lines = 0
        private set
    var level = 1
        private set
    var running = true
        private set

    var onStatsChanged: ((Int, Int, Int) -> Unit)? = null
    var onLineClear: ((Int) -> Unit)? = null
    var onPlace: (() -> Unit)? = null
    var onGameOver: (() -> Unit)? = null

    val gravityMs: Long
        get() = maxOf(120L, 800L - (level - 1) * 70L)

    private val blockPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val boardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private var cell = 0f
    private var boardLeft = 0f
    private var boardTop = 0f

    init {
        boardPaint.color = Color.parseColor("#14FFFFFF")
        gridPaint.color = Color.parseColor("#0DFFFFFF")
        edgePaint.color = Color.parseColor("#33FFFFFF")
        edgePaint.style = Paint.Style.STROKE
        edgePaint.strokeWidth = 1.5f
        labelPaint.color = Color.parseColor("#A79ED2")
        labelPaint.textAlign = Paint.Align.CENTER
        reset()
    }

    fun reset() {
        board.fill(0)
        score = 0
        lines = 0
        level = 1
        running = true
        nextCells = SHAPES[random.nextInt(SHAPES.size)].copyOf()
        nextColor = random.nextInt(7) + 1
        spawn()
        onStatsChanged?.invoke(score, lines, level)
        invalidate()
    }

    private fun spawn() {
        cells = nextCells
        colorIndex = nextColor
        px = 3
        py = 0
        nextCells = SHAPES[random.nextInt(SHAPES.size)].copyOf()
        nextColor = random.nextInt(7) + 1
        if (!canPlace(cells, px, py)) {
            running = false
            onGameOver?.invoke()
        }
    }

    private fun rotate(cells: IntArray): IntArray {
        val out = IntArray(8)
        for (i in 0 until 4) {
            val x = cells[i * 2]
            val y = cells[i * 2 + 1]
            out[i * 2] = 3 - y
            out[i * 2 + 1] = x
        }
        return out
    }

    private fun canPlace(shape: IntArray, atX: Int, atY: Int): Boolean {
        for (i in 0 until 4) {
            val x = atX + shape[i * 2]
            val y = atY + shape[i * 2 + 1]
            if (x < 0 || x >= COLS || y >= ROWS) return false
            if (y >= 0 && board[y * COLS + x] != 0) return false
        }
        return true
    }

    fun moveLeft(): Boolean = shift(-1, 0)
    fun moveRight(): Boolean = shift(1, 0)
    private fun shift(dx: Int, dy: Int): Boolean {
        if (!running) return false
        if (!canPlace(cells, px + dx, py + dy)) return false
        px += dx
        py += dy
        invalidate()
        return true
    }

    fun rotatePiece(): Boolean {
        if (!running) return false
        val turned = rotate(cells)
        for (kick in intArrayOf(0, -1, 1, -2, 2)) {
            if (canPlace(turned, px + kick, py)) {
                cells = turned
                px += kick
                invalidate()
                return true
            }
        }
        return false
    }

    /** One gravity step. Locks the piece when it can't fall further. */
    fun tick() {
        if (!running) return
        if (!canPlace(cells, px, py + 1)) lockPiece() else {
            py++
            invalidate()
        }
    }

    fun softDrop() {
        if (!running) return
        if (canPlace(cells, px, py + 1)) {
            py++
            invalidate()
        } else {
            lockPiece()
        }
    }

    fun hardDrop() {
        if (!running) return
        while (canPlace(cells, px, py + 1)) py++
        lockPiece()
    }

    private fun lockPiece() {
        for (i in 0 until 4) {
            val x = px + cells[i * 2]
            val y = py + cells[i * 2 + 1]
            if (y < 0) continue
            board[y * COLS + x] = colorIndex
        }
        onPlace?.invoke()

        val cleared = clearLines()
        if (cleared > 0) {
            lines += cleared
            score += when (cleared) {
                1 -> 100
                2 -> 300
                3 -> 500
                else -> 800
            } * level
            level = lines / 10 + 1
            onLineClear?.invoke(cleared)
        }
        onStatsChanged?.invoke(score, lines, level)
        spawn()
        invalidate()
    }

    private fun clearLines(): Int {
        var cleared = 0
        var row = ROWS - 1
        while (row >= 0) {
            var full = true
            for (c in 0 until COLS) {
                if (board[row * COLS + c] == 0) {
                    full = false
                    break
                }
            }
            if (full) {
                for (r in row downTo 1) {
                    System.arraycopy(board, (r - 1) * COLS, board, r * COLS, COLS)
                }
                for (c in 0 until COLS) board[c] = 0
                cleared++
            } else {
                row--
            }
        }
        return cleared
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val strip = height * 0.14f
        cell = minOf(width / COLS.toFloat(), (height - strip) / ROWS.toFloat())
        val boardWidth = cell * COLS
        val boardHeight = cell * ROWS
        boardLeft = (width - boardWidth) / 2f
        boardTop = strip + (height - strip - boardHeight) / 2f

        // next piece strip
        labelPaint.textSize = cell * 0.34f
        canvas.drawText("NEXT", width / 2f, strip * 0.42f, labelPaint)
        drawShape(canvas, nextCells, nextColor, width / 2f - cell * 0.9f, strip * 0.52f, cell * 0.42f)

        rect.set(boardLeft, boardTop, boardLeft + boardWidth, boardTop + boardHeight)
        canvas.drawRoundRect(rect, cell * 0.35f, cell * 0.35f, boardPaint)

        for (c in 1 until COLS) {
            val x = boardLeft + c * cell
            canvas.drawLine(x, boardTop, x, boardTop + boardHeight, gridPaint)
        }
        for (r in 1 until ROWS) {
            val y = boardTop + r * cell
            canvas.drawLine(boardLeft, y, boardLeft + boardWidth, y, gridPaint)
        }

        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                val value = board[r * COLS + c]
                if (value == 0) continue
                drawCell(canvas, boardLeft + c * cell, boardTop + r * cell, cell, value)
            }
        }

        for (i in 0 until 4) {
            val x = px + cells[i * 2]
            val y = py + cells[i * 2 + 1]
            if (y < 0) continue
            drawCell(canvas, boardLeft + x * cell, boardTop + y * cell, cell, colorIndex)
        }
    }

    private fun drawCell(canvas: Canvas, left: Float, top: Float, size: Float, colorIndex: Int) {
        rect.set(left + size * 0.07f, top + size * 0.07f, left + size * 0.93f, top + size * 0.93f)
        blockPaint.color = COLORS[colorIndex.coerceIn(0, COLORS.size - 1)]
        canvas.drawRoundRect(rect, size * 0.24f, size * 0.24f, blockPaint)
        canvas.drawRoundRect(rect, size * 0.24f, size * 0.24f, edgePaint)
    }

    private fun drawShape(canvas: Canvas, shape: IntArray, color: Int, originX: Float, originY: Float, size: Float) {
        for (i in 0 until 4) {
            val x = shape[i * 2]
            val y = shape[i * 2 + 1]
            rect.set(
                originX + x * size, originY + y * size,
                originX + x * size + size, originY + y * size + size
            )
            blockPaint.color = COLORS[color.coerceIn(0, COLORS.size - 1)]
            canvas.drawRoundRect(rect, size * 0.24f, size * 0.24f, blockPaint)
        }
    }
}
