package com.kiaijoe.sudoku.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DifficultyStatsTest {

    @Test
    fun averageAndNoHintShareComeFromTimedWinsOnly() {
        // 10 wins in all, but only the last 4 were recorded with their time and hints.
        val s = DifficultyStats(played = 12, won = 10, timedWins = 4, totalSeconds = 4 * 300 + 60, noHintWins = 3)
        assertEquals(315L, s.averageSeconds)
        assertEquals(75, s.noHintRate)
        assertEquals(83, s.winRate)
    }

    @Test
    fun nothingToAverageBeforeTheFirstTimedWin() {
        val s = DifficultyStats(played = 5, won = 3)
        assertNull(s.averageSeconds)
        assertNull(s.noHintRate)
    }
}
