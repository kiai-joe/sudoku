package com.kiaijoe.sudoku.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.kiaijoe.sudoku.engine.Difficulty
import com.kiaijoe.sudoku.engine.Generator
import com.kiaijoe.sudoku.engine.GrayImage
import com.kiaijoe.sudoku.engine.Scanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.max
import kotlin.random.Random

/**
 * Scans puzzles drawn the way photos show them: turned, tilted, under
 * uneven light and with grain, read with the same reference digits the
 * app draws.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ScannerTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val reader by lazy { PhotoScan.reader(context) }

    /**
     * [puzzle] printed in [face], photographed: turned by [degrees], leaning
     * by [skew], darker towards one side, with grain.
     */
    private fun photo(puzzle: IntArray, face: Typeface, degrees: Float, skew: Float, seed: Int): GrayImage {
        val w = 900
        val h = 1100
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(235, 235, 230))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // Some newspaper text above the grid.
        paint.color = Color.rgb(60, 60, 60)
        paint.textSize = 22f
        paint.typeface = Typeface.SERIF
        canvas.drawText("SUDOKU No. 1204  Fill the grid so every row, column and box", 60f, 110f, paint)
        canvas.translate(w / 2f, h / 2f + 40f)
        canvas.rotate(degrees)
        canvas.skew(skew, 0f)
        val side = 600f
        val cell = side / 9
        val left = -side / 2
        val top = -side / 2
        paint.color = Color.WHITE
        canvas.drawRect(left, top, left + side, top + side, paint)
        paint.color = Color.rgb(25, 25, 25)
        paint.style = Paint.Style.STROKE
        for (i in 0..9) {
            paint.strokeWidth = if (i % 3 == 0) 5f else 1.6f
            val p = i * cell
            canvas.drawLine(left, top + p, left + side, top + p, paint)
            canvas.drawLine(left + p, top, left + p, top + side, paint)
        }
        paint.style = Paint.Style.FILL
        paint.typeface = face
        paint.textSize = cell * 0.62f
        paint.textAlign = Paint.Align.CENTER
        val mid = -(paint.ascent() + paint.descent()) / 2
        for (i in 0 until 81) {
            val d = puzzle[i]
            if (d == 0) continue
            canvas.drawText(d.toString(), left + (i % 9 + 0.5f) * cell, top + (i / 9 + 0.5f) * cell + mid, paint)
        }
        val raw = PhotoScan.gray(bitmap)
        // A shadow across the page, and grain.
        val r = Random(seed)
        return GrayImage(w, h, IntArray(w * h) { i ->
            val shade = 1.0 - 0.35 * (i % w) / w
            (raw.pixels[i] * shade + r.nextInt(-12, 13)).toInt().coerceIn(0, 255)
        })
    }

    private fun puzzle(seed: Int) = Generator.generate(Difficulty.MEDIUM, Random(seed)).givens

    private fun check(puzzle: IntArray, image: GrayImage) {
        val result = Scanner.scan(image, reader)
        assertNotNull("the grid is found", result)
        result!!
        val wrong = (0 until 81).filter { result.digits[it] != puzzle[it] }
        val missedEmpty = wrong.filter { puzzle[it] == 0 || result.digits[it] == 0 }
        assertTrue("empty and filled cells are told apart: $missedEmpty", missedEmpty.isEmpty())
        val unflagged = wrong.filter { it !in result.unsure }
        val clues = puzzle.count { it != 0 }
        assertTrue(
            "at most one misread number goes unmarked: ${unflagged.map { "$it:${puzzle[it]}->${result.digits[it]}" }}",
            unflagged.size <= 1,
        )
        assertTrue("most numbers are read right (${clues - wrong.size} of $clues)", wrong.size <= max(1, clues / 10))
    }

    @Test
    fun readsAStraightPhoto() {
        val p = puzzle(1)
        check(p, photo(p, Typeface.SANS_SERIF, degrees = 0f, skew = 0f, seed = 1))
    }

    @Test
    fun readsATurnedAndTiltedPhoto() {
        val p = puzzle(2)
        check(p, photo(p, Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD), degrees = 7f, skew = 0.08f, seed = 2))
    }

    @Test
    fun readsASerifPuzzle() {
        val p = puzzle(3)
        check(p, photo(p, Typeface.SERIF, degrees = -4f, skew = -0.05f, seed = 3))
    }

    @Test
    fun findsNoGridInAPhotoWithoutOne() {
        val r = Random(4)
        val blank = GrayImage(400, 500, IntArray(400 * 500) { 220 + r.nextInt(-10, 11) })
        assertNull(Scanner.scan(blank, reader))
    }

    @Test
    fun anEmptyGridReadsEmpty() {
        val result = Scanner.scan(photo(IntArray(81), Typeface.SANS_SERIF, 3f, 0f, 5), reader)!!
        assertEquals(0, result.digits.count { it != 0 })
    }

    @Test
    fun theStraightenedGridIsSquare() {
        val result = Scanner.scan(photo(puzzle(6), Typeface.SANS_SERIF, 5f, 0f, 6), reader)!!
        assertEquals(Scanner.SIZE, result.grid.width)
        assertEquals(Scanner.SIZE, result.grid.height)
        // Its edges are the grid's border: dark all round.
        val border = (0 until Scanner.SIZE).map { result.grid[it, 1] } + (0 until Scanner.SIZE).map { result.grid[1, it] }
        assertTrue(border.average() < 140)
    }
}
