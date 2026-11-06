package com.kiaijoe.sudoku.ui.screens

import com.kiaijoe.sudoku.engine.Difficulty
import com.kiaijoe.sudoku.engine.GameState
import com.kiaijoe.sudoku.engine.Generator
import com.kiaijoe.sudoku.engine.LogEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class ReplayTest {

    private val start = GameState.from(Generator.generate(Difficulty.EASY, Random(3)))
    private val empty = (0 until 81).filter { start.values[it] == 0 }

    @Test
    fun theBoardFollowsTheLog() {
        val a = empty[0]
        val wrong = (1..9).first { it != start.solution[a] }
        val game = start.copy(log = listOf(LogEntry(a, wrong, 5), LogEntry(a, 0, 8), LogEntry(a, start.solution[a], 9)))
        assertEquals(start.givens, boardAt(game, 0))
        assertEquals(wrong, boardAt(game, 1)[a])
        assertEquals(0, boardAt(game, 2)[a])
        assertEquals(start.solution[a], boardAt(game, 3)[a])
    }

    @Test
    fun momentsAreMistakesHintsAndTheLongestPauses() {
        val (a, b, c, d, e) = empty
        val wrong = (1..9).first { it != start.solution[a] }
        val log = listOf(
            LogEntry(a, wrong, 10), // a mistake
            LogEntry(a, 0, 12), // erased: not a moment
            LogEntry(b, start.solution[b], 200), // after a 188-second pause
            LogEntry(c, start.solution[c], 210, hint = true), // a hint
            LogEntry(d, start.solution[d], 250), // after 40 seconds
            LogEntry(e, start.solution[e], 260),
        )
        val m = moments(start.copy(log = log))
        assertEquals(
            listOf(
                Moment(0, Moment.Kind.MISTAKE),
                Moment(2, Moment.Kind.PAUSE, 188),
                Moment(3, Moment.Kind.HINT),
                Moment(4, Moment.Kind.PAUSE, 40),
            ),
            m,
        )
    }
}
