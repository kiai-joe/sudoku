package com.kiaijoe.sudoku.data

import android.content.Context
import com.kiaijoe.sudoku.engine.Achievement
import com.kiaijoe.sudoku.engine.Achievements
import com.kiaijoe.sudoku.engine.Difficulty
import com.kiaijoe.sudoku.engine.Progress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Puzzles typed in by the player keep their own record on Progress and count towards achievements. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CustomStatsTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private lateinit var repo: GameRepository

    @Before
    fun setUp() {
        context.getSharedPreferences(GameRepository.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        repo = GameRepository(context)
    }

    @Test
    fun typedInPuzzlesKeepTheirOwnRecord() {
        repo.recordCustomStart()
        repo.recordCustomStart()
        repo.recordCustomWin(seconds = 420, hints = 0)

        val custom = repo.loadCustomStats()
        assertEquals(2, custom.played)
        assertEquals(1, custom.won)
        assertEquals(420L, custom.bestSeconds)
        assertEquals(100, custom.noHintRate)
        // Sumi's own levels are untouched.
        assertTrue(repo.loadLevelStats().values.all { it.played == 0 })
    }

    @Test
    fun resettingStatsClearsThem() {
        repo.recordCustomStart()
        repo.recordCustomWin(seconds = 420, hints = 0)
        repo.resetStats()
        assertEquals(DifficultyStats(), repo.loadCustomStats())
    }

    @Test
    fun theyCountTowardsAchievements() {
        val earned = Achievements.earned(Progress(customWon = 1))
        assertTrue(Achievement.FIRST_STAMP in earned)
        val hundred = Achievements.earned(Progress(won = mapOf(Difficulty.EASY to 60), customWon = 40))
        assertTrue(Achievement.HUNDRED_STAMPS in hundred)
    }
}
