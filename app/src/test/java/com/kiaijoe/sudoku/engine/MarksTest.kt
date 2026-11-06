package com.kiaijoe.sudoku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Colour marks, the board's candidates for the highlighted number, and hint nudges. */
class MarksTest {

    private val game = TestGrids.game()

    @Test
    fun marksCycleAndAreSaved() {
        val once = game.cycleMark(10)
        assertEquals(1, once.mark(10))
        assertEquals(2, once.cycleMark(10).mark(10))
        val cleared = once.cycleMark(10).cycleMark(10)
        assertEquals(0, cleared.mark(10))
        assertTrue("no marks left means none stored", cleared.marks.isEmpty())
        val marked = once.cycleMark(20).cycleMark(20)
        val back = GameCodec.decode(GameCodec.encode(marked))!!
        assertEquals(1, back.mark(10))
        assertEquals(2, back.mark(20))
        // A save from before marks still loads, with none.
        val old = GameCodec.encode(game).split("|").take(17).joinToString("|")
        assertTrue(GameCodec.decode(old)!!.marks.isEmpty())
    }

    @Test
    fun boardCandidatesLeaveOutPeersNumbers() {
        val cands = game.boardCandidates()
        for (cell in 0 until Grid.CELLS) {
            if (game.values[cell] != 0) {
                assertEquals(0, cands[cell])
                continue
            }
            // The solution's number is always still possible, and no peer's number is.
            assertTrue(cands[cell] and GameState.bit(game.solution[cell]) != 0)
            for (peer in Grid.peers[cell]) {
                val v = game.values[peer]
                if (v != 0) assertEquals(0, cands[cell] and GameState.bit(v))
            }
        }
    }

    @Test
    fun aNudgeSaysWhereWithoutSayingWhat() {
        val plan = Hints.forGame(game) as HintPlan.Teach
        for (card in plan.cards) {
            val nudge = Hints.nudge(card)
            assertTrue(nudge.text.startsWith("Look at "))
            assertTrue("the nudge lights up somewhere", nudge.cells.isNotEmpty())
            // Where to look, but not what's there: no technique name and no "is 5".
            card.technique?.let { assertTrue(nudge.text, !nudge.text.contains(it.label, ignoreCase = true)) }
            assertTrue(nudge.text, !nudge.text.contains(" is "))
        }
    }
}
