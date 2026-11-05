package com.kiaijoe.sudoku.ui.screens

import android.view.HapticFeedbackConstants
import com.kiaijoe.sudoku.ui.FeedbackKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticsTest {

    @Test
    fun everyKindVibratesAndPatternsStayShort() {
        for (sdk in listOf(26, 30, 34)) for (kind in FeedbackKind.entries) {
            val steps = hapticPattern(kind, sdk)
            assertTrue("$kind on $sdk", steps.isNotEmpty())
            assertEquals("$kind starts at once", 0L, steps.first().delayMillis)
            assertTrue("$kind on $sdk is under half a second", steps.sumOf { it.delayMillis } < 500)
        }
    }

    @Test
    fun winAndLossAreStrongerThanATap() {
        assertTrue(hapticPattern(FeedbackKind.WIN, 34).size > hapticPattern(FeedbackKind.COMPLETE, 34).size)
        assertTrue(hapticPattern(FeedbackKind.LOSE, 34).size > hapticPattern(FeedbackKind.WRONG, 34).size)
        assertEquals(HapticFeedbackConstants.REJECT, hapticPattern(FeedbackKind.LOSE, 34).first().constant)
        // Before Android 11 there is no reject effect.
        assertEquals(HapticFeedbackConstants.LONG_PRESS, hapticPattern(FeedbackKind.LOSE, 26).first().constant)
    }

    @Test
    fun hintStepsAreASoftTick() {
        assertEquals(listOf(HapticFeedbackConstants.CLOCK_TICK), hapticPattern(FeedbackKind.HINT_STEP, 34).map { it.constant })
    }
}
