package com.kiaijoe.sudoku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementsTest {

    private fun won(level: Difficulty, seconds: Long = 900, mistakes: Int = 0, hints: Int = 0, killer: Boolean = false) =
        TestGrids.game().copy(
            difficulty = level,
            status = GameStatus.WON,
            elapsedSeconds = seconds,
            mistakes = mistakes,
            hintsUsed = hints,
            cages = if (killer) listOf(Cage(45, (0 until 9).toList())) else emptyList(),
        )

    @Test
    fun nothingEarnedAtTheStart() {
        assertTrue(Achievements.earned(Progress()).isEmpty())
    }

    @Test
    fun firstWinEarnsTheFirstStamp() {
        val earned = Achievements.earned(Progress(), won(Difficulty.EASY, hints = 1, mistakes = 1))
        assertEquals(setOf(Achievement.FIRST_STAMP), earned)
    }

    @Test
    fun cleanInkNeedsMistakesShown() {
        val game = won(Difficulty.EASY)
        assertTrue(Achievement.CLEAN_INK in Achievements.earned(Progress(), game, mistakesShown = true))
        assertFalse(Achievement.CLEAN_INK in Achievements.earned(Progress(), game, mistakesShown = false))
    }

    @Test
    fun hintsAndTimeMatterFromMediumUp() {
        assertFalse(Achievement.ON_YOUR_OWN in Achievements.earned(Progress(), won(Difficulty.EASY)))
        assertTrue(Achievement.ON_YOUR_OWN in Achievements.earned(Progress(), won(Difficulty.MEDIUM)))
        assertFalse(Achievement.ON_YOUR_OWN in Achievements.earned(Progress(), won(Difficulty.HARD, hints = 2)))
        assertTrue(Achievement.SWIFT_BRUSH in Achievements.earned(Progress(), won(Difficulty.MEDIUM, seconds = 470)))
        assertFalse(Achievement.SWIFT_BRUSH in Achievements.earned(Progress(), won(Difficulty.MEDIUM, seconds = 480)))
    }

    @Test
    fun killerHasItsOwn() {
        val killerExpert = Achievements.earned(Progress(), won(Difficulty.EXPERT, killer = true))
        assertTrue(Achievement.CAGE_BREAKER in killerExpert)
        assertTrue(Achievement.BLANK_PAGE in killerExpert)
        assertFalse("Killer levels aren't classic levels", Achievement.MASTER in killerExpert)
    }

    @Test
    fun theLadderCountsSavedWinsAndThisOne() {
        val before = Progress(won = mapOf(Difficulty.EASY to 3, Difficulty.MEDIUM to 1, Difficulty.HARD to 2))
        assertFalse(Achievement.FULL_LADDER in Achievements.earned(before))
        assertTrue(Achievement.STEADY_HAND in Achievements.earned(before))
        val after = Achievements.earned(before, won(Difficulty.EXPERT))
        assertTrue(Achievement.FULL_LADDER in after)
        assertTrue(Achievement.MASTER in after)
    }

    @Test
    fun dailyMilestones() {
        val earned = Achievements.earned(Progress(dailySolved = 12, longestStreak = 7))
        assertTrue(Achievement.REGULAR in earned)
        assertTrue(Achievement.WEEK_OF_INK in earned)
        assertFalse(Achievement.MOON_CYCLE in earned)
        assertTrue(Achievement.MOON_CYCLE in Achievements.earned(Progress(longestStreak = 30)))
    }
}
