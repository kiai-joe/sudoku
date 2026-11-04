package com.kiaijoe.sudoku.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateTest {

    // Cell 2 (row 0, col 2) is empty in the test puzzle; its answer is 4.
    private val empty = 2
    private val answer = 4

    @Test
    fun correctPlacementLocksCell() {
        val g = TestGrids.game().place(empty, answer, 3, true)
        assertTrue(g.isCorrect(empty))
        assertEquals(0, g.mistakes)
        assertSame(g, g.place(empty, 1, 3, true))
        assertSame(g, g.erase(empty))
    }

    @Test
    fun wrongPlacementCountsMistakeAndCanBeErased() {
        val g = TestGrids.game().place(empty, 1, 3, true)
        assertTrue(g.isWrong(empty))
        assertEquals(1, g.mistakes)
        assertEquals(0, g.erase(empty).values[empty])
    }

    @Test
    fun reachingMistakeLimitLosesGame() {
        var g = TestGrids.game()
        g = g.place(empty, 1, 3, true)
        g = g.place(empty, 2, 3, true)
        assertEquals(GameStatus.PLAYING, g.status)
        g = g.place(empty, 3, 3, true)
        assertEquals(GameStatus.LOST, g.status)
        assertSame(g, g.place(empty, answer, 3, true))
    }

    @Test
    fun noLimitMeansNoLoss() {
        var g = TestGrids.game()
        repeat(5) { g = g.place(empty, if (it % 2 == 0) 1 else 2, null, true) }
        assertEquals(GameStatus.PLAYING, g.status)
        assertEquals(5, g.mistakes)
    }

    @Test
    fun givensCannotChange() {
        val g = TestGrids.game()
        assertSame(g, g.place(0, 9, 3, true))
        assertSame(g, g.toggleNote(0, 1))
    }

    @Test
    fun correctPlacementClearsMatchingNotesInPeers() {
        val peer = 3 // same row as cell 2, empty in the puzzle
        var g = TestGrids.game().toggleNote(peer, answer).toggleNote(peer, 6)
        assertTrue(g.hasNote(peer, answer))
        g = g.place(empty, answer, 3, true)
        assertFalse(g.hasNote(peer, answer))
        assertTrue(g.hasNote(peer, 6))
    }

    @Test
    fun autoRemoveNotesCanBeOff() {
        val peer = 3
        val g = TestGrids.game().toggleNote(peer, answer).place(empty, answer, 3, false)
        assertTrue(g.hasNote(peer, answer))
    }

    @Test
    fun fillingEverythingWins() {
        var g = TestGrids.game()
        for (cell in 0 until 81) if (!g.isGiven(cell)) g = g.place(cell, TestGrids.SOLUTION[cell], 3, true)
        assertEquals(GameStatus.WON, g.status)
        for (d in 1..9) assertEquals(0, g.remaining(d))
    }

    @Test
    fun restartClearsProgress() {
        val g = TestGrids.game().place(empty, 1, 3, true).copy(elapsedSeconds = 90).restart()
        assertEquals(g.givens, g.values)
        assertEquals(0, g.mistakes)
        assertEquals(0L, g.elapsedSeconds)
    }

    @Test
    fun codecRoundTrip() {
        val g = TestGrids.game()
            .place(empty, 1, 3, true)
            .toggleNote(3, 2)
            .copy(hintsUsed = 1)
            .copy(elapsedSeconds = 1234)
        assertEquals(g, GameCodec.decode(GameCodec.encode(g)))
    }

    @Test
    fun codecRejectsGarbage() {
        assertNull(GameCodec.decode(""))
        assertNull(GameCodec.decode("v1|EASY|123"))
        assertNull(GameCodec.decode(GameCodec.encode(TestGrids.game()).replace("v1", "v9")))
    }

    @Test
    fun codecKeepsHardestTechniqueAndReadsOlderSaves() {
        val g = GameState.from(Puzzle(TestGrids.PUZZLE, TestGrids.SOLUTION, Difficulty.HARD, Technique.X_WING))
        val text = GameCodec.encode(g)
        assertEquals(Technique.X_WING, GameCodec.decode(text)?.hardest)

        val older = text.split("|").take(10).joinToString("|")
        val decoded = GameCodec.decode(older)
        assertEquals(null, decoded?.hardest)
        assertEquals(g.copy(hardest = null), decoded)
    }

    @Test
    fun withoutCheckingNothingGivesTheAnswerAway() {
        val g = TestGrids.game()
        val cell = empty
        val right = TestGrids.SOLUTION[cell]
        val wrong = (1..9).first { it != right }

        val placedWrong = g.place(cell, wrong, mistakeLimit = 1, autoRemoveNotes = true, checking = false)
        assertEquals("no mistake counted", 0, placedWrong.mistakes)
        assertEquals("not lost", GameStatus.PLAYING, placedWrong.status)

        val placedRight = placedWrong.place(cell, right, mistakeLimit = 1, autoRemoveNotes = true, checking = false)
        val changedAgain = placedRight.place(cell, wrong, mistakeLimit = 1, autoRemoveNotes = true, checking = false)
        assertEquals("a correct cell can still be changed", wrong, changedAgain.values[cell])
        assertEquals("and erased", 0, placedRight.erase(cell, checking = false).values[cell])

        // The number pad counts every placed copy, right or wrong.
        val before = g.remaining(wrong, checking = false)
        assertEquals(before - 1, placedWrong.remaining(wrong, checking = false))
    }

    @Test
    fun withCheckingCorrectCellsStayLocked() {
        val g = TestGrids.game().place(empty, TestGrids.SOLUTION[empty], 3, true)
        val other = (1..9).first { it != TestGrids.SOLUTION[empty] }
        assertSame(g, g.place(empty, other, 3, true))
        assertSame(g, g.erase(empty))
    }

    @Test
    fun allCandidatesUseOnlyWhatIsOnTheBoard() {
        val g = TestGrids.game().withAllCandidates()
        for (cell in 0 until 81) {
            if (g.values[cell] != 0) {
                assertEquals(0, g.notes[cell])
                continue
            }
            for (d in 1..9) {
                val seen = Grid.peers[cell].any { g.values[it] == d }
                assertEquals("cell $cell digit $d", !seen, g.hasNote(cell, d))
            }
            assertTrue("the answer is always a candidate", g.hasNote(cell, TestGrids.SOLUTION[cell]))
        }
    }

    @Test
    fun completingARowColumnOrBoxIsReported() {
        var g = TestGrids.game()
        val row0 = House.ROWS[0].cells.filter { !g.isGiven(it) }
        for (cell in row0.dropLast(1)) g = g.place(cell, TestGrids.SOLUTION[cell], 3, true)
        val last = row0.last()
        assertTrue(House.ROWS[0] !in g.completedHouses(last))
        g = g.place(last, TestGrids.SOLUTION[last], 3, true)
        assertTrue(House.ROWS[0] in g.completedHouses(last))
    }
}
