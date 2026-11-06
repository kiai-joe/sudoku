package com.kiaijoe.sudoku.ui

import com.kiaijoe.sudoku.ui.components.SealAnim
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A new achievement's seal falls, lands, presses and settles, and its name only shows once it has landed. */
class SealAnimTest {

    @Test
    fun fallsFromAboveAndEndsStill() {
        val first = SealAnim.at(0f)
        assertTrue("starts big, as if above the card", first.scale > 2f)
        assertEquals(0f, first.alpha, 0f)
        assertEquals(0f, first.text, 0f)

        val last = SealAnim.at(SealAnim.TOTAL_MS)
        assertEquals(1f, last.scale, 0f)
        assertEquals(0f, last.rotation, 0f)
        assertEquals(1f, last.alpha, 0f)
        assertEquals(1f, last.text, 0f)
        // Ring and rays have spread and gone.
        assertEquals(1f, last.ring, 0f)
        assertEquals(1f, last.rays, 0f)
        assertEquals(last, SealAnim.at(SealAnim.TOTAL_MS * 4))
    }

    @Test
    fun shrinksAsItFallsThenPressesAndSettles() {
        val frames = (0..110).map { SealAnim.at(it * 10f) }
        val landing = frames.indexOfFirst { it.ring > 0f }
        assertTrue("the ring waits for the landing", landing > 0)
        for (i in 1 until landing) assertTrue("falls steadily", frames[i].scale <= frames[i - 1].scale)
        val after = frames.drop(landing).map { it.scale }
        assertTrue("squashes on landing", after.min() < 0.92f)
        assertTrue("springs back past its size", after.max() > 1.03f)
        assertTrue("its name waits for the landing", frames.take(landing).all { it.text == 0f })
    }
}
