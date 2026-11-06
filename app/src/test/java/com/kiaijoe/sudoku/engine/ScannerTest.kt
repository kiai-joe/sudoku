package com.kiaijoe.sudoku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.random.Random

/**
 * Scans puzzles drawn as photos would show them: tilted, under uneven
 * light, with grain, in a typeface the reader has no reference for.
 */
class ScannerTest {

    /** References drawn in the JVM's sans serif and monospaced faces, as the app draws them in its own. */
    private val reader = DigitReader(
        listOf(Font.SANS_SERIF, Font.MONOSPACED, Font.DIALOG).flatMap { family ->
            listOf(Font.PLAIN, Font.BOLD).flatMap { style ->
                (1..9).map { d -> d to Glyph.of(drawDigit(d, Font(family, style, 64)))!! }
            }
        },
    )

    private fun drawDigit(d: Int, font: Font): GrayImage {
        val img = BufferedImage(96, 96, BufferedImage.TYPE_BYTE_GRAY)
        val g = img.createGraphics()
        g.color = Color.WHITE
        g.fillRect(0, 0, 96, 96)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.color = Color.BLACK
        g.font = font
        g.drawString(d.toString(), 24, 72)
        g.dispose()
        return gray(img)
    }

    private fun gray(img: BufferedImage): GrayImage {
        val px = IntArray(img.width * img.height)
        img.raster.getSamples(0, 0, img.width, img.height, 0, px)
        return GrayImage(img.width, img.height, px)
    }

    /**
     * [puzzle] printed in [font], photographed: turned by [degrees], leaning
     * back by [tilt], darker towards one side, with grain.
     */
    private fun photo(puzzle: IntArray, font: Font, degrees: Double, tilt: Double, seed: Int): GrayImage {
        val w = 900
        val h = 1100
        val img = BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.color = Color(235, 235, 230)
        g.fillRect(0, 0, w, h)
        // Some newspaper text above the grid.
        g.color = Color(60, 60, 60)
        g.font = Font(Font.SERIF, Font.PLAIN, 22)
        g.drawString("SUDOKU No. 1204  Fill the grid so every row, column and box", 60, 110)
        val t = AffineTransform()
        t.translate(w / 2.0, h / 2.0 + 40)
        t.rotate(Math.toRadians(degrees))
        t.shear(tilt, 0.0)
        g.transform(t)
        val side = 600
        val cell = side / 9.0
        val left = -side / 2.0
        val top = -side / 2.0
        g.color = Color.WHITE
        g.fillRect(left.toInt(), top.toInt(), side, side)
        g.color = Color(25, 25, 25)
        for (i in 0..9) {
            g.stroke = BasicStroke(if (i % 3 == 0) 5f else 1.6f)
            val p = (i * cell).toInt()
            g.drawLine(left.toInt(), top.toInt() + p, left.toInt() + side, top.toInt() + p)
            g.drawLine(left.toInt() + p, top.toInt(), left.toInt() + p, top.toInt() + side)
        }
        g.font = font
        val fm = g.fontMetrics
        for (i in 0 until 81) {
            val d = puzzle[i]
            if (d == 0) continue
            val s = d.toString()
            val x = left + (i % 9) * cell + (cell - fm.stringWidth(s)) / 2
            val y = top + (i / 9) * cell + (cell + fm.ascent - fm.descent) / 2
            g.drawString(s, x.toFloat(), y.toFloat())
        }
        g.dispose()
        val raw = gray(img)
        // A shadow across the page, and grain.
        val r = Random(seed)
        val px = IntArray(w * h) { i ->
            val x = i % w
            val shade = 1.0 - 0.35 * x / w
            (raw.pixels[i] * shade + r.nextInt(-12, 13)).toInt().coerceIn(0, 255)
        }
        return GrayImage(w, h, px)
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
        assertTrue("at most one misread number goes unmarked: ${unflagged.map { "$it:${puzzle[it]}→${result.digits[it]}" }}", unflagged.size <= 1)
        assertTrue("most numbers are read right (${clues - wrong.size} of $clues)", wrong.size <= max(1, clues / 10))
    }

    @Test
    fun readsAStraightPhoto() {
        val p = puzzle(1)
        check(p, photo(p, Font(Font.SANS_SERIF, Font.PLAIN, 44), degrees = 0.0, tilt = 0.0, seed = 1))
    }

    @Test
    fun readsATurnedAndTiltedPhoto() {
        val p = puzzle(2)
        check(p, photo(p, Font(Font.SANS_SERIF, Font.BOLD, 42), degrees = 7.0, tilt = 0.08, seed = 2))
    }

    @Test
    fun readsASerifTypefaceItHasNoReferenceFor() {
        val p = puzzle(3)
        check(p, photo(p, Font(Font.SERIF, Font.PLAIN, 46), degrees = -4.0, tilt = -0.05, seed = 3))
    }

    @Test
    fun findsNoGridInAPhotoWithoutOne() {
        val r = Random(4)
        val blank = GrayImage(400, 500, IntArray(400 * 500) { 220 + r.nextInt(-10, 11) })
        assertNull(Scanner.scan(blank, reader))
    }

    @Test
    fun anEmptyGridReadsEmpty() {
        val empty = IntArray(81)
        val result = Scanner.scan(photo(empty, Font(Font.SANS_SERIF, Font.PLAIN, 44), 3.0, 0.0, 5), reader)!!
        assertEquals(0, result.digits.count { it != 0 })
    }

    @Test
    fun theStraightenedGridIsSquare() {
        val p = puzzle(6)
        val result = Scanner.scan(photo(p, Font(Font.SANS_SERIF, Font.PLAIN, 44), 5.0, 0.0, 6), reader)!!
        assertEquals(Scanner.SIZE, result.grid.width)
        assertEquals(Scanner.SIZE, result.grid.height)
        // Its edges are the grid's border: dark all round.
        val border = (0 until Scanner.SIZE).map { result.grid[it, 1] } + (0 until Scanner.SIZE).map { result.grid[1, it] }
        assertTrue(border.average() < 140)
    }
}
