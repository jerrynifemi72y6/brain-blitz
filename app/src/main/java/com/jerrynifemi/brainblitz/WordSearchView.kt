package com.jerrynifemi.brainblitz

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

/**
 * A 10x10 word search board drawn with the canvas.
 * Words hide in all eight directions and can be traced backwards.
 */
class WordSearchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val SIZE = 10
        private val DIRECTIONS = arrayOf(
            intArrayOf(0, 1), intArrayOf(0, -1),
            intArrayOf(1, 0), intArrayOf(-1, 0),
            intArrayOf(1, 1), intArrayOf(-1, -1),
            intArrayOf(1, -1), intArrayOf(-1, 1)
        )
    }

    private val letters = Array(SIZE) { CharArray(SIZE) { 'A' } }
    private val foundCells = HashSet<Int>()
    private val foundWords = HashSet<String>()

    var words: List<String> = emptyList()
        private set

    var onWordFound: ((String, Int) -> Unit)? = null
    var onAllFound: (() -> Unit)? = null

    private var startCell = -1
    private var endCell = -1
    private var dragging = false

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#14FFFFFF")
    }
    private val selectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#557C4DFF")
    }
    private val foundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4D16A34A")
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val rect = RectF()

    private var cell = 0f
    private var originX = 0f
    private var originY = 0f

    fun newPuzzle(wordList: List<String>, random: Random = Random.Default) {
        for (r in 0 until SIZE) for (c in 0 until SIZE) letters[r][c] = ' '
        val placed = ArrayList<String>()

        for (word in wordList.map { it.uppercase() }) {
            if (word.length > SIZE) continue
            var attempts = 0
            var done = false
            while (!done && attempts < 60) {
                attempts++
                done = tryPlace(word, random)
            }
            if (done) placed.add(word)
        }

        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        for (r in 0 until SIZE) for (c in 0 until SIZE) {
            if (letters[r][c] == ' ') letters[r][c] = alphabet[random.nextInt(alphabet.length)]
        }

        words = placed
        foundCells.clear()
        foundWords.clear()
        startCell = -1
        endCell = -1
        dragging = false
        invalidate()
    }

    private fun tryPlace(word: String, random: Random): Boolean {
        val dir = DIRECTIONS[random.nextInt(DIRECTIONS.size)]
        val maxR = SIZE - 1
        val maxC = SIZE - 1
        val r0 = random.nextInt(SIZE)
        val c0 = random.nextInt(SIZE)

        val endR = r0 + dir[0] * (word.length - 1)
        val endC = c0 + dir[1] * (word.length - 1)
        if (endR < 0 || endR > maxR || endC < 0 || endC > maxC) return false

        for (i in word.indices) {
            val r = r0 + dir[0] * i
            val c = c0 + dir[1] * i
            val existing = letters[r][c]
            if (existing != ' ' && existing != word[i]) return false
        }
        for (i in word.indices) {
            letters[r0 + dir[0] * i][c0 + dir[1] * i] = word[i]
        }
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, w)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pad = width * 0.015f
        cell = (width - pad * 2) / SIZE
        originX = pad
        originY = pad
        val radius = cell * 0.26f

        textPaint.textSize = cell * 0.44f

        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                val idx = r * SIZE + c
                val left = originX + c * cell
                val top = originY + r * cell
                rect.set(left + cell * 0.06f, top + cell * 0.06f, left + cell * 0.94f, top + cell * 0.94f)

                cellPaint.color = if (foundCells.contains(idx)) Color.parseColor("#2E16A34A")
                else Color.parseColor("#14FFFFFF")
                canvas.drawRoundRect(rect, radius, radius, cellPaint)

                if (inSelection(idx)) {
                    canvas.drawRoundRect(rect, radius, radius, foundPaint)
                }

                val cx = left + cell / 2f
                val cy = top + cell / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
                textPaint.alpha = if (foundCells.contains(idx)) 255 else 220
                canvas.drawText(letters[r][c].toString(), cx, cy, textPaint)
            }
        }

        if (dragging && startCell >= 0 && endCell >= 0) {
            for (idx in lineCells(startCell, endCell)) {
                val r = idx / SIZE
                val c = idx % SIZE
                val left = originX + c * cell
                val top = originY + r * cell
                rect.set(left + cell * 0.06f, top + cell * 0.06f, left + cell * 0.94f, top + cell * 0.94f)
                canvas.drawRoundRect(rect, radius, radius, selectPaint)
            }
        }
    }

    private fun inSelection(idx: Int): Boolean =
        dragging && startCell >= 0 && endCell >= 0 && lineCells(startCell, endCell).contains(idx)

    private fun lineCells(from: Int, to: Int): List<Int> {
        if (from < 0 || to < 0) return emptyList()
        val r1 = from / SIZE
        val c1 = from % SIZE
        val r2 = to / SIZE
        val c2 = to % SIZE
        val dr = Integer.signum(r2 - r1)
        val dc = Integer.signum(c2 - c1)
        if (dr != 0 && dc != 0 && abs(r2 - r1) != abs(c2 - c1)) return listOf(from)

        val steps = maxOf(abs(r2 - r1), abs(c2 - c1))
        val out = ArrayList<Int>(steps + 1)
        for (i in 0..steps) {
            val r = r1 + dr * i
            val c = c1 + dc * i
            if (r !in 0 until SIZE || c !in 0 until SIZE) break
            out.add(r * SIZE + c)
        }
        return out
    }

    private fun cellAt(x: Float, y: Float): Int {
        if (cell <= 0f) return -1
        val c = ((x - originX) / cell).toInt()
        val r = ((y - originY) / cell).toInt()
        if (r !in 0 until SIZE || c !in 0 until SIZE) return -1
        return r * SIZE + c
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val idx = cellAt(event.x, event.y)
                if (idx < 0) return false
                startCell = idx
                endCell = idx
                dragging = true
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!dragging) return false
                val idx = cellAt(event.x, event.y)
                if (idx >= 0 && idx != endCell && isStraight(startCell, idx)) {
                    endCell = idx
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!dragging) return false
                dragging = false
                checkSelection()
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun isStraight(a: Int, b: Int): Boolean {
        if (a < 0 || b < 0) return false
        val dr = abs(a / SIZE - b / SIZE)
        val dc = abs(a % SIZE - b % SIZE)
        return dr == 0 || dc == 0 || dr == dc
    }

    private fun checkSelection() {
        val cells = lineCells(startCell, endCell)
        if (cells.size < 3) return

        val forward = StringBuilder()
        val backward = StringBuilder()
        for (idx in cells) forward.append(letters[idx / SIZE][idx % SIZE])
        for (idx in cells.reversed()) backward.append(letters[idx / SIZE][idx % SIZE])

        val fwd = forward.toString()
        val bwd = backward.toString()
        val word = words.firstOrNull { (it == fwd || it == bwd) && !foundWords.contains(it) } ?: return

        foundWords.add(word)
        foundCells.addAll(cells)
        onWordFound?.invoke(word, cells.size)
        if (foundWords.size == words.size) onAllFound?.invoke()
    }
}
