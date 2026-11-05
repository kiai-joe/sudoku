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
            Difficulty.HARD, Difficulty.HARD, Difficulty.EXPERT,
        )
        for ((i, level) in expected.withIndex()) {
            val d = day("2026-11-02") + i
            assertEquals(level, Daily.difficultyFor(d))
            assertEquals(level, Daily.killerDifficultyFor(d))
        }
        assertEquals("Hard", Daily.label(day("2026-11-07")))
        assertEquals("Killer · Hard", Daily.label(day("2026-11-07"), killer = true))
    }

    @Test
    fun sameDayGivesSamePuzzleAndOtherDaysDiffer() {
        val today = day("2026-11-05")
        val a = Daily.puzzleFor(today)
        val b = Daily.puzzleFor(today)
        assertArrayEquals(a.givens, b.givens)
        assertFalse(a.givens.contentEquals(Daily.puzzleFor(today + 7).givens))
        val k = Daily.killerPuzzleFor(today)
        assertEquals(k.cages, Daily.killerPuzzleFor(today).cages)
        assertTrue(k.cages.isNotEmpty())
        assertFalse(k.cages == Daily.killerPuzzleFor(today + 7).cages)
    }

    @Test
    fun dailyPuzzlesAreGradedAtTheirLevel() {
        val monday = day("2026-11-02")
        for (offset in 0L until 7L) {
            val d = monday + offset
            val puzzle = Daily.puzzleFor(d)
            val grade = LogicSolver.grade(puzzle.givens, puzzle.cages)
            assertTrue("day $offset solvable", grade.solved)
            assertTrue("day $offset is classic", puzzle.cages.isEmpty())
            assertEquals("day $offset", Daily.difficultyFor(d), grade.level)

            val killer = Daily.killerPuzzleFor(d)
            val killerGrade = LogicSolver.grade(killer.givens, killer.cages)
            assertTrue("Killer day $offset solvable", killerGrade.solved)
            assertEquals(
                "Killer day $offset",
                Daily.killerDifficultyFor(d),
                Killer.levelOf(killerGrade.hardest, killer.givens.count { it != 0 }),
            )
        }
    }

    @Test
    fun gameRemembersItsDay() {
        val d = day("2026-11-05")
        val game = Daily.gameFor(d)
        assertEquals(d, game.daily)
        assertEquals(d, GameCodec.decode(GameCodec.encode(game))?.daily)
        assertNull(GameCodec.decode(GameCodec.encode(game.copy(daily = null)))?.daily)
        val killer = Daily.gameFor(d, killer = true)
        assertEquals(d, killer.daily)
        assertTrue(killer.isKiller)
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
        assertEquals(Difficulty.HARD, stats[5].difficulty)
        assertFalse(stats[5].killer)
        assertTrue(Daily.weekdayStats(results, killer = true).all { it.killer })
        assertEquals(900L, stats[6].averageSeconds)
        assertEquals(0, stats[2].solved)
        assertNull(stats[2].averageSeconds)
    }
}
