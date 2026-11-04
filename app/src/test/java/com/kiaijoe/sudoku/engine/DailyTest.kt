package com.kiaijoe.sudoku.engine

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailyTest {

    private fun day(iso: String) = LocalDate.parse(iso).toEpochDay()

    @Test
    fun levelRisesThroughTheWeek() {
        // 2026-11-02 is a Monday.
        val expected = listOf(
            Difficulty.EASY, Difficulty.EASY, Difficulty.MEDIUM, Difficulty.MEDIUM,
            Difficulty.HARD, Difficulty.MEDIUM, Difficulty.EXPERT,
        )
        for ((i, level) in expected.withIndex()) {
            val d = day("2026-11-02") + i
            assertEquals(level, Daily.difficultyFor(d))
            assertEquals("only Saturday is Killer", i == 5, Daily.isKiller(d))
        }
        assertEquals("Killer · Medium", Daily.label(day("2026-11-07")))
        assertEquals("Hard", Daily.label(day("2026-11-06")))
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
            val puzzle = Daily.puzzleFor(d)
            val grade = LogicSolver.grade(puzzle.givens, puzzle.cages)
            assertTrue("day $offset solvable", grade.solved)
            val level = if (Daily.isKiller(d)) {
                assertTrue("Saturday has cages", puzzle.cages.isNotEmpty())
                Killer.levelOf(grade.hardest, puzzle.givens.count { it != 0 })
            } else {
                grade.level
            }
            assertEquals("day $offset", Daily.difficultyFor(d), level)
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

    @Test
    fun weekdayStatsAverageEachWeekday() {
        val monday = day("2026-11-02")
        val results = mapOf(
            monday to 200L,
            monday + 7 to 400L,
            monday + 6 to 900L, // Sunday
        )
        val stats = Daily.weekdayStats(results)
        assertEquals(7, stats.size)
        assertEquals(java.time.DayOfWeek.MONDAY, stats[0].day)
        assertEquals(Difficulty.EASY, stats[0].difficulty)
        assertEquals(2, stats[0].solved)
        assertEquals(300L, stats[0].averageSeconds)
        assertEquals(200L, stats[0].bestSeconds)
        assertEquals(Difficulty.EXPERT, stats[6].difficulty)
        assertTrue("Saturday is Killer", stats[5].killer)
        assertEquals(900L, stats[6].averageSeconds)
        assertEquals(0, stats[2].solved)
        assertNull(stats[2].averageSeconds)
    }
}
