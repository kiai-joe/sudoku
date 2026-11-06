package com.kiaijoe.sudoku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The board-change log behind the replay. */
class LogTest {

    private val start = TestGrids.game()
    private val empty = (0 until Grid.CELLS).first { start.values[it] == 0 }

    @Test
    fun placingAndErasingAreLogged() {
        val placed = start.copy(elapsedSeconds = 12)
            .place(empty, 4, mistakeLimit = null, autoRemoveNotes = true, checking = false)
            .logged(start)
        assertEquals(listOf(LogEntry(empty, 4, 12)), placed.log)
        val erased = placed.copy(elapsedSeconds = 20).erase(empty, checking = false).logged(placed)
        assertEquals(LogEntry(empty, 0, 20), erased.log.last())
        assertEquals(2, erased.log.size)
    }

    @Test
    fun notesAloneAreNotLogged() {
        val noted = start.toggleNote(empty, 3).logged(start)
        assertTrue(noted.log.isEmpty())
    }

    @Test
    fun hintsAreMarkedAndRestartClears() {
        val digit = start.solution[empty]
        val hinted = start.place(empty, digit, mistakeLimit = null, autoRemoveNotes = true, checking = false).logged(start, hint = true)
        assertTrue(hinted.log.single().hint)
        assertTrue(hinted.restart().log.isEmpty())
    }

    @Test
    fun theLogKeepsTheLatestChanges() {
        var game = start
        repeat(GameState.LOG_LIMIT + 20) { i ->
            val next = game.copy(elapsedSeconds = i.toLong()).place(empty, 1 + i % 9, mistakeLimit = null, autoRemoveNotes = true, checking = false)
            game = next.logged(game)
        }
        assertEquals(GameState.LOG_LIMIT, game.log.size)
        assertEquals((GameState.LOG_LIMIT + 19).toLong(), game.log.last().second)
    }

    @Test
    fun theLogIsSavedAndOlderSavesStillLoad() {
        val game = start.copy(log = listOf(LogEntry(empty, 4, 12), LogEntry(empty, 0, 30, hint = true)))
        assertEquals(game.log, GameCodec.decode(GameCodec.encode(game))!!.log)
        val old = GameCodec.encode(start).split("|").take(16).joinToString("|")
        assertTrue(GameCodec.decode(old)!!.log.isEmpty())
    }
}
