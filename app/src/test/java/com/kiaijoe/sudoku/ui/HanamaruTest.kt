package com.kiaijoe.sudoku.ui

import com.kiaijoe.sudoku.ui.components.Hanamaru
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The hanamaru's timeline starts empty, draws in order and ends on the finished, still mark. */
class HanamaruTest {

    @Test
    fun startsBlankAndEndsFinished() {
        val first = Hanamaru.at(0f)
        assertEquals(0f, first.spiral, 0f)
        assertEquals(0f, first.petals, 0f)
        assertEquals(0f, first.burst, 0f)

        val last = Hanamaru.at(Hanamaru.TOTAL_MS)
        assertEquals(1f, last.spiral, 0f)
        assertEquals(1f, last.petals, 0f)
        assertEquals(1f, last.scale, 0f)
        // The scattered petals have gone, and a faint stain of ink stays.
        assertEquals(1f, last.burst, 0f)
        assertTrue(last.bleed in 0.4f..0.7f)
        // Past the end nothing moves.
        assertEquals(last, Hanamaru.at(Hanamaru.TOTAL_MS * 3))
    }

    @Test
    fun drawsTheSpiralBeforeThePetals() {
        var spiral = 0f
        var petals = 0f
        var ms = 0f
        while (ms <= Hanamaru.TOTAL_MS) {
            val f = Hanamaru.at(ms)
            assertTrue("strokes only grow", f.spiral >= spiral && f.petals >= petals)
            if (f.petals > 0f) assertEquals("petals wait for the spiral at $ms", 1f, f.spiral, 0f)
            spiral = f.spiral
            petals = f.petals
            ms += 10f
        }
    }

    @Test
    fun pressesAndSettlesAfterDrawing() {
        val scales = (0..200).map { Hanamaru.at(it * 10f).scale }
        assertTrue("presses down", scales.min() < 0.95f)
        assertTrue("springs back past its size", scales.max() > 1.04f)
        val firstPress = scales.indexOfFirst { it != 1f }
        assertEquals("only once the mark is drawn", 1f, Hanamaru.at(firstPress * 10f - 10f).petals, 0f)
    }
}
