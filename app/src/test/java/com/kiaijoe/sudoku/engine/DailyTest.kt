package com.kiaijoe.sudoku.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DailyTest {

    private fun day(iso: String) = LocalDate.parse(iso).toEpochDay()

    @Test
    fun levelRisesThroughTheWeek() {
        // 2026-11-02 is a Monday.
        val expected = listOf(
            Difficulty.EASY, Difficulty.EASY, Difficulty.MEDIUM, Difficulty.MEDIUM,
            Difficulty.HARD, Difficulty.HARD, Difficulty.EXPERT,
        )
        for ((i, level) in expected.withIndex()) {
            assertEquals(level, Daily.difficultyFor(day("2026-11-02") + i))
        }
    }

    @Test
    fun sameDayGivesSamePuzzleAndOtherDaysDiffer() {
        val today = day("2026-11-05")
        val a = Daily.puzzleFor(today)
        val b = Daily.puzzleFor(today)
        assertArrayEquals(a.givens, b.givens)
        assertFalse(a.givens.contentEquals(Daily.puzzleFor(today + 7).givens))
    }

    @Test
    fun dailyPuzzlesAreGradedAtTheirLevel() {
        val monday = day("2026-11-02")
        for (offset in 0L until 7L) {
            val d = monday + offset
            val grade = LogicSolver.grade(Daily.puzzleFor(d).givens)
            assertEquals("day $offset", Daily.difficultyFor(d), grade.level)
        }
    }

    @Test
    fun gameRemembersItsDay() {
        val d = day("2026-11-05")
        val game = Daily.gameFor(d)
        assertEquals(d, game.daily)
        assertEquals(d, GameCodec.decode(GameCodec.encode(game))?.daily)
        assertNull(GameCodec.decode(GameCodec.encode(game.copy(daily = null)))?.daily)
    }

    @Test
    fun streakCountsBackFromTodayOrYesterday() {
        val today = 100L
        assertEquals(0, Daily.streak(emptySet(), today))
        assertEquals(3, Daily.streak(setOf(98L, 99L, 100L), today))
        // Today not solved yet: yesterday's run still counts.
        assertEquals(2, Daily.streak(setOf(98L, 99L), today))
        // A gap before yesterday breaks it.
        assertEquals(0, Daily.streak(setOf(97L, 98L), today))
        assertEquals(1, Daily.streak(setOf(95L, 96L, 100L), today))
    }

    @Test
    fun longestStreakFindsTheLongestRun() {
        assertEquals(0, Daily.longestStreak(emptySet()))
        assertEquals(4, Daily.longestStreak(setOf(1L, 2L, 5L, 6L, 7L, 8L, 20L)))
    }
}
