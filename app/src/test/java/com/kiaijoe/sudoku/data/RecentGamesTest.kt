package com.kiaijoe.sudoku.data

import android.content.Context
import com.kiaijoe.sudoku.engine.Difficulty
import com.kiaijoe.sudoku.engine.GameState
import com.kiaijoe.sudoku.engine.GameStatus
import com.kiaijoe.sudoku.engine.Generator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.random.Random

/** The last two finished games are kept, newest first, for replaying from Progress. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecentGamesTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private lateinit var repo: GameRepository

    @Before
    fun setUp() {
        context.getSharedPreferences(GameRepository.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        repo = GameRepository(context)
    }

    private fun finished(seed: Int, status: GameStatus): GameState {
        val g = GameState.from(Generator.generate(Difficulty.EASY, Random(seed)))
        val cell = (0 until 81).first { g.values[it] == 0 }
        val placed = g.place(cell, g.solution[cell], mistakeLimit = null, autoRemoveNotes = true).logged(g)
        return placed.copy(status = status, elapsedSeconds = 100L + seed)
    }

    @Test
    fun keepsTheLastTwoNewestFirst() {
        assertTrue(repo.loadRecentGames().isEmpty())
        val a = finished(1, GameStatus.WON)
        val b = finished(2, GameStatus.LOST)
        val c = finished(3, GameStatus.WON)
        repo.recordFinished(a)
        assertEquals(listOf(a), repo.loadRecentGames())
        repo.recordFinished(b)
        repo.recordFinished(c)
        val recent = repo.loadRecentGames()
        assertEquals(listOf(c, b), recent)
        assertEquals(GameStatus.LOST, recent[1].status)
        assertEquals(1, recent[1].log.size)
    }

    @Test
    fun resettingStatsKeepsThem() {
        repo.recordFinished(finished(4, GameStatus.WON))
        repo.resetStats()
        assertEquals(1, repo.loadRecentGames().size)
    }
}
