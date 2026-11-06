package com.kiaijoe.sudoku.data

import android.content.Context
import com.kiaijoe.sudoku.engine.Daily
import com.kiaijoe.sudoku.engine.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Daily puzzles count on Progress under their level, and only once; achievements still count them apart. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DailyStatsTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val prefs get() = context.getSharedPreferences(GameRepository.PREFS_NAME, Context.MODE_PRIVATE)
    private lateinit var repo: GameRepository

    // Friday 6 November 2026, a Hard day, and the Monday before it, an Easy one.
    private val friday = LocalDate.parse("2026-11-06").toEpochDay()
    private val monday = LocalDate.parse("2026-11-02").toEpochDay()

    @Before
    fun setUp() {
        prefs.edit().clear().commit()
        repo = GameRepository(context)
    }

    @Test
    fun aDailyWinCountsUnderItsLevel() {
        assertEquals(Difficulty.HARD, Daily.difficultyFor(friday))
        repo.countPastDailies()
        repo.recordDailyStart(friday)
        repo.recordDailyWin(friday, seconds = 612, hints = 0)

        val hard = repo.loadLevelStats().getValue(Difficulty.HARD)
        assertEquals(1, hard.played)
        assertEquals(1, hard.won)
        assertEquals(612L, hard.bestSeconds)
        assertEquals(612L, hard.averageSeconds)
        assertEquals(100, hard.noHintRate)
        // Regular games alone, which achievements count from, are untouched.
        assertEquals(0, repo.loadStats().getValue(Difficulty.HARD).won)
    }

    @Test
    fun regularAndDailyGamesAddUp() {
        repo.recordStart(Difficulty.HARD)
        repo.recordWin(Difficulty.HARD, seconds = 700, hints = 2)
        repo.recordDailyStart(friday)
        repo.recordDailyWin(friday, seconds = 500, hints = 1)

        val hard = repo.loadLevelStats().getValue(Difficulty.HARD)
        assertEquals(2, hard.played)
        assertEquals(2, hard.won)
        assertEquals(500L, hard.bestSeconds)
        assertEquals(600L, hard.averageSeconds)
        assertEquals(0, hard.noHintRate)
    }

    @Test
    fun solvingTheSameDayAgainCountsOnce() {
        repo.recordDailyWin(friday, seconds = 612)
        repo.recordDailyWin(friday, seconds = 400)
        assertEquals(1, repo.loadLevelStats().getValue(Difficulty.HARD).won)
    }

    @Test
    fun dailyKillersCountUnderKiller() {
        repo.recordDailyStart(friday, killer = true)
        repo.recordDailyWin(friday, seconds = 900, killer = true)
        assertEquals(1, repo.loadKillerLevelStats().won)
        assertEquals(0, repo.loadKillerStats().won)
        assertEquals(0, repo.loadLevelStats().getValue(Difficulty.HARD).won)
    }

    @Test
    fun pastDailiesAreCountedOnce() {
        // Saved by a version that kept daily wins only in the calendar.
        prefs.edit().putStringSet("daily_results", setOf("$friday:640", "$monday:200")).commit()

        repo.countPastDailies()
        repo.countPastDailies()

        val stats = repo.loadLevelStats()
        assertEquals(1, stats.getValue(Difficulty.HARD).won)
        assertEquals(640L, stats.getValue(Difficulty.HARD).bestSeconds)
        assertEquals(1, stats.getValue(Difficulty.EASY).won)
        // Their hints weren't kept, so they stay out of the average.
        assertNull(stats.getValue(Difficulty.HARD).averageSeconds)
    }

    @Test
    fun resettingStatsDoesNotBringPastDailiesBack() {
        prefs.edit().putStringSet("daily_results", setOf("$friday:640")).commit()
        repo.countPastDailies()
        repo.resetStats()
        repo.countPastDailies()
        assertEquals(0, repo.loadLevelStats().getValue(Difficulty.HARD).won)
    }
}
