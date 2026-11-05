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

    // --- Added in the second set --------------------------------------------

    private fun earned(progress: Progress = Progress(), game: GameState? = null, mistakesShown: Boolean = true) =
        Achievements.earned(progress, game, mistakesShown)

    @Test
    fun lightningIsAFastClassicEasy() {
        assertTrue(Achievement.LIGHTNING in earned(game = won(Difficulty.EASY, seconds = 179)))
        assertFalse(Achievement.LIGHTNING in earned(game = won(Difficulty.EASY, seconds = 180)))
        assertFalse(Achievement.LIGHTNING in earned(game = won(Difficulty.EASY, seconds = 100, killer = true)))
        assertFalse(Achievement.LIGHTNING in earned(game = won(Difficulty.MEDIUM, seconds = 100)))
    }

    @Test
    fun ironWillAndBlindInkAreCleanExperts() {
        val clean = won(Difficulty.EXPERT)
        assertTrue(Achievement.IRON_WILL in earned(game = clean))
        assertFalse(Achievement.IRON_WILL in earned(game = clean, mistakesShown = false))
        assertFalse(Achievement.IRON_WILL in earned(game = won(Difficulty.EXPERT, mistakes = 1)))
        assertFalse(Achievement.IRON_WILL in earned(game = won(Difficulty.HARD)))

        assertTrue(Achievement.BLIND_INK in earned(game = clean, mistakesShown = false))
        assertFalse("not with mistakes shown", Achievement.BLIND_INK in earned(game = clean))
        assertFalse("not after a Check", Achievement.BLIND_INK in earned(game = clean.copy(checks = 1), mistakesShown = false))
        assertFalse("not with a hint", Achievement.BLIND_INK in earned(game = won(Difficulty.EXPERT, hints = 1), mistakesShown = false))
    }

    @Test
    fun gameMarksEarnTheirOwn() {
        assertTrue(Achievement.SWORDSMITH in earned(game = won(Difficulty.EXPERT).copy(hardest = Technique.SWORDFISH)))
        assertTrue(Achievement.SWORDSMITH in earned(game = won(Difficulty.EXPERT).copy(hardest = Technique.XYZ_WING)))
        assertFalse(Achievement.SWORDSMITH in earned(game = won(Difficulty.HARD).copy(hardest = Technique.X_WING)))
        assertTrue(Achievement.SECOND_WIND in earned(game = won(Difficulty.MEDIUM).copy(afterLoss = true)))
        assertFalse(Achievement.SECOND_WIND in earned(game = won(Difficulty.MEDIUM)))
        assertTrue(Achievement.PUT_TO_PRACTICE in earned(game = won(Difficulty.HARD).copy(practice = Technique.X_WING)))
        assertFalse(Achievement.PUT_TO_PRACTICE in earned(game = won(Difficulty.HARD)))
    }

    @Test
    fun restartingALostGameMarksIt() {
        val lost = TestGrids.game().copy(status = GameStatus.LOST, mistakes = 3)
        assertTrue(lost.restart().afterLoss)
        assertTrue("stays marked through another restart", lost.restart().restart().afterLoss)
        assertFalse(TestGrids.game().restart().afterLoss)
    }

    @Test
    fun checksPracticeAndRestartsAreSaved() {
        val game = TestGrids.game().copy(checks = 2, practice = Technique.X_WING, afterLoss = true)
        val back = GameCodec.decode(GameCodec.encode(game))!!
        assertEquals(2, back.checks)
        assertEquals(Technique.X_WING, back.practice)
        assertTrue(back.afterLoss)
        // A save from before these fields still loads, with none of them set.
        val old = GameCodec.encode(TestGrids.game()).split("|").take(13).joinToString("|")
        val oldBack = GameCodec.decode(old)!!
        assertEquals(0, oldBack.checks)
        assertEquals(null, oldBack.practice)
        assertFalse(oldBack.afterLoss)
    }

    @Test
    fun lessonsCount() {
        assertFalse(Achievement.FIRST_LESSON in earned())
        assertTrue(Achievement.FIRST_LESSON in earned(Progress(lessonsDone = 1)))
        assertFalse(Achievement.SCHOLAR in earned(Progress(lessonsDone = Tutorial.lessons.size - 1)))
        assertTrue(Achievement.SCHOLAR in earned(Progress(lessonsDone = Tutorial.lessons.size)))
    }

    @Test
    fun hundredStampsCountsEveryKindOfSolve() {
        val almost = Progress(
            won = mapOf(Difficulty.EASY to 40, Difficulty.HARD to 20),
            killerWon = 19,
            dailyDays = (1L..10L).toSet(),
            killerDailyDays = (101L..110L).toSet(),
        )
        assertFalse(Achievement.HUNDRED_STAMPS in earned(almost))
        assertTrue(Achievement.HUNDRED_STAMPS in earned(almost.copy(killerWon = 20)))
    }

    @Test
    fun dailyKillerAchievements() {
        val monday = java.time.LocalDate.parse("2026-11-02").toEpochDay()
        val week = (monday until monday + 7).toSet()
        val progress = Progress(killerDailyDays = week + (monday - 10 until monday - 7))
        assertTrue(Achievement.CAGED_WEEK in earned(progress))
        assertTrue(Achievement.KILLER_REGULAR in earned(progress))
        assertTrue("a Killer daily is a solve", Achievement.FIRST_STAMP in earned(progress))
        assertFalse(Achievement.TWO_BRUSHES in earned(progress))
        assertTrue(Achievement.TWO_BRUSHES in earned(progress.copy(dailyDays = setOf(monday + 3))))
        assertFalse(Achievement.CAGED_WEEK in earned(Progress(killerDailyDays = (monday until monday + 6).toSet())))
    }

    @Test
    fun fullWeekNeedsMondayToSunday() {
        val monday = java.time.LocalDate.parse("2026-11-02").toEpochDay()
        assertTrue(Achievement.FULL_WEEK in earned(Progress(dailyDays = (monday until monday + 7).toSet())))
        // Seven days in a row, but Wednesday to Tuesday, isn't one week.
        assertFalse(Achievement.FULL_WEEK in earned(Progress(dailyDays = (monday + 2 until monday + 9).toSet())))
    }
}
